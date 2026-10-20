package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.theme.CurlyColors

@Composable
fun ImagineFilterTabs(
    category: ImagineCategory,
    statusFilter: StatusFilter,
    shareFilter: ShareFilter,
    onStatusChange: (StatusFilter) -> Unit,
    onShareChange: (ShareFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (category == ImagineCategory.LISTS || category == ImagineCategory.GOALS) {
        FilterTabsRow(
            options = listOf(
                StatusFilter.ALL to "Todas",
                StatusFilter.ACTIVE to "Activas",
                StatusFilter.FINISHED to "Finalizadas"
            ),
            selected = statusFilter,
            onSelect = onStatusChange,
            modifier = modifier
        )
    } else {
        FilterTabsRow(
            options = listOf(
                ShareFilter.ALL to "Todas",
                ShareFilter.INDIVIDUAL to "Individual",
                ShareFilter.SHARED to "Compartida"
            ),
            selected = shareFilter,
            onSelect = onShareChange,
            modifier = modifier
        )
    }
}

@Composable
private fun <T> FilterTabsRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Text(
                text = label,
                color = if (isSelected) CurlyColors.DarkTextPrimary else CurlyColors.DarkTextSecondary,
                fontSize = if (isSelected) 17.sp else 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}
