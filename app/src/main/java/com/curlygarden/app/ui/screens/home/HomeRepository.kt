package com.curlygarden.app.ui.screens.home

import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class HomeUserProfile(
    val uid: String,
    val name: String,
    val photoUrl: String?
)

data class HomeLatestMemory(
    val caption: String?,
    val photoUrl: String?
)

data class HomeFirestoreData(
    val streakLog: Map<String, Boolean>,
    val timezone: String,
    val myProfile: HomeUserProfile?,
    val partnerProfile: HomeUserProfile?,
    val latestMemory: HomeLatestMemory?,
    val gameCount: Int = 0,
    val loadFailed: Boolean = false
)

class HomeRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeHome(): Flow<HomeFirestoreData> {
        val uid = auth.currentUser?.uid
            ?: return callbackFlow {
                trySend(HomeFirestoreData(emptyMap(), DEFAULT_TIMEZONE, null, null, null))
                awaitClose { }
            }

        return observeUserProfile(uid)
            .distinctUntilChanged()
            .flatMapLatest { my ->
                val coupleId = my?.let { getCoupleId(it.uid) }
                if (coupleId == null) {
                    flowOfSingle(HomeFirestoreData(emptyMap(), DEFAULT_TIMEZONE, my, null, null))
                } else {
                    combineCoupleData(coupleId, my)
                }
            }
    }

    private fun observeUserProfile(uid: String): Flow<HomeUserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val profile = snapshot?.let {
                    if (!it.exists()) null
                    else HomeUserProfile(
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

    private fun combineCoupleData(
        coupleId: String,
        my: HomeUserProfile?
    ): Flow<HomeFirestoreData> = callbackFlow {
        var streakLog: Map<String, Boolean> = emptyMap()
        var timezone = DEFAULT_TIMEZONE
        var partner: HomeUserProfile? = null
        var latestMemory: HomeLatestMemory? = null
        var dailyCompleted = 0
        var gamesRevealed = 0
        var partnerListenerRegistered = false
        var loadFailed = false

        fun emitCurrent() {
            trySend(
                HomeFirestoreData(
                    streakLog, timezone, my, partner, latestMemory,
                    gameCount = dailyCompleted + gamesRevealed,
                    loadFailed = loadFailed
                )
            )
        }

        val coupleListener = firestore.collection("couples").document(coupleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadFailed = true
                    emitCurrent()
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    emitCurrent()
                    return@addSnapshotListener
                }

                @Suppress("UNCHECKED_CAST")
                streakLog = (snapshot.get("streakLog") as? Map<String, Boolean>) ?: emptyMap()
                timezone = snapshot.getString("timezone") ?: DEFAULT_TIMEZONE

                if (!partnerListenerRegistered) {
                    partnerListenerRegistered = true
                    val user1Id = snapshot.getString("user1Id")
                    val user2Id = snapshot.getString("user2Id")
                    val partnerId = listOfNotNull(user1Id, user2Id)
                        .firstOrNull { it.isNotBlank() && it != my?.uid }

                    if (partnerId != null) {
                        firestore.collection("users").document(partnerId)
                            .addSnapshotListener { partnerSnapshot, partnerError ->
                                if (partnerError != null) {
                                    loadFailed = true
                                    emitCurrent()
                                    return@addSnapshotListener
                                }
                                partner = partnerSnapshot?.takeIf { it.exists() }?.let {
                                    HomeUserProfile(
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

        val memoryListener = firestore.collection("couples").document(coupleId)
            .collection("memories")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadFailed = true
                    emitCurrent()
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull { it.getBoolean("hidden") != true }
                latestMemory = doc?.let {
                    HomeLatestMemory(
                        caption = it.getString("caption"),
                        photoUrl = it.getString("photoUrl")
                    )
                }
                emitCurrent()
            }

        val dailyListener = firestore.collection("couples").document(coupleId)
            .collection("dailyChallenges")
            .whereEqualTo("status", "completed")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadFailed = true
                    emitCurrent()
                    return@addSnapshotListener
                }
                dailyCompleted = snapshot?.size() ?: 0
                emitCurrent()
            }

        val gamesListener = firestore.collection("couples").document(coupleId)
            .collection("gameSessions")
            .whereEqualTo("status", "revealed")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadFailed = true
                    emitCurrent()
                    return@addSnapshotListener
                }
                gamesRevealed = snapshot?.size() ?: 0
                emitCurrent()
            }

        awaitClose {
            coupleListener.remove()
            memoryListener.remove()
            dailyListener.remove()
            gamesListener.remove()
        }
    }

    private fun <T> flowOfSingle(value: T): Flow<T> = callbackFlow {
        trySend(value)
        awaitClose { }
    }
}