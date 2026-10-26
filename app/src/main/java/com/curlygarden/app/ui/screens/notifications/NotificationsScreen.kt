package com.curlygarden.app.ui.screens.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private val DotColor = Color(0xFFF4B7A6)
private val OnAccent = Color(0xFF26215C)

private data class NotificationRow(
    val key: String,
    val kind: NotificationKind,
    val actorId: String,
    val unread: Boolean,
    val createdAt: Long?,
    val inboxId: String?
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenModule: (NotificationModule) -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModelFactory())
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler(onBack = onBack)
    PushPermissionPrompt()

    val rows = if (state.tab == NotificationTab.TURN) {
        state.turn.map { item ->
            NotificationRow("turn_${item.kind.key}_${item.refId.orEmpty()}", item.kind, item.actorId, false, null, null)
        }
    } else {
        state.inbox.filter { it.kind.tab == NotificationTab.NEWS }.map {
            NotificationRow(it.id, it.kind, it.actorId, !it.read, it.createdAt, it.id)
        }
    }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NotificationsHeader(onBack = onBack)
            Spacer(Modifier.height(16.dp))
            NotificationTabs(
                selected = state.tab,
                turnPending = state.turn.isNotEmpty(),
                newsPending = state.unreadCount > 0,
                onSelect = viewModel::selectTab,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> CenteredLoading()
                state.session?.partnerUid == null -> NotificationsEmpty(
                    title = "Todavía no tienes a nadie vinculado",
                    body = "Invita a alguien y aquí verás lo que te espera."
                )
                state.loadFailed && rows.isEmpty() -> NotificationsEmpty(
                    title = "No se pudieron cargar",
                    body = "Revisa tu conexión y vuelve a abrir esta pantalla."
                )
                rows.isEmpty() -> NotificationsEmpty(
                    title = if (state.tab == NotificationTab.TURN) "Nada pendiente" else "Sin novedades",
                    body = if (state.tab == NotificationTab.TURN) {
                        "Cuando el otro espere algo de ti, lo verás aquí."
                    } else {
                        "Aquí aparecerán las cartas, resultados y recuerdos nuevos."
                    }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.tab == NotificationTab.NEWS && state.unreadCount > 0) {
                        item(key = "mark_all") {
                            Text(
                                "Marcar todo como leído",
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CurlyColors.DarkLavenderLight,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = viewModel::markAllRead)
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                    itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                        NotificationCard(
                            row = row,
                            name = state.nameFor(row.actorId),
                            actor = state.profileFor(row.actorId),
                            onClick = {
                                row.inboxId?.let(viewModel::markRead)
                                onOpenModule(row.kind.module)
                            },
                            modifier = Modifier.entrance(index.coerceAtMost(8), play = index < 8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TalkCircleButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver", tint = CurlyColors.DarkTextPrimary)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            "Notificaciones",
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = CurlyColors.DarkTextPrimary
        )
    }
}

@Composable
private fun NotificationTabs(
    selected: NotificationTab,
    turnPending: Boolean,
    newsPending: Boolean,
    onSelect: (NotificationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CurlyColors.DarkSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NotificationTab.entries.forEach { tab ->
                val active = tab == selected
                val pending = if (tab == NotificationTab.TURN) turnPending else newsPending
                Surface(
                    onClick = { onSelect(tab) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (active) CurlyColors.DarkLavenderPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tab.label,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) Color.White else CurlyColors.DarkTextSecondary
                        )
                        if (pending) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(DotColor, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    row: NotificationRow,
    name: String,
    actor: TalkProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val module = row.kind.module
    val accent = Color(module.color)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurface,
        border = if (row.unread) {
            BorderStroke(1.dp, CurlyColors.DarkLavenderPrimary.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, CurlyColors.DarkBorder.copy(alpha = 0.5f))
        },
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.97f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (row.kind.usesActorName()) {
                TalkAvatar(profile = actor, fallbackColor = CurlyColors.DarkLavenderPrimary, size = 48.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(module.icon(), contentDescription = null, tint = OnAccent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notificationText(row.kind, name),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (row.unread) FontWeight.Bold else FontWeight.SemiBold,
                    color = CurlyColors.DarkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(accent, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        module.label,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = CurlyColors.DarkTextSecondary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            if (row.createdAt != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatNotificationAge(row.createdAt),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = CurlyColors.DarkTextMuted
                    )
                    if (row.unread) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(DotColor, CircleShape)
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = CurlyColors.DarkTextMuted
                )
            }
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CurlyColors.DarkLavenderPrimary, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun NotificationsEmpty(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(CurlyColors.DarkSurface, radius = size.minDimension / 2f, center = center)
                drawCircle(CurlyColors.DarkSurfaceElevated, radius = size.minDimension * 0.33f, center = center)
                drawCircle(DotColor, radius = 5.dp.toPx(), center = Offset(size.width * 0.82f, size.height * 0.24f))
                drawCircle(Color(0xFFBFB9FD), radius = 4.dp.toPx(), center = Offset(size.width * 0.14f, size.height * 0.34f))
                drawCircle(Color(0xFF9BBFE0), radius = 3.dp.toPx(), center = Offset(size.width * 0.74f, size.height * 0.86f))
            }
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = CurlyColors.DarkTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = CurlyColors.DarkTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun NotificationModule.icon(): ImageVector = when (this) {
    NotificationModule.TALK -> Icons.Outlined.ChatBubbleOutline
    NotificationModule.DRAW -> Icons.Outlined.Brush
    NotificationModule.PLAY -> Icons.Outlined.SportsEsports
    NotificationModule.IMAGINE -> Icons.Outlined.Lightbulb
    NotificationModule.MEMORIES -> Icons.Outlined.PhotoLibrary
}
