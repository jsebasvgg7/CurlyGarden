package com.curlygarden.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64

/**
 * Resultado de intentar iniciar sesion, usado por el ViewModel para decidir
 * a donde navegar: si el usuario ya tiene perfil en Firestore (users/{uid})
 * va directo a home/onboarding de pareja; si es la primera vez, va a
 * completar su perfil (nombre, foto) antes de seguir con el flujo de
 * invitacion de la seccion 2.4 del documento maestro.
 */
sealed class SignInResult {
    data class Success(val isNewUser: Boolean) : SignInResult()
    data class Error(val message: String) : SignInResult()
    object Cancelled : SignInResult()
}

/**
 * Maneja el login con Google usando Credential Manager (reemplazo moderno
 * de GoogleSignInClient, que Google marco como deprecado) + Firebase Auth.
 *
 * IMPORTANTE: `context` debe ser un contexto de tipo Activity (no
 * applicationContext). Credential Manager necesita una Activity para
 * poder lanzar visualmente el selector de cuentas de Google; con
 * applicationContext falla con "Failed to launch the selector UI" sin
 * ningun error visible en la UI, y este repositorio terminaba
 * interpretandolo como si el usuario hubiera cancelado el flujo
 * (ver AuthViewModelFactory).
 *
 * No se maneja el envio de la invitacion dentro de esta clase (eso sigue
 * pasando por fuera de la app, por WhatsApp/SMS, segun la seccion 2.4);
 * esta clase solo resuelve "quien es este usuario" y si ya existe su
 * perfil en Firestore.
 */
class AuthRepository(
    private val context: Context,
    private val webClientId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Lanza el selector de cuentas de Google del sistema, intercambia el
     * id token por credenciales de Firebase, y revisa si ya existe un
     * documento users/{uid} para saber si es un usuario nuevo.
     *
     * filterByAuthorizedAccounts = false permite que aparezcan todas las
     * cuentas Google del dispositivo (no solo las ya usadas antes con esta
     * app), lo cual tiene sentido para el primer login.
     */
    suspend fun signInWithGoogle(): SignInResult {
        val nonce = generateNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Error("Tipo de credencial inesperado.")
            }

            val googleIdTokenCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                return SignInResult.Error("No se pudo leer la credencial de Google.")
            }

            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken, null
            )
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val isNewFirebaseUser = authResult.additionalUserInfo?.isNewUser ?: false

            val uid = authResult.user?.uid
                ?: return SignInResult.Error("No se pudo obtener el usuario autenticado.")

            // isNewUser de Firebase Auth indica si es la primera vez que se
            // autentica con este proveedor, pero lo que de verdad nos importa
            // para navegar es si ya completo su perfil en Firestore (podria
            // haber cerrado la app a medio onboarding).
            val hasProfile = if (isNewFirebaseUser) {
                false
            } else {
                firestore.collection("users").document(uid).get().await().exists()
            }

            SignInResult.Success(isNewUser = !hasProfile)
        } catch (e: GetCredentialException) {
            // El usuario cerro el selector de cuentas sin elegir ninguna.
            SignInResult.Cancelled
        } catch (e: Exception) {
            SignInResult.Error(e.message ?: "Error desconocido al iniciar sesion.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Nonce aleatorio para el flujo de Google Sign-In, tal como lo pide la
     * documentacion de Credential Manager para mitigar ataques de replay.
     */
    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}