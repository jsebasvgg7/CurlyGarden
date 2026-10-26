package com.curlygarden.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.curlygarden.app.data.streak.calculateStreak
import com.curlygarden.app.data.streak.weekDaysCompleted
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.curlygarden.app.ui.screens.notifications.NotificationsRepository
import kotlinx.coroutines.flow.combine

data class HomeUiState(
    val isLoading: Boolean = true,
    val notificationBadge: Int = 0,
    val streakDays: Int = 0,
    val weekDaysCompleted: Set<java.time.LocalDate> = emptySet(),
    val myProfile: HomeUserProfile? = null,
    val partnerProfile: HomeUserProfile? = null,
    val latestMemory: HomeLatestMemory? = null,
    val gameCount: Int = 0,
    val loadFailed: Boolean = false
)

class HomeViewModel(
    private val repository: HomeRepository,
    private val notifications: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val homeFlow = repository.observeHome().catch {
        emit(HomeFirestoreData(emptyMap(), DEFAULT_TIMEZONE, null, null, null, loadFailed = true))
    }

    private val badgeFlow = notifications.observeBadge().catch { emit(0) }

    val uiState: StateFlow<HomeUiState> = combine(homeFlow, badgeFlow) { data, badge ->
            HomeUiState(
                isLoading = false,
                notificationBadge = badge,
                streakDays = calculateStreak(data.streakLog, data.timezone),
                weekDaysCompleted = weekDaysCompleted(data.streakLog, data.timezone),
                myProfile = data.myProfile,
                partnerProfile = data.partnerProfile,
                latestMemory = data.latestMemory,
                gameCount = data.gameCount,
                loadFailed = data.loadFailed
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )
}