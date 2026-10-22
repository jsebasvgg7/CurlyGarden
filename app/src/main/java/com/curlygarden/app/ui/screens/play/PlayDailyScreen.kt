package com.curlygarden.app.ui.screens.play

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Sun
import compose.icons.feathericons.Zap

private fun dailyPrompt(type: DailyType, index: Int): String {
    val bank = if (type == DailyType.QUESTION) PlayBank.dailyQuestions else PlayBank.dailyActions
    return bank.getOrNull(index).orEmpty()
}

@Composable
internal fun PlayDailyScreen(
    state: PlayUiState,
    viewModel: PlayViewModel,
    onBack: () -> Unit
) {
    val cardState = state.dailyState
    val subtitle = when (cardState) {
        DailyCardState.PENDING -> "Pregunta o acción"
        DailyCardState.WAITING -> "Esperando al otro"
        DailyCardState.COMPLETED -> "Resultado"
    }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PlayHeader(title = "Reto del día", subtitle = subtitle, onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp)
            ) {
                if (state.dateKey.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CurlyColors.DarkLavenderPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    val prompt = dailyPrompt(state.dailyType, state.dailyBankIndex)
                    val isQuestion = state.dailyType == DailyType.QUESTION
                    when (cardState) {
                        DailyCardState.PENDING ->
                            if (isQuestion) {
                                DailyQuestionForm(prompt, state, viewModel)
                            } else {
                                DailyActionForm(prompt, state, viewModel)
                            }
                        DailyCardState.WAITING ->
                            DailyWaiting(prompt, isQuestion, state)
                        DailyCardState.COMPLETED ->
                            DailyResult(prompt, isQuestion, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyPromptCard(
    prompt: String,
    isQuestion: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (completed) {
                Row(
                    modifier = Modifier
                        .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(50))
                        .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(PlayDone, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = null,
                            tint = PlayAccentText,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Completado",
                        color = CurlyColors.DarkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DailyIconBadge(if (isQuestion) FeatherIcons.Sun else FeatherIcons.Zap)
                    Spacer(Modifier.width(10.dp))
                    PlayPill(text = "Hoy", filled = false)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                prompt,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DailyIconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(CurlyColors.DarkSurfaceElevated, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CurlyColors.DarkLavenderLight,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DailyQuestionForm(
    prompt: String,
    state: PlayUiState,
    viewModel: PlayViewModel
) {
    var text by rememberSaveable { mutableStateOf("") }
    val canSend = text.trim().isNotEmpty() && !state.isWorking

    DailyPromptCard(prompt, isQuestion = true, completed = false, modifier = Modifier.entrance(0))
    Spacer(Modifier.height(16.dp))
    TextField(
        value = text,
        onValueChange = { if (it.length <= PLAY_ANSWER_MAX) text = it },
        modifier = Modifier
            .fillMaxWidth()
            .entrance(1),
        placeholder = {
            Text("Escribe tu respuesta...", color = CurlyColors.DarkTextMuted)
        },
        minLines = 4,
        maxLines = 6,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CurlyColors.DarkSurface,
            unfocusedContainerColor = CurlyColors.DarkSurface,
            focusedTextColor = CurlyColors.DarkTextPrimary,
            unfocusedTextColor = CurlyColors.DarkTextPrimary,
            cursorColor = CurlyColors.DarkLavenderPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "${text.length}/$PLAY_ANSWER_MAX",
        color = if (text.length >= PLAY_ANSWER_MAX - 10) {
            CurlyColors.DarkLavenderLight
        } else {
            CurlyColors.DarkTextMuted
        },
        fontSize = 12.sp,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    DailyButton(
        label = "Enviar",
        icon = null,
        enabled = canSend,
        working = state.isWorking,
        onClick = { viewModel.submitDailyText(text) },
        modifier = Modifier.entrance(2)
    )
}

@Composable
private fun DailyActionForm(
    prompt: String,
    state: PlayUiState,
    viewModel: PlayViewModel
) {
    DailyPromptCard(prompt, isQuestion = false, completed = false, modifier = Modifier.entrance(0))
    Spacer(Modifier.height(20.dp))
    DailyButton(
        label = "Hecho",
        icon = FeatherIcons.Check,
        enabled = !state.isWorking,
        working = state.isWorking,
        onClick = viewModel::markActionDone,
        modifier = Modifier.entrance(1)
    )
    Spacer(Modifier.height(18.dp))
    Text(
        "Cuando los dos lo marquen, se revelará el resultado.",
        color = CurlyColors.DarkLavenderLight,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .entrance(2)
    )
}

@Composable
private fun DailyWaiting(
    prompt: String,
    isQuestion: Boolean,
    state: PlayUiState
) {
    DailyPromptCard(prompt, isQuestion, completed = false, modifier = Modifier.entrance(0))
    Spacer(Modifier.height(16.dp))
    if (isQuestion) {
        val mine = state.dailyResponses.mine?.text
        if (!mine.isNullOrBlank()) {
            DailyAnswerCard(
                profile = state.session?.myProfile,
                name = "Tú",
                text = mine,
                modifier = Modifier.entrance(1)
            )
            Spacer(Modifier.height(24.dp))
        }
        DailyWaitNotice("Ya respondiste. Esperando al otro...", Modifier.entrance(2))
    } else {
        DailyButton(
            label = "Hecho",
            icon = FeatherIcons.Check,
            enabled = false,
            working = false,
            onClick = {},
            modifier = Modifier.entrance(1)
        )
        Spacer(Modifier.height(24.dp))
        DailyWaitNotice("Ya lo marcaste. Esperando al otro...", Modifier.entrance(2))
    }
}

@Composable
private fun DailyResult(
    prompt: String,
    isQuestion: Boolean,
    state: PlayUiState
) {
    DailyPromptCard(prompt, isQuestion, completed = true, modifier = Modifier.entrance(0))
    Spacer(Modifier.height(12.dp))
    if (isQuestion) {
        val mine = state.dailyResponses.mine?.text
        val partner = state.dailyResponses.partner?.text
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!mine.isNullOrBlank()) {
                DailyAnswerCard(
                    profile = state.session?.myProfile,
                    name = "Tú",
                    text = mine,
                    modifier = Modifier.entrance(1)
                )
            }
            if (!partner.isNullOrBlank()) {
                DailyAnswerCard(
                    profile = state.session?.partnerProfile,
                    name = state.session?.partnerProfile?.name?.ifBlank { null } ?: "El otro",
                    text = partner,
                    modifier = Modifier.entrance(2)
                )
            }
        }
    } else {
        Text(
            "Los dos lo hicieron.",
            color = CurlyColors.DarkTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .entrance(1)
        )
    }
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .entrance(3),
        contentAlignment = Alignment.Center
    ) {
        PlayPointsPill(points = POINTS_DAILY)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DailyAnswerCard(
    profile: PlayProfile?,
    name: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayAvatar(profile = profile, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    name,
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 17.sp,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun DailyWaitNotice(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            FeatherIcons.Clock,
            contentDescription = null,
            tint = CurlyColors.DarkLavenderLight,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            color = CurlyColors.DarkTextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun DailyButton(
    label: String,
    icon: ImageVector?,
    enabled: Boolean,
    working: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        color = CurlyColors.DarkLavenderPrimary.copy(alpha = if (enabled || working) 1f else 0.4f),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressScale(pressedScale = 0.97f, enabled = enabled)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (working) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
