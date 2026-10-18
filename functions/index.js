const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

// Alfabeto sin caracteres ambiguos (sin 0/O, 1/I) tal como quedo pendiente
// en la documentacion del proyecto.
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 6;
const CODE_TTL_HOURS = 48;

function generateCode() {
  let code = "";
  for (let i = 0; i < CODE_LENGTH; i++) {
    const idx = Math.floor(Math.random() * CODE_ALPHABET.length);
    code += CODE_ALPHABET[idx];
  }
  return code;
}

/**
 * createInviteCode
 *
 * El usuario 1 llama a esta funcion para empezar el flujo de "invitar".
 * Si el usuario ya tiene una pareja creada sin user2Id (invitacion pendiente),
 * reutiliza esa couple y genera un nuevo codigo para ella. Si no, crea la
 * couple y el codigo desde cero.
 *
 * Corre con Admin SDK para poder escribir en couples/{coupleId} e
 * inviteCodes/{code}, colecciones que el cliente no puede escribir
 * directamente segun las reglas de Firestore.
 *
 * IMPORTANTE (fix 2026-09-07): Firestore exige que TODAS las lecturas de
 * una transaccion ocurran antes que cualquier escritura
 * ("Firestore transactions require all reads to be executed before all
 * writes"). La version anterior escribia coupleRef/userRef y despues leia
 * inviteCodeRef para revisar colisiones, lo cual violaba esa regla y
 * tronaba con un error no manejado (visible como INTERNAL en el cliente).
 * Ahora se generan y leen todos los datos necesarios primero, y las
 * escrituras van al final, sin ninguna lectura intercalada.
 */
exports.createInviteCode = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError(
      "unauthenticated",
      "Debes iniciar sesion para invitar a alguien."
    );
  }

  const userRef = db.collection("users").doc(uid);

  const result = await db.runTransaction(async (tx) => {
    // --- FASE 1: SOLO LECTURAS ---

    const userSnap = await tx.get(userRef);
    const existingCoupleId = userSnap.exists ? userSnap.data().coupleId : null;

    let coupleRef;
    let coupleSnap = null;

    if (existingCoupleId) {
      coupleRef = db.collection("couples").doc(existingCoupleId);
      coupleSnap = await tx.get(coupleRef);
    } else {
      // Doc nuevo: no existe todavia, no hace falta leerlo.
      coupleRef = db.collection("couples").doc();
    }

    // Generamos el codigo y lo leemos ANTES de escribir nada. La
    // probabilidad de colision con CODE_LENGTH=6 y este alfabeto es
    // minima, pero de igual forma se revisa que no exista ya.
    const code = generateCode();
    const inviteCodeRef = db.collection("inviteCodes").doc(code);
    const inviteSnap = await tx.get(inviteCodeRef);

    // --- Validaciones (no son I/O, pueden ir despues de las lecturas) ---

    if (existingCoupleId) {
      if (!coupleSnap.exists) {
        throw new HttpsError(
          "failed-precondition",
          "Tu pareja registrada ya no existe."
        );
      }

      const couple = coupleSnap.data();

      if (couple.user2Id) {
        throw new HttpsError(
          "failed-precondition",
          "Ya estas vinculado con alguien, no puedes generar un nuevo codigo."
        );
      }

      if (couple.user1Id !== uid) {
        throw new HttpsError(
          "failed-precondition",
          "Solo quien creo la invitacion puede generar un nuevo codigo."
        );
      }
    }

    if (inviteSnap.exists) {
      // Colision improbable: quien llama puede simplemente reintentar.
      throw new HttpsError(
        "aborted",
        "No se pudo generar un codigo unico, intenta de nuevo."
      );
    }

    // --- FASE 2: SOLO ESCRITURAS (ninguna lectura despues de este punto) ---

    if (!existingCoupleId) {
      tx.set(coupleRef, {
        user1Id: uid,
        user2Id: "",
        timezone: "America/Bogota",
        createdAt: FieldValue.serverTimestamp(),
      });
      tx.set(userRef, { coupleId: coupleRef.id }, { merge: true });
    }

    const expiresAt = new Date(
      Date.now() + CODE_TTL_HOURS * 60 * 60 * 1000
    );

    tx.set(inviteCodeRef, {
      coupleId: coupleRef.id,
      createdBy: uid,
      createdAt: FieldValue.serverTimestamp(),
      expiresAt,
      used: false,
    });

    return { code, coupleId: coupleRef.id, expiresAt: expiresAt.toISOString() };
  });

  return { success: true, ...result };
});

/**
 * redeemInviteCode
 *
 * El usuario 2 llama a esta funcion con el codigo de invitacion que recibio.
 * La funcion valida el codigo, lo vincula al usuario que llama, y actualiza
 * tanto el documento couples/{coupleId} como users/{callerUid}.
 *
 * Toda la operacion corre con el Admin SDK (se salta las reglas de Firestore)
 * y dentro de una transaccion para evitar condiciones de carrera si dos
 * personas intentan usar el mismo codigo al mismo tiempo.
 */
exports.redeemInviteCode = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError(
      "unauthenticated",
      "Debes iniciar sesion para usar un codigo de invitacion."
    );
  }

  const rawCode = request.data?.code;
  if (typeof rawCode !== "string" || rawCode.trim().length === 0) {
    throw new HttpsError("invalid-argument", "Debes enviar un codigo valido.");
  }

  // Normalizamos: mayusculas y sin espacios, tal como se generan los codigos.
  const code = rawCode.trim().toUpperCase();

  const inviteCodeRef = db.collection("inviteCodes").doc(code);
  const userRef = db.collection("users").doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const [inviteSnap, userSnap] = await Promise.all([
      tx.get(inviteCodeRef),
      tx.get(userRef),
    ]);

    if (!inviteSnap.exists) {
      throw new HttpsError("not-found", "Ese codigo no existe.");
    }

    const invite = inviteSnap.data();

    if (invite.used) {
      throw new HttpsError(
        "failed-precondition",
        "Ese codigo ya fue utilizado."
      );
    }

    const now = new Date();
    const expiresAt = invite.expiresAt?.toDate?.() ?? null;
    if (expiresAt && now > expiresAt) {
      throw new HttpsError("failed-precondition", "Ese codigo ya expiro.");
    }

    if (invite.createdBy === uid) {
      throw new HttpsError(
        "failed-precondition",
        "No puedes usar tu propio codigo de invitacion."
      );
    }

    const existingCoupleId = userSnap.exists ? userSnap.data().coupleId : null;
    if (existingCoupleId) {
      throw new HttpsError(
        "failed-precondition",
        "Ya estas vinculado a otra pareja."
      );
    }

    const coupleRef = db.collection("couples").doc(invite.coupleId);
    const coupleSnap = await tx.get(coupleRef);

    if (!coupleSnap.exists) {
      throw new HttpsError(
        "failed-precondition",
        "La pareja asociada a este codigo ya no existe."
      );
    }

    const couple = coupleSnap.data();
    if (couple.user2Id) {
      throw new HttpsError(
        "failed-precondition",
        "Esa pareja ya tiene dos personas vinculadas."
      );
    }

    if (couple.user1Id === uid) {
      throw new HttpsError(
        "failed-precondition",
        "No puedes vincularte a tu propia pareja."
      );
    }

    // Todo valido: aplicamos los tres cambios de forma atomica.
    // linkedAt marca el momento exacto del canje (no el de la invitacion),
    // que es lo que "tiempo de vinculo" necesita mostrar en Perfil. Las
    // parejas creadas antes de este cambio no tendran linkedAt; el cliente
    // usa createdAt como respaldo en ese caso (ver ProfileRepository).
    tx.update(coupleRef, {
      user2Id: uid,
      linkedAt: FieldValue.serverTimestamp(),
    });
    tx.set(userRef, { coupleId: invite.coupleId }, { merge: true });
    tx.update(inviteCodeRef, {
      used: true,
      usedBy: uid,
      usedAt: FieldValue.serverTimestamp(),
    });

    return { coupleId: invite.coupleId };
  });

  return { success: true, coupleId: result.coupleId };
});

/**
 * cancelPendingInvite
 *
 * Permite al usuario que genero una invitacion cancelarla, PERO SOLO si
 * todavia no se vinculo nadie (couple.user2Id == ""). Si ya hay una
 * segunda persona vinculada, esta funcion se niega a desvincular: ese
 * caso lo resuelve unlinkCouple (ver mas abajo).
 *
 * Efectos al cancelar:
 * - Borra el documento couples/{coupleId} (nunca tuvo user2, no hay nada
 *   compartido que perder).
 * - Borra cualquier inviteCodes/{code} activo (no usado) que apunte a esa
 *   couple, para que un codigo viejo no pueda canjearse despues de
 *   cancelado.
 * - Limpia coupleId en users/{uid} (set con merge, campo removido) para
 *   que el usuario pueda volver a elegir "invitar" o "tengo un codigo"
 *   desde cero.
 *
 * Corre con Admin SDK y usa una transaccion respetando el orden
 * lecturas-antes-que-escrituras.
 */
exports.cancelPendingInvite = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError(
      "unauthenticated",
      "Debes iniciar sesion para hacer esto."
    );
  }

  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    // --- FASE 1: SOLO LECTURAS ---

    const userSnap = await tx.get(userRef);
    const coupleId = userSnap.exists ? userSnap.data().coupleId : null;

    if (!coupleId) {
      throw new HttpsError(
        "failed-precondition",
        "No tienes ninguna invitacion pendiente."
      );
    }

    const coupleRef = db.collection("couples").doc(coupleId);
    const coupleSnap = await tx.get(coupleRef);

    if (!coupleSnap.exists) {
      // Ya no existe por alguna razon: igual limpiamos el coupleId huerfano.
      tx.set(userRef, { coupleId: FieldValue.delete() }, { merge: true });
      return;
    }

    const couple = coupleSnap.data();

    if (couple.user1Id !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Solo quien creo la invitacion puede cancelarla."
      );
    }

    if (couple.user2Id) {
      throw new HttpsError(
        "failed-precondition",
        "Ya estas vinculado con alguien, no se puede cancelar desde aqui."
      );
    }

    // Buscamos codigos activos (no usados) que apunten a esta couple,
    // para invalidarlos junto con la couple. where() dentro de una
    // transaccion cuenta como lectura, asi que va en esta fase.
    const activeInvitesSnap = await tx.get(
      db.collection("inviteCodes")
        .where("coupleId", "==", coupleId)
        .where("used", "==", false)
    );

    // --- FASE 2: SOLO ESCRITURAS ---

    tx.delete(coupleRef);
    activeInvitesSnap.forEach((doc) => tx.delete(doc.ref));
    tx.set(userRef, { coupleId: FieldValue.delete() }, { merge: true });
  });

  return { success: true };
});

/**
 * unlinkCouple
 *
 * Resuelve el caso que cancelPendingInvite deja explicitamente fuera de
 * alcance: desvincular una pareja YA completa (con ambos user1Id y
 * user2Id), donde ya puede existir contenido compartido (mensajes,
 * memories, drawings).
 *
 * Decision de producto (2026-09-20): no se archiva nada. La racha y el
 * tiempo de vinculo se resetean sin dejar registro; el contenido normal
 * (messages, memories, drawings, albums) se deja intacto igual que antes,
 * pero streakLog y linkedAt vuelven a su estado inicial. Si mas adelante
 * se decide guardar un historial para desvinculaciones, se reintroduce
 * aqui.
 * - couples/{coupleId} resetea streakLog a {} y borra linkedAt.
 * - coupleId se limpia (FieldValue.delete) en users/{uid} de AMBOS
 *   integrantes, no solo de quien llama, para que los dos puedan volver
 *   a pasar por el onboarding de invitar/ingresar codigo desde cero.
 * - Cualquier inviteCodes/{code} activo (no usado) que apunte a esa
 *   couple se borra, igual que en cancelPendingInvite.
 *
 * Cualquiera de los dos integrantes puede llamar a esta funcion (a
 * diferencia de cancelPendingInvite, que solo permite a quien creo la
 * invitacion cancelarla) porque desvincularse es una decision que
 * cualquiera de los dos deberia poder tomar por su cuenta.
 *
 * Corre con Admin SDK y respeta el orden lecturas-antes-que-escrituras.
 */
exports.unlinkCouple = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError(
      "unauthenticated",
      "Debes iniciar sesion para hacer esto."
    );
  }

  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    // --- FASE 1: SOLO LECTURAS ---

    const userSnap = await tx.get(userRef);
    const coupleId = userSnap.exists ? userSnap.data().coupleId : null;

    if (!coupleId) {
      throw new HttpsError(
        "failed-precondition",
        "No estas vinculado con nadie."
      );
    }

    const coupleRef = db.collection("couples").doc(coupleId);
    const coupleSnap = await tx.get(coupleRef);

    if (!coupleSnap.exists) {
      // Ya no existe por alguna razon: igual limpiamos el coupleId huerfano
      // del lado de quien llama (no podemos saber quien era el otro sin
      // el documento couple).
      tx.set(userRef, { coupleId: FieldValue.delete() }, { merge: true });
      return;
    }

    const couple = coupleSnap.data();

    if (couple.user1Id !== uid && couple.user2Id !== uid) {
      throw new HttpsError(
        "permission-denied",
        "No perteneces a esta pareja."
      );
    }

    if (!couple.user2Id) {
      // Invitacion sin completar todavia: no es el caso de esta funcion,
      // se usa cancelPendingInvite para ese escenario.
      throw new HttpsError(
        "failed-precondition",
        "Todavia no tienes pareja vinculada; usa la opcion de cancelar invitacion."
      );
    }

    const partnerId = couple.user1Id === uid ? couple.user2Id : couple.user1Id;
    const partnerRef = db.collection("users").doc(partnerId);

    // Buscamos codigos activos (no usados) que apunten a esta couple,
    // para invalidarlos junto con el archivado. where() dentro de una
    // transaccion cuenta como lectura, asi que va en esta fase.
    const activeInvitesSnap = await tx.get(
      db.collection("inviteCodes")
        .where("coupleId", "==", coupleId)
        .where("used", "==", false)
    );

    // --- FASE 2: SOLO ESCRITURAS ---

    tx.update(coupleRef, {
      streakLog: {},
      linkedAt: FieldValue.delete(),
    });
    activeInvitesSnap.forEach((doc) => tx.delete(doc.ref));
    tx.set(userRef, { coupleId: FieldValue.delete() }, { merge: true });
    tx.set(partnerRef, { coupleId: FieldValue.delete() }, { merge: true });
  });

  return { success: true };
});