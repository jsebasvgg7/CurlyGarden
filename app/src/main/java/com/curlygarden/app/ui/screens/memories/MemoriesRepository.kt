package com.curlygarden.app.ui.screens.memories

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class MemoriesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    private fun memoriesOf(coupleId: String): CollectionReference =
        firestore.collection("couples").document(coupleId).collection("memories")

    private fun albumsOf(coupleId: String): CollectionReference =
        firestore.collection("couples").document(coupleId).collection("albums")

    suspend fun resolveSession(): MemoriesSession? {
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

        return MemoriesSession(
            coupleId = coupleId,
            myUid = uid,
            partnerUid = partnerUid,
            myProfile = myDoc.toProfile(uid),
            partnerProfile = partnerProfile
        )
    }

    fun observeMemories(coupleId: String): Flow<List<Memory>> = callbackFlow {
        val listener = memoriesOf(coupleId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toMemory() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun observeAlbums(coupleId: String): Flow<List<Album>> = callbackFlow {
        val listener = albumsOf(coupleId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.toAlbum() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createAlbum(coupleId: String, name: String, colorIndex: Int): String {
        val uid = requireUid()
        val ref = albumsOf(coupleId).document()
        val data = hashMapOf<String, Any>(
            "name" to name.trim().take(MEMORIES_ALBUM_NAME_MAX),
            "colorIndex" to colorIndex.coerceIn(0, MemoriesAlbumColors.lastIndex),
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()
        return ref.id
    }

    suspend fun createMemory(
        coupleId: String,
        photo: MemoryPhoto,
        caption: String,
        dateMillis: Long,
        albumId: String?
    ): String {
        val uid = requireUid()
        if (photo.bytes.size.toLong() > MEMORIES_MAX_PHOTO_BYTES) throw PhotoTooLargeException()

        val ref = memoriesOf(coupleId).document()
        val uploaded = uploadPhotoToCloudinary(functions, photo)

        val data = hashMapOf<String, Any>(
            "photoUrl" to uploaded.url,
            "publicId" to uploaded.publicId,
            "bytes" to uploaded.bytes,
            "caption" to caption.trim().take(MEMORIES_CAPTION_MAX),
            "date" to Timestamp(Date(dateMillis)),
            "aspectRatio" to photo.aspectRatio.coerceAtLeast(0.01f).toDouble(),
            "createdBy" to uid,
            "hidden" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!albumId.isNullOrBlank()) data["albumId"] = albumId
        ref.set(data).await()
        return ref.id
    }

    suspend fun setHidden(coupleId: String, memoryId: String, hidden: Boolean) {
        memoriesOf(coupleId).document(memoryId).update("hidden", hidden).await()
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Sin sesión")

    private fun DocumentSnapshot.toProfile(uid: String) = MemoriesProfile(
        uid = uid,
        name = getString("name").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty()
    )

    private fun DocumentSnapshot.createdMillis(): Long =
        getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE

    private fun DocumentSnapshot.toMemory(): Memory? {
        val photoUrl = getString("photoUrl")?.takeIf { it.isNotBlank() } ?: return null
        return Memory(
            id = id,
            photoUrl = photoUrl,
            caption = getString("caption").orEmpty(),
            dateMillis = getTimestamp("date")?.toDate()?.time ?: createdMillis(),
            aspectRatio = getDouble("aspectRatio")?.toFloat()?.takeIf { it > 0f } ?: 1f,
            albumId = getString("albumId")?.takeIf { it.isNotBlank() },
            createdBy = getString("createdBy").orEmpty(),
            hidden = getBoolean("hidden") ?: false,
            createdAt = createdMillis()
        )
    }

    private fun DocumentSnapshot.toAlbum() = Album(
        id = id,
        name = getString("name").orEmpty(),
        colorIndex = (getLong("colorIndex") ?: 0L).toInt().coerceIn(0, MemoriesAlbumColors.lastIndex),
        coverPhotoUrl = getString("coverPhotoUrl")?.takeIf { it.isNotBlank() },
        createdBy = getString("createdBy").orEmpty(),
        createdAt = createdMillis()
    )
}
