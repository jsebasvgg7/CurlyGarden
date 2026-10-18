package com.curlygarden.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateTo: AuthDestination? = null
)

enum class AuthDestination {
    CreateProfile,
    CoupleOnboarding
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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