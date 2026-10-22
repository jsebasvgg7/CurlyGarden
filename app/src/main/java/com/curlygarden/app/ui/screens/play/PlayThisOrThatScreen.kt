package com.curlygarden.app.ui.screens.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Layers
import compose.icons.feathericons.Play
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PICK_ADVANCE_DELAY = 250L
private const val SCREEN_TITLE = "¿Qué prefieres?"

private enum class ThisOrThatStage { INTRO, PICKING, WAITING, RESULT }

@Composable
internal fun PlayThisOrThatScreen(
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    val type = PlayGameType.THIS_OR_THAT
    val myUid = state.session?.myUid.orEmpty()
    val active = state.activeSession(type)
    val last = state.lastRevealed(type)
    val stage = when {
        active == null && last == null -> ThisOrThatStage.INTRO
        active == null -> ThisOrThatStage.RESULT
        myUid in active.answeredBy -> ThisOrThatStage.WAITING
        else -> ThisOrThatStage.PICKING
    }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (stage == ThisOrThatStage.PICKING && active != null) {
                ThisOrThatCards(active, state, viewModel, onBack)
            } else {
                PlayHeader(
                    title = SCREEN_TITLE,
                    subtitle = when (stage) {
                        ThisOrThatStage.INTRO -> "5 tarjetas, 2 opciones"
                        ThisOrThatStage.WAITING -> "Esperando al otro"
                        else -> "Resultado"
                    },
                    onBack = onBack
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp)
                ) {
                    when (stage) {
                        ThisOrThatStage.INTRO -> ThisOrThatIntro(state, viewModel)
                        ThisOrThatStage.WAITING -> ThisOrThatWaiting()
                        else -> if (last != null) ThisOrThatResult(last, state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThisOrThatIntro(state: PlayUiState, viewModel: PlayViewModel) {
    PlayDepthCard(
        color = PlayRose,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.entrance(0)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(PlayAccentText.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    FeatherIcons.Layers,
                    contentDescription = null,
                    tint = PlayAccentText,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                SCREEN_TITLE,
                color = PlayAccentText,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Cinco tarjetas con dos opciones. Cada quien elige en privado y, cuando los dos terminen, descubren en cuáles coincidieron.",
                color = PlayAccentText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .entrance(1),
        contentAlignment = Alignment.Center
    ) {
        PlayPointsPill(points = POINTS_THIS_OR_THAT)
    }
    Spacer(Modifier.height(20.dp))
    DailyButton(
        label = "Empezar",
        icon = FeatherIcons.Play,
        enabled = !state.isWorking,
        working = state.isWorking,
        onClick = { viewModel.startGame(PlayGameType.THIS_OR_THAT) },
        modifier = Modifier.entrance(2)
    )
}

@Composable
private fun ColumnScope.ThisOrThatCards(
    game: GameSession,
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    val total = PLAY_PICKS_PER_GAME
    val pairs = remember(game.id) { game.questionIds.mapNotNull { PlayBank.thisOrThat.getOrNull(it) } }
    val picks = remember(game.id) { mutableStateMapOf<Int, Int>() }
    var index by remember(game.id) { mutableIntStateOf(0) }
    var locked by remember(game.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = index > 0 || locked) {
        if (!locked) index -= 1
    }

    PlayHeader(
        title = SCREEN_TITLE,
        subtitle = "Tarjeta ${index + 1} de $total",
        onBack = {
            when {
                locked -> Unit
                index > 0 -> index -= 1
                else -> onBack()
            }
        }
    )

    if (pairs.size < total) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PlayCenteredNotice("Esta partida usa tarjetas que ya no están disponibles.")
        }
        return
    }

    PlayProgressBar(
        total = total,
        current = index,
        modifier = Modifier.padding(top = 20.dp)
    )

    val pair = pairs[index]
    val canPick = !locked && !state.isWorking
    val choose = { option: Int ->
        picks[index] = option
        if (index < total - 1) {
            locked = true
            val target = index + 1
            scope.launch {
                delay(PICK_ADVANCE_DELAY)
                index = target
                locked = false
            }
        }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        key(index) {
            PlayChoiceCard(
                letter = "A",
                text = pair.a,
                color = PlayRose,
                selected = picks[index] == 0,
                enabled = canPick,
                onClick = { choose(0) },
                modifier = Modifier.entrance(0)
            )
            Text(
                "o",
                color = CurlyColors.DarkTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(1)
            )
            PlayChoiceCard(
                letter = "B",
                text = pair.b,
                color = PlaySky,
                selected = picks[index] == 1,
                enabled = canPick,
                onClick = { choose(1) },
                modifier = Modifier.entrance(2)
            )
        }
    }

    if (index == total - 1) {
        DailyButton(
            label = "Enviar mis elecciones",
            icon = FeatherIcons.Check,
            enabled = picks.size == total && !state.isWorking,
            working = state.isWorking,
            onClick = { viewModel.submitAnswers(List(total) { picks.getValue(it) }) }
        )
    } else {
        Text(
            "Toca una opción para continuar",
            color = CurlyColors.DarkLavenderLight,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(top = 18.dp)
        )
    }
}

@Composable
private fun ThisOrThatWaiting() {
    PlayWaitingContent(
        color = PlayRose,
        message = "Ya elegiste tus 5 tarjetas. El resultado aparece cuando el otro termine.",
        pill = { modifier -> PlayPointsPill(points = POINTS_THIS_OR_THAT, modifier = modifier) }
    )
}

@Composable
private fun ThisOrThatResult(
    game: GameSession,
    state: PlayUiState,
    viewModel: PlayViewModel
) {
    LaunchedEffect(game.id) { viewModel.loadReveal(game.id) }

    val total = PLAY_PICKS_PER_GAME
    val session = state.session
    val reveal = state.reveal
    val pairs = game.questionIds.mapNotNull { PlayBank.thisOrThat.getOrNull(it) }

    if (reveal == null) {
        PlayResultLoading()
        return
    }

    val mine = session?.myUid?.let { reveal.answers[it] }
    val other = session?.partnerUid?.let { reveal.answers[it] }
    if (mine == null || other == null || pairs.size < total || mine.size < total || other.size < total) {
        PlayCenteredNotice("No se pudo cargar el resultado.")
        return
    }

    val matched = List(total) { mine[it] == other[it] }
    val matches = matched.count { it }
    val message = when {
        matches >= 4 -> "Casi siempre en la misma página."
        matches >= 2 -> "Bastante en común y algo por descubrir."
        else -> "Mucho por descubrir del otro."
    }

    PlayDepthCard(
        color = PlayAccent,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.entrance(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Coincidieron en",
                color = PlayAccentText,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            Text(
                "$matches de $total",
                color = PlayAccentText,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                color = PlayAccentText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        pairs.take(total).forEachIndexed { i, pair ->
            ThisOrThatResultRow(
                number = i + 1,
                pair = pair,
                myPick = mine[i],
                otherPick = other[i],
                matched = matched[i],
                myProfile = session?.myProfile,
                otherProfile = session?.partnerProfile,
                modifier = Modifier.entrance(i + 1)
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    DailyButton(
        label = "Jugar otra partida",
        icon = FeatherIcons.Play,
        enabled = !state.isWorking,
        working = state.isWorking,
        onClick = { viewModel.startGame(PlayGameType.THIS_OR_THAT) },
        modifier = Modifier.entrance(total + 1)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ThisOrThatResultRow(
    number: Int,
    pair: ThisOrThatPair,
    myPick: Int,
    otherPick: Int,
    matched: Boolean,
    myProfile: PlayProfile?,
    otherProfile: PlayProfile?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tarjeta $number",
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .background(
                            if (matched) PlayDone.copy(alpha = 0.2f) else CurlyColors.DarkSurfaceElevated,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (matched) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = null,
                            tint = PlayDone,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        if (matched) "Coincidieron" else "Distinto",
                        color = if (matched) PlayDone else CurlyColors.DarkTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ThisOrThatChoice(
                letter = "A",
                text = pair.a,
                color = PlayRose,
                chosenBy = listOfNotNull(
                    if (myPick == 0) myProfile else null,
                    if (otherPick == 0) otherProfile else null
                ),
                chosen = myPick == 0 || otherPick == 0
            )
            Spacer(Modifier.height(6.dp))
            ThisOrThatChoice(
                letter = "B",
                text = pair.b,
                color = PlaySky,
                chosenBy = listOfNotNull(
                    if (myPick == 1) myProfile else null,
                    if (otherPick == 1) otherProfile else null
                ),
                chosen = myPick == 1 || otherPick == 1
            )
        }
    }
}

@Composable
private fun ThisOrThatChoice(
    letter: String,
    text: String,
    color: Color,
    chosenBy: List<PlayProfile>,
    chosen: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (chosen) color.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (chosen) color else CurlyColors.DarkSurfaceElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter,
                color = if (chosen) PlayAccentText else CurlyColors.DarkTextSecondary,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = if (chosen) CurlyColors.DarkTextPrimary else CurlyColors.DarkTextSecondary,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            chosenBy.forEach { profile -> PlayAvatar(profile = profile, size = 24.dp) }
        }
    }
}
