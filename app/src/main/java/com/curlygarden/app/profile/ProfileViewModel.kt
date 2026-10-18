package com.curlygarden.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.couple.CoupleActionResult
import com.curlygarden.app.couple.CoupleRepository
import com.curlygarden.app.data.streak.calculateStreak
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de UI de la pantalla de Perfil ("me"). ProfileScreen no conoce
 * Firestore ni el repositorio: solo consume este estado, igual que
 * HomeScreen consume HomeUiState.
 *
 * isUnlinking/unlinkError/unlinked son especificos del modal "Estado de
 * Conexion": permiten mostrar progreso mientras corre la Cloud Function
 * unlinkCouple y un mensaje de error si falla, sin mezclar ese estado
 * transitorio con la carga en tiempo real de myProfile/partnerProfile.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val myProfile: MyProfileData? = null,
    val partnerProfile: MyProfileData? = null,
    val streakDays: Int = 0,
    val daysLinked: Int = 0,
    val isUnlinking: Boolean = false,
    val unlinkError: String? = null,
    val unlinked: Boolean = false
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val coupleRepository: CoupleRepository = CoupleRepository()
) : ViewModel() {

    private val firestoreState: StateFlow<ProfileFirestoreData> =
        repository.observeProfile()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ProfileFirestoreData(null, null)
            )

    private val _unlinkState = MutableStateFlow(UnlinkState())

    val uiState: StateFlow<ProfileUiState> = combine(
        firestoreState, _unlinkState
    ) { firestoreData, unlink ->
        // linkedAt es el momento del canje del codigo (lo escribe
        // redeemInviteCode); las parejas creadas antes de ese cambio no lo
        // tienen, asi que createdAt sirve de respaldo (ver documento maestro,
        // seccion Racha / tiempo de vinculo).
        val linkStartMillis = firestoreData.linkedAtMillis ?: firestoreData.createdAtMillis
        val daysLinked = linkStartMillis?.let {
            ChronoUnit.DAYS.between(Instant.ofEpochMilli(it), Instant.now()).toInt()
        } ?: 0

        ProfileUiState(
            isLoading = false,
            myProfile = firestoreData.myProfile,
            partnerProfile = firestoreData.partnerProfile,
            streakDays = calculateStreak(firestoreData.streakLog, firestoreData.timezone),
            daysLinked = daysLinked,
            isUnlinking = unlink.isUnlinking,
            unlinkError = unlink.error,
            unlinked = unlink.unlinked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

    /**
     * Desvincula la pareja actual (boton "Desvincular" dentro del modal de
     * "Estado de Conexion"). Al terminar con exito, unlinked=true; la UI
     * usa eso para cerrar el modal (el listener de observeProfile ya
     * limpiara partnerProfile solo, en cuanto Firestore refleje el cambio).
     */
    fun onUnlinkConfirmed() {
        if (_unlinkState.value.isUnlinking) return

        _unlinkState.value = _unlinkState.value.copy(isUnlinking = true, error = null)

        viewModelScope.launch {
            when (val result = coupleRepository.unlinkCouple()) {
                is CoupleActionResult.Success -> {
                    _unlinkState.value = UnlinkState(unlinked = true)
                }
                is CoupleActionResult.Error -> {
                    _unlinkState.value = _unlinkState.value.copy(
                        isUnlinking = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun dismissUnlinkError() {
        _unlinkState.value = _unlinkState.value.copy(error = null)
    }

    /** Se llama despues de que la UI ya reacciono a unlinked=true (cerro el modal). */
    fun onUnlinkHandled() {
        _unlinkState.value = UnlinkState()
    }

    private data class UnlinkState(
        val isUnlinking: Boolean = false,
        val error: String? = null,
        val unlinked: Boolean = false
    )
}