package com.curlygarden.app.ui.screens.play

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlayRoute {
    INDEX,
    DAILY,
    THIS_OR_THAT,
    KNOW_ME
}

data class PlayUiState(
    val isLoading: Boolean = true,
    val session: PlaySession? = null,
    val route: PlayRoute = PlayRoute.INDEX,
    val dateKey: String = "",
    val dailyType: DailyType = DailyType.QUESTION,
    val dailyBankIndex: Int = 0,
    val daily: DailyChallenge? = null,
    val dailyResponses: DailyResponses = DailyResponses(),
    val sessions: List<GameSession> = emptyList(),
    val stats: PlayStats = PlayStats(),
    val reveal: GameReveal? = null,
    val isWorking: Boolean = false,
    val loadFailed: Boolean = false
) {
    val dailyState: DailyCardState
        get() = DailyCardState.PENDING

    fun activeSession(type: PlayGameType): GameSession? = null

    fun gameState(type: PlayGameType): GameCardState = GameCardState.NEW_GAME

    fun profileFor(uid: String): PlayProfile? = when (uid) {
        session?.myUid -> session?.myProfile
        else -> session?.partnerProfile
    }
}

class PlayViewModel(
    private val repository: PlayRepository = PlayRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun open(route: PlayRoute) {
    }

    fun back(): Boolean = false

    fun submitDailyText(text: String) {
    }

    fun markActionDone() {
    }

    fun startGame(type: PlayGameType) {
    }

    fun submitAnswers(picks: List<Int>) {
    }

    fun submitGuesses(picks: List<Int>) {
    }

    fun loadReveal(sessionId: String) {
    }

    fun clearError() {
    }
}
