package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImagineGoalForm(
    initial: ImagineGoal?,
    isWorking: Boolean,
    onSubmit: (title: String, content: String, targetDateText: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.targetDateText.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }

    val canSubmit = title.isNotBlank() && date.isNotBlank() && !isWorking

    ImagineField(
        value = title,
        onValueChange = { title = it },
        label = "Título",
        maxChars = IMAGINE_TITLE_MAX
    )
    Spacer(Modifier.height(8.dp))
    ImagineField(
        value = date,
        onValueChange = { date = it },
        label = "¿Para cuándo? Ej. Un año",
        maxChars = IMAGINE_GOAL_DATE_MAX
    )
    Spacer(Modifier.height(8.dp))
    ImagineField(
        value = content,
        onValueChange = { content = it },
        label = "Descripción (opcional)",
        maxChars = IMAGINE_GOAL_CONTENT_MAX,
        minLines = 3,
        maxLines = 6
    )
    Spacer(Modifier.height(16.dp))
    ImagineFormActions(
        submitLabel = if (initial == null) "Publicar" else "Guardar",
        canSubmit = canSubmit,
        onSubmit = { onSubmit(title, content, date) },
        onDelete = if (initial != null) onDelete else null
    )
}
