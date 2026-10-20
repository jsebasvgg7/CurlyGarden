package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable

@Composable
fun ImagineCapsuleCreateForm(
    isWorking: Boolean,
    onSubmit: (
        title: String,
        mode: CapsuleMode,
        unlockDate: Long,
        text: String,
        attachment: ImagineAttachment?
    ) -> Unit,
    onDismiss: () -> Unit
) {
}

@Composable
fun ImagineCapsuleContributeForm(
    capsule: ImagineCapsule,
    isWorking: Boolean,
    onSubmit: (text: String, attachment: ImagineAttachment?) -> Unit,
    onDismiss: () -> Unit
) {
}
