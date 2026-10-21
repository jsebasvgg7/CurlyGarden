package com.curlygarden.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
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

sealed class SignInResult {
    data class Success(val isNewUser: Boolean) : SignInResult()
    data class Error(val message: String) : SignInResult()
    object Cancelled : SignInResult()
}

class AuthRepository(
    private val context: Context,
    private val webClientId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

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

            val hasProfile = if (isNewFirebaseUser) {
                false
            } else {
                firestore.collection("users").document(uid).get().await().exists()
            }

            SignInResult.Success(isNewUser = !hasProfile)
        } catch (e: GetCredentialException) {
            SignInResult.Cancelled
        } catch (e: Exception) {
            SignInResult.Error(e.message ?: "Error desconocido al iniciar sesion.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun clearCredentialState() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}