package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ImagineCapsuleCreateForm(
    canShare: Boolean,
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
    var title by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(CapsuleMode.INDIVIDUAL) }
    var unlockDate by remember { mutableStateOf<Long?>(null) }
    var text by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<ImagineAttachment?>(null) }

    val canSubmit = title.isNotBlank() && text.isNotBlank() && unlockDate != null && !isWorking

    ImagineField(
        value = title,
        onValueChange = { title = it },
        label = "Título",
        maxChars = IMAGINE_TITLE_MAX
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeOption(
            label = "Individual",
            selected = mode == CapsuleMode.INDIVIDUAL,
            enabled = true,
            onClick = { mode = CapsuleMode.INDIVIDUAL },
            modifier = Modifier.weight(1f)
        )
        ModeOption(
            label = "Compartida",
            selected = mode == CapsuleMode.SHARED,
            enabled = canShare,
            onClick = { mode = CapsuleMode.SHARED },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = when {
            !canShare -> "Vincula a alguien para crear una cápsula compartida."
            mode == CapsuleMode.SHARED ->
                "Cada quien escribe su parte sin ver la del otro. Se revelan juntas en la fecha. No se puede cambiar después."
            else ->
                "Solo tú escribes. El otro la lee desde la fecha de apertura. No se puede cambiar después."
        },
        color = CurlyColors.DarkTextMuted,
        fontSize = 11.sp
    )
    Spacer(Modifier.height(12.dp))
    UnlockDateField(unlockDate = unlockDate, onSelected = { unlockDate = it })
    Spacer(Modifier.height(8.dp))
    ImagineField(
        value = text,
        onValueChange = { text = it },
        label = "Tu mensaje para el futuro",
        maxChars = IMAGINE_CAPSULE_TEXT_MAX,
        minLines = 3,
        maxLines = 6
    )
    Spacer(Modifier.height(8.dp))
    ImagineAttachmentSelector(
        attachment = attachment,
        onAttachmentChange = { attachment = it }
    )
    Spacer(Modifier.height(16.dp))
    ImagineFormActions(
        submitLabel = "Sellar cápsula",
        canSubmit = canSubmit,
        onSubmit = { unlockDate?.let { onSubmit(title, mode, it, text, attachment) } },
        onDelete = null
    )
}

@Composable
fun ImagineCapsuleContributeForm(
    capsule: ImagineCapsule,
    isWorking: Boolean,
    onSubmit: (text: String, attachment: ImagineAttachment?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<ImagineAttachment?>(null) }

    val canSubmit = text.isNotBlank() && !isWorking

    Text(
        text = capsule.title,
        color = CurlyColors.DarkTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = "Se abre el ${formatImagineUnlock(capsule.unlockDate)}",
        color = CurlyColors.DarkTextSecondary,
        fontSize = 12.sp
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Escribe tu parte sin ver la del otro. Cuando la envíes, no podrás editarla.",
        color = CurlyColors.DarkTextMuted,
        fontSize = 11.sp
    )
    Spacer(Modifier.height(12.dp))
    ImagineField(
        value = text,
        onValueChange = { text = it },
        label = "Tu parte",
        maxChars = IMAGINE_CAPSULE_TEXT_MAX,
        minLines = 3,
        maxLines = 6
    )
    Spacer(Modifier.height(8.dp))
    ImagineAttachmentSelector(
        attachment = attachment,
        onAttachmentChange = { attachment = it }
    )
    Spacer(Modifier.height(16.dp))
    ImagineFormActions(
        submitLabel = "Enviar mi parte",
        canSubmit = canSubmit,
        onSubmit = { onSubmit(text, attachment) },
        onDelete = null
    )
}

@Composable
private fun ModeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkSurface,
        modifier = modifier
    ) {
        Text(
            text = label,
            color = if (enabled) CurlyColors.DarkTextPrimary else CurlyColors.DarkTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp)
        )
    }
}

@Composable
private fun UnlockDateField(unlockDate: Long?, onSelected: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Surface(
        onClick = { open = true },
        shape = RoundedCornerShape(18.dp),
        color = CurlyColors.DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                FeatherIcons.Calendar,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = unlockDate?.let { "Se abre el ${formatImagineUnlock(it)}" } ?: "¿Cuándo se abre?",
                color = if (unlockDate != null) CurlyColors.DarkTextPrimary else CurlyColors.DarkTextMuted,
                fontSize = 14.sp
            )
        }
    }

    if (open) {
        ImagineDatePickerDialog(
            initial = unlockDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            onDismiss = { open = false },
            onConfirm = { date ->
                onSelected(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                open = false
            }
        )
    }
}

private val WeekdayLabels = listOf("D", "L", "M", "M", "J", "V", "S")

@Composable
internal fun ImagineDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    pastOnly: Boolean = false
) {
    val today = remember { LocalDate.now() }
    val earliest = remember { today.plusDays(1) }
    val firstMonth = remember { YearMonth.from(earliest) }
    val lastMonth = remember { YearMonth.from(today) }
    var month by remember { mutableStateOf(YearMonth.from(initial ?: if (pastOnly) today else earliest)) }
    var selected by remember { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TalkCircleButton(
                        onClick = { month = month.minusMonths(1) },
                        enabled = pastOnly || month > firstMonth,
                        size = 34.dp
                    ) {
                        Icon(
                            FeatherIcons.ChevronLeft,
                            contentDescription = "mes anterior",
                            tint = CurlyColors.DarkTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = monthTitle(month),
                        color = CurlyColors.DarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TalkCircleButton(
                        onClick = { month = month.plusMonths(1) },
                        enabled = !pastOnly || month < lastMonth,
                        size = 34.dp
                    ) {
                        Icon(
                            FeatherIcons.ChevronRight,
                            contentDescription = "mes siguiente",
                            tint = CurlyColors.DarkTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    WeekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            color = CurlyColors.DarkTextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                monthWeeks(month).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            DayCell(
                                date = date,
                                enabled = date != null && if (pastOnly) !date.isAfter(today) else !date.isBefore(earliest),
                                selected = date != null && date == selected,
                                onClick = { selected = date },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancelar",
                        color = CurlyColors.DarkLavenderLight,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    Text(
                        text = "Aceptar",
                        color = if (selected != null) CurlyColors.DarkLavenderPrimary else CurlyColors.DarkTextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = selected != null) { selected?.let(onConfirm) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) CurlyColors.DarkLavenderPrimary else Color.Transparent)
                    .clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = if (enabled) CurlyColors.DarkTextPrimary else CurlyColors.DarkTextMuted,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private fun monthWeeks(month: YearMonth): List<List<LocalDate?>> {
    val leading = month.atDay(1).dayOfWeek.value % 7
    val days: List<LocalDate?> = List<LocalDate?>(leading) { null } +
        (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = days + List<LocalDate?>((42 - days.size).coerceAtLeast(0)) { null }
    return padded.chunked(7)
}

private fun monthTitle(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() } +
        " " + month.year
