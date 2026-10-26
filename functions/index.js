const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

Object.assign(exports, require("./notifications"));
Object.assign(exports, require("./cloudinary"));
Object.assign(exports, require("./cloudinaryCleanup"));

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
    const userSnap = await tx.get(userRef);
    const existingCoupleId = userSnap.exists ? userSnap.data().coupleId : null;

    let coupleRef;
    let coupleSnap = null;

    if (existingCoupleId) {
      coupleRef = db.collection("couples").doc(existingCoupleId);
      coupleSnap = await tx.get(coupleRef);
    } else {
      coupleRef = db.collection("couples").doc();
    }

    const code = generateCode();
    const inviteCodeRef = db.collection("inviteCodes").doc(code);
    const inviteSnap = await tx.get(inviteCodeRef);

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
      throw new HttpsError(
        "aborted",
        "No se pudo generar un codigo unico, intenta de nuevo."
      );
    }

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

    const activeInvitesSnap = await tx.get(
      db.collection("inviteCodes")
        .where("coupleId", "==", coupleId)
        .where("used", "==", false)
    );

    tx.delete(coupleRef);
    activeInvitesSnap.forEach((doc) => tx.delete(doc.ref));
    tx.set(userRef, { coupleId: FieldValue.delete() }, { merge: true });
  });

  return { success: true };
});

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
      throw new HttpsError(
        "failed-precondition",
        "Todavia no tienes pareja vinculada; usa la opcion de cancelar invitacion."
      );
    }

    const partnerId = couple.user1Id === uid ? couple.user2Id : couple.user1Id;
    const partnerRef = db.collection("users").doc(partnerId);

    const activeInvitesSnap = await tx.get(
      db.collection("inviteCodes")
        .where("coupleId", "==", coupleId)
        .where("used", "==", false)
    );

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