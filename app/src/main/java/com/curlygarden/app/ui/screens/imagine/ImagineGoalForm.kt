package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable

@Composable
fun ImagineGoalForm(
    initial: ImagineGoal?,
    isWorking: Boolean,
    onSubmit: (title: String, content: String, targetDateText: String) -> Unit,
    onDismiss: () -> Unit
) {
}
