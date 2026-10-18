package com.curlygarden.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.data.streak.calculateStreak
import com.curlygarden.app.data.streak.weekDaysCompleted
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val streakDays: Int = 0,
    val weekDaysCompleted: Set<java.time.LocalDate> = emptySet(),
    val myProfile: HomeUserProfile? = null,
    val partnerProfile: HomeUserProfile? = null,
    val latestMemory: HomeLatestMemory? = null
)

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeHome()
        .map { data ->
            HomeUiState(
                isLoading = false,
                streakDays = calculateStreak(data.streakLog, data.timezone),
                weekDaysCompleted = weekDaysCompleted(data.streakLog, data.timezone),
                myProfile = data.myProfile,
                partnerProfile = data.partnerProfile,
                latestMemory = data.latestMemory
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )
}