package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.components.CurlyToast
import kotlinx.coroutines.delay

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
    val errorMessage by viewModel.errorMessage.collectAsState()
    var step by remember { mutableStateOf(DrawStep.TOPIC) }

    LaunchedEffect(errorMessage, step) {
        if (errorMessage != null && step != DrawStep.HISTORY) {
            delay(2500)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.status) {
        if (step == DrawStep.HISTORY) return@LaunchedEffect
        step = when (uiState.status) {
            DrawRoundStatus.WAITING_TOPICS -> DrawStep.TOPIC
            DrawRoundStatus.DRAWING -> DrawStep.CANVAS
            DrawRoundStatus.REVEALED -> DrawStep.REVEAL
            DrawRoundStatus.LOADING -> step
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
        errorMessage?.takeIf { step != DrawStep.HISTORY }?.let {
            CurlyToast(message = it, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
