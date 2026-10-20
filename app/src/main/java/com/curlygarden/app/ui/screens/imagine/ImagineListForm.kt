package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.X

@Composable
fun ImagineListForm(
    initial: ImagineList?,
    isWorking: Boolean,
    onSubmit: (title: String, items: List<ListItem>) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    val items = remember { mutableStateListOf<ListItem>().apply { addAll(initial?.items.orEmpty()) } }

    val canSubmit = title.isNotBlank() && !isWorking

    ImagineField(
        value = title,
        onValueChange = { title = it },
        label = "Título",
        maxChars = IMAGINE_TITLE_MAX
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Ítems",
        color = CurlyColors.DarkTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    items.forEachIndexed { index, item ->
        ListItemRow(
            text = item.text,
            onTextChange = { items[index] = items[index].copy(text = it) },
            onRemove = { items.removeAt(index) }
        )
        Spacer(Modifier.height(6.dp))
    }
    AddItemButton(
        enabled = items.size < IMAGINE_LIST_MAX_ITEMS,
        onClick = { items.add(ListItem("")) }
    )
    Spacer(Modifier.height(16.dp))
    ImagineFormActions(
        submitLabel = if (initial == null) "Publicar" else "Guardar",
        canSubmit = canSubmit,
        onSubmit = { onSubmit(title, items.toList()) },
        onDelete = if (initial != null) onDelete else null
    )
}

@Composable
private fun ListItemRow(
    text: String,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { if (it.length <= IMAGINE_LIST_ITEM_MAX) onTextChange(it) },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Escribe un ítem", color = CurlyColors.DarkTextMuted) },
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            colors = imagineFieldColors()
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            FeatherIcons.X,
            contentDescription = "quitar ítem",
            tint = CurlyColors.DarkTextSecondary,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(7.dp)
        )
    }
}

@Composable
private fun AddItemButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = CurlyColors.DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                FeatherIcons.Plus,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (enabled) "Agregar ítem" else "Máximo $IMAGINE_LIST_MAX_ITEMS ítems",
                color = CurlyColors.DarkTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
