package com.curlygarden.app.ui.screens.imagine

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

data class ImagineNav(
    val category: ImagineCategory? = null,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val shareFilter: ShareFilter = ShareFilter.ALL,
    val index: Int = 0
)

data class ImagineUiState(
    val isLoading: Boolean = true,
    val session: ImagineSession? = null,
    val data: ImagineData = ImagineData(),
    val loadFailed: Boolean = false,
    val category: ImagineCategory? = null,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val shareFilter: ShareFilter = ShareFilter.ALL,
    val index: Int = 0,
    val isWorking: Boolean = false,
    val capsuleEntries: Map<String, CapsuleEntries> = emptyMap()
) {
    val visibleItems: List<ImagineItem>
        get() = when (category) {
            ImagineCategory.LISTS -> data.lists.filterLists(statusFilter)
            ImagineCategory.GOALS -> data.goals.filterGoals(statusFilter)
            ImagineCategory.IDEAS -> data.ideas.filterIdeas(shareFilter)
            ImagineCategory.CAPSULES -> data.capsules.filterCapsules(shareFilter)
            null -> emptyList()
        }

    val current: ImagineItem?
        get() = visibleItems.getOrNull(index)

    val canGoOlder: Boolean
        get() = index < visibleItems.lastIndex

    val canGoNewer: Boolean
        get() = index > 0

    fun profileFor(uid: String): ImagineProfile? = when (uid) {
        session?.myUid -> session?.myProfile
        else -> session?.partnerProfile
    }
}

private data class ImagineLocal(
    val nav: ImagineNav,
    val working: Boolean,
    val entries: Map<String, CapsuleEntries>,
    val failed: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class ImagineViewModel(
    private val repository: ImagineRepository = ImagineRepository()
) : ViewModel() {

    private val sessionState = MutableStateFlow<ImagineSession?>(null)
    private val sessionLoaded = MutableStateFlow(false)
    private val nav = MutableStateFlow(ImagineNav())
    private val isWorking = MutableStateFlow(false)
    private val entriesState = MutableStateFlow<Map<String, CapsuleEntries>>(emptyMap())
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

    private val dataFlow: Flow<ImagineData> = sessionState.flatMapLatest { session ->
        if (session == null) {
            flowOf(ImagineData())
        } else {
            combine(
                repository.observeIdeas(session.coupleId).safe(),
                repository.observeLists(session.coupleId).safe(),
                repository.observeGoals(session.coupleId).safe(),
                repository.observeCapsules(session.coupleId).safe()
            ) { ideas, lists, goals, capsules ->
                ImagineData(ideas = ideas, lists = lists, goals = goals, capsules = capsules)
            }
        }
    }

    private val localFlow: Flow<ImagineLocal> = combine(
        nav, isWorking, entriesState, loadFailed
    ) { n, working, entries, failed -> ImagineLocal(n, working, entries, failed) }

    val uiState: StateFlow<ImagineUiState> = combine(
        sessionLoaded, sessionState, dataFlow, localFlow
    ) { loaded, session, data, local ->
        val base = ImagineUiState(
            isLoading = !loaded,
            session = session,
            data = data,
            loadFailed = local.failed,
            category = local.nav.category,
            statusFilter = local.nav.statusFilter,
            shareFilter = local.nav.shareFilter,
            index = local.nav.index,
            isWorking = local.working,
            capsuleEntries = local.entries
        )
        base.copy(index = local.nav.index.coerceIn(0, (base.visibleItems.size - 1).coerceAtLeast(0)))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImagineUiState())

    fun selectCategory(category: ImagineCategory?) {
        nav.value = ImagineNav(category = category)
    }

    fun setStatusFilter(filter: StatusFilter) {
        nav.value = nav.value.copy(statusFilter = filter, index = 0)
    }

    fun setShareFilter(filter: ShareFilter) {
        nav.value = nav.value.copy(shareFilter = filter, index = 0)
    }

    fun goOlder() {
        val state = uiState.value
        if (state.canGoOlder) nav.value = nav.value.copy(index = state.index + 1)
    }

    fun goNewer() {
        val state = uiState.value
        if (state.canGoNewer) nav.value = nav.value.copy(index = state.index - 1)
    }

    fun goLatest() {
        nav.value = nav.value.copy(index = 0)
    }

    fun loadCapsuleEntries(capsule: ImagineCapsule) {
        val session = sessionState.value ?: return
        viewModelScope.launch {
            runCatching { repository.loadCapsuleEntries(session, capsule) }
                .onSuccess { entriesState.value = entriesState.value + (capsule.id to it) }
                .onFailure { _errorMessage.value = "No se pudo cargar la cápsula" }
        }
    }

    fun createIdea(
        title: String,
        content: String,
        attachment: ImagineAttachment?,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || content.isBlank()) return
        work("No se pudo guardar la idea", onSuccess) { session ->
            repository.createIdea(session.coupleId, title, content, attachment)
        }
    }

    fun updateIdea(
        idea: ImagineIdea,
        title: String,
        content: String,
        newAttachment: ImagineAttachment?,
        removeAttachment: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || content.isBlank()) return
        work("No se pudo guardar la idea", onSuccess) { session ->
            repository.updateIdea(session.coupleId, idea.id, title, content, newAttachment, removeAttachment)
        }
    }

    fun createList(title: String, items: List<ListItem>, onSuccess: () -> Unit = {}) {
        if (title.isBlank()) return
        work("No se pudo guardar la lista", onSuccess) { session ->
            repository.createList(session.coupleId, title, items)
        }
    }

    fun updateList(list: ImagineList, title: String, items: List<ListItem>, onSuccess: () -> Unit = {}) {
        if (title.isBlank()) return
        work("No se pudo guardar la lista", onSuccess) { session ->
            repository.updateList(session.coupleId, list.id, title, items)
        }
    }

    fun toggleListItem(list: ImagineList, itemIndex: Int) {
        if (itemIndex !in list.items.indices) return
        val items = list.items.mapIndexed { i, item ->
            if (i == itemIndex) item.copy(done = !item.done) else item
        }
        quick("No se pudo actualizar la lista") { session ->
            repository.updateList(session.coupleId, list.id, list.title, items)
        }
    }

    fun createGoal(title: String, content: String, targetDateText: String, onSuccess: () -> Unit = {}) {
        if (title.isBlank() || targetDateText.isBlank()) return
        work("No se pudo guardar la meta", onSuccess) { session ->
            repository.createGoal(session.coupleId, title, content, targetDateText)
        }
    }

    fun updateGoal(
        goal: ImagineGoal,
        title: String,
        content: String,
        targetDateText: String,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || targetDateText.isBlank()) return
        work("No se pudo guardar la meta", onSuccess) { session ->
            repository.updateGoal(session.coupleId, goal.id, title, content, targetDateText)
        }
    }

    fun setGoalFinished(goal: ImagineGoal, finished: Boolean) {
        val status = if (finished) ImagineStatus.FINISHED else ImagineStatus.ACTIVE
        quick("No se pudo actualizar la meta") { session ->
            repository.setGoalStatus(session.coupleId, goal.id, status)
        }
    }

    fun createCapsule(
        title: String,
        mode: CapsuleMode,
        unlockDate: Long,
        text: String,
        attachment: ImagineAttachment?,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || text.isBlank()) return
        if (unlockDate <= System.currentTimeMillis()) {
            _errorMessage.value = "La fecha de apertura debe ser futura"
            return
        }
        work("No se pudo guardar la cápsula", onSuccess) { session ->
            repository.createCapsule(session.coupleId, title, mode, unlockDate, text, attachment)
        }
    }

    fun contributeToCapsule(
        capsule: ImagineCapsule,
        text: String,
        attachment: ImagineAttachment?,
        onSuccess: () -> Unit = {}
    ) {
        val session = sessionState.value ?: return
        if (text.isBlank() || !capsule.canContribute(session.myUid)) return
        work("No se pudo guardar tu parte", {
            loadCapsuleEntries(capsule)
            onSuccess()
        }) { current ->
            repository.contributeToCapsule(current.coupleId, capsule.id, text, attachment)
        }
    }

    fun join(item: ImagineShareable) {
        val category = categoryOf(item) ?: return
        quick("No se pudo unir") { session ->
            repository.join(session.coupleId, category, item.id)
        }
    }

    fun toggleLocked(item: ImagineShareable) {
        val session = sessionState.value ?: return
        if (item.createdBy != session.myUid) return
        val category = categoryOf(item) ?: return
        quick("No se pudo cambiar el candado") { current ->
            repository.setLocked(current.coupleId, category, item.id, !item.locked)
        }
    }

    fun toggleLike(item: ImagineItem) {
        val session = sessionState.value ?: return
        val category = categoryOf(item) ?: return
        val liked = !item.isLikedBy(session.myUid)
        quick("No se pudo actualizar el me gusta") { current ->
            repository.setLiked(current.coupleId, category, item.id, liked)
        }
    }

    fun delete(item: ImagineShareable) {
        val session = sessionState.value ?: return
        if (item.createdBy != session.myUid) return
        val category = categoryOf(item) ?: return
        quick("No se pudo borrar") { current ->
            repository.delete(current.coupleId, category, item.id)
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private fun categoryOf(item: ImagineItem): ImagineCategory? = when (item) {
        is ImagineIdea -> ImagineCategory.IDEAS
        is ImagineList -> ImagineCategory.LISTS
        is ImagineGoal -> ImagineCategory.GOALS
        is ImagineCapsule -> ImagineCategory.CAPSULES
        else -> null
    }

    private fun work(
        failure: String,
        onSuccess: () -> Unit,
        block: suspend (ImagineSession) -> Unit
    ) {
        val session = sessionState.value ?: return
        if (isWorking.value) return
        viewModelScope.launch {
            isWorking.value = true
            runCatching { block(session) }
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = messageFor(it, failure) }
            isWorking.value = false
        }
    }

    private fun quick(failure: String, block: suspend (ImagineSession) -> Unit) {
        val session = sessionState.value ?: return
        viewModelScope.launch {
            runCatching { block(session) }
                .onFailure { _errorMessage.value = messageFor(it, failure) }
        }
    }

    private fun messageFor(error: Throwable, fallback: String): String =
        (error as? AttachmentTooLargeException)?.message ?: fallback
}
