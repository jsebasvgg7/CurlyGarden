const {
  onDocumentCreated,
  onDocumentUpdated,
  onDocumentWritten,
} = require("firebase-functions/v2/firestore");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

const db = getFirestore();

const REGION = "us-central1";
const FALLBACK_NAME = "El otro";
const SLOTS = ["user1", "user2"];

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
    module: "talk",
    inbox: true,
    push: true,
    text: (name) => `${name} te dejó una carta`,
  },
  draw_topic_received: {
    module: "draw",
    inbox: false,
    push: true,
    text: (name) => `${name} ya envió su tema. Te toca enviar el tuyo`,
  },
  draw_ready_to_draw: {
    module: "draw",
    inbox: false,
    push: true,
    text: () => "Ya pueden dibujar",
  },
  draw_revealed: {
    module: "draw",
    inbox: true,
    push: true,
    text: () => "Ya pueden ver los dibujos",
  },
  draw_next_round: {
    module: "draw",
    inbox: false,
    push: false,
    text: (name) => `${name} quiere dibujar de nuevo`,
  },
  play_daily_answered: {
    module: "play",
    inbox: false,
    push: true,
    text: (name) => `${name} ya respondió el reto de hoy`,
  },
  play_session_new: {
    module: "play",
    inbox: false,
    push: true,
    text: (name) => `${name} empezó una partida`,
  },
  play_guess_turn: {
    module: "play",
    inbox: false,
    push: true,
    text: () => "Ya puedes adivinar",
  },
  play_result: {
    module: "play",
    inbox: true,
    push: true,
    text: () => "Ya hay resultado",
  },
  imagine_capsule_pending: {
    module: "imagine",
    inbox: false,
    push: true,
    text: () => "Falta tu parte en una cápsula",
  },
  imagine_joined: {
    module: "imagine",
    inbox: true,
    push: false,
    text: (name) => `${name} se unió a algo tuyo`,
  },
  memory_new: {
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

async function writeInbox({ recipientUid, actorUid, kind, refId, eventId }) {
  const def = KINDS[kind];
  const data = {
    kind,
    module: def.module,
    actorId: actorUid,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  };
  if (refId) data.refId = refId;
  await db
    .collection("users")
    .doc(recipientUid)
    .collection("notifications")
    .doc(`${eventId}_${kind}`)
    .set(data);
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
      title: MODULE_LABELS[def.module],
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

async function notify({ pair, recipientUid, kind, refId, eventId }) {
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
      await writeInbox({ recipientUid, actorUid, kind, refId, eventId });
    } catch (error) {
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
    if (!capsule || capsule.mode !== "shared" || !capsule.createdBy) return;
    const pair = await loadPair(event.params.coupleId);
    if (!pair) return;
    await notify({
      pair,
      recipientUid: otherMember(pair, capsule.createdBy),
      kind: "imagine_capsule_pending",
      refId: event.params.capsuleId,
      eventId: event.id,
    });
  }
);

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
