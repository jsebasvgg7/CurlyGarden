package com.curlygarden.app.ui.screens.draw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curlygarden.app.ui.screens.memories.MemoriesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DrawUiState(
    val isLoading: Boolean = true,
    val coupleId: String? = null,
    val hasPartner: Boolean = true,
    val status: DrawRoundStatus = DrawRoundStatus.LOADING,
    val topicISent: String? = null,
    val topicForMe: String? = null,
    val partnerHasSentTopic: Boolean = false,
    val myStrokes: List<DrawStroke> = emptyList(),
    val partnerStrokes: List<DrawStroke> = emptyList(),
    val myReadyToReveal: Boolean = false,
    val partnerReadyToReveal: Boolean = false,
    val myReadyForNext: Boolean = false,
    val partnerReadyForNext: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DrawViewModel(
    private val repository: DrawRepository = DrawRepository(),
    private val memoriesRepository: MemoriesRepository = MemoriesRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _isSendingTopic = MutableStateFlow(false)
    val isSendingTopic: StateFlow<Boolean> = _isSendingTopic.asStateFlow()

    private val _isStartingNextRound = MutableStateFlow(false)
    val isStartingNextRound: StateFlow<Boolean> = _isStartingNextRound.asStateFlow()

    private val _isSavingDrawing = MutableStateFlow(false)
    val isSavingDrawing: StateFlow<Boolean> = _isSavingDrawing.asStateFlow()

    private val _savingMemoryIds = MutableStateFlow<Set<String>>(emptySet())
    val savingMemoryIds: StateFlow<Set<String>> = _savingMemoryIds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val sessionFlow: StateFlow<Pair<String, DrawSlots>?> = flow {
        val uid = auth.currentUser?.uid
        val coupleId = uid?.let {
            firestore.collection("users").document(it).get().await().getString("coupleId")
        }?.takeIf { it.isNotBlank() }

        if (coupleId == null) {
            emit(null)
        } else {
            emit(coupleId to repository.resolveSlots(coupleId))
        }
    }.catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<DrawUiState> = sessionFlow
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(DrawUiState(isLoading = false, coupleId = null, hasPartner = false))
            } else {
                val (coupleId, slots) = session
                repository.observeRound(coupleId, slots)
                    .map { data ->
                        DrawUiState(
                            isLoading = false,
                            coupleId = coupleId,
                            hasPartner = true,
                            status = if (data.exists) data.status else DrawRoundStatus.WAITING_TOPICS,
                            topicISent = data.topicISent,
                            topicForMe = data.topicForMe,
                            partnerHasSentTopic = data.partnerHasSentTopic,
                            myStrokes = data.myStrokes,
                            partnerStrokes = data.partnerStrokes,
                            myReadyToReveal = data.myReadyToReveal,
                            partnerReadyToReveal = data.partnerReadyToReveal,
                            myReadyForNext = data.myReadyForNext,
                            partnerReadyForNext = data.partnerReadyForNext
                        )
                    }
                    .catch {
                        emit(DrawUiState(isLoading = false, coupleId = coupleId))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DrawUiState()
        )

    val history: StateFlow<List<DrawHistoryEntry>> = sessionFlow
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                repository.observeHistory(session.first, session.second)
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveToMemories(entry: DrawHistoryEntry) {
        val session = sessionFlow.value ?: return
        if (entry.savedToMemoriesByMe || entry.id in _savingMemoryIds.value) return
        if (entry.myStrokes.isEmpty()) {
            _errorMessage.value = "No hay dibujo para guardar"
            return
        }

        viewModelScope.launch {
            _savingMemoryIds.value = _savingMemoryIds.value + entry.id
            runCatching {
                val photo = rasterizeDrawing(entry.myStrokes)
                memoriesRepository.createMemory(
                    coupleId = session.first,
                    photo = photo,
                    caption = entry.topicForMe.orEmpty(),
                    dateMillis = entry.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    albumId = null
                )
                repository.setSavedToMemories(session.first, session.second, entry.id, true)
            }.onFailure { _errorMessage.value = "No se pudo guardar en recuerdos" }
            _savingMemoryIds.value = _savingMemoryIds.value - entry.id
        }
    }

    fun sendTopic(topicForPartner: String) {
        val session = sessionFlow.value ?: return
        val trimmed = topicForPartner.trim()
        if (trimmed.isEmpty() || _isSendingTopic.value) return

        viewModelScope.launch {
            _isSendingTopic.value = true
            runCatching { repository.sendTopic(session.first, session.second, trimmed) }
                .onFailure { _errorMessage.value = "No se pudo enviar el tema" }
            _isSendingTopic.value = false
        }
    }

    fun sendRandomTopic() {
        sendTopic(repository.randomTopic())
    }

    fun submitDrawing(strokes: List<DrawStroke>) {
        val session = sessionFlow.value ?: return
        if (_isSavingDrawing.value) return

        viewModelScope.launch {
            _isSavingDrawing.value = true
            runCatching {
                repository.saveMyStrokes(session.first, session.second, strokes)
                repository.markReadyToReveal(session.first, session.second)
            }.onFailure { _errorMessage.value = "No se pudo guardar el dibujo" }
            _isSavingDrawing.value = false
        }
    }

    fun confirmNextRound() {
        val session = sessionFlow.value ?: return
        if (_isStartingNextRound.value) return
        viewModelScope.launch {
            _isStartingNextRound.value = true
            runCatching { repository.confirmNextRound(session.first, session.second) }
                .onFailure { _errorMessage.value = "No se pudo iniciar otra ronda" }
            _isStartingNextRound.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}