package com.curlygarden.app.ui.screens.talk

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.theme.CurlyColors

private const val DATE_GAP_MS = 60L * 60L * 1000L

@Composable
fun TalkHistoryScreen(
    viewModel: TalkViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.historyMessages.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val myUid = state.session?.myUid
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                TalkCircleButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "atras",
                        tint = CurlyColors.DarkTextPrimary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CurlyColors.DarkSurface, RoundedCornerShape(28.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TalkAvatar(
                        profile = state.session?.myProfile,
                        fallbackColor = CurlyColors.DarkLavenderPrimary,
                        size = 52.dp
                    )
                    Box(Modifier.padding(start = 0.dp)) {
                        TalkAvatar(
                            profile = state.session?.partnerProfile,
                            fallbackColor = CurlyColors.DarkLavenderLight,
                            size = 52.dp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Historial",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Todo lo que se han dejado",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))

                if (messages.isEmpty()) {
                    val hasPartner = state.session?.partnerUid != null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (hasPartner) {
                                "Todavia no hay nada. Las notas que se dejen apareceran aqui."
                            } else {
                                "Cuando vincules a alguien, aqui aparecera todo lo que se dejen."
                            },
                            color = CurlyColors.DarkTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(CurlyColors.DarkBackground.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { Spacer(Modifier.height(6.dp)) }
                        items(messages.size) { i ->
                            val message = messages[i]
                            val previous = messages.getOrNull(i - 1)
                            val showDate = previous == null ||
                                message.timestamp == Long.MAX_VALUE ||
                                (previous.timestamp != Long.MAX_VALUE &&
                                    message.timestamp - previous.timestamp > DATE_GAP_MS)
                            if (showDate) {
                                Text(
                                    formatHistoryDate(message.timestamp),
                                    color = CurlyColors.DarkTextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }
                            HistoryBubble(message = message, isMine = message.senderId == myUid)
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBubble(message: TalkMessage, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(
                    color = if (isMine) CurlyColors.DarkLavenderLight.copy(alpha = 0.75f)
                    else CurlyColors.DarkSurfaceElevated,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isMine) 20.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 20.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (!message.replyPreview.isNullOrBlank()) {
                Text(
                    "↩ ${message.replyPreview}",
                    color = if (isMine) CurlyColors.DarkBackground.copy(alpha = 0.6f)
                    else CurlyColors.DarkLavenderLight,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                message.text,
                color = if (isMine) CurlyColors.DarkBackground else CurlyColors.DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
