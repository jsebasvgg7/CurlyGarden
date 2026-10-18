package com.curlygarden.app.startup

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * A donde debe caer la app al arrancar, resuelto revisando Firestore y no
 * solo el estado de Firebase Auth. Esto cierra el hueco que ya advertia el
 * documento maestro (seccion 2.5.1 / historial de decisiones): isNewUser
 * de Firebase Auth no cubre el caso de alguien que cerro la app a medio
 * completar su perfil o a medio vincularse con su pareja.
 */
enum class StartupDestination {
    Splash,           // no hay sesion activa
    CreateProfile,    // hay sesion pero users/{uid} no existe todavia
    CoupleOnboarding, // hay perfil pero coupleId esta vacio/ausente
    Home              // perfil y coupleId completos
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
