package com.curlygarden.app.ui.screens.memories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun MemoriesScreen(onBack: () -> Unit) {
    val viewModel: MemoriesViewModel = viewModel(factory = MemoriesViewModelFactory())
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showAlbumDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var memoryAlbumId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage, showAlbumDialog, showMemoryDialog) {
        if (errorMessage != null && !showAlbumDialog && !showMemoryDialog) {
            delay(2500)
            viewModel.dismissError()
        }
    }

    BackHandler { if (!viewModel.back()) onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.route) {
            MemoriesRoute.INDEX -> MemoriesIndexScreen(
                state = state,
                onBack = onBack,
                onNewAlbum = {
                    viewModel.dismissError()
                    showAlbumDialog = true
                },
                onOpenAlbum = viewModel::openAlbum,
                onAddToAlbum = { albumId ->
                    viewModel.dismissError()
                    memoryAlbumId = albumId
                    showMemoryDialog = true
                }
            )
            MemoriesRoute.ALBUM -> MemoriesAlbumScreen(
                state = state,
                onBack = { viewModel.back() },
                onOpenMemory = viewModel::openMemory,
                onAdd = {
                    viewModel.dismissError()
                    memoryAlbumId = state.albumId
                    showMemoryDialog = true
                },
                onHome = onBack
            )
            MemoriesRoute.DETAIL -> MemoriesDetailScreen(
                state = state,
                onBack = { viewModel.back() },
                onToggleHidden = { memory ->
                    viewModel.setHidden(memory, !memory.hidden) { viewModel.back() }
                },
                onDelete = { memory ->
                    viewModel.deleteMemory(memory) { viewModel.back() }
                }
            )
        }
        val tab = state.tab
        val showNav = tab != null &&
            !state.isLoading &&
            state.hasPartner &&
            !state.loadFailed &&
            !(state.route == MemoriesRoute.INDEX && state.data.albums.isEmpty())
        if (tab != null && showNav) {
            MemoriesNavPill(
                selected = tab,
                onSelect = viewModel::selectTab,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, bottom = 34.dp)
            )
        }
        if (showAlbumDialog) {
            MemoriesAlbumDialog(
                isWorking = state.isWorking,
                error = errorMessage,
                onCreate = { name, colorIndex ->
                    viewModel.createAlbum(name, colorIndex) { showAlbumDialog = false }
                },
                onDismiss = {
                    viewModel.dismissError()
                    showAlbumDialog = false
                }
            )
        } else if (showMemoryDialog) {
            MemoriesMemoryDialog(
                albums = state.data.albums,
                initialAlbumId = memoryAlbumId,
                isWorking = state.isWorking,
                error = errorMessage,
                onSave = { photo, caption, dateMillis, albumId ->
                    viewModel.createMemory(photo, caption, dateMillis, albumId) {
                        showMemoryDialog = false
                    }
                },
                onDismiss = {
                    viewModel.dismissError()
                    showMemoryDialog = false
                }
            )
        } else {
            errorMessage?.let { message ->
                MemoriesToast(message, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
