package com.curlygarden.app.ui.screens.play

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.components.CurlyDepthCard
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Clock
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Star
import compose.icons.feathericons.Sun
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal val PlayRose = Color(0xFFF4B7A6)
internal val PlaySky = Color(0xFF9BBFE0)
private const val PLAY_RECENT_LIMIT = 3

@Composable
internal fun PlayIndexContent(
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    val showEleven = !state.isLoading && !state.loadFailed && state.session?.partnerUid != null

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PlayHeader(
                title = "Jugar",
                subtitle = "Juegos para disfrutar juntos",
                onBack = onBack,
                titleSize = 24.sp,
                trailing = {
                    if (showEleven) {
                        PlayElevenEntry(onClick = { viewModel.open(PlayRoute.ELEVEN) })
                    }
                }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        color = CurlyColors.DarkLavenderPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    state.session?.partnerUid == null -> PlayCenteredNotice(
                        "Todavía no tienes a nadie vinculado. Invita a alguien para poder jugar."
                    )
                    state.loadFailed -> PlayCenteredNotice("No se pudieron cargar los juegos")
                    else -> PlayIndexList(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun PlayIndexList(state: PlayUiState, viewModel: PlayViewModel) {
    val recent = state.sessions
        .filter { it.status == SessionStatus.REVEALED }
        .sortedByDescending { it.createdAtMillis }
        .take(PLAY_RECENT_LIMIT)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlayLevelPill(stats = state.stats, modifier = Modifier.entrance(0))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.entrance(1)
        ) {
            PlaySectionTitle(icon = FeatherIcons.Star, text = "Nuestros juegos")
            PlayDailyHero(
                cardState = state.dailyState,
                onClick = { viewModel.open(PlayRoute.DAILY) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(182.dp)
                .entrance(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayGameCard(
                title = "¿Qué prefieres?",
                subtitle = "5 tarjetas, 2 opciones",
                pill = gamePill(state, PlayGameType.THIS_OR_THAT),
                icon = FeatherIcons.Layers,
                color = PlayRose,
                onClick = { viewModel.open(PlayRoute.THIS_OR_THAT) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PlayGameCard(
                title = "¿Cuánto me conoces?",
                subtitle = "5 preguntas, 4 opciones",
                pill = gamePill(state, PlayGameType.KNOW_ME),
                icon = FeatherIcons.HelpCircle,
                color = PlaySky,
                onClick = { viewModel.open(PlayRoute.KNOW_ME) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        if (recent.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.entrance(3)
            ) {
                PlaySectionTitle(icon = FeatherIcons.Clock, text = "Jugados recientemente")
                PlayRecentList(
                    sessions = recent,
                    onClick = { game ->
                        viewModel.open(
                            if (game.type == PlayGameType.THIS_OR_THAT) {
                                PlayRoute.THIS_OR_THAT
                            } else {
                                PlayRoute.KNOW_ME
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayElevenEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = CurlyColors.DarkLavenderPrimary,
        modifier = Modifier
            .size(44.dp)
            .pressScale(pressedScale = 0.9f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "11",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clearAndSetSemantics {}
            )
            Icon(
                FeatherIcons.Star,
                contentDescription = "Pedir un deseo a las 11:11",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
private fun PlayLevelPill(stats: PlayStats, modifier: Modifier = Modifier) {
    val fraction = stats.progress / POINTS_PER_LEVEL.toFloat()
    Surface(
        shape = RoundedCornerShape(50),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription =
                    "Nivel ${stats.level}, ${stats.progress} de $POINTS_PER_LEVEL puntos"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Lv. ${stats.level}",
                color = CurlyColors.DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CurlyColors.DarkSurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(PlayAccent, RoundedCornerShape(50))
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "${stats.progress}/$POINTS_PER_LEVEL",
                color = CurlyColors.DarkTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PlaySectionTitle(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CurlyColors.DarkLavenderLight,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayDailyHero(
    cardState: DailyCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (cardState) {
        DailyCardState.PENDING -> "Ver reto"
        DailyCardState.WAITING -> "Esperando..."
        DailyCardState.COMPLETED -> "Ver resultado"
    }
    val waiting = cardState == DailyCardState.WAITING

    CurlyDepthCard(
        color = PlayAccent,
        shape = RoundedCornerShape(26.dp),
        onClick = onClick,
        modifier = modifier.pressScale(pressedScale = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, bottom = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .background(PlayAccentText.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        "HOY",
                        color = PlayAccentText,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reto del día",
                    color = PlayAccentText,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "¿Pregunta o acción? Conoce un poco más al otro.",
                    color = PlayAccentText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .background(
                            if (waiting) PlayAccentText.copy(alpha = 0.14f) else PlayAccentText,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (waiting) PlayAccentText else PlayAccent,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(PlayAccentText.copy(alpha = 0.07f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FeatherIcons.Sun,
                        contentDescription = null,
                        tint = PlayAccentText,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayGameCard(
    title: String,
    subtitle: String,
    pill: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    CurlyDepthCard(
        color = color,
        shape = RoundedCornerShape(22.dp),
        fillHeight = true,
        onClick = onClick,
        modifier = if (onClick != null) modifier.pressScale(pressedScale = 0.96f) else modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                icon,
                contentDescription = null,
                tint = PlayAccentText.copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 22.dp, y = 22.dp)
                    .size(104.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PlayAccentText.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = PlayAccentText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        title,
                        color = PlayAccentText,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = PlayAccentText,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(PlayAccentText.copy(alpha = 0.14f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            pill,
                            color = PlayAccentText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayRecentList(
    sessions: List<GameSession>,
    onClick: (GameSession) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            sessions.forEachIndexed { index, game ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CurlyColors.DarkBorder)
                    )
                }
                PlayRecentRow(game = game, onClick = { onClick(game) })
            }
        }
    }
}

@Composable
private fun PlayRecentRow(game: GameSession, onClick: () -> Unit) {
    val isThisOrThat = game.type == PlayGameType.THIS_OR_THAT
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .pressScale(pressedScale = 0.98f)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isThisOrThat) PlayRose else PlaySky, CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isThisOrThat) "¿Qué prefieres?" else "¿Cuánto me conoces?",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Completada · ${relativeDays(game.createdAtMillis)}",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                FeatherIcons.ArrowRight,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun gamePill(state: PlayUiState, type: PlayGameType): String =
    when (state.gameState(type)) {
        GameCardState.NEW_GAME ->
            if (state.activeSession(type) != null) "Te toca jugar" else "Nueva partida"
        GameCardState.WAITING -> "Esperando..."
        GameCardState.YOUR_TURN_TO_GUESS -> "Te toca adivinar"
        GameCardState.SEE_RESULT -> "Ver resultado"
    }

private fun relativeDays(millis: Long): String {
    if (millis == Long.MAX_VALUE) return "Ahora"
    val zone = ZoneId.systemDefault()
    val then = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(then, LocalDate.now(zone))
    return when {
        days <= 0L -> "Hoy"
        days == 1L -> "Ayer"
        else -> "Hace $days días"
    }
}
