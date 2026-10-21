package com.curlygarden.app.ui.screens.talk

import com.curlygarden.app.ui.animation.pressScale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TalkBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.curlyDarkBackground()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.42f,
                center = Offset(x = size.width * 1.1f, y = -size.height * 0.05f)
            )
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.48f,
                center = Offset(x = -size.width * 0.15f, y = size.height * 1.05f)
            )
        }
        content()
    }
}

@Composable
fun TalkCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = CurlyColors.DarkSurfaceElevated.copy(alpha = if (enabled) 1f else 0.5f),
        modifier = modifier.pressScale(pressedScale = 0.9f, enabled = enabled).size(size)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun TalkAvatar(
    profile: TalkProfile?,
    fallbackColor: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size)
            .background(CurlyColors.DarkBackground, shape = CircleShape)
            .padding(3.dp)
            .background(fallbackColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val photoUrl = profile?.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initial = profile?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.33f).sp
            )
        }
    }
}

private val dateFormat = SimpleDateFormat("d 'de' MMM. HH:mm", Locale("es"))
private val dayFormat = SimpleDateFormat("d MMM, HH:mm", Locale("es"))

fun formatCardDate(timestamp: Long): String =
    if (timestamp == Long.MAX_VALUE) "enviando…" else dateFormat.format(Date(timestamp))

fun formatHistoryDate(timestamp: Long): String =
    if (timestamp == Long.MAX_VALUE) "enviando…" else dayFormat.format(Date(timestamp)).uppercase(Locale("es"))