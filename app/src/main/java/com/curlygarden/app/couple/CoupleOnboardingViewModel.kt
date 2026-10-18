package com.curlygarden.app.couple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CoupleOnboardingStep {
    ChooseAction,
    ShowingInviteCode,
    EnteringCode
}

data class CoupleOnboardingUiState(
    val step: CoupleOnboardingStep = CoupleOnboardingStep.ChooseAction,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val generatedCode: String? = null,
    val codeInput: String = "",
    val linked: Boolean = false
)

class CoupleOnboardingViewModel(
    private val repository: CoupleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoupleOnboardingUiState())
    val uiState: StateFlow<CoupleOnboardingUiState> = _uiState.asStateFlow()

    fun onInviteChosen() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            repository.createInviteCode()
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        step = CoupleOnboardingStep.ShowingInviteCode,
                        generatedCode = result.code
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }

    fun onHaveCodeChosen() {
        _uiState.value = _uiState.value.copy(
            step = CoupleOnboardingStep.EnteringCode,
            errorMessage = null
        )
    }

    fun onCodeInputChanged(newCode: String) {
        _uiState.value = _uiState.value.copy(
            codeInput = newCode.uppercase(),
            errorMessage = null
        )
    }

    fun onRedeemClicked() {
        val code = _uiState.value.codeInput.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Escribe el código que te compartieron.")
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = repository.redeemInviteCode(code)) {
                is CoupleActionResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, linked = true)
                }
                is CoupleActionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun onBackToChoose() {
        _uiState.value = _uiState.value.copy(
            step = CoupleOnboardingStep.ChooseAction,
            errorMessage = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
