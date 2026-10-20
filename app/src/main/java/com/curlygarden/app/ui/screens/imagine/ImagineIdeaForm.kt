package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable

@Composable
fun ImagineIdeaForm(
    initial: ImagineIdea?,
    isWorking: Boolean,
    onSubmit: (title: String, content: String, attachment: ImagineAttachment?, removeAttachment: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
}
