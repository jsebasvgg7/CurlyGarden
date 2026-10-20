package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable

@Composable
fun ImagineListForm(
    initial: ImagineList?,
    isWorking: Boolean,
    onSubmit: (title: String, items: List<ListItem>) -> Unit,
    onDismiss: () -> Unit
) {
}
