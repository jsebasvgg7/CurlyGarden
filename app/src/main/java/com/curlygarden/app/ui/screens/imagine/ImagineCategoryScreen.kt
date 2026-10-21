package com.curlygarden.app.ui.screens.imagine

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.curlygarden.app.ui.animation.pressScale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import kotlinx.coroutines.delay

private fun ImagineCategory.addLabel(): String = when (this) {
    ImagineCategory.LISTS -> "Añade nueva lista"
    ImagineCategory.CAPSULES -> "Añade nueva cápsula"
    ImagineCategory.GOALS -> "Añade nueva meta"
    ImagineCategory.IDEAS -> "Añade nueva idea"
}

@Composable
fun ImagineCategoryScreen(
    category: ImagineCategory,
    state: ImagineUiState,
    viewModel: ImagineViewModel,
    onBack: () -> Unit
) {
    val current = state.current
    val errorMessage by viewModel.errorMessage.collectAsState()
    val uriHandler = LocalUriHandler.current
    var composer by remember { mutableStateOf<ImagineComposerTarget?>(null) }
    var revealedId by remember { mutableStateOf<String?>(null) }
    val capsule = current as? ImagineCapsule
    val myUid = state.session?.myUid
    LaunchedEffect(capsule?.id, capsule?.contributors?.size, myUid) {
        if (capsule != null && myUid != null && (capsule.isUnlocked() || myUid in capsule.contributors)) {
            viewModel.loadCapsuleEntries(capsule)
        }
    }
    val openAttachment: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            CategoryHeader(category = category, onBack = onBack)
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = if (state.data.countFor(category) == 0) {
                    Arrangement.Top
                } else {
                    Arrangement.Center
                }
            ) {
                ImagineFilterTabs(
                    category = category,
                    statusFilter = state.statusFilter,
                    shareFilter = state.shareFilter,
                    onStatusChange = viewModel::setStatusFilter,
                    onShareChange = viewModel::setShareFilter
                )
                Spacer(Modifier.height(16.dp))
                if (current == null) {
                    EmptyCategory(
                        category = category,
                        hasAny = state.data.countFor(category) > 0
                    )
                } else {
                    AnimatedContent(
                        targetState = current.id,
                        transitionSpec = {
                            val ids = state.visibleItems.map { it.id }
                            val from = ids.indexOf(initialState)
                            val to = ids.indexOf(targetState)
                            if (from < 0 || to < 0 || from == to) {
                                fadeIn() togetherWith fadeOut()
                            } else {
                                val dir = if (to > from) 1 else -1
                                (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    ) { -dir * it / 8 }) togetherWith
                                    (fadeOut(tween(160)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        ) { dir * it / 8 })
                            }
                        },
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        label = "imagine-card"
                    ) { id ->
                        val item = state.visibleItems.firstOrNull { it.id == id }
                        if (item != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                CategoryCard(
                                    item = item,
                                    state = state,
                                    viewModel = viewModel,
                                    onEdit = { composer = ImagineComposerTarget.Edit(it) },
                                    onContribute = { composer = ImagineComposerTarget.Contribute(it) },
                                    onOpenCapsule = {
                                        revealedId = it.id
                                        viewModel.loadCapsuleEntries(it)
                                    },
                                    onOpenAttachment = openAttachment
                                )
                            }
                        }
                    }
                }
            }

            if (current != null) {
                NavigationArrows(
                    canGoOlder = state.canGoOlder,
                    canGoNewer = state.canGoNewer,
                    onOlder = viewModel::goOlder,
                    onNewer = viewModel::goNewer
                )
            }

            BottomBar(
                label = category.addLabel(),
                canGoNewer = state.canGoNewer,
                canAdd = state.session != null,
                onLatest = viewModel::goLatest,
                onAdd = { composer = ImagineComposerTarget.Create(category) }
            )
        }

        composer?.let { target ->
            ImagineComposer(
                target = target,
                state = state,
                viewModel = viewModel,
                onDismiss = { composer = null }
            )
        }

        val revealed = revealedId?.let { id -> state.data.capsules.firstOrNull { it.id == id } }
        val session = state.session
        if (revealed != null && session != null) {
            CapsuleRevealDialog(
                capsule = revealed,
                entries = state.capsuleEntries[revealed.id],
                myProfile = session.myProfile,
                partnerProfile = session.partnerProfile,
                onOpenAttachment = openAttachment,
                onDismiss = { revealedId = null }
            )
        }

        errorMessage?.takeIf { composer == null }?.let { message ->
            LaunchedEffect(message) {
                delay(2500)
                viewModel.dismissError()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CurlyColors.DarkSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(message, color = CurlyColors.DarkTextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: ImagineCategory, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TalkCircleButton(onClick = onBack) {
            Icon(
                FeatherIcons.ArrowLeft,
                contentDescription = "atras",
                tint = CurlyColors.DarkTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            category.label(),
            color = CurlyColors.DarkTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        CategoryBadge(category = category, size = 44.dp)
    }
}

@Composable
private fun EmptyCategory(category: ImagineCategory, hasAny: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryBadge(category = category, size = 64.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (hasAny) {
                "No hay elementos con este filtro"
            } else {
                "Todavía no hay ${category.label().lowercase()} por aquí"
            },
            color = CurlyColors.DarkTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (!hasAny) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Usa el botón de abajo para añadir el primero.",
                color = CurlyColors.DarkTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    item: ImagineItem,
    state: ImagineUiState,
    viewModel: ImagineViewModel,
    onEdit: (ImagineShareable) -> Unit,
    onContribute: (ImagineCapsule) -> Unit,
    onOpenCapsule: (ImagineCapsule) -> Unit,
    onOpenAttachment: (String) -> Unit
) {
    val session = state.session ?: return
    when (item) {
        is ImagineIdea -> ShareableCard(
            item = item,
            statusLabel = ImagineStatus.ACTIVE.value,
            state = state,
            viewModel = viewModel,
            onEdit = onEdit
        ) {
            IdeaCardContent(idea = item, onOpenAttachment = onOpenAttachment)
        }
        is ImagineGoal -> ShareableCard(
            item = item,
            statusLabel = item.status.value,
            state = state,
            viewModel = viewModel,
            onEdit = onEdit
        ) {
            GoalCardContent(
                goal = item,
                canEdit = item.canEdit(session.myUid),
                onToggleFinished = { viewModel.setGoalFinished(item, it) }
            )
        }
        is ImagineList -> ShareableCard(
            item = item,
            statusLabel = item.status.value,
            state = state,
            viewModel = viewModel,
            onEdit = onEdit
        ) {
            ListCardContent(
                list = item,
                canEdit = item.canEdit(session.myUid),
                onToggleItem = { viewModel.toggleListItem(item, it) }
            )
        }
        is ImagineCapsule -> {
            val authors = if (item.mode == CapsuleMode.SHARED) {
                listOfNotNull(session.myProfile, session.partnerProfile)
            } else {
                listOfNotNull(state.profileFor(item.createdBy))
            }
            val unlocked = item.isUnlocked()
            val trailing: (@Composable () -> Unit)? = if (unlocked) {
                {
                    ImagineCapsuleOpenButton(
                        shared = item.mode == CapsuleMode.SHARED,
                        onClick = { onOpenCapsule(item) }
                    )
                }
            } else {
                null
            }
            ImagineCardFrame(
                item = item,
                myUid = session.myUid,
                authors = authors,
                statusLabel = ImagineStatus.ACTIVE.value,
                action = EntryAction.NONE,
                showLock = false,
                onAction = {},
                onLike = { viewModel.toggleLike(item) },
                onLock = {},
                trailing = trailing
            ) {
                if (unlocked) {
                    CapsuleReadyContent(capsule = item)
                } else {
                    CapsuleSealedContent(
                        capsule = item,
                        myUid = session.myUid,
                        myEntry = state.capsuleEntries[item.id]?.mine,
                        onOpenAttachment = onOpenAttachment,
                        onContribute = if (item.canContribute(session.myUid)) {
                            { onContribute(item) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun ShareableCard(
    item: ImagineShareable,
    statusLabel: String,
    state: ImagineUiState,
    viewModel: ImagineViewModel,
    onEdit: (ImagineShareable) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val session = state.session ?: return
    val action = item.actionFor(session.myUid)
    val authors = item.participants().mapNotNull { state.profileFor(it) }
    ImagineCardFrame(
        item = item,
        myUid = session.myUid,
        authors = authors,
        statusLabel = statusLabel,
        action = action,
        showLock = true,
        onAction = {
            if (action == EntryAction.EDIT) onEdit(item) else viewModel.join(item)
        },
        onLike = { viewModel.toggleLike(item) },
        onLock = { viewModel.toggleLocked(item) },
        content = content
    )
}

@Composable
private fun NavigationArrows(
    canGoOlder: Boolean,
    canGoNewer: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TalkCircleButton(onClick = onOlder, enabled = canGoOlder, size = 46.dp) {
            Icon(
                Icons.AutoMirrored.Outlined.Undo,
                contentDescription = "anterior",
                tint = CurlyColors.DarkTextPrimary
            )
        }
        Spacer(Modifier.width(14.dp))
        TalkCircleButton(onClick = onNewer, enabled = canGoNewer, size = 46.dp) {
            Icon(
                Icons.AutoMirrored.Outlined.Redo,
                contentDescription = "siguiente",
                tint = CurlyColors.DarkTextPrimary
            )
        }
    }
}

@Composable
private fun BottomBar(
    label: String,
    canGoNewer: Boolean,
    canAdd: Boolean,
    onLatest: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TalkCircleButton(onClick = onLatest, enabled = canGoNewer, size = 52.dp) {
            Icon(
                Icons.Outlined.KeyboardDoubleArrowUp,
                contentDescription = "mas reciente",
                tint = CurlyColors.DarkTextPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            onClick = onAdd,
            enabled = canAdd,
            shape = RoundedCornerShape(26.dp),
            color = CurlyColors.DarkSurfaceElevated,
            modifier = Modifier
                .weight(1f)
                .pressScale(pressedScale = 0.97f, enabled = canAdd)
                .height(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    label,
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
