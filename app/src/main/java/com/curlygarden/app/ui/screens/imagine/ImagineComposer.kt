package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors

private val FormErrorColor = Color(0xFFF4B7A6)

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
    val error by viewModel.errorMessage.collectAsState()
    val myUid = state.session?.myUid
    val locked = state.isWorking
    val close: () -> Unit = {
        viewModel.dismissError()
        onDismiss()
    }
    val created: () -> Unit = {
        viewModel.goLatest()
        close()
    }

    LaunchedEffect(target) { viewModel.dismissError() }

    when (target) {
        is ImagineComposerTarget.Create -> when (target.category) {
            ImagineCategory.IDEAS -> ComposerShell("Nueva idea", error, locked, close) {
                ImagineIdeaForm(
                    initial = null,
                    isWorking = state.isWorking,
                    onSubmit = { title, content, attachment, _ ->
                        viewModel.createIdea(title, content, attachment, created)
                    },
                    onDismiss = close,
                    onDelete = null
                )
            }
            ImagineCategory.GOALS -> ComposerShell("Nueva meta", error, locked, close) {
                ImagineGoalForm(
                    initial = null,
                    isWorking = state.isWorking,
                    onSubmit = { title, content, date ->
                        viewModel.createGoal(title, content, date, created)
                    },
                    onDismiss = close,
                    onDelete = null
                )
            }
            ImagineCategory.LISTS -> ComposerShell("Nueva lista", error, locked, close) {
                ImagineListForm(
                    initial = null,
                    isWorking = state.isWorking,
                    onSubmit = { title, items ->
                        viewModel.createList(title, items, created)
                    },
                    onDismiss = close,
                    onDelete = null
                )
            }
            ImagineCategory.CAPSULES -> ComposerShell("Nueva cápsula", error, locked, close) {
                ImagineCapsuleCreateForm(
                    canShare = state.session?.partnerUid != null,
                    isWorking = state.isWorking,
                    onSubmit = { title, mode, unlockDate, text, attachment ->
                        viewModel.createCapsule(title, mode, unlockDate, text, attachment, created)
                    },
                    onDismiss = close
                )
            }
        }
        is ImagineComposerTarget.Edit -> when (val item = target.item) {
            is ImagineIdea -> ComposerShell("Editar idea", error, locked, close) {
                ImagineIdeaForm(
                    initial = item,
                    isWorking = state.isWorking,
                    onSubmit = { title, content, attachment, remove ->
                        viewModel.updateIdea(item, title, content, attachment, remove, close)
                    },
                    onDismiss = close,
                    onDelete = if (item.createdBy == myUid) {
                        {
                            viewModel.delete(item)
                            close()
                        }
                    } else {
                        null
                    }
                )
            }
            is ImagineGoal -> ComposerShell("Editar meta", error, locked, close) {
                ImagineGoalForm(
                    initial = item,
                    isWorking = state.isWorking,
                    onSubmit = { title, content, date ->
                        viewModel.updateGoal(item, title, content, date, close)
                    },
                    onDismiss = close,
                    onDelete = if (item.createdBy == myUid) {
                        {
                            viewModel.delete(item)
                            close()
                        }
                    } else {
                        null
                    }
                )
            }
            is ImagineList -> ComposerShell("Editar lista", error, locked, close) {
                ImagineListForm(
                    initial = item,
                    isWorking = state.isWorking,
                    onSubmit = { title, items ->
                        viewModel.updateList(item, title, items, close)
                    },
                    onDismiss = close,
                    onDelete = if (item.createdBy == myUid) {
                        {
                            viewModel.delete(item)
                            close()
                        }
                    } else {
                        null
                    }
                )
            }
            else -> Unit
        }
        is ImagineComposerTarget.Contribute ->
            ComposerShell("Aportar a la cápsula", error, locked, close) {
                ImagineCapsuleContributeForm(
                    capsule = target.capsule,
                    isWorking = state.isWorking,
                    onSubmit = { text, attachment ->
                        viewModel.contributeToCapsule(target.capsule, text, attachment, close)
                    },
                    onDismiss = close
                )
            }
    }
}

@Composable
private fun ComposerShell(
    title: String,
    error: String?,
    locked: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = { if (!locked) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    enabled = !locked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = CurlyColors.DarkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TalkCircleButton(onClick = onDismiss, enabled = !locked, size = 34.dp) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "cerrar",
                            tint = CurlyColors.DarkTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = FormErrorColor, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun ImagineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxChars: Int,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = { if (it.length <= maxChars) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = CurlyColors.DarkTextMuted) },
        supportingText = {
            Text(
                text = "${value.length}/$maxChars",
                color = if (value.length >= maxChars - 10) CurlyColors.DarkLavenderLight else CurlyColors.DarkTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = imagineFieldColors()
    )
}

@Composable
internal fun imagineFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = CurlyColors.DarkSurface,
    unfocusedContainerColor = CurlyColors.DarkSurface,
    focusedTextColor = CurlyColors.DarkTextPrimary,
    unfocusedTextColor = CurlyColors.DarkTextPrimary,
    cursorColor = CurlyColors.DarkLavenderPrimary,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
internal fun ImagineFormActions(
    submitLabel: String,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onDelete != null) {
            Surface(
                onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                shape = RoundedCornerShape(20.dp),
                color = CurlyColors.DarkSurface
            ) {
                Text(
                    text = if (confirmDelete) "¿Seguro? Toca otra vez" else "Borrar",
                    color = FormErrorColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
        Surface(
            onClick = onSubmit,
            enabled = canSubmit,
            shape = RoundedCornerShape(20.dp),
            color = if (canSubmit) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkBorder
        ) {
            Text(
                text = submitLabel,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
}
