package com.curlygarden.app.ui.screens.memories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus

@Composable
internal fun MemoriesIndexScreen(
    state: MemoriesUiState,
    onBack: () -> Unit,
    onNewAlbum: () -> Unit = {},
    onOpenAlbum: ((String?) -> Unit)? = null,
    onAddToAlbum: ((String) -> Unit)? = null
) {
    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            MemoriesHeader(
                title = "Recuerdos",
                subtitle = "Lo que guardamos juntos",
                onBack = onBack
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        color = CurlyColors.DarkLavenderPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                    !state.hasPartner -> MemoriesNotice(
                        "Todavía no tienes a nadie vinculado. Invita a alguien para guardar recuerdos."
                    )
                    state.loadFailed -> MemoriesNotice("No se pudieron cargar los recuerdos")
                    state.data.albums.isEmpty() && state.data.countFor(null) == 0 ->
                        MemoriesEmpty(onCreate = onNewAlbum)
                    else -> {
                        MemoriesAlbumList(state, onOpenAlbum, onAddToAlbum)
                        MemoriesFab(
                            onClick = onNewAlbum,
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
private fun MemoriesAlbumList(
    state: MemoriesUiState,
    onOpenAlbum: ((String?) -> Unit)?,
    onAddToAlbum: ((String) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "all") {
            MemoriesAllCard(
                count = state.data.countFor(null),
                onClick = onOpenAlbum?.let { open -> { open(null) } }
            )
        }
        items(state.data.albums, key = { it.id }) { album ->
            MemoriesAlbumCard(
                album = album,
                photos = state.data.collageFor(album.id),
                count = state.data.countFor(album.id),
                creator = state.profileFor(album.createdBy),
                onClick = onOpenAlbum?.let { open -> { open(album.id) } },
                onAdd = onAddToAlbum?.let { add -> { add(album.id) } }
            )
        }
    }
}

@Composable
private fun MemoriesEmpty(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MemoriesEmptyIllustration()
        Spacer(Modifier.height(24.dp))
        Text(
            "Todo empieza con un álbum",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Crea uno para empezar a guardar recuerdos",
            color = CurlyColors.DarkLavenderLight,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        MemoriesDepthCard(
            color = CurlyColors.DarkLavenderPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .pressScale(pressedScale = 0.97f),
            onClick = onCreate
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Crear mi primer álbum",
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun MemoriesFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Nuevo álbum"
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = CurlyColors.DarkLavenderPrimary,
        modifier = modifier
            .size(60.dp)
            .pressScale(pressedScale = 0.9f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                FeatherIcons.Plus,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.MemoriesNotice(message: String) {
    Text(
        message,
        color = CurlyColors.DarkTextSecondary,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
    )
}
