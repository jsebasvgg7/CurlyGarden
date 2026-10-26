package com.curlygarden.app.ui.screens.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.curlygarden.app.data.network.rejectIfOffline

data class TalkUiState(
    val isLoading: Boolean = true,
    val session: TalkSession? = null,
    val feed: TalkFeed = TalkFeed.Loading,
    val index: Int = 0,
    val hasUnseenNewer: Boolean = false,
    val isSending: Boolean = false
) {
    val messages: List<TalkMessage>
        get() = (feed as? TalkFeed.Ready)?.messages.orEmpty()

    val current: TalkMessage?
        get() = messages.getOrNull(index)

    val canGoOlder: Boolean
        get() = index < messages.lastIndex || (feed as? TalkFeed.Ready)?.hasMore == true

    val canGoNewer: Boolean
        get() = index > 0

    fun profileFor(uid: String): TalkProfile? = when (uid) {
        session?.myUid -> session?.myProfile
        else -> session?.partnerProfile
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TalkViewModel(
    private val repository: TalkRepository = TalkRepository()
) : ViewModel() {

    private val sessionState = MutableStateFlow<TalkSession?>(null)
    private val sessionLoaded = MutableStateFlow(false)
    private val limit = MutableStateFlow(TALK_PAGE_SIZE)
    private val index = MutableStateFlow(0)
    private val isSending = MutableStateFlow(false)
    private val seenRequested = HashSet<String>()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.value = runCatching { repository.resolveSession() }.getOrNull()
            sessionLoaded.value = true
        }
    }

    private val feedFlow = sessionState.flatMapLatest { session ->
        if (session == null) {
            flowOf<TalkFeed>(TalkFeed.Ready(emptyList(), false))
        } else {
            limit.flatMapLatest { currentLimit ->
                repository.observeLatest(session.coupleId, currentLimit)
                    .map<List<TalkMessage>, TalkFeed> { list ->
                        TalkFeed.Ready(list, hasMore = list.size.toLong() >= currentLimit)
                    }
                    .catch { emit(TalkFeed.Error("No se pudieron cargar los mensajes")) }
            }
        }
    }

    val historyMessages: StateFlow<List<TalkMessage>> = sessionState
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else repository.observeAll(session.coupleId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TalkUiState> = combine(
        sessionLoaded, sessionState, feedFlow, index, isSending
    ) { loaded, session, feed, idx, sending ->
        val messages = (feed as? TalkFeed.Ready)?.messages.orEmpty()
        val safeIndex = idx.coerceIn(0, (messages.size - 1).coerceAtLeast(0))
        val newest = messages.firstOrNull()
        val unseenNewer = session != null &&
            safeIndex > 0 &&
            newest != null &&
            newest.senderId != session.myUid &&
            !newest.seen

        TalkUiState(
            isLoading = !loaded || feed is TalkFeed.Loading,
            session = session,
            feed = feed,
            index = safeIndex,
            hasUnseenNewer = unseenNewer,
            isSending = sending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TalkUiState())

    fun goOlder() {
        val state = uiState.value
        val next = state.index + 1
        when {
            next <= state.messages.lastIndex -> index.value = next
            (state.feed as? TalkFeed.Ready)?.hasMore == true -> {
                limit.value = limit.value + TALK_PAGE_SIZE
            }
        }
    }

    fun goNewer() {
        val next = uiState.value.index - 1
        if (next >= 0) index.value = next
    }

    fun goLatest() {
        index.value = 0
    }

    fun onMessageVisible(message: TalkMessage) {
        val session = sessionState.value ?: return
        if (message.senderId == session.myUid || message.seen) return
        if (!seenRequested.add(message.id)) return
        viewModelScope.launch {
            runCatching { repository.markSeen(session.coupleId, message.id) }
                .onFailure { seenRequested.remove(message.id) }
        }
    }

    fun send(text: String, replyTo: TalkMessage?) {
        val session = sessionState.value ?: return
        if (isSending.value) return
        if (text.isBlank()) return
        if (_errorMessage.rejectIfOffline()) return
        viewModelScope.launch {
            isSending.value = true
            runCatching { repository.send(session.coupleId, text, replyTo) }
                .onSuccess { index.value = 0 }
                .onFailure { _errorMessage.value = "No se pudo enviar el mensaje" }
            isSending.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
