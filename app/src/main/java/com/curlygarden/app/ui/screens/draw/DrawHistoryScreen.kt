package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Heart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DrawHistoryScreen(
    onBackClicked: () -> Unit = {},
    viewModel: DrawViewModel = viewModel(factory = DrawViewModelFactory())
) {
    val history by viewModel.history.collectAsState()
    val saving by viewModel.savingMemoryIds.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    LaunchedEffect(error) {
        if (error != null) {
            delay(2500)
            viewModel.dismissError()
        }
    }

    Box(modifier = Modifier.curlyDarkBackground()) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBackClicked,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            FeatherIcons.ArrowLeft,
                            contentDescription = "volver",
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Historial",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkTextPrimary
                    )
                    Text(
                        "Tus rondas anteriores",
                        fontSize = 12.sp,
                        color = CurlyColors.DarkTextSecondary
                    )
                }
            }

            Spacer(Modifier.padding(top = 20.dp))

            error?.let {
                Text(
                    it,
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Todavía no hay rondas guardadas",
                        color = CurlyColors.DarkTextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(history, key = { it.id }) { entry ->
                        HistoryCard(
                            entry = entry,
                            isSaving = entry.id in saving,
                            onSaveMemory = { viewModel.saveToMemories(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: DrawHistoryEntry,
    isSaving: Boolean,
    onSaveMemory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.topicForMe ?: "Sin tema",
                        color = CurlyColors.DarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatDate(entry.createdAt),
                        color = CurlyColors.DarkTextMuted,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    onClick = onSaveMemory,
                    enabled = !entry.savedToMemoriesByMe && !isSaving,
                    shape = CircleShape,
                    color = if (entry.savedToMemoriesByMe) {
                        CurlyColors.DarkLavenderPrimary
                    } else {
                        CurlyColors.DarkSurface
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = CurlyColors.DarkLavenderLight,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                FeatherIcons.Heart,
                                contentDescription = if (entry.savedToMemoriesByMe) {
                                    "guardado en recuerdos"
                                } else {
                                    "guardar en recuerdos"
                                },
                                tint = if (entry.savedToMemoriesByMe) {
                                    Color.White
                                } else {
                                    CurlyColors.DarkTextSecondary
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.padding(top = 10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryThumb(strokes = entry.myStrokes, modifier = Modifier.weight(1f))
                HistoryThumb(strokes = entry.partnerStrokes, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryThumb(
    strokes: List<DrawStroke>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.aspectRatio(3f / 4f)
    ) {
        if (strokes.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Text("—", color = CurlyColors.DarkTextMuted, fontSize = 12.sp)
            }
        } else {
            DrawingPreview(strokes = strokes)
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    return SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(millis))
}