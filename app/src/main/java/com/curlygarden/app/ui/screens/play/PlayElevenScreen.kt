package com.curlygarden.app.ui.screens.play

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.popIn
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Star
import kotlin.math.cos
import kotlin.math.sin

private val ELEVEN_RING_MARGIN = 28.dp
private val ELEVEN_DOT_RADIUS = 6.dp
private val ELEVEN_MESSAGE_HEIGHT = 112.dp
private val ELEVEN_FIXED_HEIGHT = 236.dp

private data class ElevenSparkle(val angle: Float, val offset: Dp, val arm: Dp)

private val ELEVEN_SPARKLES = listOf(
    ElevenSparkle(-130f, 18.dp, 5.dp),
    ElevenSparkle(-35f, 22.dp, 3.5.dp),
    ElevenSparkle(155f, 16.dp, 3.5.dp),
    ElevenSparkle(50f, 20.dp, 5.dp)
)

@Composable
internal fun PlayElevenScreen(
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PlayHeader(
                title = "11:11 pm",
                subtitle = "No olvides pedir tu deseo",
                onBack = onBack,
                titleSize = 22.sp
            )
            when {
                state.session?.partnerUid == null -> ElevenNotice(
                    "Todavía no tienes a nadie vinculado. Invita a alguien para poder jugar."
                )
                state.loadFailed -> ElevenNotice("No se pudo cargar el 11:11")
                else -> ElevenContent(state, viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.ElevenNotice(message: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        PlayCenteredNotice(message)
    }
}

@Composable
private fun ColumnScope.ElevenContent(state: PlayUiState, viewModel: PlayViewModel) {
    val clock = state.elevenClock
    val myUid = state.session?.myUid.orEmpty()

    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val diameter = minOf(maxWidth * 0.6f, 240.dp, maxHeight - ELEVEN_FIXED_HEIGHT)
            .coerceAtLeast(140.dp)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ElevenRing(clock, diameter, Modifier.entrance(0))
            Spacer(Modifier.height(4.dp))
            ElevenMessage(clock, Modifier.entrance(1))
            Spacer(Modifier.height(16.dp))
            ElevenPressButton(
                phase = clock.phase,
                working = state.isWorking,
                onClick = viewModel::pressEleven,
                modifier = Modifier.entrance(2)
            )
        }
    }

    ElevenPodium(state.elevenPodium, myUid, Modifier.entrance(3))
}

@Composable
private fun ElevenRing(clock: ElevenClock, diameter: Dp, modifier: Modifier = Modifier) {
    val phase = clock.phase
    val target = if (phase == ElevenPhase.PRESSED) 1f else clock.progress
    val fraction = remember { Animatable(target) }

    LaunchedEffect(phase, target) {
        when {
            phase == ElevenPhase.PRESSED ->
                fraction.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            target > fraction.value -> fraction.snapTo(target)
            else -> fraction.animateTo(target, tween(1000, easing = LinearEasing))
        }
    }

    val lit = phase == ElevenPhase.OPEN || phase == ElevenPhase.PRESSED
    val glow by animateFloatAsState(if (lit) 1f else 0.45f, tween(400), label = "elevenGlow")
    val sparkle by animateFloatAsState(if (lit) 1f else 0.3f, tween(400), label = "elevenSparkle")

    val description = when {
        phase == ElevenPhase.PRESSED -> "Ya presionaste hoy"
        phase == ElevenPhase.BEFORE && clock.isCountingDown ->
            "Faltan ${clock.secondsLeft} segundos para las 11:11"
        phase == ElevenPhase.BEFORE -> "11:11, todavía no disponible"
        phase == ElevenPhase.OPEN -> "Quedan ${clock.secondsLeft} segundos para presionar"
        else -> "La hora de las 11:11 terminó"
    }

    Box(
        modifier = modifier
            .size(diameter + ELEVEN_RING_MARGIN * 2)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glow }
        ) {
            val ringRadius = diameter.toPx() / 2f - ELEVEN_DOT_RADIUS.toPx()
            val lavender = CurlyColors.DarkLavenderPrimary
            drawCircle(lavender.copy(alpha = 0.05f), radius = ringRadius + 28.dp.toPx())
            drawCircle(lavender.copy(alpha = 0.08f), radius = ringRadius + 18.dp.toPx())
            drawCircle(lavender.copy(alpha = 0.12f), radius = ringRadius + 8.dp.toPx())
        }

        Canvas(modifier = Modifier.size(diameter)) {
            val strokePx = 2.5.dp.toPx()
            val dotPx = ELEVEN_DOT_RADIUS.toPx()
            val radius = size.minDimension / 2f - dotPx
            val value = fraction.value
            val dim = if (phase == ElevenPhase.MISSED) 0.5f else 1f
            val lavender = CurlyColors.DarkLavenderPrimary
            val trackAlpha = if (value > 0f) 0.22f else 0.4f

            drawCircle(CurlyColors.DarkSurface.copy(alpha = 0.6f), radius = radius)
            drawCircle(
                color = lavender.copy(alpha = 0.14f * dim),
                radius = radius - 14.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = lavender.copy(alpha = trackAlpha * dim),
                radius = radius,
                style = Stroke(width = strokePx)
            )

            val startAngle = -90f + 360f * (1f - value)
            if (value > 0f) {
                drawArc(
                    color = lavender.copy(alpha = dim),
                    startAngle = startAngle,
                    sweepAngle = 360f * value,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            if (phase != ElevenPhase.PRESSED) {
                val angle = Math.toRadians(startAngle.toDouble())
                drawCircle(
                    color = CurlyColors.DarkLavenderLight.copy(
                        alpha = if (value > 0f) dim else 0.6f * dim
                    ),
                    radius = dotPx,
                    center = Offset(
                        center.x + radius * cos(angle).toFloat(),
                        center.y + radius * sin(angle).toFloat()
                    )
                )
            }
        }

        ElevenRingCenter(clock, diameter)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = sparkle }
        ) {
            val ringRadius = diameter.toPx() / 2f - ELEVEN_DOT_RADIUS.toPx()
            val strokePx = 1.5.dp.toPx()
            ELEVEN_SPARKLES.forEach { spark ->
                val angle = Math.toRadians(spark.angle.toDouble())
                val distance = ringRadius + spark.offset.toPx()
                val c = Offset(
                    center.x + distance * cos(angle).toFloat(),
                    center.y + distance * sin(angle).toFloat()
                )
                val arm = spark.arm.toPx()
                drawLine(
                    color = CurlyColors.DarkLavenderLight,
                    start = Offset(c.x - arm, c.y),
                    end = Offset(c.x + arm, c.y),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = CurlyColors.DarkLavenderLight,
                    start = Offset(c.x, c.y - arm),
                    end = Offset(c.x, c.y + arm),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ElevenRingCenter(clock: ElevenClock, diameter: Dp) {
    val big = (diameter.value * 0.2f).sp
    val bigLine = (diameter.value * 0.23f).sp
    val small = (diameter.value * 0.065f).coerceAtLeast(12f).sp
    val smallLine = (diameter.value * 0.09f).coerceAtLeast(16f).sp

    when {
        clock.phase == ElevenPhase.PRESSED -> Icon(
            FeatherIcons.Check,
            contentDescription = null,
            tint = CurlyColors.DarkLavenderLight,
            modifier = Modifier
                .size(diameter * 0.3f)
                .popIn()
        )
        clock.phase == ElevenPhase.BEFORE && clock.isCountingDown -> Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                elevenTimer(clock.secondsLeft),
                color = CurlyColors.DarkTextPrimary,
                fontSize = big,
                lineHeight = bigLine,
                fontWeight = FontWeight.Bold
            )
            Text(
                "para las 11:11",
                color = CurlyColors.DarkLavenderLight,
                fontSize = small,
                lineHeight = smallLine
            )
        }
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "11:11",
                color = if (clock.phase == ElevenPhase.MISSED) {
                    CurlyColors.DarkTextMuted
                } else {
                    CurlyColors.DarkTextPrimary
                },
                fontSize = big,
                lineHeight = bigLine,
                fontWeight = FontWeight.Bold
            )
            if (clock.phase == ElevenPhase.OPEN) {
                Text(
                    elevenTimer(clock.secondsLeft),
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = small,
                    lineHeight = smallLine
                )
            }
        }
    }
}

private fun elevenTimer(seconds: Int): String {
    val minutes = (seconds / 60).toString().padStart(2, '0')
    val rest = (seconds % 60).toString().padStart(2, '0')
    return "$minutes:$rest"
}

@Composable
private fun ElevenMessage(clock: ElevenClock, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ELEVEN_MESSAGE_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = clock.phase,
            animationSpec = tween(220),
            label = "elevenMessage"
        ) { phase ->
            when (phase) {
                ElevenPhase.OPEN -> ElevenMessageBlock(
                    icon = FeatherIcons.Star,
                    iconTint = CurlyColors.DarkTextPrimary,
                    headline = null,
                    body = "Cierra los ojos y pide tu deseo",
                    bodyColor = CurlyColors.DarkTextPrimary
                )
                ElevenPhase.BEFORE -> ElevenMessageBlock(
                    icon = FeatherIcons.Star,
                    iconTint = CurlyColors.DarkLavenderLight,
                    headline = "No disponible aún",
                    body = if (clock.isCountingDown) {
                        "Prepárate, ya casi es la hora."
                    } else {
                        "Se habilita a las 11:11 pm."
                    },
                    bodyColor = CurlyColors.DarkTextSecondary
                )
                ElevenPhase.PRESSED -> ElevenMessageBlock(
                    icon = null,
                    iconTint = Color.Unspecified,
                    headline = "¡Listo!",
                    body = "Ganaste +$POINTS_ELEVEN puntos. Vuelve mañana a las 11:11 pm.",
                    bodyColor = CurlyColors.DarkTextSecondary
                )
                ElevenPhase.MISSED -> ElevenMessageBlock(
                    icon = FeatherIcons.Star,
                    iconTint = CurlyColors.DarkTextMuted,
                    headline = "Vuelve mañana",
                    body = "La hora terminó. Se habilita otra vez a las 11:11 pm.",
                    bodyColor = CurlyColors.DarkTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ElevenMessageBlock(
    icon: ImageVector?,
    iconTint: Color,
    headline: String?,
    body: String,
    bodyColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
        if (headline != null) {
            Text(
                headline,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            body,
            color = bodyColor,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )
    }
}

@Composable
private fun ElevenPressButton(
    phase: ElevenPhase,
    working: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val open = phase == ElevenPhase.OPEN
    val enabled = open && !working

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (open) PlayAccent else CurlyColors.DarkSurfaceElevated,
        border = if (open) {
            BorderStroke(2.dp, lerp(PlayAccent, Color.Black, 0.45f))
        } else {
            BorderStroke(1.dp, CurlyColors.DarkBorder)
        },
        modifier = modifier
            .height(48.dp)
            .widthIn(min = 200.dp)
            .pressScale(pressedScale = 0.97f, enabled = enabled)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                working -> CircularProgressIndicator(
                    color = PlayAccentText,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                phase == ElevenPhase.PRESSED -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = null,
                        tint = PlayDone,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Ya presionaste",
                        color = CurlyColors.DarkTextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> Text(
                    "presiona ahora",
                    color = if (open) PlayAccentText else CurlyColors.DarkTextMuted,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ElevenPodium(
    entries: List<ElevenPodiumEntry>,
    myUid: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(CurlyColors.DarkBorder)
                    )
                }
                ElevenPodiumRow(rank = index + 1, entry = entry, myUid = myUid)
            }
        }
    }
}

@Composable
private fun ElevenPodiumRow(rank: Int, entry: ElevenPodiumEntry, myUid: String) {
    val name = entry.profile.name.ifBlank {
        if (entry.profile.uid == myUid) "Tú" else "El otro"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clearAndSetSemantics {
                contentDescription = "Puesto $rank, $name, ${entry.count} deseos pedidos"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayAvatar(entry.profile, 48.dp)
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Deseos pedidos #${entry.count}",
                color = CurlyColors.DarkTextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (rank == 1) PlayAccent else CurlyColors.DarkLavenderPrimary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$rank",
                color = if (rank == 1) PlayAccentText else Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
