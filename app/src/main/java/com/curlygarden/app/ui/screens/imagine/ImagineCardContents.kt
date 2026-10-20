package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.Check

@Composable
fun IdeaCardContent(
    idea: ImagineIdea,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = idea.content,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        val url = idea.attachmentUrl
        if (!url.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            ImagineAttachmentView(
                url = url,
                type = idea.attachmentType,
                onOpen = onOpenAttachment,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GoalCardContent(
    goal: ImagineGoal,
    canEdit: Boolean,
    onToggleFinished: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val finished = goal.status == ImagineStatus.FINISHED
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                FeatherIcons.Calendar,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Para: ${goal.targetDateText}",
                color = CurlyColors.DarkLavenderLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (goal.content.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = goal.content,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        if (canEdit || finished) {
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = canEdit) { onToggleFinished(!finished) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (finished) CurlyColors.DarkLavenderPrimary else Color.Transparent)
                        .border(1.5.dp, CurlyColors.DarkLavenderLight, RoundedCornerShape(7.dp))
                ) {
                    if (finished) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (finished) "Lograda" else "Marcar como lograda",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ListCardContent(
    list: ImagineList,
    canEdit: Boolean,
    onToggleItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
}
