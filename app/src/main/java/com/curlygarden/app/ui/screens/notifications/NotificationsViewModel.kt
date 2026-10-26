package com.curlygarden.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.data.network.rejectIfOffline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.curlygarden.app.ui.screens.talk.TalkProfile

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val session: NotificationsSession? = null,
    val inbox: List<InboxNotification> = emptyList(),
    val settings: NotificationSettings = NotificationSettings(),
    val turn: List<TurnItem> = emptyList(),
    val loadFailed: Boolean = false,
    val tab: NotificationTab = NotificationTab.TURN
) {
    val unreadCount: Int
        get() = inbox.count { !it.read }

    fun nameFor(uid: String): String = when (uid) {
        session?.myUid -> session?.myProfile?.name
        else -> session?.partnerProfile?.name
    }?.takeIf { it.isNotBlank() } ?: NOTIFICATIONS_FALLBACK_NAME

    fun profileFor(uid: String): TalkProfile? {
        val profile = if (uid == session?.myUid) session?.myProfile else session?.partnerProfile
        return profile?.let { TalkProfile(it.uid, it.name, it.photoUrl) }
    }

    fun photoFor(uid: String): String = when (uid) {
        session?.myUid -> session?.myProfile?.photoUrl
        else -> session?.partnerProfile?.photoUrl
    }.orEmpty()
}

private data class NotificationsData(
    val inbox: List<InboxNotification>,
    val settings: NotificationSettings,
    val turn: List<TurnItem>
)

private data class NotificationsLocal(
    val tab: NotificationTab,
    val failed: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    private val repository: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val sessionState = MutableStateFlow<NotificationsSession?>(null)
    private val sessionLoaded = MutableStateFlow(false)
    private val tab = MutableStateFlow(NotificationTab.TURN)
    private val loadFailed = MutableStateFlow(false)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.value = runCatching { repository.resolveSession() }.getOrNull()
            sessionLoaded.value = true
        }
    }

    private val inboxFlow: Flow<List<InboxNotification>> = sessionState.flatMapLatest { session ->
        if (session == null) {
            flowOf(emptyList())
        } else {
            repository.observeInbox(session.myUid).catch {
                loadFailed.value = true
                emit(emptyList())
            }
        }
    }

    private val settingsFlow: Flow<NotificationSettings> = sessionState.flatMapLatest { session ->
        if (session == null) {
            flowOf(NotificationSettings())
        } else {
            repository.observeSettings(session.myUid).catch {
                loadFailed.value = true
                emit(NotificationSettings())
            }
        }
    }

    private val turnFlow: Flow<List<TurnItem>> = sessionState.flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else repository.observeTurn(session)
    }

    private val dataFlow: Flow<NotificationsData> = combine(
        inboxFlow, settingsFlow, turnFlow
    ) { inbox, settings, turn -> NotificationsData(inbox, settings, turn) }

    private val localFlow: Flow<NotificationsLocal> = combine(tab, loadFailed) { t, failed ->
        NotificationsLocal(t, failed)
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        sessionLoaded, sessionState, dataFlow, localFlow
    ) { loaded, session, data, local ->
        NotificationsUiState(
            isLoading = !loaded,
            session = session,
            inbox = data.inbox,
            settings = data.settings,
            turn = data.turn,
            loadFailed = local.failed,
            tab = local.tab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUiState())

    fun selectTab(value: NotificationTab) {
        tab.value = value
    }

    fun markRead(notificationId: String) {
        val session = sessionState.value ?: return
        val target = uiState.value.inbox.firstOrNull { it.id == notificationId } ?: return
        if (target.read) return
        viewModelScope.launch {
            runCatching { repository.markRead(session.myUid, notificationId) }
        }
    }

    fun markAllRead() {
        val session = sessionState.value ?: return
        val unread = uiState.value.inbox.filter { !it.read }.map { it.id }
        if (unread.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.markAllRead(session.myUid, unread) }
        }
    }

    fun setPushEnabled(enabled: Boolean) {
        if (_errorMessage.rejectIfOffline()) return
        viewModelScope.launch {
            runCatching { repository.setPushEnabled(enabled) }
                .onFailure { _errorMessage.value = "No se pudo guardar el ajuste" }
        }
    }

    fun setKindEnabled(kind: NotificationKind, enabled: Boolean) {
        if (_errorMessage.rejectIfOffline()) return
        viewModelScope.launch {
            runCatching { repository.setKindEnabled(kind, enabled) }
                .onFailure { _errorMessage.value = "No se pudo guardar el ajuste" }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
