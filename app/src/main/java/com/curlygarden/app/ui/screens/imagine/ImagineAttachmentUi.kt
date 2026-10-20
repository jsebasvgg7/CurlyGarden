package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ImagineAttachmentView(
    url: String,
    type: AttachmentType?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
}

@Composable
fun ImagineAttachmentPicker(
    attachment: ImagineAttachment?,
    existingUrl: String?,
    errorMessage: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
}
