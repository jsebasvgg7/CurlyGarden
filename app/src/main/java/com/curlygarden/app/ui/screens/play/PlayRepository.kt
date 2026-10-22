package com.curlygarden.app.ui.screens.play

import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.curlygarden.app.data.streak.todayKeyFor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlayRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun couple(coupleId: String): DocumentReference =
        firestore.collection("couples").document(coupleId)

    private fun dailyDoc(coupleId: String, dateKey: String): DocumentReference =
        couple(coupleId).collection("dailyChallenges").document(dateKey)

    private fun sessionsCol(coupleId: String): CollectionReference =
        couple(coupleId).collection("gameSessions")

    private fun statsDoc(coupleId: String, uid: String): DocumentReference =
        couple(coupleId).collection("playStats").document(uid)

    suspend fun resolveSession(): PlaySession? {
        val uid = auth.currentUser?.uid ?: return null
        val myDoc = firestore.collection("users").document(uid).get().await()
        val coupleId = myDoc.getString("coupleId")?.takeIf { it.isNotBlank() } ?: return null

        val coupleDoc = couple(coupleId).get().await()
        val user1 = coupleDoc.getString("user1Id").orEmpty()
        val user2 = coupleDoc.getString("user2Id").orEmpty()
        val partnerUid = listOf(user1, user2).firstOrNull { it.isNotBlank() && it != uid }

        val partnerProfile = partnerUid?.let {
            runCatching { firestore.collection("users").document(it).get().await() }
                .getOrNull()
                ?.toProfile(it)
        }

        return PlaySession(
            coupleId = coupleId,
            myUid = uid,
            partnerUid = partnerUid,
            timezone = coupleDoc.getString("timezone")?.takeIf { it.isNotBlank() } ?: DEFAULT_TIMEZONE,
            myProfile = myDoc.toProfile(uid),
            partnerProfile = partnerProfile
        )
    }

    fun observeDaily(coupleId: String, dateKey: String): Flow<DailyChallenge?> = callbackFlow {
        val listener = dailyDoc(coupleId, dateKey).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.takeIf { it.exists() }?.toDaily())
        }
        awaitClose { listener.remove() }
    }

    fun observeSessions(coupleId: String): Flow<List<GameSession>> = callbackFlow {
        val listener = sessionsCol(coupleId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.map { it.toGameSession() } ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    fun observeStats(coupleId: String, uid: String): Flow<PlayStats> = callbackFlow {
        val listener = statsDoc(coupleId, uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(PlayStats(points = snapshot?.pointsValue() ?: 0))
        }
        awaitClose { listener.remove() }
    }

    suspend fun loadDailyResponses(
        session: PlaySession,
        dateKey: String,
        includePartner: Boolean
    ): DailyResponses {
        val responses = dailyDoc(session.coupleId, dateKey).collection("responses")
        val mine = responses.document(session.myUid).get().await().toResponse(session.myUid)
        val partnerUid = session.partnerUid
        val partner = if (includePartner && partnerUid != null) {
            responses.document(partnerUid).get().await().toResponse(partnerUid)
        } else {
            null
        }
        return DailyResponses(mine = mine, partner = partner)
    }

    suspend fun submitDaily(
        session: PlaySession,
        dateKey: String,
        type: DailyType,
        bankIndex: Int,
        text: String?
    ) {
        val uid = session.myUid
        val cleanText = text.orEmpty().trim().take(PLAY_ANSWER_MAX)
        require(type != DailyType.QUESTION || cleanText.isNotEmpty())

        val dailyRef = dailyDoc(session.coupleId, dateKey)
        val responseRef = dailyRef.collection("responses").document(uid)
        val statsRef = statsDoc(session.coupleId, uid)
        val coupleRef = couple(session.coupleId)

        firestore.runTransaction { transaction ->
            val daily = transaction.get(dailyRef)
            val stats = transaction.get(statsRef)

            val answeredBy = daily.strings("answeredBy")
            if (uid in answeredBy) return@runTransaction null

            val effectiveType = if (daily.exists()) DailyType.from(daily.getString("type")) else type

            val response = hashMapOf<String, Any>("createdAt" to FieldValue.serverTimestamp())
            if (effectiveType == DailyType.QUESTION) response["text"] = cleanText
            transaction.set(responseRef, response)

            if (daily.exists()) {
                transaction.update(
                    dailyRef,
                    mapOf(
                        "answeredBy" to FieldValue.arrayUnion(uid),
                        "status" to "completed"
                    )
                )
                transaction.set(
                    coupleRef,
                    mapOf("streakLog" to mapOf(dateKey to true)),
                    SetOptions.merge()
                )
            } else {
                transaction.set(
                    dailyRef,
                    hashMapOf<String, Any>(
                        "type" to type.value,
                        "bankIndex" to bankIndex,
                        "status" to "open",
                        "answeredBy" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            transaction.set(statsRef, mapOf("points" to stats.pointsValue() + POINTS_DAILY))
            null
        }.await()
    }

    suspend fun createSession(
        session: PlaySession,
        type: PlayGameType,
        questionIds: List<Int>
    ) {
        require(questionIds.size == PLAY_PICKS_PER_GAME)

        val data = hashMapOf<String, Any>(
            "type" to type.value,
            "questionIds" to questionIds,
            "status" to SessionStatus.ANSWERING.value,
            "answeredBy" to emptyList<String>(),
            "createdBy" to session.myUid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (type == PlayGameType.KNOW_ME) {
            data["guessedBy"] = emptyList<String>()
            data["scored"] = emptyList<String>()
        }
        sessionsCol(session.coupleId).document().set(data).await()
    }

    suspend fun submitAnswers(
        session: PlaySession,
        sessionId: String,
        picks: List<Int>
    ) {
        val uid = session.myUid
        val sessionRef = sessionsCol(session.coupleId).document(sessionId)
        val answersRef = sessionRef.collection("answers").document(uid)
        val statsRef = statsDoc(session.coupleId, uid)
        val coupleRef = couple(session.coupleId)
        val todayKey = todayKeyFor(session.timezone)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(sessionRef)
            val stats = transaction.get(statsRef)

            val type = PlayGameType.from(snapshot.getString("type"))
            val answeredBy = snapshot.strings("answeredBy")
            val status = SessionStatus.from(snapshot.getString("status"))
            if (status != SessionStatus.ANSWERING || uid in answeredBy) return@runTransaction null
            requireValidPicks(type, picks)

            transaction.set(
                answersRef,
                hashMapOf<String, Any>(
                    "picks" to picks,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            val completes = answeredBy.size + 1 == 2
            val updates = hashMapOf<String, Any>("answeredBy" to FieldValue.arrayUnion(uid))
            if (completes) {
                updates["status"] = if (type == PlayGameType.THIS_OR_THAT) {
                    SessionStatus.REVEALED.value
                } else {
                    SessionStatus.GUESSING.value
                }
            }
            transaction.update(sessionRef, updates)

            if (type == PlayGameType.THIS_OR_THAT) {
                transaction.set(statsRef, mapOf("points" to stats.pointsValue() + POINTS_THIS_OR_THAT))
                if (completes) {
                    transaction.set(
                        coupleRef,
                        mapOf("streakLog" to mapOf(todayKey to true)),
                        SetOptions.merge()
                    )
                }
            }
            null
        }.await()
    }

    suspend fun submitGuesses(
        session: PlaySession,
        sessionId: String,
        picks: List<Int>
    ) {
        val uid = session.myUid
        val sessionRef = sessionsCol(session.coupleId).document(sessionId)
        val guessesRef = sessionRef.collection("guesses").document(uid)
        val coupleRef = couple(session.coupleId)
        val todayKey = todayKeyFor(session.timezone)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(sessionRef)

            val guessedBy = snapshot.strings("guessedBy")
            val status = SessionStatus.from(snapshot.getString("status"))
            if (status != SessionStatus.GUESSING || uid in guessedBy) return@runTransaction null
            requireValidPicks(PlayGameType.KNOW_ME, picks)

            transaction.set(
                guessesRef,
                hashMapOf<String, Any>(
                    "picks" to picks,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            val completes = guessedBy.size + 1 == 2
            val updates = hashMapOf<String, Any>("guessedBy" to FieldValue.arrayUnion(uid))
            if (completes) {
                updates["status"] = SessionStatus.REVEALED.value
                transaction.set(
                    coupleRef,
                    mapOf("streakLog" to mapOf(todayKey to true)),
                    SetOptions.merge()
                )
            }
            transaction.update(sessionRef, updates)
            null
        }.await()
    }

    suspend fun loadReveal(
        session: PlaySession,
        sessionId: String
    ): GameReveal {
        val sessionRef = sessionsCol(session.coupleId).document(sessionId)
        val answers = mutableMapOf<String, List<Int>>()
        val guesses = mutableMapOf<String, List<Int>>()

        for (uid in listOfNotNull(session.myUid, session.partnerUid)) {
            sessionRef.collection("answers").document(uid).get().await().picks()
                ?.let { answers[uid] = it }
            sessionRef.collection("guesses").document(uid).get().await().picks()
                ?.let { guesses[uid] = it }
        }
        return GameReveal(answers = answers, guesses = guesses)
    }

    suspend fun awardGuessPoints(
        session: PlaySession,
        sessionId: String,
        points: Int
    ) {
        val uid = session.myUid
        val sessionRef = sessionsCol(session.coupleId).document(sessionId)
        val statsRef = statsDoc(session.coupleId, uid)
        val safePoints = points.coerceIn(0, PLAY_PICKS_PER_GAME * POINTS_GUESS)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(sessionRef)
            val stats = transaction.get(statsRef)

            val status = SessionStatus.from(snapshot.getString("status"))
            if (status != SessionStatus.REVEALED || uid in snapshot.strings("scored")) {
                return@runTransaction null
            }

            transaction.update(sessionRef, mapOf("scored" to FieldValue.arrayUnion(uid)))
            if (safePoints > 0) {
                transaction.set(statsRef, mapOf("points" to stats.pointsValue() + safePoints))
            }
            null
        }.await()
    }

    private fun requireValidPicks(type: PlayGameType, picks: List<Int>) {
        val max = if (type == PlayGameType.THIS_OR_THAT) 1 else 3
        require(picks.size == PLAY_PICKS_PER_GAME && picks.all { it in 0..max })
    }

    private fun DocumentSnapshot.toProfile(uid: String) = PlayProfile(
        uid = uid,
        name = getString("name").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty()
    )

    private fun DocumentSnapshot.strings(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    private fun DocumentSnapshot.pointsValue(): Int = getLong("points")?.toInt() ?: 0

    private fun DocumentSnapshot.picks(): List<Int>? =
        if (!exists()) null else (get("picks") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }

    private fun DocumentSnapshot.toResponse(uid: String): DailyResponse? =
        if (exists()) DailyResponse(authorUid = uid, text = getString("text")) else null

    private fun DocumentSnapshot.toDaily() = DailyChallenge(
        dateKey = id,
        type = DailyType.from(getString("type")),
        bankIndex = (getLong("bankIndex") ?: 0L).toInt(),
        isCompleted = getString("status") == "completed",
        answeredBy = strings("answeredBy")
    )

    private fun DocumentSnapshot.toGameSession() = GameSession(
        id = id,
        type = PlayGameType.from(getString("type")),
        questionIds = (get("questionIds") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
            ?: emptyList(),
        status = SessionStatus.from(getString("status")),
        answeredBy = strings("answeredBy"),
        guessedBy = strings("guessedBy"),
        scored = strings("scored"),
        createdBy = getString("createdBy").orEmpty(),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE
    )
}
