package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.CornerUpLeft
import compose.icons.feathericons.CornerUpRight
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Send
import compose.icons.feathericons.Trash2

private const val MIN_POINT_DISTANCE = 2.5f

private class CanvasStroke(
    val color: Color,
    val width: Float,
    val isEraser: Boolean
) {
    val points = mutableListOf<Offset>()
    private var cachedPath: Path? = null
    private var cachedCount = -1

    fun addPoint(point: Offset) {
        val last = points.lastOrNull()
        if (last == null || (point - last).getDistance() >= MIN_POINT_DISTANCE) {
            points.add(point)
        }
    }

    fun path(): Path {
        val existing = cachedPath
        if (existing != null && cachedCount == points.size) return existing
        val rebuilt = buildSmoothPath(points)
        cachedPath = rebuilt
        cachedCount = points.size
        return rebuilt
    }
}

private val canvasBackground = CurlyColors.DarkSurface

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawCanvasScreen(
    onBackClicked: () -> Unit = {},
    onRevealReady: () -> Unit = {},
    viewModel: DrawViewModel = viewModel(factory = DrawViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSavingDrawing.collectAsState()

    LaunchedEffect(uiState.status) {
        if (uiState.status == DrawRoundStatus.REVEALED) {
            onRevealReady()
        }
    }

    val strokes = remember { mutableStateListOf<CanvasStroke>() }
    val undone = remember { mutableStateListOf<CanvasStroke>() }
    val hasStrokes by remember { derivedStateOf { strokes.isNotEmpty() } }
    val hasUndone by remember { derivedStateOf { undone.isNotEmpty() } }
    var tick by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var currentColor by remember { mutableStateOf(drawPalette.first()) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }
    var isEraser by remember { mutableStateOf(false) }

    val canSubmit = hasStrokes &&
            !uiState.myReadyToReveal &&
            !isSaving &&
            canvasSize.width > 0

    Box(modifier = Modifier.curlyDarkBackground()) {
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
                RoundIconButton(onClick = onBackClicked, enabled = true) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "volver", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tu turno",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkTextPrimary
                    )
                    Text(
                        if (uiState.myReadyToReveal) "Esperando al otro" else "Dibuja tu tema",
                        fontSize = 12.sp,
                        color = CurlyColors.DarkTextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                undone.add(strokes.removeAt(strokes.lastIndex))
                                tick++
                            }
                        },
                        enabled = hasStrokes
                    ) {
                        Icon(
                            FeatherIcons.CornerUpLeft,
                            contentDescription = "deshacer",
                            tint = Color.White
                        )
                    }
                    RoundIconButton(
                        onClick = {
                            if (undone.isNotEmpty()) {
                                strokes.add(undone.removeAt(undone.lastIndex))
                                tick++
                            }
                        },
                        enabled = hasUndone
                    ) {
                        Icon(
                            FeatherIcons.CornerUpRight,
                            contentDescription = "rehacer",
                            tint = Color.White
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardWidth = minOf(maxWidth, maxHeight * 0.75f)
                val cardHeight = cardWidth * (4f / 3f)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = canvasBackground,
                    border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(currentColor, strokeWidth, isEraser) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val stroke = CanvasStroke(
                                            color = if (isEraser) canvasBackground else currentColor,
                                            width = strokeWidth,
                                            isEraser = isEraser
                                        )
                                        stroke.addPoint(down.position)
                                        strokes.add(stroke)
                                        undone.clear()
                                        down.consume()
                                        tick++
                                        drag(down.id) { change ->
                                            change.historical.forEach { stroke.addPoint(it.position) }
                                            stroke.addPoint(change.position)
                                            change.consume()
                                            tick++
                                        }
                                    }
                                }
                        ) {
                            tick.let { }
                            strokes.forEach { stroke ->
                                val points = stroke.points
                                if (points.isEmpty()) return@forEach

                                if (points.size == 1) {
                                    drawCircle(
                                        color = stroke.color,
                                        radius = stroke.width / 2f,
                                        center = points.first()
                                    )
                                    return@forEach
                                }

                                drawPath(
                                    path = stroke.path(),
                                    color = stroke.color,
                                    style = Stroke(
                                        width = stroke.width,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = canvasBackground,
                                border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .pointerInput(Unit) { detectTapGestures { } }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tema:",
                                        color = CurlyColors.DarkTextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        uiState.topicForMe ?: "Cargando…",
                                        color = CurlyColors.DarkTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    viewModel.submitDrawing(strokes.toNormalizedStrokes(canvasSize))
                                },
                                enabled = canSubmit,
                                shape = CircleShape,
                                color = if (canSubmit || uiState.myReadyToReveal) {
                                    CurlyColors.DarkLavenderPrimary
                                } else {
                                    CurlyColors.DarkBorder
                                },
                                border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    when {
                                        isSaving -> CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        uiState.myReadyToReveal -> Icon(
                                            FeatherIcons.Check,
                                            contentDescription = "enviado",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        else -> Icon(
                                            FeatherIcons.Send,
                                            contentDescription = "mostrar al otro",
                                            tint = Color.White.copy(alpha = if (canSubmit) 1f else 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CurlyColors.DarkSurfaceElevated,
                border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToolChip(
                            label = "Pincel",
                            selected = !isEraser,
                            onClick = { isEraser = false },
                            modifier = Modifier.weight(1f)
                        ) { tint ->
                            Icon(
                                FeatherIcons.Edit2,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ToolChip(
                            label = "Borrador",
                            selected = isEraser,
                            onClick = { isEraser = true },
                            modifier = Modifier.weight(1f)
                        ) { tint ->
                            Icon(
                                FeatherIcons.Trash2,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Color",
                        color = CurlyColors.DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        drawPalette.forEach { color ->
                            val selected = color == currentColor && !isEraser
                            Surface(
                                onClick = {
                                    currentColor = color
                                    isEraser = false
                                },
                                shape = CircleShape,
                                color = color,
                                border = if (selected) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.size(32.dp)
                            ) {}
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Tamaño",
                        color = CurlyColors.DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 4f..44f,
                        colors = SliderDefaults.colors(
                            thumbColor = CurlyColors.DarkLavenderPrimary,
                            activeTrackColor = CurlyColors.DarkLavenderPrimary,
                            inactiveTrackColor = CurlyColors.DarkBorder
                        )
                    )
                }
            }
        }
    }
}

private fun List<CanvasStroke>.toNormalizedStrokes(size: IntSize): List<DrawStroke> {
    if (size.width <= 0 || size.height <= 0) return emptyList()
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    val minSide = minOf(width, height)

    return map { stroke ->
        DrawStroke(
            points = stroke.points.flatMap { point ->
                listOf(point.x / width, point.y / height)
            },
            color = stroke.color.toArgbLong(),
            width = stroke.width / minSide
        )
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
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

@Composable
private fun ToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkSurface,
        border = if (selected) null else BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon(if (selected) Color.White else Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}