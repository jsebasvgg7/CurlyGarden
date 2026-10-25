package com.curlygarden.app.ui.screens.memories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun MemoriesScreen(onBack: () -> Unit) {
    val viewModel: MemoriesViewModel = viewModel(factory = MemoriesViewModelFactory())
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
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
                onOpenAlbum = viewModel::openAlbum
            )
            MemoriesRoute.ALBUM -> MemoriesAlbumScreen(
                state = state,
                onBack = { viewModel.back() },
                onOpenMemory = viewModel::openMemory,
                onToggleHidden = viewModel::setShowHidden
            )
            MemoriesRoute.DETAIL -> MemoriesDetailScreen(
                state = state,
                onBack = { viewModel.back() },
                onToggleHidden = { memory ->
                    viewModel.setHidden(memory, !memory.hidden) { viewModel.back() }
                }
            )
        }
        errorMessage?.let { message ->
            MemoriesToast(message, Modifier.align(Alignment.BottomCenter))
        }
    }
}
