package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CapsuleSealedContent(
    capsule: ImagineCapsule,
    modifier: Modifier = Modifier
) {
}

@Composable
fun CapsuleOpenedContent(
    capsule: ImagineCapsule,
    entries: CapsuleEntries?,
    myProfile: ImagineProfile,
    partnerProfile: ImagineProfile?,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
}

@Composable
fun CapsuleRevealDialog(
    capsule: ImagineCapsule,
    entries: CapsuleEntries?,
    myProfile: ImagineProfile,
    partnerProfile: ImagineProfile?,
    onOpenAttachment: (String) -> Unit,
    onDismiss: () -> Unit
) {
}
