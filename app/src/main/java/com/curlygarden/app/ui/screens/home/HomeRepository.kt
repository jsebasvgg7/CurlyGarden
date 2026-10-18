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

/**
 * Datos minimos de un perfil (users/{uid}) que necesita el Home:
 * nombre y foto para los avatares superpuestos.
 */
data class HomeUserProfile(
    val uid: String,
    val name: String,
    val photoUrl: String?
)

/**
 * Ultimo recuerdo guardado (couples/{coupleId}/memories), usado en la
 * card "nuestro espacio". Nulo si la pareja aun no tiene ningun memory.
 */
data class HomeLatestMemory(
    val caption: String?,
    val photoUrl: String?
)

/**
 * Snapshot combinado de todo lo que el Home necesita de Firestore:
 * racha de la pareja (streakLog + timezone, ver StreakCalculator), ambos
 * perfiles (para los avatares) y el ultimo memory.
 *
 * streakDays ya no se lee de Firestore: la racha es un calculo derivado
 * de streakLog (Opcion A, ver documento maestro seccion Racha), no un
 * contador persistido.
 */
data class HomeFirestoreData(
    val streakLog: Map<String, Boolean>,
    val timezone: String,
    val myProfile: HomeUserProfile?,
    val partnerProfile: HomeUserProfile?,
    val latestMemory: HomeLatestMemory?
)

/**
 * Repositorio de solo-lectura para la pantalla Home. Sigue el mismo patron
 * que AuthRepository: no gestiona estado propio, expone Flows que el
 * ViewModel combina y traduce a HomeUiState.
 *
 * Enfoque elegido (ver conversacion): no se persiste el coupleId en ningun
 * estado de sesion aparte. Cada vez que se observa el Home se lee primero
 * users/{uid} (barato, un solo doc) para sacar el coupleId, y a partir de
 * ahi se encadenan los listeners de couples/{coupleId} y de la subcoleccion
 * memories. Esto evita duplicar la fuente de verdad que ya definen las
 * reglas de Firestore (users/{uid}.coupleId).
 */
class HomeRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Observa todo lo necesario para el Home del usuario actualmente
     * logueado. Emite un nuevo HomeFirestoreData cada vez que cambia la
     * racha, cualquiera de los dos perfiles, o el ultimo memory.
     *
     * Si el usuario no tiene coupleId todavia (no ha completado el
     * onboarding de pareja de la seccion 2.4), streakDays queda en 0 y los
     * perfiles/memory quedan en null; la UI decide que mostrar en ese caso.
     */
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

    // --- Perfil propio ---

    private fun observeUserProfile(uid: String): Flow<HomeUserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
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

    // --- Datos de la pareja (racha, companero, ultimo memory) ---

    private fun combineCoupleData(
        coupleId: String,
        my: HomeUserProfile?
    ): Flow<HomeFirestoreData> = callbackFlow {
        var streakLog: Map<String, Boolean> = emptyMap()
        var timezone = DEFAULT_TIMEZONE
        var partner: HomeUserProfile? = null
        var latestMemory: HomeLatestMemory? = null
        var partnerListenerRegistered = false

        fun emitCurrent() {
            trySend(HomeFirestoreData(streakLog, timezone, my, partner, latestMemory))
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

                if (!partnerListenerRegistered) {
                    partnerListenerRegistered = true
                    val user1Id = snapshot.getString("user1Id")
                    val user2Id = snapshot.getString("user2Id")
                    val partnerId = listOfNotNull(user1Id, user2Id)
                        .firstOrNull { it.isNotBlank() && it != my?.uid }

                    if (partnerId != null) {
                        firestore.collection("users").document(partnerId)
                            .addSnapshotListener { partnerSnapshot, _ ->
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
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                val doc = snapshot?.documents?.firstOrNull()
                latestMemory = doc?.let {
                    HomeLatestMemory(
                        caption = it.getString("caption"),
                        photoUrl = it.getString("photoUrl")
                    )
                }
                emitCurrent()
            }

        awaitClose {
            coupleListener.remove()
            memoryListener.remove()
        }
    }

    /** Flow que emite un unico valor fijo, usado cuando aun no hay coupleId. */
    private fun <T> flowOfSingle(value: T): Flow<T> = callbackFlow {
        trySend(value)
        awaitClose { }
    }
}