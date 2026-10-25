package com.curlygarden.app.ui.screens.memories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.EyeOff
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Grid

internal val MemoriesOutline = CurlyColors.DarkLavenderLight.copy(alpha = 0.4f)

private val RingWidth = 1.5.dp
private val RingGap = 4.dp

@Composable
internal fun MemoriesRingBadge(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .border(RingWidth, color, CircleShape)
            .padding(RingGap)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
internal fun MemoriesRingButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: (() -> Unit)?,
    size: Dp,
    color: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = CircleShape,
        color = color,
        modifier = modifier
            .size(size)
            .pressScale(pressedScale = 0.9f, enabled = onClick != null)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

@Composable
internal fun MemoriesPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val active = enabled && !loading
    Surface(
        onClick = onClick,
        enabled = active,
        shape = CircleShape,
        color = CurlyColors.DarkLavenderPrimary,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .pressScale(pressedScale = 0.97f, enabled = active)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun MemoriesNavPill(
    selected: MemoriesTab,
    onSelect: (MemoriesTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(CurlyColors.DarkSurfaceElevated, CircleShape)
            .border(1.dp, MemoriesOutline, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemoriesTab.values().forEach { tab ->
            NavPillItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun NavPillItem(tab: MemoriesTab, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) CurlyColors.DarkLavenderPrimary else Color.Transparent,
        modifier = Modifier
            .size(48.dp)
            .pressScale(pressedScale = 0.92f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                tab.icon(),
                contentDescription = tab.label,
                tint = if (selected) Color.White else CurlyColors.DarkTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun MemoriesTab.icon(): ImageVector = when (this) {
    MemoriesTab.ALBUMS -> FeatherIcons.Folder
    MemoriesTab.ALL -> FeatherIcons.Grid
    MemoriesTab.HIDDEN -> FeatherIcons.EyeOff
}