package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.curlygarden.app.ui.theme.CurlyColors

private val hueStops: List<Color> =
    listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }

@Composable
internal fun DrawRainbowSwatch(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = if (selected) BorderStroke(2.dp, Color.White) else null,
        modifier = modifier.size(32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.sweepGradient(hueStops)),
            contentAlignment = Alignment.Center
        ) {
            if (selected && color != null) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
internal fun DrawColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val start = remember(initial) {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(initial.toArgb(), out)
        out
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, saturation, brightness)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = CurlyColors.DarkSurfaceElevated,
            border = BorderStroke(1.dp, CurlyColors.DarkBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Elige un color",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                SaturationBrightnessPanel(hue, saturation, brightness) { s, b ->
                    saturation = s
                    brightness = b
                }
                Spacer(Modifier.height(16.dp))
                HueBar(hue) { hue = it }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Surface(
                        onClick = { onConfirm(color) },
                        shape = RoundedCornerShape(22.dp),
                        color = CurlyColors.DarkLavenderPrimary
                    ) {
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .width(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Usar color",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationBrightnessPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (Float, Float) -> Unit
) {
    var box by remember { mutableStateOf(IntSize.Zero) }

    fun update(position: Offset) {
        if (box.width == 0 || box.height == 0) return
        onChange(
            (position.x / box.width).coerceIn(0f, 1f),
            1f - (position.y / box.height).coerceIn(0f, 1f)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { box = it }
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val center = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(Color.Black.copy(alpha = 0.3f), 11.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        drawCircle(Color.White, 10.dp.toPx(), center, style = Stroke(3.dp.toPx()))
    }
}

@Composable
private fun HueBar(hue: Float, onChange: (Float) -> Unit) {
    var box by remember { mutableStateOf(IntSize.Zero) }

    fun update(position: Offset) {
        if (box.width == 0) return
        onChange((position.x / box.width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .onSizeChanged { box = it }
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ -> update(change.position) }
                )
            }
    ) {
        drawRect(Brush.horizontalGradient(hueStops))
        val center = Offset(hue / 360f * size.width, size.height / 2f)
        drawCircle(Color.Black.copy(alpha = 0.3f), 12.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        drawCircle(Color.White, 11.dp.toPx(), center, style = Stroke(3.dp.toPx()))
    }
}
