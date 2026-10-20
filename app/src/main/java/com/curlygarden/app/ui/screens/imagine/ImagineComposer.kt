package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable

sealed interface ImagineComposerTarget {
    data class Create(val category: ImagineCategory) : ImagineComposerTarget
    data class Edit(val item: ImagineShareable) : ImagineComposerTarget
    data class Contribute(val capsule: ImagineCapsule) : ImagineComposerTarget
}

@Composable
fun ImagineComposer(
    target: ImagineComposerTarget,
    state: ImagineUiState,
    viewModel: ImagineViewModel,
    onDismiss: () -> Unit
) {
}
