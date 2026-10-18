package com.curlygarden.app.ui.screens.draw

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Download

@Composable
fun DrawRevealScreen(
    onBackClicked: () -> Unit = {},
    onRoundClosed: () -> Unit = {},
    onDownloadClicked: () -> Unit = {},
    viewModel: DrawViewModel = viewModel(factory = DrawViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val isStartingNext by viewModel.isStartingNextRound.collectAsState()

    var showingMine by remember { mutableStateOf(true) }

    val iConfirmed = uiState.myReadyForNext
    val partnerConfirmed = uiState.partnerReadyForNext

    Box(modifier = Modifier.curlyDarkBackground()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.42f,
                center = Offset(x = size.width * 1.1f, y = -size.height * 0.05f)
            )
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.48f,
                center = Offset(x = -size.width * 0.15f, y = size.height * 1.05f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RevealCircleButton(onClick = onBackClicked) {
                    Icon(
                        FeatherIcons.ArrowLeft,
                        contentDescription = "volver",
                        tint = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Listo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkTextPrimary
                    )
                    Text(
                        "Así quedaron los dos dibujos",
                        fontSize = 12.sp,
                        color = CurlyColors.DarkTextSecondary
                    )
                }
                RevealCircleButton(onClick = onDownloadClicked) {
                    Icon(
                        FeatherIcons.Download,
                        contentDescription = "descargar",
                        tint = Color.White
                    )
                }
            }

            AnimatedContent(
                targetState = showingMine,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "revealSwitch"
            ) { mine ->
                RevealPage(
                    topic = if (mine) uiState.topicForMe else uiState.topicISent,
                    label = if (mine) "TU DIBUJO" else "SU DIBUJO",
                    strokes = if (mine) uiState.myStrokes else uiState.partnerStrokes
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RevealCircleButton(
                        onClick = { showingMine = true },
                        enabled = !showingMine
                    ) {
                        Icon(
                            FeatherIcons.ArrowLeft,
                            contentDescription = "ver mi dibujo",
                            tint = Color.White.copy(alpha = if (showingMine) 0.35f else 1f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    RevealCircleButton(
                        onClick = { showingMine = false },
                        enabled = showingMine
                    ) {
                        Icon(
                            FeatherIcons.ArrowRight,
                            contentDescription = "ver el otro dibujo",
                            tint = Color.White.copy(alpha = if (showingMine) 1f else 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    if (showingMine) "Pulsa para ver el otro dibujo" else "Pulsa para volver al tuyo",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(16.dp))

                Surface(
                    onClick = { viewModel.confirmNextRound() },
                    enabled = !iConfirmed && !isStartingNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (iConfirmed) CurlyColors.DarkSurfaceElevated else CurlyColors.DarkLavenderPrimary,
                    border = if (iConfirmed) BorderStroke(1.dp, CurlyColors.DarkBorder) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Nuevo dibujo",
                            color = if (iConfirmed) CurlyColors.DarkTextMuted else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    when {
                        iConfirmed && !partnerConfirmed -> "Esperando…"
                        !iConfirmed && partnerConfirmed -> "El otro ya quiere empezar"
                        else -> " "
                    },
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RevealPage(
    topic: String?,
    label: String,
    strokes: List<DrawStroke>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            topic ?: "—",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = CurlyColors.DarkTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(14.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val cardWidth = minOf(maxWidth, maxHeight * 0.75f)
            val cardHeight = cardWidth * (4f / 3f)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CurlyColors.DarkSurface,
                border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
            ) {
                if (strokes.isEmpty()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Sin trazos",
                            color = CurlyColors.DarkTextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    DrawingPreview(strokes = strokes)
                }
            }
        }
    }
}

@Composable
private fun RevealCircleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}