package com.curlygarden.app.ui.screens.play

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkAvatar
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.screens.talk.TalkProfile
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Sun
import kotlinx.coroutines.delay

internal val PlayAccent = Color(0xFFEBC27A)
internal val PlayAccentText = Color(0xFF26215C)
internal val PlayDone = Color(0xFF8FCDA8)

@Composable
fun PlayScreen(onBack: () -> Unit) {
    val viewModel: PlayViewModel = viewModel(factory = PlayViewModelFactory())
    val state by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.open(PlayRoute.INDEX) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(2500)
            viewModel.clearError()
        }
    }

    BackHandler { if (!viewModel.back()) onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.route) {
            PlayRoute.INDEX -> PlayIndexContent(state, viewModel, onBack)
            PlayRoute.DAILY -> PlayDailyScreen(state, viewModel, onBack = { viewModel.back() })
            PlayRoute.THIS_OR_THAT -> PlayThisOrThatScreen(state, viewModel, onBack = { viewModel.back() })
            PlayRoute.KNOW_ME -> PlayKnowMeScreen(state, viewModel, onBack = { viewModel.back() })
        }
        errorMessage?.let { message ->
            PlayToast(message, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
internal fun PlayIndexContent(
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
                title = "Jugar",
                subtitle = "Juegos para conocerse más y divertirse juntos.",
                onBack = onBack,
                titleSize = 24.sp
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayDailyEntry(
            cardState = state.dailyState,
            onClick = { viewModel.open(PlayRoute.DAILY) },
            modifier = Modifier.entrance(0)
        )
        PlayGameEntry(
            title = "¿Qué prefieres?",
            subtitle = "5 tarjetas, 2 opciones. ¿Coincidirán?",
            icon = FeatherIcons.Layers,
            modifier = Modifier.entrance(1)
        )
        PlayGameEntry(
            title = "¿Cuánto me conoces?",
            subtitle = "5 preguntas, 4 opciones. Adivina qué respondió el otro.",
            icon = FeatherIcons.HelpCircle,
            modifier = Modifier.entrance(2)
        )
        PlayLevelCard(stats = state.stats, modifier = Modifier.entrance(3))
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlayDailyEntry(
    cardState: DailyCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (cardState) {
        DailyCardState.PENDING -> "Ver reto"
        DailyCardState.WAITING -> "Esperando al otro"
        DailyCardState.COMPLETED -> "Ver resultado"
    }
    val waiting = cardState == DailyCardState.WAITING

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.5.dp, PlayAccent),
        modifier = modifier
            .pressScale(pressedScale = 0.98f)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(PlayAccent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    FeatherIcons.Sun,
                    contentDescription = null,
                    tint = PlayAccentText,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                PlayPill(text = "Hoy", filled = true)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reto del día",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "¿Pregunta o acción?",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 12.sp
                )
                Text(
                    "Conoce un poco más al otro.",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (waiting) PlayAccent.copy(alpha = 0.22f) else PlayAccent,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        color = if (waiting) PlayAccent else PlayAccentText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayGameEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .alpha(0.7f)
                    .background(CurlyColors.DarkSurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = CurlyColors.DarkLavenderLight,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(0.7f)
            ) {
                Text(
                    title,
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            PlayPill(text = "Muy pronto", filled = false)
        }
    }
}

@Composable
private fun PlayLevelCard(stats: PlayStats, modifier: Modifier = Modifier) {
    val fraction = stats.progress / POINTS_PER_LEVEL.toFloat()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tu nivel",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "${stats.progress}/$POINTS_PER_LEVEL",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lv. ${stats.level}",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
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
            }
        }
    }
}

@Composable
internal fun PlayHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    titleSize: TextUnit = 17.sp
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TalkCircleButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                FeatherIcons.ArrowLeft,
                contentDescription = "atras",
                tint = CurlyColors.DarkTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = CurlyColors.DarkTextPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun PlayPill(text: String, filled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                if (filled) PlayAccent else CurlyColors.DarkSurfaceElevated,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            color = if (filled) PlayAccentText else CurlyColors.DarkLavenderLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun PlayPointsPill(points: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(PlayAccent, RoundedCornerShape(50))
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Text(
            "+$points puntos",
            color = PlayAccentText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun PlayAvatar(profile: PlayProfile?, size: Dp) {
    TalkAvatar(
        profile = profile?.let { TalkProfile(uid = it.uid, name = it.name, photoUrl = it.photoUrl) },
        fallbackColor = CurlyColors.DarkLavenderPrimary,
        size = size
    )
}

@Composable
internal fun PlayCenteredNotice(message: String) {
    Text(
        message,
        color = CurlyColors.DarkTextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    )
}

@Composable
private fun PlayToast(message: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            message,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}
