package com.curlygarden.app.ui.screens.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userDoc(uid: String): DocumentReference =
        firestore.collection("users").document(uid)

    private fun inboxOf(uid: String): CollectionReference =
        userDoc(uid).collection("notifications")

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Sin sesión")

    suspend fun resolveSession(): NotificationsSession? {
        val uid = auth.currentUser?.uid ?: return null
        val myDoc = userDoc(uid).get().await()
        val coupleId = myDoc.getString("coupleId")?.takeIf { it.isNotBlank() }

        val partnerUid = coupleId?.let {
            val coupleDoc = firestore.collection("couples").document(it).get().await()
            listOf(
                coupleDoc.getString("user1Id").orEmpty(),
                coupleDoc.getString("user2Id").orEmpty()
            ).firstOrNull { id -> id.isNotBlank() && id != uid }
        }

        val partnerProfile = partnerUid?.let {
            runCatching { userDoc(it).get().await() }.getOrNull()?.toProfile(it)
        }

        return NotificationsSession(
            myUid = uid,
            partnerUid = partnerUid,
            myProfile = myDoc.toProfile(uid),
            partnerProfile = partnerProfile
        )
    }

    fun observeInbox(uid: String): Flow<List<InboxNotification>> = callbackFlow {
        val listener = inboxOf(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(NOTIFICATIONS_INBOX_LIMIT.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toInbox() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observeSettings(uid: String): Flow<NotificationSettings> = callbackFlow {
        val listener = userDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toSettings() ?: NotificationSettings())
        }
        awaitClose { listener.remove() }
    }

    suspend fun markRead(uid: String, notificationId: String) {
        inboxOf(uid).document(notificationId).update("read", true).await()
    }

    suspend fun markAllRead(uid: String, notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        val batch = firestore.batch()
        notificationIds.forEach { batch.update(inboxOf(uid).document(it), "read", true) }
        batch.commit().await()
    }

    suspend fun setPushEnabled(enabled: Boolean) {
        userDoc(requireUid()).update("pushEnabled", enabled).await()
    }

    suspend fun setKindEnabled(kind: NotificationKind, enabled: Boolean) {
        userDoc(requireUid()).update("notificationPrefs.${kind.key}", enabled).await()
    }

    suspend fun addPushToken(token: String) {
        userDoc(requireUid()).update("pushTokens", FieldValue.arrayUnion(token)).await()
    }

    suspend fun removePushToken(token: String) {
        userDoc(requireUid()).update("pushTokens", FieldValue.arrayRemove(token)).await()
    }

    private fun DocumentSnapshot.toProfile(uid: String) = NotificationsProfile(
        uid = uid,
        name = getString("name").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty()
    )

    private fun DocumentSnapshot.toSettings(): NotificationSettings {
        val raw = get("notificationPrefs") as? Map<*, *> ?: emptyMap<Any, Any>()
        val prefs = raw.entries.mapNotNull { (key, value) ->
            val name = key as? String ?: return@mapNotNull null
            val flag = value as? Boolean ?: return@mapNotNull null
            name to flag
        }.toMap()
        return NotificationSettings(
            pushEnabled = getBoolean("pushEnabled") ?: true,
            prefs = prefs
        )
    }

    private fun DocumentSnapshot.toInbox(): InboxNotification? {
        val kind = NotificationKind.fromKey(getString("kind")) ?: return null
        return InboxNotification(
            id = id,
            kind = kind,
            actorId = getString("actorId").orEmpty(),
            refId = getString("refId"),
            read = getBoolean("read") ?: false,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE
        )
    }
}
