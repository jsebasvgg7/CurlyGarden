package com.curlygarden.app.ui.screens.imagine

import com.curlygarden.app.ui.animation.pressScale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.DownloadCloud
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Inbox
import compose.icons.feathericons.Layers
import compose.icons.feathericons.MapPin

private val ListsColor = Color(0xFF22C55E)
private val CapsulesColor = Color(0xFFD97706)
private val GoalsColor = Color(0xFFEF2B2B)
private val IdeasColor = Color(0xFF8E7FE8)

internal fun ImagineCategory.label(): String = when (this) {
    ImagineCategory.LISTS -> "Listas"
    ImagineCategory.CAPSULES -> "Cápsulas"
    ImagineCategory.GOALS -> "Metas"
    ImagineCategory.IDEAS -> "Ideas"
}

internal fun ImagineCategory.color(): Color = when (this) {
    ImagineCategory.LISTS -> ListsColor
    ImagineCategory.CAPSULES -> CapsulesColor
    ImagineCategory.GOALS -> GoalsColor
    ImagineCategory.IDEAS -> IdeasColor
}

internal fun ImagineCategory.icon(): ImageVector = when (this) {
    ImagineCategory.LISTS -> FeatherIcons.CheckCircle
    ImagineCategory.CAPSULES -> FeatherIcons.Inbox
    ImagineCategory.GOALS -> FeatherIcons.MapPin
    ImagineCategory.IDEAS -> FeatherIcons.Edit2
}

private fun elementsLabel(count: Int): String =
    if (count == 1) "1 elemento" else "$count elementos"

@Composable
fun ImagineScreen(onBack: () -> Unit = {}) {
    val viewModel: ImagineViewModel = viewModel(factory = ImagineViewModelFactory())
    val state by viewModel.uiState.collectAsState()
    val category = state.category

    BackHandler(enabled = category != null) { viewModel.selectCategory(null) }

    if (category == null) {
        ImagineIndex(
            state = state,
            onBack = onBack,
            onSelect = viewModel::selectCategory
        )
    } else {
        ImagineCategoryScreen(
            category = category,
            state = state,
            viewModel = viewModel,
            onBack = { viewModel.selectCategory(null) }
        )
    }
}

@Composable
private fun ImagineIndex(
    state: ImagineUiState,
    onBack: () -> Unit,
    onSelect: (ImagineCategory) -> Unit
) {
    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            TalkCircleButton(onClick = onBack) {
                Icon(
                    FeatherIcons.ArrowLeft,
                    contentDescription = "atras",
                    tint = CurlyColors.DarkTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        color = CurlyColors.DarkLavenderPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    state.session == null -> IndexNotice(
                        "Todavía no tienes a nadie vinculado. Invita a alguien para poder imaginar juntos."
                    )
                    state.loadFailed -> IndexNotice("No se pudieron cargar los elementos")
                    else -> IndexContent(state = state, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun IndexContent(
    state: ImagineUiState,
    onSelect: (ImagineCategory) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = CurlyColors.DarkSurfaceElevated,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    FeatherIcons.DownloadCloud,
                    contentDescription = null,
                    tint = CurlyColors.DarkTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Imagina",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Ideas que construimos juntos.",
            color = CurlyColors.DarkTextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(36.dp))

        ImagineCategory.values().toList().chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { category ->
                    CategoryCard(
                        category = category,
                        count = state.data.countFor(category),
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Icon(
            FeatherIcons.Layers,
            contentDescription = null,
            tint = CurlyColors.DarkTextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            elementsLabel(state.data.total),
            color = CurlyColors.DarkTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryCard(
    category: ImagineCategory,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier.pressScale().aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                count.toString(),
                color = CurlyColors.DarkTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 14.dp)
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CategoryBadge(category = category, size = 56.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    category.label(),
                    color = CurlyColors.DarkTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
internal fun CategoryBadge(category: ImagineCategory, size: Dp) {
    Surface(
        shape = CircleShape,
        color = category.color(),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                category.icon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
private fun IndexNotice(message: String) {
    Text(
        message,
        color = CurlyColors.DarkTextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    )
}
