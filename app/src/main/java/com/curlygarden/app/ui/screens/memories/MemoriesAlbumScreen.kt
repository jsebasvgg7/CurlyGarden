package com.curlygarden.app.ui.screens.memories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Home
import compose.icons.feathericons.Image

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MemoriesAlbumScreen(
    state: MemoriesUiState,
    onBack: () -> Unit,
    onOpenMemory: (String) -> Unit,
    onAdd: () -> Unit = {},
    onHome: () -> Unit = {}
) {
    val album = state.album
    val items = state.shownMemories
    val title = if (state.showHidden) "Ocultos" else album?.name ?: "Todos los recuerdos"
    val subtitle = when {
        album != null -> "${memoriesCountLabel(items.size)} · ${formatMemoryDate(album.createdAt)}"
        else -> memoriesCountLabel(items.size)
    }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            MemoriesHeader(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                trailing = {
                    TalkCircleButton(onClick = onHome) {
                        Icon(
                            FeatherIcons.Home,
                            contentDescription = "inicio",
                            tint = CurlyColors.DarkTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (items.isEmpty()) {
                    MemoriesAlbumEmpty(
                        hidden = state.showHidden,
                        isAlbum = album != null,
                        onAdd = onAdd
                    )
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 20.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(items, key = { it.id }) { memory ->
                            MemoryTile(memory, onClick = { onOpenMemory(memory.id) })
                        }
                    }
                    if (!state.showHidden) {
                        MemoriesFab(
                            onClick = onAdd,
                            description = "Agregar recuerdo",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryTile(memory: Memory, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(memory.aspectRatio.coerceIn(0.6f, 1.6f))
            .pressScale(pressedScale = 0.97f)
            .clip(shape)
            .background(CurlyColors.DarkSurface)
            .clickable(onClick = onClick)
            .border(1.5.dp, Color.White.copy(alpha = 0.85f), shape)
    ) {
        AsyncImage(
            model = memoryThumbUrl(memory.photoUrl),
            contentDescription = memory.caption.ifBlank { null },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            formatMemoryDate(memory.dateMillis),
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MemoriesAlbumEmpty(hidden: Boolean, isAlbum: Boolean, onAdd: () -> Unit) {
    val title = when {
        hidden -> "No hay recuerdos ocultos"
        isAlbum -> "Este álbum está vacío"
        else -> "Todavía no hay recuerdos"
    }
    val subtitle = when {
        hidden -> "Lo que ocultes aparecerá aquí"
        isAlbum -> "Agrega el primer recuerdo"
        else -> "Agrega el primero"
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        MemoriesDepthCard(
            color = albumColor(1),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MemoriesRingBadge(size = 72.dp, color = MemoriesInk) {
                    Icon(
                        FeatherIcons.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    color = MemoriesInk,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = MemoriesInk.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                if (!hidden) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        onClick = onAdd,
                        shape = CircleShape,
                        color = MemoriesInk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(pressedScale = 0.97f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Agregar recuerdo",
                                color = Color.White,
                                fontSize = 16.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
