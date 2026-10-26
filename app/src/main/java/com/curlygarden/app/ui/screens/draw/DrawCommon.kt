package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.curlygarden.app.ui.theme.CurlyColors

val drawPalette: List<Color> = listOf(
    CurlyColors.DarkLavenderPrimary,
    CurlyColors.DarkLavenderLight,
    Color(0xFF9BBFE0),
    Color(0xFFF4B7A6),
    Color(0xFFEBC27A),
    Color(0xFF8FCDA8),
    Color(0xFFFFFFFF)
)

fun Color.toArgbLong(): Long {
    val a = (alpha * 255f).toInt().coerceIn(0, 255).toLong()
    val r = (red * 255f).toInt().coerceIn(0, 255).toLong()
    val g = (green * 255f).toInt().coerceIn(0, 255).toLong()
    val b = (blue * 255f).toInt().coerceIn(0, 255).toLong()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

fun Long.toComposeColor(): Color = Color(this.toInt())

fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    var startX = points[0].x
    var startY = points[0].y
    for (index in 1 until points.size - 1) {
        val controlX = points[index].x
        val controlY = points[index].y
        val endX = (controlX + points[index + 1].x) / 2f
        val endY = (controlY + points[index + 1].y) / 2f
        path.cubicTo(
            startX + (controlX - startX) * 2f / 3f,
            startY + (controlY - startY) * 2f / 3f,
            endX + (controlX - endX) * 2f / 3f,
            endY + (controlY - endY) * 2f / 3f,
            endX,
            endY
        )
        startX = endX
        startY = endY
    }

    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

fun DrawScope.drawNormalizedStrokes(strokes: List<DrawStroke>) {
    val minSide = minOf(size.width, size.height)
    strokes.firstOrNull { it.fill }?.let { drawRect(it.color.toComposeColor()) }
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), Paint())
    strokes.forEach { stroke ->
        val flat = stroke.points
        if (stroke.fill || flat.size < 2) return@forEach
        val blend = if (stroke.eraser) BlendMode.Clear else BlendMode.SrcOver

        val lineWidth = (stroke.width * minSide).coerceAtLeast(1f)
        val color = stroke.color.toComposeColor()

        val offsets = ArrayList<Offset>(flat.size / 2)
        var index = 0
        while (index + 1 < flat.size) {
            offsets.add(Offset(flat[index] * size.width, flat[index + 1] * size.height))
            index += 2
        }

        if (offsets.size == 1) {
            drawCircle(color = color, radius = lineWidth / 2f, center = offsets[0], blendMode = blend)
            return@forEach
        }

        drawPath(
            path = buildSmoothPath(offsets),
            color = color,
            style = Stroke(
                width = lineWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            blendMode = blend
        )
    }
    canvas.restore()
}

@Composable
fun DrawingPreview(
    strokes: List<DrawStroke>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawNormalizedStrokes(strokes)
    }
}