package com.curlygarden.app.ui.screens.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.imagine.ImagineField
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check

private val ComposerErrorColor = Color(0xFFF4B7A6)
private val ComposerCheckColor = Color(0xFF26215C)

@Composable
internal fun MemoriesAlbumDialog(
    isWorking: Boolean,
    error: String?,
    onCreate: (name: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    val canSubmit = name.isNotBlank() && !isWorking

    Dialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    enabled = !isWorking,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(28.dp))
                    .border(1.dp, MemoriesOutline, RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(24.dp)
            ) {
                Text(
                    "Nuevo álbum",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                ImagineField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del álbum",
                    maxChars = MEMORIES_ALBUM_NAME_MAX
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Color del álbum",
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MemoriesAlbumColors.indices.forEach { index ->
                        ColorDot(
                            color = albumColor(index),
                            selected = index == colorIndex,
                            enabled = !isWorking,
                            onClick = { colorIndex = index }
                        )
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        error,
                        color = ComposerErrorColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(24.dp))
                MemoriesPrimaryButton(
                    text = "Crear álbum",
                    onClick = { onCreate(name, colorIndex) },
                    enabled = name.isNotBlank(),
                    loading = isWorking
                )
            }
        }
    }
}

@Composable
private fun ColorDot(
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .pressScale(pressedScale = 0.9f, enabled = enabled)
            .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
            .padding(5.dp)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = color,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = "color elegido",
                        tint = ComposerCheckColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
