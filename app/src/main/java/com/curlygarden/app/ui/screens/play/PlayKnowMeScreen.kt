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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.components.CurlyDepthCard
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.Play
import compose.icons.feathericons.X
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PICK_ADVANCE_DELAY = 250L
private const val SCREEN_TITLE = "¿Cuánto me conoces?"
private const val OPTIONS_PER_QUESTION = 4
private const val MAX_GUESS_POINTS = PLAY_PICKS_PER_GAME * POINTS_GUESS
private val OPTION_LETTERS = listOf("A", "B", "C", "D")
private val OPTION_COLORS = listOf(PlaySky, PlayRose, PlaySky, PlayRose)

private enum class KnowMeStage {
    INTRO,
    ANSWERING,
    WAITING_ANSWERS,
    GUESSING,
    WAITING_GUESSES,
    RESULT
}

@Composable
internal fun PlayKnowMeScreen(
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    val type = PlayGameType.KNOW_ME
    val myUid = state.session?.myUid.orEmpty()
    val active = state.activeSession(type)
    val last = state.lastRevealed(type)
    val partnerName = state.session?.partnerProfile?.name
        ?.takeIf { it.isNotBlank() } ?: "el otro"
    val stage = when {
        active == null && last == null -> KnowMeStage.INTRO
        active == null -> KnowMeStage.RESULT
        active.status == SessionStatus.GUESSING ->
            if (myUid in active.guessedBy) KnowMeStage.WAITING_GUESSES else KnowMeStage.GUESSING
        myUid in active.answeredBy -> KnowMeStage.WAITING_ANSWERS
        else -> KnowMeStage.ANSWERING
    }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if ((stage == KnowMeStage.ANSWERING || stage == KnowMeStage.GUESSING) && active != null) {
                KnowMePicking(
                    game = active,
                    guessing = stage == KnowMeStage.GUESSING,
                    partnerName = partnerName,
                    state = state,
                    viewModel = viewModel,
                    onBack = onBack
                )
            } else {
                PlayHeader(
                    title = SCREEN_TITLE,
                    subtitle = when (stage) {
                        KnowMeStage.INTRO -> "5 preguntas, 4 opciones"
                        KnowMeStage.WAITING_ANSWERS, KnowMeStage.WAITING_GUESSES -> "Esperando al otro"
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
                        KnowMeStage.INTRO -> KnowMeIntro(state, viewModel)
                        KnowMeStage.WAITING_ANSWERS -> PlayWaitingContent(
                            color = PlaySky,
                            message = "Ya respondiste las 5 preguntas sobre ti. Cuando el otro termine, podrás adivinar sus respuestas.",
                            pill = { modifier ->
                                PlayPointsPill(points = MAX_GUESS_POINTS, prefix = "Hasta ", modifier = modifier)
                            }
                        )
                        KnowMeStage.WAITING_GUESSES -> PlayWaitingContent(
                            color = PlaySky,
                            message = "Ya enviaste lo que crees que respondió $partnerName. El resultado aparece cuando termine.",
                            pill = { modifier ->
                                PlayPointsPill(points = MAX_GUESS_POINTS, prefix = "Hasta ", modifier = modifier)
                            }
                        )
                        else -> if (last != null) KnowMeResult(last, state, viewModel, partnerName)
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowMeIntro(state: PlayUiState, viewModel: PlayViewModel) {
    CurlyDepthCard(
        color = PlaySky,
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
                    FeatherIcons.HelpCircle,
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
                "Cinco preguntas con cuatro opciones. Primero respondes sobre ti y, cuando los dos terminen, adivinas qué respondió el otro.",
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
        PlayPointsPill(points = MAX_GUESS_POINTS, prefix = "Hasta ")
    }
    Spacer(Modifier.height(20.dp))
    DailyButton(
        label = "Empezar",
        icon = FeatherIcons.Play,
        enabled = !state.isWorking,
        working = state.isWorking,
        onClick = { viewModel.startGame(PlayGameType.KNOW_ME) },
        modifier = Modifier.entrance(2)
    )
}

@Composable
private fun ColumnScope.KnowMePicking(
    game: GameSession,
    guessing: Boolean,
    partnerName: String,
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    val total = PLAY_PICKS_PER_GAME
    val questions = remember(game.id) { game.questionIds.mapNotNull { PlayBank.knowMe.getOrNull(it) } }
    val picks = remember(game.id, guessing) { mutableStateMapOf<Int, Int>() }
    var index by remember(game.id, guessing) { mutableIntStateOf(0) }
    var locked by remember(game.id, guessing) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = index > 0 || locked) {
        if (!locked) index -= 1
    }

    PlayHeader(
        title = SCREEN_TITLE,
        subtitle = "${if (guessing) "Adivina" else "Sobre ti"} · ${index + 1} de $total",
        onBack = {
            when {
                locked -> Unit
                index > 0 -> index -= 1
                else -> onBack()
            }
        }
    )

    if (questions.size < total || questions.any { it.options.size < OPTIONS_PER_QUESTION }) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PlayCenteredNotice("Esta partida usa preguntas que ya no están disponibles.")
        }
        return
    }

    PlayProgressBar(
        total = total,
        current = index,
        modifier = Modifier.padding(top = 20.dp)
    )

    val question = questions[index]
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
            KnowMeQuestionCard(
                label = if (guessing) "¿Qué respondió $partnerName?" else "Responde sobre ti",
                prompt = question.prompt,
                modifier = Modifier.entrance(0)
            )
            question.options.take(OPTIONS_PER_QUESTION).forEachIndexed { i, option ->
                PlayChoiceCard(
                    letter = OPTION_LETTERS[i],
                    text = option,
                    color = OPTION_COLORS[i],
                    selected = picks[index] == i,
                    enabled = canPick,
                    onClick = { choose(i) },
                    modifier = Modifier.entrance(i + 1),
                    minHeight = 64.dp,
                    textSize = 18.sp,
                    textLineHeight = 24.sp
                )
            }
        }
    }

    if (index == total - 1) {
        DailyButton(
            label = if (guessing) "Enviar mis suposiciones" else "Enviar mis respuestas",
            icon = FeatherIcons.Check,
            enabled = picks.size == total && !state.isWorking,
            working = state.isWorking,
            onClick = {
                val list = List(total) { picks.getValue(it) }
                if (guessing) viewModel.submitGuesses(list) else viewModel.submitAnswers(list)
            },
            modifier = Modifier.padding(top = 12.dp)
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
private fun KnowMeQuestionCard(
    label: String,
    prompt: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                label,
                color = CurlyColors.DarkLavenderLight,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                prompt,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun KnowMeResult(
    game: GameSession,
    state: PlayUiState,
    viewModel: PlayViewModel,
    partnerName: String
) {
    LaunchedEffect(game.id) { viewModel.loadReveal(game.id) }

    val total = PLAY_PICKS_PER_GAME
    val session = state.session
    val reveal = state.reveal
    val questions = game.questionIds.mapNotNull { PlayBank.knowMe.getOrNull(it) }

    if (reveal == null) {
        PlayResultLoading()
        return
    }

    val myUid = session?.myUid
    val otherUid = session?.partnerUid
    val myAnswers = myUid?.let { reveal.answers[it] }
    val otherAnswers = otherUid?.let { reveal.answers[it] }
    val myGuesses = myUid?.let { reveal.guesses[it] }
    val otherGuesses = otherUid?.let { reveal.guesses[it] }
    if (
        myAnswers == null || otherAnswers == null || myGuesses == null || otherGuesses == null ||
        questions.size < total ||
        listOf(myAnswers, otherAnswers, myGuesses, otherGuesses).any { it.size < total }
    ) {
        PlayCenteredNotice("No se pudo cargar el resultado.")
        return
    }

    val myHits = List(total) { myGuesses[it] == otherAnswers[it] }
    val otherHits = List(total) { otherGuesses[it] == myAnswers[it] }
    val myScore = myHits.count { it }
    val otherScore = otherHits.count { it }

    CurlyDepthCard(
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
                "Aciertos",
                color = PlayAccentText,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                KnowMeScore(
                    name = "Tú",
                    score = myScore,
                    total = total,
                    modifier = Modifier.weight(1f)
                )
                KnowMeScore(
                    name = partnerName,
                    score = otherScore,
                    total = total,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    if (myScore > 0) {
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(1),
            contentAlignment = Alignment.Center
        ) {
            PlayPointsPill(points = myScore * POINTS_GUESS)
        }
    }
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        questions.take(total).forEachIndexed { i, question ->
            KnowMeResultRow(
                number = i + 1,
                prompt = question.prompt,
                myAnswer = question.options.getOrNull(myAnswers[i]).orEmpty(),
                otherAnswer = question.options.getOrNull(otherAnswers[i]).orEmpty(),
                myGuess = question.options.getOrNull(myGuesses[i]).orEmpty(),
                otherGuess = question.options.getOrNull(otherGuesses[i]).orEmpty(),
                myHit = myHits[i],
                otherHit = otherHits[i],
                partnerName = partnerName,
                myProfile = session?.myProfile,
                otherProfile = session?.partnerProfile,
                modifier = Modifier.entrance(i + 2)
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    DailyButton(
        label = "Jugar otra partida",
        icon = FeatherIcons.Play,
        enabled = !state.isWorking,
        working = state.isWorking,
        onClick = { viewModel.startGame(PlayGameType.KNOW_ME) },
        modifier = Modifier.entrance(total + 2)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun KnowMeScore(
    name: String,
    score: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$score de $total",
            color = PlayAccentText,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            name,
            color = PlayAccentText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KnowMeResultRow(
    number: Int,
    prompt: String,
    myAnswer: String,
    otherAnswer: String,
    myGuess: String,
    otherGuess: String,
    myHit: Boolean,
    otherHit: Boolean,
    partnerName: String,
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
            Text(
                "Pregunta $number",
                color = CurlyColors.DarkTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                prompt,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            KnowMeLine(
                profile = myProfile,
                label = "Tú respondiste",
                text = myAnswer,
                hit = null
            )
            KnowMeLine(
                profile = otherProfile,
                label = "$partnerName adivinó",
                text = otherGuess,
                hit = otherHit
            )
            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CurlyColors.DarkBorder)
            )
            KnowMeLine(
                profile = otherProfile,
                label = "$partnerName respondió",
                text = otherAnswer,
                hit = null
            )
            KnowMeLine(
                profile = myProfile,
                label = "Tú adivinaste",
                text = myGuess,
                hit = myHit
            )
        }
    }
}

@Composable
private fun KnowMeLine(
    profile: PlayProfile?,
    label: String,
    text: String,
    hit: Boolean?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayAvatar(profile = profile, size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = CurlyColors.DarkTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (hit != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        (if (hit) PlayDone else PlayRose).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hit) FeatherIcons.Check else FeatherIcons.X,
                    contentDescription = if (hit) "Acertó" else "Falló",
                    tint = if (hit) PlayDone else PlayRose,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
