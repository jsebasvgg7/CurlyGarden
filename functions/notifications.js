const {
  onDocumentCreated,
  onDocumentUpdated,
  onDocumentWritten,
} = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

const db = getFirestore();

const REGION = "us-central1";
const FALLBACK_NAME = "El otro";
const SLOTS = ["user1", "user2"];
const DEFAULT_TIMEZONE = "America/Bogota";
const CAPSULE_LOOKBACK_MS = 3 * 24 * 60 * 60 * 1000;
const CAPSULE_NOTIFY_FROM_HOUR = 8;

const MODULE_LABELS = {
  talk: "Hablar",
  draw: "Dibujar",
  play: "Jugar",
  imagine: "Imaginar",
  memories: "Recuerdos",
};

const STALE_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

const KINDS = {
  talk_message: {
    title: "Una carta nueva",
    module: "talk",
    inbox: true,
    push: true,
    text: (name) => `${name} te dejó una carta`,
  },
  draw_topic_received: {
    title: "Te toca elegir un tema",
    module: "draw",
    inbox: false,
    push: true,
    text: (name) => `${name} ya envió su tema. Te toca enviar el tuyo`,
  },
  draw_ready_to_draw: {
    title: "¡A dibujar!",
    module: "draw",
    inbox: false,
    push: true,
    text: () => "Ya pueden dibujar",
  },
  draw_revealed: {
    title: "Dibujos revelados",
    module: "draw",
    inbox: true,
    push: true,
    text: () => "Ya pueden ver los dibujos",
  },
  draw_next_round: {
    title: "Otra ronda de dibujo",
    module: "draw",
    inbox: false,
    push: false,
    text: (name) => `${name} quiere dibujar de nuevo`,
  },
  play_daily_answered: {
    title: "Reto del día",
    module: "play",
    inbox: false,
    push: true,
    text: (name) => `${name} ya respondió el reto de hoy`,
  },
  play_session_new: {
    title: "Nueva partida",
    module: "play",
    inbox: false,
    push: true,
    text: (name) => `${name} empezó una partida`,
  },
  play_guess_turn: {
    title: "¿Cuánto te conoce?",
    module: "play",
    inbox: false,
    push: true,
    text: () => "Ya puedes adivinar",
  },
  play_result: {
    title: "Resultado listo",
    module: "play",
    inbox: true,
    push: true,
    text: () => "Ya hay resultado",
  },
  imagine_new: {
    title: "Algo nuevo en Imaginar",
    module: "imagine",
    inbox: true,
    push: true,
    text: (name) => `${name} añadió algo en Imaginar`,
  },
  imagine_capsule_pending: {
    title: "Cápsula del tiempo",
    module: "imagine",
    inbox: false,
    push: true,
    text: () => "Falta tu parte en una cápsula",
  },
  imagine_joined: {
    title: "Ahora es de los dos",
    module: "imagine",
    inbox: true,
    push: false,
    text: (name) => `${name} se unió a algo tuyo`,
  },
  imagine_capsule_ready: {
    title: "Cápsula lista",
    module: "imagine",
    inbox: true,
    push: true,
    text: () => "Una cápsula ya se puede abrir",
  },
  memory_new: {
    title: "Nuevo recuerdo",
    module: "memories",
    inbox: true,
    push: true,
    text: (name) => `${name} guardó un recuerdo`,
  },
};

async function loadPair(coupleId) {
  const snap = await db.collection("couples").doc(coupleId).get();
  if (!snap.exists) return null;
  const user1Id = snap.get("user1Id") || "";
  const user2Id = snap.get("user2Id") || "";
  if (!user1Id || !user2Id) return null;
  return { user1Id, user2Id };
}

function otherMember(pair, uid) {
  if (uid === pair.user1Id) return pair.user2Id;
  if (uid === pair.user2Id) return pair.user1Id;
  return null;
}

function uidOfSlot(pair, slot) {
  if (slot === "user1") return pair.user1Id;
  if (slot === "user2") return pair.user2Id;
  return null;
}

function otherSlot(slot) {
  return slot === "user1" ? "user2" : "user1";
}

function newTopicSlots(before, after) {
  const was = before.topics || {};
  const now = after.topics || {};
  return SLOTS.filter((slot) => now[slot] && !was[slot]);
}

function newTrueSlots(before, after, field) {
  const was = before[field] || {};
  const now = after[field] || {};
  return SLOTS.filter((slot) => now[slot] === true && was[slot] !== true);
}

async function writeInbox({ recipientUid, actorUid, kind, refId, eventId, once }) {
  const def = KINDS[kind];
  const data = {
    kind,
    module: def.module,
    actorId: actorUid,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  };
  if (refId) data.refId = refId;
  const ref = db
    .collection("users")
    .doc(recipientUid)
    .collection("notifications")
    .doc(`${eventId}_${kind}`);
  if (once) {
    await ref.create(data);
  } else {
    await ref.set(data);
  }
}

async function sendPush({ recipientRef, recipientSnap, kind, name }) {
  const def = KINDS[kind];
  const enabled = recipientSnap.get("pushEnabled") !== false;
  const prefs = recipientSnap.get("notificationPrefs") || {};
  const wanted = typeof prefs[kind] === "boolean" ? prefs[kind] : def.push;
  const tokens = (recipientSnap.get("pushTokens") || []).filter(
    (token) => typeof token === "string" && token.length > 0
  );
  if (!enabled || !wanted || tokens.length === 0) return;

  const response = await getMessaging().sendEachForMulticast({
    tokens,
    notification: {
      title: def.title || MODULE_LABELS[def.module],
      body: def.text(name),
    },
    data: { kind, module: def.module },
    android: {
      priority: "high",
      notification: { channelId: def.module },
    },
  });

  const stale = [];
  response.responses.forEach((result, index) => {
    if (!result.success && STALE_TOKEN_CODES.has(result.error && result.error.code)) {
      stale.push(tokens[index]);
    }
  });
  if (stale.length > 0) {
    await recipientRef.update({ pushTokens: FieldValue.arrayRemove(...stale) });
  }
}

async function notify({ pair, recipientUid, kind, refId, eventId, once }) {
  if (!recipientUid) return;
  const actorUid = otherMember(pair, recipientUid);
  if (!actorUid) return;
  const def = KINDS[kind];

  const recipientRef = db.collection("users").doc(recipientUid);
  let recipientSnap;
  let name = FALLBACK_NAME;
  try {
    const [recipient, actor] = await Promise.all([
      recipientRef.get(),
      db.collection("users").doc(actorUid).get(),
    ]);
    if (!recipient.exists) return;
    recipientSnap = recipient;
    name = (actor.exists && actor.get("name")) || FALLBACK_NAME;
  } catch (error) {
    console.error("notify: lectura fallida", kind, error);
    return;
  }

  if (def.inbox) {
    try {
      await writeInbox({ recipientUid, actorUid, kind, refId, eventId, once });
    } catch (error) {
      if (once && error.code === 6) return;
      console.error("notify: bandeja fallida", kind, error);
    }
  }

  try {
    await sendPush({ recipientRef, recipientSnap, kind, name });
  } catch (error) {
    console.error("notify: push fallido", kind, error);
  }
}

exports.notifyTalkMessage = onDocumentCreated(
  { document: "couples/{coupleId}/messages/{messageId}", region: REGION },
  async (event) => {
    const message = event.data && event.data.data();
    if (!message) return;
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid: otherMember(pair, message.senderId),
      kind: "talk_message",
      refId: event.params.messageId,
      eventId: event.id,
    });
  }
);

exports.notifyDrawRound = onDocumentWritten(
  { document: "couples/{coupleId}/drawRound/current", region: REGION },
  async (event) => {
    const afterSnap = event.data && event.data.after;
    if (!afterSnap || !afterSnap.exists) return;
    const after = afterSnap.data();
    const before = event.data.before.exists ? event.data.before.data() : {};
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;

    const send = (slot, kind) =>
      notify({
        pair,
        recipientUid: uidOfSlot(pair, slot),
        kind,
        eventId: event.id,
      });

    const topicSlots = newTopicSlots(before, after);
    if (after.status === "waiting_topics" && topicSlots.length === 1) {
      await send(topicSlots[0], "draw_topic_received");
      return;
    }
    if (after.status === "drawing" && before.status !== "drawing" && topicSlots.length === 1) {
      await send(topicSlots[0], "draw_ready_to_draw");
      return;
    }

    if (after.status === "revealed" && before.status !== "revealed") {
      const ready = newTrueSlots(before, after, "readyToReveal");
      if (ready.length === 1) await send(otherSlot(ready[0]), "draw_revealed");
      return;
    }

    if (after.status === "revealed") {
      const next = newTrueSlots(before, after, "readyForNext");
      if (next.length === 1) await send(otherSlot(next[0]), "draw_next_round");
    }
  }
);

exports.notifyDailyChallenge = onDocumentCreated(
  { document: "couples/{coupleId}/dailyChallenges/{date}", region: REGION },
  async (event) => {
    const daily = event.data && event.data.data();
    if (!daily) return;
    const first = Array.isArray(daily.answeredBy) ? daily.answeredBy[0] : null;
    if (!first) return;
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid: otherMember(pair, first),
      kind: "play_daily_answered",
      refId: event.params.date,
      eventId: event.id,
    });
  }
);

exports.notifySessionCreated = onDocumentCreated(
  { document: "couples/{coupleId}/gameSessions/{sessionId}", region: REGION },
  async (event) => {
    const session = event.data && event.data.data();
    if (!session || !session.createdBy) return;
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid: otherMember(pair, session.createdBy),
      kind: "play_session_new",
      refId: event.params.sessionId,
      eventId: event.id,
    });
  }
);

exports.notifySessionUpdated = onDocumentUpdated(
  { document: "couples/{coupleId}/gameSessions/{sessionId}", region: REGION },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after || before.status === after.status) return;

    let recipientUid = null;
    let kind = null;
    if (after.type === "knowMe" && after.status === "guessing") {
      recipientUid = Array.isArray(after.answeredBy) ? after.answeredBy[0] : null;
      kind = "play_guess_turn";
    } else if (after.status === "revealed") {
      const finishedFirst = after.type === "knowMe" ? after.guessedBy : after.answeredBy;
      recipientUid = Array.isArray(finishedFirst) ? finishedFirst[0] : null;
      kind = "play_result";
    }
    if (!recipientUid || !kind) return;

    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid,
      kind,
      refId: event.params.sessionId,
      eventId: event.id,
    });
  }
);

exports.notifyCapsuleCreated = onDocumentCreated(
  { document: "couples/{coupleId}/imagineCapsules/{capsuleId}", region: REGION },
  async (event) => {
    const capsule = event.data && event.data.data();
    if (!capsule || !capsule.createdBy) return;
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid: otherMember(pair, capsule.createdBy),
      kind: capsule.mode === "shared" ? "imagine_capsule_pending" : "imagine_new",
      refId: event.params.capsuleId,
      eventId: event.id,
    });
  }
);

function createdHandler(collection) {
  return onDocumentCreated(
    { document: `couples/{coupleId}/${collection}/{entryId}`, region: REGION },
    async (event) => {
      const entry = event.data && event.data.data();
      if (!entry || !entry.createdBy) return;
      const pair = await loadPair(event.params.coupleId);
      if (!pair) return;
      await notify({
        pair,
        recipientUid: otherMember(pair, entry.createdBy),
        kind: "imagine_new",
        refId: event.params.entryId,
        eventId: event.id,
      });
    }
  );
}

exports.notifyIdeaCreated = createdHandler("imagineIdeas");
exports.notifyListCreated = createdHandler("imagineLists");
exports.notifyGoalCreated = createdHandler("imagineGoals");

function joinedHandler(collection) {
  return onDocumentUpdated(
    { document: `couples/{coupleId}/${collection}/{entryId}`, region: REGION },
    async (event) => {
      const before = event.data.before.data();
      const after = event.data.after.data();
      if (!before || !after || !after.createdBy) return;
      const wasShared = Array.isArray(before.sharedWith) && before.sharedWith.length > 0;
      const isShared = Array.isArray(after.sharedWith) && after.sharedWith.length > 0;
      if (wasShared || !isShared) return;
      const pair = await loadPair(event.params.coupleId);
      if (!pair) return;
      await notify({
        pair,
        recipientUid: after.createdBy,
        kind: "imagine_joined",
        refId: event.params.entryId,
        eventId: event.id,
      });
    }
  );
}

exports.notifyIdeaJoined = joinedHandler("imagineIdeas");
exports.notifyListJoined = joinedHandler("imagineLists");
exports.notifyGoalJoined = joinedHandler("imagineGoals");

exports.notifyMemoryCreated = onDocumentCreated(
  { document: "couples/{coupleId}/memories/{memoryId}", region: REGION },
  async (event) => {
    const memory = event.data && event.data.data();
    if (!memory || !memory.createdBy) return;
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid: otherMember(pair, memory.createdBy),
      kind: "memory_new",
      refId: event.params.memoryId,
      eventId: event.id,
    });
  }
);

function localHour(timezone) {
  try {
    const hour = parseInt(
      new Intl.DateTimeFormat("en-US", {
        hour: "numeric",
        hourCycle: "h23",
        timeZone: timezone,
      }).format(new Date()),
      10
    );
    return Number.isNaN(hour) ? CAPSULE_NOTIFY_FROM_HOUR : hour;
  } catch (error) {
    return CAPSULE_NOTIFY_FROM_HOUR;
  }
}

exports.notifyCapsulesReady = onSchedule(
  { schedule: "every 60 minutes", region: REGION },
  async () => {
    const now = Timestamp.now();
    const since = Timestamp.fromMillis(now.toMillis() - CAPSULE_LOOKBACK_MS);
    const couples = await db.collection("couples").get();

    for (const couple of couples.docs) {
      const user1Id = couple.get("user1Id") || "";
      const user2Id = couple.get("user2Id") || "";
      if (!user1Id || !user2Id) continue;
      if (localHour(couple.get("timezone") || DEFAULT_TIMEZONE) < CAPSULE_NOTIFY_FROM_HOUR) {
        continue;
      }
      const pair = { user1Id, user2Id };

      try {
        const capsules = await couple.ref
          .collection("imagineCapsules")
          .where("unlockDate", "<=", now)
          .where("unlockDate", ">", since)
          .get();
        for (const capsule of capsules.docs) {
          for (const recipientUid of [user1Id, user2Id]) {
            await notify({
              pair,
              recipientUid,
              kind: "imagine_capsule_ready",
              refId: capsule.id,
              eventId: `capsule_${capsule.id}`,
              once: true,
            });
          }
        }
      } catch (error) {
        console.error("notifyCapsulesReady", couple.id, error);
      }
    }
  }
);
