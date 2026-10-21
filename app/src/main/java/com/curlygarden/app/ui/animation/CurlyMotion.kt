package com.curlygarden.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp

fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    enabled: Boolean = true
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val slop = LocalViewConfiguration.current.touchSlop
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                pressed = true
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val moved = (change.position - down.position).getDistance() > slop
                } while (change.pressed && !moved)
                pressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

fun Modifier.entrance(index: Int, play: Boolean = true): Modifier = composed {
    var visible by remember { mutableStateOf(!play) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 360,
            delayMillis = if (play) index * 60 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "entrance"
    )
    this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 14.dp.toPx()
    }
}

fun Modifier.popWhen(active: Boolean, peak: Float = 1.35f): Modifier = composed {
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(active) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        if (active) {
            scale.animateTo(peak, tween(110, easing = FastOutSlowInEasing))
            scale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

fun Modifier.popIn(): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "popIn"
    )
    this.graphicsLayer {
        val s = 0.85f + 0.15f * progress
        scaleX = s
        scaleY = s
        alpha = progress.coerceIn(0f, 1f)
    }
}
