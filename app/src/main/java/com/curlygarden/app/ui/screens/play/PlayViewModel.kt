package com.curlygarden.app.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.data.streak.todayKeyFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PlayRoute {
    INDEX,
    DAILY,
    THIS_OR_THAT,
    KNOW_ME,
    ELEVEN
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
    val eleven: Map<String, ElevenStats> = emptyMap(),
    val elevenClock: ElevenClock = ElevenClock(),
    val reveal: GameReveal? = null,
    val isWorking: Boolean = false,
    val loadFailed: Boolean = false
) {
    val dailyState: DailyCardState
        get() {
            val myUid = session?.myUid ?: return DailyCardState.PENDING
            val challenge = daily ?: return DailyCardState.PENDING
            return when {
                challenge.isCompleted -> DailyCardState.COMPLETED
                myUid in challenge.answeredBy -> DailyCardState.WAITING
                else -> DailyCardState.PENDING
            }
        }

    val elevenPodium: List<ElevenPodiumEntry>
        get() {
            val current = session ?: return emptyList()
            val mine = eleven[current.myUid]
            val entries = listOfNotNull(
                ElevenPodiumEntry(current.myProfile, mine?.count ?: 0, mine?.pressedAtMillis ?: 0L),
                current.partnerProfile?.let { partner ->
                    val theirs = eleven[partner.uid]
                    ElevenPodiumEntry(partner, theirs?.count ?: 0, theirs?.pressedAtMillis ?: 0L)
                }
            )
            return entries.sortedWith(
                compareByDescending<ElevenPodiumEntry> { it.count }
                    .thenByDescending { it.lastPressedMillis }
            )
        }

    fun activeSession(type: PlayGameType): GameSession? =
        sessions
            .filter { it.type == type && it.status != SessionStatus.REVEALED }
            .minByOrNull { it.createdAtMillis }

    fun lastRevealed(type: PlayGameType): GameSession? =
        sessions
            .filter { it.type == type && it.status == SessionStatus.REVEALED }
            .maxByOrNull { it.createdAtMillis }

    fun sessionToShow(type: PlayGameType): GameSession? =
        activeSession(type) ?: lastRevealed(type)

    fun gameState(type: PlayGameType): GameCardState {
        val myUid = session?.myUid.orEmpty()
        val active = activeSession(type)
            ?: return if (lastRevealed(type) != null) GameCardState.SEE_RESULT else GameCardState.NEW_GAME
        return when (active.status) {
            SessionStatus.ANSWERING ->
                if (myUid in active.answeredBy) GameCardState.WAITING else GameCardState.NEW_GAME
            SessionStatus.GUESSING ->
                if (myUid in active.guessedBy) GameCardState.WAITING else GameCardState.YOUR_TURN_TO_GUESS
            SessionStatus.REVEALED -> GameCardState.SEE_RESULT
        }
    }

    fun profileFor(uid: String): PlayProfile? = when (uid) {
        session?.myUid -> session?.myProfile
        else -> session?.partnerProfile
    }
}

private data class PlayData(
    val daily: DailyChallenge? = null,
    val sessions: List<GameSession> = emptyList(),
    val stats: PlayStats = PlayStats(),
    val eleven: Map<String, ElevenStats> = emptyMap()
)

private data class PlayLocal(
    val route: PlayRoute = PlayRoute.INDEX,
    val dateKey: String = "",
    val isWorking: Boolean = false,
    val dailyResponses: DailyResponses = DailyResponses(),
    val reveal: GameReveal? = null,
    val loadFailed: Boolean = false
)

private fun dailyPlan(dateKey: String): Pair<DailyType, Int> {
    val epochDay = runCatching { LocalDate.parse(dateKey).toEpochDay() }.getOrNull()
        ?: return DailyType.QUESTION to 0
    val type = if (epochDay % 2L == 0L) DailyType.QUESTION else DailyType.ACTION
    val size = if (type == DailyType.QUESTION) PlayBank.dailyQuestions.size else PlayBank.dailyActions.size
    val index = if (size == 0) 0 else ((epochDay / 2L) % size).toInt()
    return type to index
}

private fun routeFor(type: PlayGameType): PlayRoute = when (type) {
    PlayGameType.THIS_OR_THAT -> PlayRoute.THIS_OR_THAT
    PlayGameType.KNOW_ME -> PlayRoute.KNOW_ME
}

private fun typeFor(route: PlayRoute): PlayGameType? = when (route) {
    PlayRoute.THIS_OR_THAT -> PlayGameType.THIS_OR_THAT
    PlayRoute.KNOW_ME -> PlayGameType.KNOW_ME
    else -> null
}

private fun bankSize(type: PlayGameType): Int = when (type) {
    PlayGameType.THIS_OR_THAT -> PlayBank.thisOrThat.size
    PlayGameType.KNOW_ME -> PlayBank.knowMe.size
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayViewModel(
    private val repository: PlayRepository = PlayRepository()
) : ViewModel() {

    private val sessionState = MutableStateFlow<PlaySession?>(null)
    private val sessionLoaded = MutableStateFlow(false)
    private val local = MutableStateFlow(PlayLocal())
    private val elevenClock = MutableStateFlow(ElevenClock())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private fun markLoadFailed() {
        local.update { it.copy(loadFailed = true) }
    }

    private val data: StateFlow<PlayData> = combine(
        sessionState,
        local.map { it.dateKey }.distinctUntilChanged()
    ) { session, dateKey -> session to dateKey }
        .flatMapLatest { (session, dateKey) ->
            if (session == null || dateKey.isBlank()) {
                flowOf(PlayData())
            } else {
                combine(
                    repository.observeDaily(session.coupleId, dateKey)
                        .catch { markLoadFailed(); emit(null) },
                    repository.observeSessions(session.coupleId)
                        .catch { markLoadFailed(); emit(emptyList()) },
                    repository.observeStats(session.coupleId, session.myUid)
                        .catch { markLoadFailed(); emit(PlayStats()) },
                    repository.observeEleven(
                        session.coupleId,
                        listOfNotNull(session.myUid, session.partnerUid)
                    ).catch { markLoadFailed(); emit(emptyMap()) }
                ) { daily, sessions, stats, eleven -> PlayData(daily, sessions, stats, eleven) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayData())

    val uiState: StateFlow<PlayUiState> = combine(
        sessionLoaded, sessionState, data, local, elevenClock
    ) { loaded, session, current, state, clock ->
        val plan = dailyPlan(state.dateKey)
        PlayUiState(
            isLoading = !loaded,
            session = session,
            route = state.route,
            dateKey = state.dateKey,
            dailyType = current.daily?.type ?: plan.first,
            dailyBankIndex = current.daily?.bankIndex ?: plan.second,
            daily = current.daily,
            dailyResponses = state.dailyResponses,
            sessions = current.sessions,
            stats = current.stats,
            eleven = current.eleven,
            elevenClock = clock,
            reveal = state.reveal,
            isWorking = state.isWorking,
            loadFailed = state.loadFailed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayUiState())

    init {
        viewModelScope.launch {
            val session = runCatching { repository.resolveSession() }.getOrNull()
            if (session != null) {
                local.update { it.copy(dateKey = todayKeyFor(session.timezone)) }
            }
            sessionState.value = session
            sessionLoaded.value = true
        }
        viewModelScope.launch {
            data.map { state -> state.daily?.let { Triple(it.dateKey, it.isCompleted, it.answeredBy) } }
                .distinctUntilChanged()
                .collect { refreshResponses() }
        }
        viewModelScope.launch {
            combine(
                local.map { it.route }.distinctUntilChanged(),
                sessionState,
                data.map { state -> sessionState.value?.let { state.eleven[it.myUid]?.lastDay } }
                    .distinctUntilChanged()
            ) { route, session, lastDay -> Triple(route, session, lastDay) }
                .flatMapLatest { (route, session, lastDay) ->
                    if (route != PlayRoute.ELEVEN || session == null) {
                        emptyFlow<ElevenClock>()
                    } else {
                        flow {
                            while (true) {
                                emit(computeElevenClock(session.timezone, lastDay))
                                delay(1000L - System.currentTimeMillis() % 1000L)
                            }
                        }
                    }
                }
                .collect { clock -> elevenClock.value = clock }
        }
    }

    private suspend fun refreshResponses() {
        val session = sessionState.value
        val daily = data.value.daily
        if (session == null || daily == null || session.myUid !in daily.answeredBy) {
            local.update { it.copy(dailyResponses = DailyResponses()) }
            return
        }
        runCatching { repository.loadDailyResponses(session, daily.dateKey, daily.isCompleted) }
            .onSuccess { responses -> local.update { it.copy(dailyResponses = responses) } }
            .onFailure { _errorMessage.value = "No se pudieron cargar las respuestas" }
    }

    fun open(route: PlayRoute) {
        val session = sessionState.value
        local.update {
            it.copy(
                route = route,
                reveal = null,
                dateKey = session?.let { s -> todayKeyFor(s.timezone) } ?: it.dateKey
            )
        }
    }

    fun back(): Boolean {
        if (local.value.route == PlayRoute.INDEX) return false
        local.update { it.copy(route = PlayRoute.INDEX, reveal = null) }
        return true
    }

    fun submitDailyText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > PLAY_ANSWER_MAX) return
        submitDaily(DailyType.QUESTION, trimmed)
    }

    fun markActionDone(text: String) {
        val trimmed = text.trim()
        if (trimmed.length > PLAY_ANSWER_MAX) return
        submitDaily(DailyType.ACTION, trimmed.ifEmpty { null })
    }

    private fun submitDaily(expected: DailyType, text: String?) {
        val state = uiState.value
        val session = state.session ?: return
        if (session.partnerUid == null || state.dateKey.isBlank()) return
        if (state.dailyType != expected || state.dailyState != DailyCardState.PENDING) return
        work("No se pudo enviar tu respuesta") {
            repository.submitDaily(it, state.dateKey, state.dailyType, state.dailyBankIndex, text)
        }
    }

    fun pressEleven() {
        val state = uiState.value
        val session = state.session ?: return
        if (state.route != PlayRoute.ELEVEN) return
        val lastDay = state.eleven[session.myUid]?.lastDay
        if (computeElevenClock(session.timezone, lastDay).phase != ElevenPhase.OPEN) return
        val dateKey = todayKeyFor(session.timezone)
        work("No se pudo registrar tu 11:11") { repository.pressEleven(it, dateKey) }
    }

    fun startGame(type: PlayGameType) {
        val state = uiState.value
        val session = state.session ?: return
        if (session.partnerUid == null) return
        if (state.activeSession(type) != null) {
            open(routeFor(type))
            return
        }
        val total = bankSize(type)
        if (total < PLAY_PICKS_PER_GAME) {
            _errorMessage.value = "Todavía no hay preguntas para este juego"
            return
        }
        val used = state.sessions.filter { it.type == type }.flatMap { it.questionIds }.toSet()
        val fresh = (0 until total).filter { it !in used }
        val pool = if (fresh.size >= PLAY_PICKS_PER_GAME) fresh else (0 until total).toList()
        val questionIds = pool.shuffled().take(PLAY_PICKS_PER_GAME)
        work("No se pudo crear la partida", onSuccess = { open(routeFor(type)) }) {
            repository.createSession(it, type, questionIds)
        }
    }

    fun submitAnswers(picks: List<Int>) {
        val state = uiState.value
        val type = typeFor(state.route) ?: return
        val active = state.activeSession(type) ?: return
        val myUid = state.session?.myUid ?: return
        if (active.status != SessionStatus.ANSWERING || myUid in active.answeredBy) return
        work("No se pudieron enviar tus respuestas") {
            repository.submitAnswers(it, active.id, picks)
        }
    }

    fun submitGuesses(picks: List<Int>) {
        val state = uiState.value
        val type = typeFor(state.route) ?: return
        val active = state.activeSession(type) ?: return
        val myUid = state.session?.myUid ?: return
        if (active.status != SessionStatus.GUESSING || myUid in active.guessedBy) return
        work("No se pudieron enviar tus respuestas") {
            repository.submitGuesses(it, active.id, picks)
        }
    }

    fun loadReveal(sessionId: String) {
        val session = sessionState.value ?: return
        val game = data.value.sessions.firstOrNull { it.id == sessionId } ?: return
        if (game.status != SessionStatus.REVEALED) return
        viewModelScope.launch {
            runCatching { repository.loadReveal(session, sessionId) }
                .onSuccess { reveal ->
                    local.update { it.copy(reveal = reveal) }
                    awardGuessPointsIfNeeded(session, game, reveal)
                }
                .onFailure { _errorMessage.value = "No se pudo cargar el resultado" }
        }
    }

    private suspend fun awardGuessPointsIfNeeded(
        session: PlaySession,
        game: GameSession,
        reveal: GameReveal
    ) {
        if (game.type != PlayGameType.KNOW_ME || session.myUid in game.scored) return
        val partnerUid = session.partnerUid ?: return
        val mine = reveal.guesses[session.myUid] ?: return
        val real = reveal.answers[partnerUid] ?: return
        val hits = mine.indices.count { it < real.size && mine[it] == real[it] }
        runCatching { repository.awardGuessPoints(session, game.id, hits * POINTS_GUESS) }
            .onFailure { _errorMessage.value = "No se pudieron guardar tus puntos" }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun work(
        failure: String,
        onSuccess: () -> Unit = {},
        block: suspend (PlaySession) -> Unit
    ) {
        val session = sessionState.value ?: return
        if (local.value.isWorking) return
        viewModelScope.launch {
            local.update { it.copy(isWorking = true) }
            runCatching { block(session) }
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = failure }
            local.update { it.copy(isWorking = false) }
        }
    }
}
