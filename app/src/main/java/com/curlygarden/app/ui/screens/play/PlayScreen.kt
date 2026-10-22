package com.curlygarden.app.ui.screens.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.screens.talk.TalkAvatar
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.screens.talk.TalkProfile
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
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
internal fun PlayPointsPill(points: Int, modifier: Modifier = Modifier, prefix: String = "") {
    Box(
        modifier = modifier
            .background(PlayAccent, RoundedCornerShape(50))
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Text(
            "$prefix+$points puntos",
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
