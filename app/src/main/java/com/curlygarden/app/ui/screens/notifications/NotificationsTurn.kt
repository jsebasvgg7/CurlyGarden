package com.curlygarden.app.ui.screens.notifications

import com.curlygarden.app.data.streak.todayKeyFor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class TurnItem(
    val kind: NotificationKind,
    val actorId: String,
    val refId: String? = null
)

internal class NotificationsTurnSource(private val firestore: FirebaseFirestore) {

    fun observe(session: NotificationsSession): Flow<List<TurnItem>> {
        val coupleId = session.coupleId
        val partner = session.partnerUid
        val slot = session.mySlot
        if (coupleId == null || partner == null || slot == null) return flowOf(emptyList())

        val partnerSlot = if (slot == "user1") "user2" else "user1"
        val couple = firestore.collection("couples").document(coupleId)
        val me = session.myUid

        return combine(
            documentFlow(couple.collection("drawRound").document("current"))
                .map { drawItems(it, slot, partnerSlot, partner) }
                .safe(),
            documentFlow(couple.collection("dailyChallenges").document(todayKeyFor(session.timezone)))
                .map { dailyItems(it, me, partner) }
                .safe(),
            queryFlow(couple.collection("gameSessions").whereIn("status", listOf("answering", "guessing")))
                .map { docs -> docs.flatMap { sessionItems(it, me, partner) } }
                .safe(),
            queryFlow(couple.collection("imagineCapsules"))
                .map { docs -> docs.flatMap { capsuleItems(it, me, partner) } }
                .safe()
        ) { draw, daily, sessions, capsules -> draw + daily + sessions + capsules }
    }

    private fun Flow<List<TurnItem>>.safe(): Flow<List<TurnItem>> = catch { emit(emptyList()) }

    private fun documentFlow(ref: DocumentReference): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot)
        }
        awaitClose { listener.remove() }
    }

    private fun queryFlow(query: Query): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    private fun drawItems(
        round: DocumentSnapshot?,
        slot: String,
        partnerSlot: String,
        partner: String
    ): List<TurnItem> {
        if (round == null || !round.exists()) return emptyList()
        val topics = round.get("topics") as? Map<*, *>
        val ready = round.get("readyToReveal") as? Map<*, *>
        val next = round.get("readyForNext") as? Map<*, *>
        return when (round.getString("status")) {
            "waiting_topics" ->
                if (topics?.get(slot) != null && topics?.get(partnerSlot) == null) {
                    listOf(TurnItem(NotificationKind.DRAW_TOPIC_RECEIVED, partner))
                } else emptyList()
            "drawing" ->
                if (ready?.get(slot) != true) {
                    listOf(TurnItem(NotificationKind.DRAW_READY_TO_DRAW, partner))
                } else emptyList()
            "revealed" ->
                if (next?.get(partnerSlot) == true && next?.get(slot) != true) {
                    listOf(TurnItem(NotificationKind.DRAW_NEXT_ROUND, partner))
                } else emptyList()
            else -> emptyList()
        }
    }

    private fun dailyItems(daily: DocumentSnapshot?, me: String, partner: String): List<TurnItem> {
        if (daily == null || !daily.exists() || daily.getString("status") != "open") return emptyList()
        val answered = daily.uids("answeredBy")
        return if (partner in answered && me !in answered) {
            listOf(TurnItem(NotificationKind.PLAY_DAILY_ANSWERED, partner, daily.id))
        } else emptyList()
    }

    private fun sessionItems(session: DocumentSnapshot, me: String, partner: String): List<TurnItem> {
        val status = session.getString("status")
        return when {
            status == "answering" &&
                me !in session.uids("answeredBy") &&
                session.getString("createdBy") != me ->
                listOf(TurnItem(NotificationKind.PLAY_SESSION_NEW, partner, session.id))
            status == "guessing" &&
                session.getString("type") == "knowMe" &&
                me !in session.uids("guessedBy") ->
                listOf(TurnItem(NotificationKind.PLAY_GUESS_TURN, partner, session.id))
            else -> emptyList()
        }
    }

    private fun capsuleItems(capsule: DocumentSnapshot, me: String, partner: String): List<TurnItem> {
        if (capsule.getString("mode") != "shared") return emptyList()
        val unlock = capsule.getTimestamp("unlockDate") ?: return emptyList()
        if (unlock.compareTo(Timestamp.now()) <= 0) return emptyList()
        return if (me !in capsule.uids("contributors")) {
            listOf(TurnItem(NotificationKind.IMAGINE_CAPSULE_PENDING, partner, capsule.id))
        } else emptyList()
    }

    private fun DocumentSnapshot.uids(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()
}
