package com.curlygarden.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateProfileUiState(
    val name: String = "",
    val photoUrl: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

class CreateProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateProfileUiState())
    val uiState: StateFlow<CreateProfileUiState> = _uiState.asStateFlow()

    init {
        // Precarga nombre y foto desde la cuenta de Google, editables por
        // la persona antes de guardar (seccion 2.5 del documento maestro).
        repository.getGoogleHint()?.let { hint ->
            _uiState.value = _uiState.value.copy(
                name = hint.suggestedName,
                photoUrl = hint.suggestedPhotoUrl
            )
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(name = newName, errorMessage = null)
    }

    fun onSaveClicked() {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Escribe tu nombre para continuar.")
            return
        }
        if (_uiState.value.isSaving) return

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                repository.saveProfile(name = name, photoUrl = _uiState.value.photoUrl)
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "No se pudo guardar tu perfil."
                )
            }
        }
    }
}
