package com.curlygarden.app.ui.screens.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.components.CurlyDepthCard
import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Clock

@Composable
internal fun PlayProgressBar(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { step ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (step <= current) PlayAccent else CurlyColors.DarkSurfaceElevated,
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
internal fun PlayChoiceCard(
    letter: String,
    text: String,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 112.dp,
    textSize: TextUnit = 22.sp,
    textLineHeight: TextUnit = 28.sp
) {
    CurlyDepthCard(
        color = color,
        shape = RoundedCornerShape(24.dp),
        onClick = if (enabled) onClick else null,
        modifier = modifier
            .pressScale(pressedScale = 0.97f, enabled = enabled)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PlayAccentText.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letter,
                    color = PlayAccentText,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text,
                color = PlayAccentText,
                fontSize = textSize,
                lineHeight = textLineHeight,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (selected) {
                            Modifier.background(PlayAccentText, CircleShape)
                        } else {
                            Modifier.border(2.dp, PlayAccentText.copy(alpha = 0.3f), CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlayWaitingContent(
    color: Color,
    message: String,
    pill: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .entrance(0),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                FeatherIcons.Clock,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Esperando al otro",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.entrance(1)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            color = CurlyColors.DarkTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .entrance(2)
        )
        Spacer(Modifier.height(20.dp))
        pill(Modifier.entrance(3))
    }
}

@Composable
internal fun PlayResultLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = CurlyColors.DarkLavenderPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}
