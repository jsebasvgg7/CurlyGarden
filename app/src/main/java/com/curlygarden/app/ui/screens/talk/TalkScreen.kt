package com.curlygarden.app.ui.screens.talk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.theme.CurlyColors

private enum class TalkStep { CARD, HISTORY }

@Composable
fun TalkScreen(onBack: () -> Unit = {}) {
    val viewModel: TalkViewModel = viewModel(factory = TalkViewModelFactory())
    var step by rememberSaveable { mutableStateOf(TalkStep.CARD) }

    when (step) {
        TalkStep.CARD -> TalkCardScreen(
            viewModel = viewModel,
            onBack = onBack,
            onOpenHistory = { step = TalkStep.HISTORY }
        )
        TalkStep.HISTORY -> TalkHistoryScreen(
            viewModel = viewModel,
            onBack = { step = TalkStep.CARD }
        )
    }
}

@Composable
private fun TalkCardScreen(
    viewModel: TalkViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var composer by remember { mutableStateOf<ComposerRequest?>(null) }

    val current = state.current
    LaunchedEffect(current?.id) {
        if (current != null) viewModel.onMessageVisible(current)
    }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TalkCircleButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "atras",
                        tint = CurlyColors.DarkTextPrimary
                    )
                }
                TalkCircleButton(onClick = onOpenHistory) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = "historial",
                        tint = CurlyColors.DarkTextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        color = CurlyColors.DarkLavenderPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    state.session == null -> NoPartnerNotice()
                    state.feed is TalkFeed.Error -> ErrorNotice((state.feed as TalkFeed.Error).message)
                    current == null -> EmptyCard(
                        onWrite = { composer = ComposerRequest(replyTo = null) }
                    )
                    else -> AnimatedContent(
                        targetState = current,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "talk-card"
                    ) { message ->
                        val isMine = message.senderId == state.session?.myUid
                        MessageCardView(
                            message = message,
                            author = state.profileFor(message.senderId),
                            isMine = isMine,
                            onReply = { composer = ComposerRequest(replyTo = message) }
                        )
                    }
                }
            }

            if (current != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TalkCircleButton(
                        onClick = viewModel::goOlder,
                        enabled = state.canGoOlder,
                        size = 46.dp
                    ) {
                        Icon(
                            Icons.Outlined.Undo,
                            contentDescription = "anterior",
                            tint = CurlyColors.DarkTextPrimary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    TalkCircleButton(
                        onClick = viewModel::goNewer,
                        enabled = state.canGoNewer,
                        size = 46.dp
                    ) {
                        Icon(
                            Icons.Outlined.Redo,
                            contentDescription = "siguiente",
                            tint = CurlyColors.DarkTextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TalkCircleButton(
                        onClick = viewModel::goLatest,
                        enabled = state.canGoNewer,
                        size = 52.dp
                    ) {
                        Icon(
                            Icons.Outlined.KeyboardDoubleArrowUp,
                            contentDescription = "mas reciente",
                            tint = CurlyColors.DarkTextPrimary
                        )
                    }
                    if (state.hasUnseenNewer) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .background(CurlyColors.DarkLavenderPrimary, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    onClick = { composer = ComposerRequest(replyTo = null) },
                    enabled = state.session != null,
                    shape = RoundedCornerShape(26.dp),
                    color = CurlyColors.DarkSurfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Publica algo",
                            color = CurlyColors.DarkTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        composer?.let { request ->
            ComposerDialog(
                replyTo = request.replyTo,
                isSending = state.isSending,
                onDismiss = { composer = null },
                onSend = { text ->
                    viewModel.send(text, request.replyTo)
                    composer = null
                }
            )
        }

        errorMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2500)
                viewModel.dismissError()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CurlyColors.DarkSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(message, color = CurlyColors.DarkTextPrimary, fontSize = 13.sp)
            }
        }
    }
}

private data class ComposerRequest(val replyTo: TalkMessage?)

@Composable
private fun MessageCardView(
    message: TalkMessage,
    author: TalkProfile?,
    isMine: Boolean,
    onReply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TalkAvatar(
            profile = author,
            fallbackColor = if (isMine) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkLavenderLight,
            size = 104.dp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = author?.name?.trim()?.takeIf { it.isNotBlank() }?.substringBefore(" ") ?: "",
            color = CurlyColors.DarkTextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(26.dp))

        val backColor = CurlyColors.DarkBorder.copy(alpha = 0.55f)
        val midColor = CurlyColors.DarkSurface
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .drawBehind {
                    val corner = CornerRadius(28.dp.toPx())
                    rotate(degrees = -4f, pivot = center) {
                        drawRoundRect(color = backColor, cornerRadius = corner)
                    }
                    rotate(degrees = 3f, pivot = center) {
                        drawRoundRect(color = midColor, cornerRadius = corner)
                    }
                }
                .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Icon(
                Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = CurlyColors.DarkTextMuted,
                modifier = Modifier.size(26.dp)
            )
            if (!message.replyPreview.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Reply,
                        contentDescription = null,
                        tint = CurlyColors.DarkLavenderLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        message.replyPreview,
                        color = CurlyColors.DarkLavenderLight,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                message.text,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 21.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isMine) {
                    Surface(
                        onClick = onReply,
                        shape = RoundedCornerShape(20.dp),
                        color = CurlyColors.DarkLavenderPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Responder",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        if (message.seen) "Visto" else "Enviado",
                        color = CurlyColors.DarkTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            formatCardDate(message.timestamp),
            color = CurlyColors.DarkTextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyCard(onWrite: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = CurlyColors.DarkTextMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Todavia no hay nada por aqui",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Deja la primera nota para el otro.",
            color = CurlyColors.DarkTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onWrite,
            shape = RoundedCornerShape(20.dp),
            color = CurlyColors.DarkLavenderPrimary
        ) {
            Text(
                "Escribir",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun NoPartnerNotice() {
    Text(
        "Todavia no tienes a nadie vinculado. Invita a alguien para poder dejar mensajes.",
        color = CurlyColors.DarkTextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    )
}

@Composable
private fun ErrorNotice(message: String) {
    Text(
        message,
        color = CurlyColors.DarkTextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    )
}
