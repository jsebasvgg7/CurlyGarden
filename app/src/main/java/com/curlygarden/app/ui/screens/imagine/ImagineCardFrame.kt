package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ImagineCardFrame(
    item: ImagineItem,
    myUid: String,
    authors: List<ImagineProfile>,
    statusLabel: String,
    action: EntryAction,
    showLock: Boolean,
    onAction: () -> Unit,
    onLike: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
}

@Composable
fun ImagineAuthorship(
    authors: List<ImagineProfile>,
    shared: Boolean,
    modifier: Modifier = Modifier
) {
}

@Composable
fun ImagineActionButton(
    action: EntryAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
}

@Composable
fun ImagineIconRow(
    liked: Boolean,
    locked: Boolean,
    showLock: Boolean,
    canLock: Boolean,
    onLike: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
}
