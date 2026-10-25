package com.curlygarden.app.ui.screens.memories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff

private val PhotoMaxHeight = 460.dp

@Composable
internal fun MemoriesDetailScreen(
    state: MemoriesUiState,
    onBack: () -> Unit,
    onToggleHidden: (Memory) -> Unit
) {
    val memory = state.currentMemory

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            MemoriesHeader(
                title = "Recuerdo",
                subtitle = null,
                onBack = onBack,
                trailing = {
                    if (memory != null) {
                        TalkCircleButton(onClick = { onToggleHidden(memory) }) {
                            Icon(
                                if (memory.hidden) FeatherIcons.Eye else FeatherIcons.EyeOff,
                                contentDescription = if (memory.hidden) "Restaurar recuerdo" else "Ocultar recuerdo",
                                tint = CurlyColors.DarkTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )

            if (memory == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No se encontró el recuerdo",
                        color = CurlyColors.DarkTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                    MemoryPhoto(memory)
                    Spacer(Modifier.height(16.dp))
                    MemoryInfoCard(
                        memory = memory,
                        album = memory.albumId?.let { state.data.album(it) },
                        author = state.profileFor(memory.createdBy)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryPhoto(memory: Memory) {
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val ratio = memory.aspectRatio.coerceIn(0.5f, 1.8f)
        val photoHeight = (maxWidth / ratio).coerceAtMost(PhotoMaxHeight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(photoHeight)
                .clip(shape)
                .background(CurlyColors.DarkSurface)
                .border(2.dp, Color.White.copy(alpha = 0.9f), shape)
        ) {
            AsyncImage(
                model = memory.photoUrl,
                contentDescription = memory.caption.ifBlank { null },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MemoryInfoCard(memory: Memory, album: Album?, author: MemoriesProfile?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, MemoriesOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (memory.caption.isNotBlank()) {
                Text(
                    memory.caption,
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatMemoryDate(memory.dateMillis),
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                if (album != null) {
                    Text(
                        album.name,
                        color = MemoriesInk,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .background(albumColor(album.colorIndex), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CurlyColors.DarkBorder)
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemoriesAvatar(author, 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    buildAnnotatedString {
                        append("Guardado por ")
                        withStyle(SpanStyle(color = CurlyColors.DarkTextPrimary, fontWeight = FontWeight.Medium)) {
                            append(author?.name?.ifBlank { null } ?: "el otro")
                        }
                    },
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
