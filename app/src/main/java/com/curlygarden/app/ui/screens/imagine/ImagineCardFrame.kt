package com.curlygarden.app.ui.screens.imagine

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Heart
import compose.icons.feathericons.Lock
import compose.icons.feathericons.Maximize2
import compose.icons.feathericons.Unlock

private val ActiveBadgeColor = Color(0xFF22C55E)
private val LikedColor = Color(0xFFF4B7A6)

@Composable
fun ImagineCardFrame(
    item: ImagineItem,
    myUid: String,
    authors: List<ImagineProfile>,
    statusLabel: String,
    action: EntryAction,
    showLock: Boolean,
    onAction: () -> Unit,
    onLike: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shared = authors.size > 1
    val locked = (item as? ImagineShareable)?.locked == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        StatusBadge(statusLabel)
        Spacer(Modifier.height(10.dp))
        Text(
            text = item.title,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatImagineCreated(item.createdAt),
            color = CurlyColors.DarkTextMuted,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ImagineAuthorship(
                authors = authors,
                shared = shared,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            if (trailing != null) {
                trailing()
            } else {
                ImagineActionButton(action = action, onClick = onAction, shared = shared)
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
        Spacer(Modifier.height(12.dp))
        ImagineIconRow(
            liked = item.isLikedBy(myUid),
            locked = locked,
            showLock = showLock,
            canLock = item.createdBy == myUid,
            onLike = onLike,
            onLock = onLock,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun StatusBadge(label: String) {
    val color = if (label == ImagineStatus.FINISHED.value) {
        CurlyColors.DarkLavenderPrimary
    } else {
        ActiveBadgeColor
    }
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ImagineAuthorship(
    authors: List<ImagineProfile>,
    shared: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box {
            authors.take(2).forEachIndexed { index, profile ->
                ImagineAvatar(
                    profile = profile,
                    size = 46.dp,
                    modifier = Modifier.padding(start = (index * 30).dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = if (shared) "Compartida" else "Publicado por",
                color = CurlyColors.DarkTextSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
            Text(
                text = authors
                    .map { it.name.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(" & "),
                color = CurlyColors.DarkTextPrimary,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ImagineAvatar(
    profile: ImagineProfile,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(CurlyColors.DarkSurfaceElevated, CircleShape)
            .padding(2.dp)
            .background(CurlyColors.DarkLavenderPrimary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (profile.photoUrl.isNotBlank()) {
            AsyncImage(
                model = profile.photoUrl,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = profile.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

@Composable
fun ImagineActionButton(
    action: EntryAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shared: Boolean = false
) {
    when (action) {
        EntryAction.NONE -> Unit
        EntryAction.JOIN -> ActionPill(label = "Unirse", onClick = onClick, modifier = modifier)
        EntryAction.EDIT -> if (shared) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = CurlyColors.DarkLavenderPrimary,
                modifier = modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        FeatherIcons.Edit2,
                        contentDescription = "editar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            ActionPill(label = "Editar", onClick = onClick, modifier = modifier)
        }
    }
}

@Composable
fun ImagineCapsuleOpenButton(
    shared: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shared) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = CurlyColors.DarkLavenderPrimary,
            modifier = modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    FeatherIcons.Maximize2,
                    contentDescription = "abrir",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else {
        ActionPill(label = "Abrir", onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun ActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = CurlyColors.DarkLavenderPrimary,
        modifier = modifier
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ImagineIconRow(
    liked: Boolean,
    locked: Boolean,
    showLock: Boolean,
    canLock: Boolean,
    onLike: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLock) {
            IconCircle(onClick = if (canLock) onLock else null) {
                Icon(
                    if (locked) FeatherIcons.Lock else FeatherIcons.Unlock,
                    contentDescription = if (locked) "bloqueada" else "desbloqueada",
                    tint = if (locked) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IconCircle(onClick = onLike) {
            Icon(
                if (liked) Icons.Filled.Favorite else FeatherIcons.Heart,
                contentDescription = "me gusta",
                tint = if (liked) LikedColor else CurlyColors.DarkTextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        IconCircle(onClick = null) {
            Icon(
                FeatherIcons.Camera,
                contentDescription = null,
                tint = CurlyColors.DarkTextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun IconCircle(
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = CurlyColors.DarkSurface,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    } else {
        Surface(
            shape = CircleShape,
            color = CurlyColors.DarkSurface,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}
