package com.curlygarden.app.ui.screens.imagine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun IdeaCardContent(
    idea: ImagineIdea,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
}

@Composable
fun GoalCardContent(
    goal: ImagineGoal,
    canEdit: Boolean,
    onToggleFinished: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
}

@Composable
fun ListCardContent(
    list: ImagineList,
    canEdit: Boolean,
    onToggleItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
}
