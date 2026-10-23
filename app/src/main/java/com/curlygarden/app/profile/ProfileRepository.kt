package com.curlygarden.app.profile

import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await

data class GoogleProfileHint(
    val suggestedName: String,
    val suggestedPhotoUrl: String?
)

data class MyProfileData(
    val uid: String,
    val name: String,
    val photoUrl: String?
)

data class ProfileFirestoreData(
    val myProfile: MyProfileData?,
    val partnerProfile: MyProfileData?,
    val streakLog: Map<String, Boolean> = emptyMap(),
    val timezone: String = DEFAULT_TIMEZONE,
    val linkedAtMillis: Long? = null,
    val createdAtMillis: Long? = null,
    val points: Int = 0
)

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getGoogleHint(): GoogleProfileHint? {
        val user = auth.currentUser ?: return null
        return GoogleProfileHint(
            suggestedName = user.displayName ?: "",
            suggestedPhotoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun saveProfile(name: String, photoUrl: String?) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No hay sesion activa para guardar el perfil.")

        val data = mutableMapOf<String, Any>("name" to name)
        photoUrl?.let { data["photoUrl"] = it }

        firestore.collection("users").document(uid)
            .set(data)
            .await()
    }

    fun observeProfile(): Flow<ProfileFirestoreData> {
        val uid = auth.currentUser?.uid
            ?: return callbackFlow {
                trySend(ProfileFirestoreData(null, null))
                awaitClose { }
            }

        return observeUserDoc(uid)
            .distinctUntilChanged()
            .flatMapLatest { my ->
                val coupleId = my?.let { getCoupleId(it.uid) }
                if (coupleId == null) {
                    flowOfSingle(ProfileFirestoreData(my, null))
                } else {
                    observePartner(coupleId, myUid = uid, my = my)
                }
            }
    }

    private fun observeUserDoc(uid: String): Flow<MyProfileData?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                val profile = snapshot?.let {
                    if (!it.exists()) null
                    else MyProfileData(
                        uid = uid,
                        name = it.getString("name") ?: "",
                        photoUrl = it.getString("photoUrl")
                    )
                }
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun getCoupleId(uid: String): String? {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.getString("coupleId")?.takeIf { it.isNotBlank() }
    }

    private fun observePartner(
        coupleId: String,
        myUid: String,
        my: MyProfileData?
    ): Flow<ProfileFirestoreData> = callbackFlow {
        var partner: MyProfileData? = null
        var streakLog: Map<String, Boolean> = emptyMap()
        var timezone = DEFAULT_TIMEZONE
        var linkedAtMillis: Long? = null
        var createdAtMillis: Long? = null
        var points = 0
        var partnerListenerRegistered = false

        fun emitCurrent() {
            trySend(
                ProfileFirestoreData(
                    my, partner, streakLog, timezone, linkedAtMillis, createdAtMillis,
                    points = points
                )
            )
        }

        val coupleListener = firestore.collection("couples").document(coupleId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) {
                    emitCurrent()
                    return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                streakLog = (snapshot.get("streakLog") as? Map<String, Boolean>) ?: emptyMap()
                timezone = snapshot.getString("timezone") ?: DEFAULT_TIMEZONE
                linkedAtMillis = snapshot.getTimestamp("linkedAt")?.toDate()?.time
                createdAtMillis = snapshot.getTimestamp("createdAt")?.toDate()?.time

                if (!partnerListenerRegistered) {
                    partnerListenerRegistered = true
                    val user1Id = snapshot.getString("user1Id")
                    val user2Id = snapshot.getString("user2Id")
                    val partnerId = listOfNotNull(user1Id, user2Id)
                        .firstOrNull { it.isNotBlank() && it != myUid }

                    if (partnerId != null) {
                        firestore.collection("users").document(partnerId)
                            .addSnapshotListener { partnerSnapshot, _ ->
                                partner = partnerSnapshot?.takeIf { it.exists() }?.let {
                                    MyProfileData(
                                        uid = partnerId,
                                        name = it.getString("name") ?: "",
                                        photoUrl = it.getString("photoUrl")
                                    )
                                }
                                emitCurrent()
                            }
                    }
                }
                emitCurrent()
            }

        val statsListener = firestore.collection("couples").document(coupleId)
            .collection("playStats").document(myUid)
            .addSnapshotListener { snapshot, _ ->
                points = snapshot?.getLong("points")?.toInt() ?: 0
                emitCurrent()
            }

        awaitClose {
            coupleListener.remove()
            statsListener.remove()
        }
    }

    private fun <T> flowOfSingle(value: T): Flow<T> = callbackFlow {
        trySend(value)
        awaitClose { }
    }
}