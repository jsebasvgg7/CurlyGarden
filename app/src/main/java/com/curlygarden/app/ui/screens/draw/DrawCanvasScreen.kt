package com.curlygarden.app.ui.screens.draw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.CornerUpLeft
import compose.icons.feathericons.CornerUpRight
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Send
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.Sun
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

private val canvasLight = Color.White
private val canvasDark = CurlyColors.DarkSurface

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
    var customColor by remember { mutableStateOf<Color?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(DrawTool.Pencil) }
    val toolWidths = remember {
        mutableStateMapOf<DrawTool, Float>().apply {
            DrawTool.values().forEach { put(it, it.defaultWidth) }
        }
    }
    var lightCanvas by remember { mutableStateOf(true) }
    var showSize by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val strokeWidth = toolWidths.getValue(tool)
    val isEraser = tool == DrawTool.Eraser
    val canvasBackground = if (lightCanvas) canvasLight else canvasDark
    val selectColor: (Color) -> Unit = {
        currentColor = it
        if (tool == DrawTool.Eraser) tool = DrawTool.Pencil
    }

    LaunchedEffect(confirmClear) {
        if (confirmClear) {
            delay(3000)
            confirmClear = false
        }
    }

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

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(23.dp),
                    color = CurlyColors.DarkSurface,
                    border = BorderStroke(1.dp, CurlyColors.DarkBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "Tema:",
                            color = CurlyColors.DarkTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            uiState.topicForMe ?: "Cargando…",
                            color = CurlyColors.DarkTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3
                        )
                    }
                }

                Surface(
                    onClick = {
                        viewModel.submitDrawing(strokes.toNormalizedStrokes(canvasSize, canvasBackground))
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

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 14.dp),
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
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(currentColor, strokeWidth, tool) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val stroke = CanvasStroke(
                                            color = when (tool) {
                                                DrawTool.Eraser -> Color.Black
                                                else -> currentColor.copy(alpha = tool.alpha)
                                            },
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
                                        center = points.first(),
                                        blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
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
                                    ),
                                    blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
                                )
                            }
                        }

                        Surface(
                            onClick = { lightCanvas = !lightCanvas },
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .semantics {
                                    contentDescription = if (lightCanvas) "Lienzo oscuro" else "Lienzo claro"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (lightCanvas) FeatherIcons.Moon else FeatherIcons.Sun,
                                    contentDescription = null,
                                    tint = if (lightCanvas) Color(0xFF9C98B8) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(CurlyColors.DarkBorder)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Selector de colores",
                        color = CurlyColors.DarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
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
                                onClick = { selectColor(color) },
                                shape = CircleShape,
                                color = color,
                                border = if (selected) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.size(32.dp)
                            ) {}
                        }
                        DrawRainbowSwatch(
                            color = customColor,
                            selected = customColor != null && customColor == currentColor && !isEraser,
                            onClick = { showColorPicker = true }
                        )
                    }

                    if (showColorPicker) {
                        DrawColorPickerDialog(
                            initial = customColor ?: currentColor,
                            onDismiss = { showColorPicker = false },
                            onConfirm = {
                                customColor = it
                                selectColor(it)
                                showColorPicker = false
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val sizeButton: @Composable () -> Unit = {
                        DrawToolButton(
                            label = "Tamaño",
                            selected = showSize,
                            onClick = { showSize = !showSize }
                        ) { tint ->
                            Icon(
                                FeatherIcons.Sliders,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showSize) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = strokeWidth,
                                onValueChange = { toolWidths[tool] = it },
                                valueRange = tool.range,
                                colors = SliderDefaults.colors(
                                    thumbColor = CurlyColors.DarkLavenderPrimary,
                                    activeTrackColor = CurlyColors.DarkLavenderPrimary,
                                    inactiveTrackColor = CurlyColors.DarkBorder
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(10.dp))
                            sizeButton()
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DrawTool.values().forEach { item ->
                                DrawToolButton(
                                    label = item.label,
                                    selected = tool == item,
                                    onClick = { tool = item }
                                ) { tint -> DrawToolIcon(item, tint) }
                            }
                            DrawToolButton(
                                label = if (confirmClear) "Confirmar borrar todo" else "Borrar todo",
                                selected = false,
                                enabled = hasStrokes,
                                containerColor = if (confirmClear) Color(0xFFE57373) else null,
                                onClick = {
                                    if (confirmClear) {
                                        strokes.clear()
                                        undone.clear()
                                        tick++
                                        confirmClear = false
                                    } else {
                                        confirmClear = true
                                    }
                                }
                            ) { tint ->
                                Icon(
                                    if (confirmClear) FeatherIcons.Check else FeatherIcons.Trash2,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            sizeButton()
                        }
                    }
                }
            }
        }
    }
}

private fun List<CanvasStroke>.toNormalizedStrokes(size: IntSize, background: Color): List<DrawStroke> {
    if (size.width <= 0 || size.height <= 0) return emptyList()
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    val minSide = minOf(width, height)

    val fill = DrawStroke(color = background.toArgbLong(), width = 0f, fill = true)
    return listOf(fill) + map { stroke ->
        DrawStroke(
            points = stroke.points.flatMap { point ->
                listOf(point.x / width, point.y / height)
            },
            color = stroke.color.toArgbLong(),
            width = stroke.width / minSide,
            eraser = stroke.isEraser
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
