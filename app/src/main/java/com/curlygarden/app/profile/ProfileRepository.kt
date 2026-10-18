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

/**
 * Datos de Google disponibles de inmediato tras el login, usados para
 * precargar el formulario de CreateProfileScreen (nombre y foto), tal
 * como describe la seccion 2.5 del documento maestro: "nombre y foto de
 * perfil llegan listos desde la cuenta de Google, pero editables".
 */
data class GoogleProfileHint(
    val suggestedName: String,
    val suggestedPhotoUrl: String?
)

/**
 * Perfil minimo (users/{uid}) que necesita la pantalla de Perfil ("me"):
 * mismo shape que HomeUserProfile, pero definido aca para no acoplar el
 * paquete profile al paquete ui.screens.home.
 */
data class MyProfileData(
    val uid: String,
    val name: String,
    val photoUrl: String?
)

/**
 * Snapshot combinado que necesita ProfileScreen: mi perfil y el de mi
 * pareja (para la card "Estado de Conexion"). partnerProfile es null si
 * todavia no hay coupleId (no se completo el onboarding de la seccion 2.4).
 *
 * streakDays ya no se lee de Firestore, se calcula (ver StreakCalculator).
 * linkedAtMillis / createdAtMillis alimentan la card de "tiempo de
 * vinculo": linkedAtMillis es null si la pareja se creo antes de que
 * redeemInviteCode empezara a escribir linkedAt, y en ese caso
 * createdAtMillis sirve de respaldo (ver documento maestro).
 */
data class ProfileFirestoreData(
    val myProfile: MyProfileData?,
    val partnerProfile: MyProfileData?,
    val streakLog: Map<String, Boolean> = emptyMap(),
    val timezone: String = DEFAULT_TIMEZONE,
    val linkedAtMillis: Long? = null,
    val createdAtMillis: Long? = null
)

/**
 * Repositorio del perfil propio (users/{uid}). No maneja coupleId: eso es
 * responsabilidad exclusiva de las Cloud Functions
 * createInviteCode/redeemInviteCode (ver CoupleRepository), que corren con
 * Admin SDK y se saltan las reglas de Firestore a proposito para evitar
 * que el cliente falsifique ese campo.
 *
 * observeProfile() sigue el mismo patron que HomeRepository.observeHome():
 * primero escucha users/{uid} propio, y a partir de su coupleId encadena
 * el listener del perfil de la pareja.
 */
class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Lee displayName/photoUrl directamente del FirebaseUser (vienen del
     * proveedor Google, sin necesidad de ninguna llamada extra a Firestore
     * ni a la API de Google). Nulo si por alguna razon no hay sesion activa.
     */
    fun getGoogleHint(): GoogleProfileHint? {
        val user = auth.currentUser ?: return null
        return GoogleProfileHint(
            suggestedName = user.displayName ?: "",
            suggestedPhotoUrl = user.photoUrl?.toString()
        )
    }

    /**
     * Crea (o sobreescribe) el documento users/{uid} con el perfil
     * completado por la persona. No incluye coupleId en absoluto: las
     * reglas de Firestore (create de couples/{coupleId}) exigen
     * myProfile().coupleId == null, y un campo ausente en Firestore se lee
     * como null en las reglas de seguridad, asi que omitirlo aqui (en vez
     * de escribir "") es lo correcto. coupleId solo lo escribe despues la
     * Cloud Function createInviteCode/redeemInviteCode via Admin SDK.
     */
    suspend fun saveProfile(name: String, photoUrl: String?) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No hay sesion activa para guardar el perfil.")

        val data = mutableMapOf<String, Any>("name" to name)
        photoUrl?.let { data["photoUrl"] = it }

        firestore.collection("users").document(uid)
            .set(data)
            .await()
    }

    /**
     * Observa mi perfil y, si ya tengo coupleId, el de mi pareja. Usado por
     * ProfileScreen para la card "Estado de Conexion" y el nombre/foto de
     * la cabecera. Si no hay sesion activa, emite un unico valor vacio.
     */
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
        var partnerListenerRegistered = false

        fun emitCurrent() {
            trySend(
                ProfileFirestoreData(
                    my, partner, streakLog, timezone, linkedAtMillis, createdAtMillis
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

        awaitClose { coupleListener.remove() }
    }

    private fun <T> flowOfSingle(value: T): Flow<T> = callbackFlow {
        trySend(value)
        awaitClose { }
    }
}