package com.curlygarden.app.ui.screens.draw

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class DrawStep {
    TOPIC,
    CANVAS,
    REVEAL,
    HISTORY
}

@Composable
fun DrawFlow(
    onExit: () -> Unit,
    viewModel: DrawViewModel = viewModel(factory = DrawViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableStateOf(DrawStep.TOPIC) }

    LaunchedEffect(uiState.status) {
        if (step == DrawStep.HISTORY) return@LaunchedEffect
        step = when (uiState.status) {
            DrawRoundStatus.WAITING_TOPICS -> DrawStep.TOPIC
            DrawRoundStatus.DRAWING -> DrawStep.CANVAS
            DrawRoundStatus.REVEALED -> DrawStep.REVEAL
            DrawRoundStatus.LOADING -> step
        }
    }

    when (step) {
        DrawStep.TOPIC -> DrawTopicScreen(
            onBackClicked = onExit,
            onTopicsReady = { step = DrawStep.CANVAS },
            onHistoryClicked = { step = DrawStep.HISTORY },
            viewModel = viewModel
        )
        DrawStep.CANVAS -> DrawCanvasScreen(
            onBackClicked = onExit,
            onRevealReady = { step = DrawStep.REVEAL },
            viewModel = viewModel
        )
        DrawStep.REVEAL -> DrawRevealScreen(
            onBackClicked = onExit,
            viewModel = viewModel
        )
        DrawStep.HISTORY -> DrawHistoryScreen(
            onBackClicked = { step = DrawStep.TOPIC },
            viewModel = viewModel
        )
    }
}