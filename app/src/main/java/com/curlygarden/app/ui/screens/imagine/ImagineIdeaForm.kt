package com.curlygarden.app.ui.screens.imagine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ImagineIdeaForm(
    initial: ImagineIdea?,
    isWorking: Boolean,
    onSubmit: (title: String, content: String, attachment: ImagineAttachment?, removeAttachment: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }
    var attachment by remember { mutableStateOf<ImagineAttachment?>(null) }
    var removeExisting by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val picked = context.readImagineAttachment(uri)
            when {
                picked == null -> attachmentError = "No se pudo leer el archivo"
                picked.sizeBytes > IMAGINE_MAX_ATTACHMENT_BYTES -> attachmentError = "El archivo supera los 20 MB"
                else -> {
                    attachment = picked
                    attachmentError = null
                    removeExisting = false
                }
            }
        }
    }

    val canSubmit = title.isNotBlank() && content.isNotBlank() && !isWorking

    ImagineField(
        value = title,
        onValueChange = { title = it },
        label = "Título",
        maxChars = IMAGINE_TITLE_MAX
    )
    Spacer(Modifier.height(8.dp))
    ImagineField(
        value = content,
        onValueChange = { content = it },
        label = "Tu idea",
        maxChars = IMAGINE_IDEA_MAX,
        minLines = 3,
        maxLines = 6
    )
    Spacer(Modifier.height(8.dp))
    ImagineAttachmentPicker(
        attachment = attachment,
        existingUrl = if (removeExisting) null else initial?.attachmentUrl,
        errorMessage = attachmentError,
        onPick = { launcher.launch("*/*") },
        onClear = {
            if (attachment != null) {
                attachment = null
            } else {
                removeExisting = true
            }
            attachmentError = null
        }
    )
    Spacer(Modifier.height(16.dp))
    ImagineFormActions(
        submitLabel = if (initial == null) "Publicar" else "Guardar",
        canSubmit = canSubmit,
        onSubmit = { onSubmit(title, content, attachment, removeExisting) },
        onDelete = if (initial != null) onDelete else null
    )
}
