package com.curlygarden.app.ui.screens.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class MemoriesRoute { INDEX, ALBUM, DETAIL }

data class MemoriesNav(
    val route: MemoriesRoute = MemoriesRoute.INDEX,
    val albumId: String? = null,
    val showHidden: Boolean = false,
    val memoryId: String? = null
)

data class MemoriesUiState(
    val isLoading: Boolean = true,
    val session: MemoriesSession? = null,
    val data: MemoriesData = MemoriesData(),
    val loadFailed: Boolean = false,
    val route: MemoriesRoute = MemoriesRoute.INDEX,
    val albumId: String? = null,
    val showHidden: Boolean = false,
    val memoryId: String? = null,
    val isWorking: Boolean = false
) {
    val hasPartner: Boolean
        get() = session?.partnerUid != null

    val album: Album?
        get() = albumId?.let { data.album(it) }

    val shownMemories: List<Memory>
        get() = data.memoriesFor(albumId, showHidden)

    val hiddenCount: Int
        get() = data.hiddenCount(albumId)

    val currentMemory: Memory?
        get() = memoryId?.let { data.memory(it) }

    fun profileFor(uid: String): MemoriesProfile? = when (uid) {
        session?.myUid -> session?.myProfile
        else -> session?.partnerProfile
    }
}

private data class MemoriesLocal(
    val nav: MemoriesNav,
    val working: Boolean,
    val failed: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class MemoriesViewModel(
    private val repository: MemoriesRepository = MemoriesRepository()
) : ViewModel() {

    private val sessionState = MutableStateFlow<MemoriesSession?>(null)
    private val sessionLoaded = MutableStateFlow(false)
    private val nav = MutableStateFlow(MemoriesNav())
    private val isWorking = MutableStateFlow(false)
    private val loadFailed = MutableStateFlow(false)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.value = runCatching { repository.resolveSession() }.getOrNull()
            sessionLoaded.value = true
        }
    }

    private fun <T> Flow<List<T>>.safe(): Flow<List<T>> = catch {
        loadFailed.value = true
        emit(emptyList())
    }

    private val dataFlow: Flow<MemoriesData> = sessionState.flatMapLatest { session ->
        if (session == null) {
            flowOf(MemoriesData())
        } else {
            combine(
                repository.observeMemories(session.coupleId).safe(),
                repository.observeAlbums(session.coupleId).safe()
            ) { memories, albums -> MemoriesData(memories = memories, albums = albums) }
        }
    }

    private val localFlow: Flow<MemoriesLocal> = combine(
        nav, isWorking, loadFailed
    ) { n, working, failed -> MemoriesLocal(n, working, failed) }

    val uiState: StateFlow<MemoriesUiState> = combine(
        sessionLoaded, sessionState, dataFlow, localFlow
    ) { loaded, session, data, local ->
        MemoriesUiState(
            isLoading = !loaded,
            session = session,
            data = data,
            loadFailed = local.failed,
            route = local.nav.route,
            albumId = local.nav.albumId,
            showHidden = local.nav.showHidden,
            memoryId = local.nav.memoryId,
            isWorking = local.working
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MemoriesUiState())

    fun openAlbum(albumId: String?) {
        nav.value = MemoriesNav(route = MemoriesRoute.ALBUM, albumId = albumId)
    }

    fun setShowHidden(show: Boolean) {
        nav.value = nav.value.copy(showHidden = show)
    }

    fun openMemory(memoryId: String) {
        nav.value = nav.value.copy(route = MemoriesRoute.DETAIL, memoryId = memoryId)
    }

    fun back(): Boolean {
        val current = nav.value
        return when {
            current.route == MemoriesRoute.DETAIL -> {
                nav.value = current.copy(route = MemoriesRoute.ALBUM, memoryId = null)
                true
            }
            current.route == MemoriesRoute.ALBUM && current.showHidden -> {
                nav.value = current.copy(showHidden = false)
                true
            }
            current.route == MemoriesRoute.ALBUM -> {
                nav.value = MemoriesNav()
                true
            }
            else -> false
        }
    }

    fun createAlbum(name: String, colorIndex: Int, onSuccess: (String) -> Unit = {}) {
        if (name.isBlank()) return
        work("No se pudo crear el álbum", onSuccess) { session ->
            repository.createAlbum(session.coupleId, name, colorIndex)
        }
    }

    fun createMemory(
        photo: MemoryPhoto,
        caption: String,
        dateMillis: Long,
        albumId: String?,
        onSuccess: (String) -> Unit = {}
    ) {
        work("No se pudo guardar el recuerdo", onSuccess) { session ->
            repository.createMemory(session.coupleId, photo, caption, dateMillis, albumId)
        }
    }

    fun setHidden(memory: Memory, hidden: Boolean, onSuccess: () -> Unit = {}) {
        val session = sessionState.value ?: return
        viewModelScope.launch {
            runCatching { repository.setHidden(session.coupleId, memory.id, hidden) }
                .onSuccess { onSuccess() }
                .onFailure {
                    _errorMessage.value =
                        if (hidden) "No se pudo ocultar el recuerdo" else "No se pudo restaurar el recuerdo"
                }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private fun <T> work(
        failure: String,
        onSuccess: (T) -> Unit,
        block: suspend (MemoriesSession) -> T
    ) {
        val session = sessionState.value ?: return
        if (isWorking.value) return
        viewModelScope.launch {
            isWorking.value = true
            runCatching { block(session) }
                .onSuccess { onSuccess(it) }
                .onFailure { _errorMessage.value = messageFor(it, failure) }
            isWorking.value = false
        }
    }

    private fun messageFor(error: Throwable, fallback: String): String =
        (error as? PhotoTooLargeException)?.message ?: fallback
}
