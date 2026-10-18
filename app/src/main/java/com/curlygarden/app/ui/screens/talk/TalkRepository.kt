package com.curlygarden.app.ui.screens.talk

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TalkRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun messages(coupleId: String) =
        firestore.collection("couples").document(coupleId).collection("messages")

    suspend fun resolveSession(): TalkSession? {
        val uid = auth.currentUser?.uid ?: return null
        val myDoc = firestore.collection("users").document(uid).get().await()
        val coupleId = myDoc.getString("coupleId")?.takeIf { it.isNotBlank() } ?: return null

        val coupleDoc = firestore.collection("couples").document(coupleId).get().await()
        val user1 = coupleDoc.getString("user1Id").orEmpty()
        val user2 = coupleDoc.getString("user2Id").orEmpty()
        val partnerUid = listOf(user1, user2).firstOrNull { it.isNotBlank() && it != uid }

        val partnerProfile = partnerUid?.let {
            runCatching { firestore.collection("users").document(it).get().await() }
                .getOrNull()
                ?.toProfile(it)
        }

        return TalkSession(
            coupleId = coupleId,
            myUid = uid,
            partnerUid = partnerUid,
            myProfile = myDoc.toProfile(uid),
            partnerProfile = partnerProfile
        )
    }

    private fun DocumentSnapshot.toProfile(uid: String) = TalkProfile(
        uid = uid,
        name = getString("name").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty()
    )

    fun observeLatest(coupleId: String, limit: Long): Flow<List<TalkMessage>> = callbackFlow {
        val listener = messages(coupleId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.filter { !it.metadata.hasPendingWrites() || it.getTimestamp("timestamp") != null }
                    ?.map { it.toMessage() }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeAll(coupleId: String): Flow<List<TalkMessage>> = callbackFlow {
        val listener = messages(coupleId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.toMessage() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    private fun DocumentSnapshot.toMessage() = TalkMessage(
        id = id,
        senderId = getString("senderId").orEmpty(),
        text = getString("text").orEmpty(),
        timestamp = getTimestamp("timestamp")?.toDate()?.time ?: Long.MAX_VALUE,
        seen = getBoolean("seen") ?: false,
        replyTo = getString("replyTo"),
        replyPreview = getString("replyPreview")
    )

    suspend fun send(coupleId: String, text: String, replyTo: TalkMessage?) {
        val uid = auth.currentUser?.uid ?: return
        val clean = text.trim().take(TALK_MAX_CHARS)
        if (clean.isEmpty()) return

        val data = hashMapOf<String, Any>(
            "senderId" to uid,
            "text" to clean,
            "timestamp" to FieldValue.serverTimestamp(),
            "seen" to false
        )
        if (replyTo != null) {
            data["replyTo"] = replyTo.id
            data["replyPreview"] = replyTo.text.take(60)
        }
        messages(coupleId).add(data).await()
    }

    suspend fun markSeen(coupleId: String, messageId: String) {
        messages(coupleId).document(messageId).update("seen", true).await()
    }
}
