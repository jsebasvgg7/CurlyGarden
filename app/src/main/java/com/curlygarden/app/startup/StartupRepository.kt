package com.curlygarden.app.startup

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class StartupDestination {
    Splash,
    CreateProfile,
    CoupleOnboarding,
    Home
}

class StartupRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun resolveStartupDestination(): StartupDestination {
        val uid = auth.currentUser?.uid ?: return StartupDestination.Splash

        val snapshot = firestore.collection("users").document(uid).get().await()
        if (!snapshot.exists()) return StartupDestination.CreateProfile

        val coupleId = snapshot.getString("coupleId")
        return if (coupleId.isNullOrBlank()) {
            StartupDestination.CoupleOnboarding
        } else {
            StartupDestination.Home
        }
    }
}
