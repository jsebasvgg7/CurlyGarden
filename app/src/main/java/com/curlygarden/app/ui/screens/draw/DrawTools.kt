package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Edit2

internal enum class DrawTool(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val defaultWidth: Float,
    val alpha: Float
) {
    Pencil("Lápiz", 2f..14f, 6f, 1f),
    Marker("Marcador", 10f..32f, 18f, 1f),
    Highlighter("Resaltador", 20f..48f, 30f, 0.4f),
    Eraser("Borrador", 10f..48f, 24f, 1f)
}

@Composable
internal fun DrawToolButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    content: @Composable (Color) -> Unit
) {
    val filled = selected || containerColor != null
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.3f)
        filled -> Color.White
        else -> Color.White.copy(alpha = 0.75f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = containerColor ?: if (selected) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkSurface,
        border = if (filled) null else BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier
            .size(42.dp)
            .semantics { contentDescription = label }
    ) {
        Box(contentAlignment = Alignment.Center) {
            content(tint)
        }
    }
}

@Composable
internal fun DrawToolIcon(tool: DrawTool, tint: Color) {
    when (tool) {
        DrawTool.Pencil -> Icon(FeatherIcons.Edit2, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        DrawTool.Marker -> Icon(Icons.Outlined.Brush, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        DrawTool.Highlighter -> Icon(Icons.Outlined.Highlight, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        DrawTool.Eraser -> EraserGlyph(tint)
    }
}

@Composable
private fun EraserGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        rotate(-40f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(size.width * 0.18f, size.height * 0.28f),
                size = Size(size.width * 0.64f, size.height * 0.44f),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.5f, size.height * 0.28f),
                end = Offset(size.width * 0.5f, size.height * 0.72f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
