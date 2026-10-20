package com.curlygarden.app.ui.screens.imagine

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

private data class UploadedAttachment(
    val url: String,
    val type: AttachmentType
)

class ImagineRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private fun collection(coupleId: String, category: ImagineCategory): CollectionReference =
        firestore.collection("couples").document(coupleId).collection(category.collection)

    private fun entriesOf(coupleId: String, capsuleId: String): CollectionReference =
        collection(coupleId, ImagineCategory.CAPSULES).document(capsuleId).collection("entries")

    suspend fun resolveSession(): ImagineSession? {
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

        return ImagineSession(
            coupleId = coupleId,
            myUid = uid,
            partnerUid = partnerUid,
            myProfile = myDoc.toProfile(uid),
            partnerProfile = partnerProfile
        )
    }

    fun observeIdeas(coupleId: String): Flow<List<ImagineIdea>> =
        observe(coupleId, ImagineCategory.IDEAS) { it.toIdea() }

    fun observeLists(coupleId: String): Flow<List<ImagineList>> =
        observe(coupleId, ImagineCategory.LISTS) { it.toList() }

    fun observeGoals(coupleId: String): Flow<List<ImagineGoal>> =
        observe(coupleId, ImagineCategory.GOALS) { it.toGoal() }

    fun observeCapsules(coupleId: String): Flow<List<ImagineCapsule>> =
        observe(coupleId, ImagineCategory.CAPSULES) { it.toCapsule() }

    private fun <T> observe(
        coupleId: String,
        category: ImagineCategory,
        mapper: (DocumentSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        val listener = collection(coupleId, category)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map(mapper) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createIdea(
        coupleId: String,
        title: String,
        content: String,
        attachment: ImagineAttachment?
    ) {
        val uid = requireUid()
        val ref = collection(coupleId, ImagineCategory.IDEAS).document()
        val uploaded = attachment?.let { upload(coupleId, ref.id, it) }

        val data = hashMapOf<String, Any>(
            "title" to title.trim().take(IMAGINE_TITLE_MAX),
            "content" to content.trim().take(IMAGINE_IDEA_MAX),
            "createdBy" to uid,
            "sharedWith" to emptyList<String>(),
            "locked" to false,
            "likedBy" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (uploaded != null) {
            data["attachmentUrl"] = uploaded.url
            data["attachmentType"] = uploaded.type.value
        }
        ref.set(data).await()
    }

    suspend fun updateIdea(
        coupleId: String,
        ideaId: String,
        title: String,
        content: String,
        newAttachment: ImagineAttachment?,
        removeAttachment: Boolean
    ) {
        val updates = hashMapOf<String, Any>(
            "title" to title.trim().take(IMAGINE_TITLE_MAX),
            "content" to content.trim().take(IMAGINE_IDEA_MAX)
        )
        when {
            newAttachment != null -> {
                val uploaded = upload(coupleId, ideaId, newAttachment)
                updates["attachmentUrl"] = uploaded.url
                updates["attachmentType"] = uploaded.type.value
            }
            removeAttachment -> {
                updates["attachmentUrl"] = FieldValue.delete()
                updates["attachmentType"] = FieldValue.delete()
            }
        }
        collection(coupleId, ImagineCategory.IDEAS).document(ideaId).update(updates).await()
    }

    suspend fun createList(coupleId: String, title: String, items: List<ListItem>) {
        val uid = requireUid()
        val clean = cleanItems(items)
        collection(coupleId, ImagineCategory.LISTS).add(
            hashMapOf(
                "title" to title.trim().take(IMAGINE_TITLE_MAX),
                "items" to clean.map { it.toMap() },
                "createdBy" to uid,
                "sharedWith" to emptyList<String>(),
                "locked" to false,
                "likedBy" to emptyList<String>(),
                "status" to ImagineStatus.ACTIVE.value,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateList(
        coupleId: String,
        listId: String,
        title: String,
        items: List<ListItem>
    ) {
        val clean = cleanItems(items)
        val finished = clean.isNotEmpty() && clean.all { it.done }
        collection(coupleId, ImagineCategory.LISTS).document(listId).update(
            mapOf(
                "title" to title.trim().take(IMAGINE_TITLE_MAX),
                "items" to clean.map { it.toMap() },
                "status" to (if (finished) ImagineStatus.FINISHED else ImagineStatus.ACTIVE).value
            )
        ).await()
    }

    suspend fun createGoal(
        coupleId: String,
        title: String,
        content: String,
        targetDateText: String
    ) {
        val uid = requireUid()
        collection(coupleId, ImagineCategory.GOALS).add(
            hashMapOf(
                "title" to title.trim().take(IMAGINE_TITLE_MAX),
                "content" to content.trim().take(IMAGINE_GOAL_CONTENT_MAX),
                "targetDateText" to targetDateText.trim().take(IMAGINE_GOAL_DATE_MAX),
                "createdBy" to uid,
                "sharedWith" to emptyList<String>(),
                "locked" to false,
                "likedBy" to emptyList<String>(),
                "status" to ImagineStatus.ACTIVE.value,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateGoal(
        coupleId: String,
        goalId: String,
        title: String,
        content: String,
        targetDateText: String
    ) {
        collection(coupleId, ImagineCategory.GOALS).document(goalId).update(
            mapOf(
                "title" to title.trim().take(IMAGINE_TITLE_MAX),
                "content" to content.trim().take(IMAGINE_GOAL_CONTENT_MAX),
                "targetDateText" to targetDateText.trim().take(IMAGINE_GOAL_DATE_MAX)
            )
        ).await()
    }

    suspend fun setGoalStatus(coupleId: String, goalId: String, status: ImagineStatus) {
        collection(coupleId, ImagineCategory.GOALS).document(goalId)
            .update("status", status.value)
            .await()
    }

    suspend fun createCapsule(
        coupleId: String,
        title: String,
        mode: CapsuleMode,
        unlockDate: Long,
        text: String,
        attachment: ImagineAttachment?
    ) {
        val uid = requireUid()
        val capsuleRef = collection(coupleId, ImagineCategory.CAPSULES).document()
        val uploaded = attachment?.let { upload(coupleId, capsuleRef.id, it, "${uid}_") }

        val capsuleData = hashMapOf<String, Any>(
            "title" to title.trim().take(IMAGINE_TITLE_MAX),
            "mode" to mode.value,
            "unlockDate" to Timestamp(Date(unlockDate)),
            "contributors" to listOf(uid),
            "createdBy" to uid,
            "likedBy" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.batch()
            .set(capsuleRef, capsuleData)
            .set(entriesOf(coupleId, capsuleRef.id).document(uid), entryData(text, uploaded))
            .commit()
            .await()
    }

    suspend fun contributeToCapsule(
        coupleId: String,
        capsuleId: String,
        text: String,
        attachment: ImagineAttachment?
    ) {
        val uid = requireUid()
        val uploaded = attachment?.let { upload(coupleId, capsuleId, it, "${uid}_") }
        val capsuleRef = collection(coupleId, ImagineCategory.CAPSULES).document(capsuleId)

        firestore.batch()
            .update(capsuleRef, "contributors", FieldValue.arrayUnion(uid))
            .set(entriesOf(coupleId, capsuleId).document(uid), entryData(text, uploaded))
            .commit()
            .await()
    }

    suspend fun loadCapsuleEntries(session: ImagineSession, capsule: ImagineCapsule): CapsuleEntries {
        val entries = entriesOf(session.coupleId, capsule.id)
        val mine = entries.document(session.myUid).get().await().toEntry()
        val partner = if (capsule.isUnlocked() && session.partnerUid != null) {
            runCatching { entries.document(session.partnerUid).get().await().toEntry() }.getOrNull()
        } else {
            null
        }
        return CapsuleEntries(capsuleId = capsule.id, mine = mine, partner = partner)
    }

    suspend fun join(coupleId: String, category: ImagineCategory, id: String) {
        val uid = requireUid()
        collection(coupleId, category).document(id).update("sharedWith", listOf(uid)).await()
    }

    suspend fun setLocked(coupleId: String, category: ImagineCategory, id: String, locked: Boolean) {
        collection(coupleId, category).document(id).update("locked", locked).await()
    }

    suspend fun setLiked(coupleId: String, category: ImagineCategory, id: String, liked: Boolean) {
        val uid = requireUid()
        val value = if (liked) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        collection(coupleId, category).document(id).update("likedBy", value).await()
    }

    suspend fun delete(coupleId: String, category: ImagineCategory, id: String) {
        collection(coupleId, category).document(id).delete().await()
    }

    private suspend fun upload(
        coupleId: String,
        entryId: String,
        attachment: ImagineAttachment,
        prefix: String = ""
    ): UploadedAttachment {
        if (attachment.sizeBytes > IMAGINE_MAX_ATTACHMENT_BYTES) throw AttachmentTooLargeException()

        val safeName = attachment.fileName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeLast(80)
            .ifBlank { "archivo" }
        val path = "idea_attachments/$coupleId/$entryId/$prefix${System.currentTimeMillis()}_$safeName"
        val ref = storage.reference.child(path)

        val metadata = StorageMetadata.Builder()
            .apply { attachment.mimeType?.let { setContentType(it) } }
            .build()

        ref.putFile(attachment.uri, metadata).await()
        return UploadedAttachment(ref.downloadUrl.await().toString(), attachment.type())
    }

    private fun entryData(text: String, uploaded: UploadedAttachment?): HashMap<String, Any> {
        val data = hashMapOf<String, Any>(
            "text" to text.trim().take(IMAGINE_CAPSULE_TEXT_MAX),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (uploaded != null) {
            data["attachmentUrl"] = uploaded.url
            data["attachmentType"] = uploaded.type.value
        }
        return data
    }

    private fun cleanItems(items: List<ListItem>): List<ListItem> = items
        .map { ListItem(it.text.trim().take(IMAGINE_LIST_ITEM_MAX), it.done) }
        .filter { it.text.isNotEmpty() }
        .take(IMAGINE_LIST_MAX_ITEMS)

    private fun ListItem.toMap(): Map<String, Any> = mapOf("text" to text, "done" to done)

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Sin sesión")

    private fun DocumentSnapshot.toProfile(uid: String) = ImagineProfile(
        uid = uid,
        name = getString("name").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty()
    )

    private fun DocumentSnapshot.strings(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    private fun DocumentSnapshot.createdMillis(): Long =
        getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE

    private fun DocumentSnapshot.toIdea() = ImagineIdea(
        id = id,
        title = getString("title").orEmpty(),
        content = getString("content").orEmpty(),
        createdBy = getString("createdBy").orEmpty(),
        sharedWith = strings("sharedWith"),
        locked = getBoolean("locked") ?: false,
        likedBy = strings("likedBy"),
        attachmentUrl = getString("attachmentUrl"),
        attachmentType = AttachmentType.from(getString("attachmentType")),
        createdAt = createdMillis()
    )

    private fun DocumentSnapshot.toList() = ImagineList(
        id = id,
        title = getString("title").orEmpty(),
        items = (get("items") as? List<*>)?.mapNotNull { raw ->
            (raw as? Map<*, *>)?.let {
                ListItem(
                    text = it["text"] as? String ?: "",
                    done = it["done"] as? Boolean ?: false
                )
            }
        } ?: emptyList(),
        createdBy = getString("createdBy").orEmpty(),
        sharedWith = strings("sharedWith"),
        locked = getBoolean("locked") ?: false,
        likedBy = strings("likedBy"),
        status = ImagineStatus.from(getString("status")),
        createdAt = createdMillis()
    )

    private fun DocumentSnapshot.toGoal() = ImagineGoal(
        id = id,
        title = getString("title").orEmpty(),
        content = getString("content").orEmpty(),
        targetDateText = getString("targetDateText").orEmpty(),
        createdBy = getString("createdBy").orEmpty(),
        sharedWith = strings("sharedWith"),
        locked = getBoolean("locked") ?: false,
        likedBy = strings("likedBy"),
        status = ImagineStatus.from(getString("status")),
        createdAt = createdMillis()
    )

    private fun DocumentSnapshot.toCapsule() = ImagineCapsule(
        id = id,
        title = getString("title").orEmpty(),
        mode = CapsuleMode.from(getString("mode")),
        unlockDate = getTimestamp("unlockDate")?.toDate()?.time ?: 0L,
        contributors = strings("contributors"),
        createdBy = getString("createdBy").orEmpty(),
        likedBy = strings("likedBy"),
        createdAt = createdMillis()
    )

    private fun DocumentSnapshot.toEntry(): CapsuleEntry? {
        if (!exists()) return null
        return CapsuleEntry(
            authorUid = id,
            text = getString("text").orEmpty(),
            attachmentUrl = getString("attachmentUrl"),
            attachmentType = AttachmentType.from(getString("attachmentType")),
            createdAt = createdMillis()
        )
    }
}
