package com.curlygarden.app.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.curlygarden.app.R

/**
 * webClientId: viene del Client ID tipo "Web application" generado por
 * Firebase al activar el proveedor Google en Authentication > Sign-in method.
 * Se guarda en res/values/strings.xml (o google-services.json ya lo expone
 * via R.string.default_web_client_id si usas el plugin de Google Services).
 *
 * IMPORTANTE: se pasa el `context` tal cual (contexto de la Activity que
 * crea este factory, ver MainActivity con LocalContext.current), SIN
 * convertirlo a applicationContext. Credential Manager necesita un
 * contexto de tipo Activity para poder lanzar el selector de cuentas de
 * Google visualmente; con applicationContext falla con
 * "Failed to launch the selector UI" sin mostrar ningun error visible,
 * y AuthRepository lo terminaba interpretando como si el usuario hubiera
 * cancelado el flujo.
 */
class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = AuthRepository(
            context = context,
            webClientId = context.getString(R.string.default_web_client_id)
        )
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(repository) as T
    }
}