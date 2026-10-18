package com.curlygarden.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de login. `navigateTo` es un evento de una sola vez
 * (se consume y se limpia) para que un recomposition no vuelva a disparar
 * la navegacion.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateTo: AuthDestination? = null
)

enum class AuthDestination {
    CreateProfile,   // primera vez: falta nombre/foto en Firestore
    CoupleOnboarding // ya tiene perfil, falta vincularse o ya tiene pareja -> Home
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Util para MainActivity: permite saltarse Splash+Login si Firebase ya
     * tiene una sesion activa de una apertura anterior de la app.
     */
    fun currentUserExists(): Boolean = repository.currentUser != null

    fun onSignInWithGoogleClicked() {
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = repository.signInWithGoogle()) {
                is SignInResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        navigateTo = if (result.isNewUser) {
                            AuthDestination.CreateProfile
                        } else {
                            AuthDestination.CoupleOnboarding
                        }
                    )
                }
                is SignInResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                SignInResult.Cancelled -> {
                    // El usuario cerro el selector de cuentas: no es un error,
                    // simplemente volvemos al estado inicial sin mensaje.
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigateTo = null)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}