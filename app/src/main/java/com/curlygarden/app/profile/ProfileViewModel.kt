package com.curlygarden.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.couple.CoupleActionResult
import com.curlygarden.app.couple.CoupleRepository
import com.curlygarden.app.data.streak.calculateStreak
import com.curlygarden.app.ui.screens.play.PlayStats
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val myProfile: MyProfileData? = null,
    val partnerProfile: MyProfileData? = null,
    val streakDays: Int = 0,
    val daysLinked: Int = 0,
    val level: Int = 1,
    val levelProgress: Int = 0,
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
        val linkStartMillis = firestoreData.linkedAtMillis ?: firestoreData.createdAtMillis
        val daysLinked = linkStartMillis?.let {
            ChronoUnit.DAYS.between(Instant.ofEpochMilli(it), Instant.now()).toInt()
        } ?: 0

        val stats = PlayStats(points = firestoreData.points)

        ProfileUiState(
            isLoading = false,
            myProfile = firestoreData.myProfile,
            partnerProfile = firestoreData.partnerProfile,
            streakDays = calculateStreak(firestoreData.streakLog, firestoreData.timezone),
            daysLinked = daysLinked,
            level = stats.level,
            levelProgress = stats.progress,
            isUnlinking = unlink.isUnlinking,
            unlinkError = unlink.error,
            unlinked = unlink.unlinked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

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

    fun onUnlinkHandled() {
        _unlinkState.value = UnlinkState()
    }

    private data class UnlinkState(
        val isUnlinking: Boolean = false,
        val error: String? = null,
        val unlinked: Boolean = false
    )
}