package com.curlygarden.app.ui.screens.imagine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.File
import compose.icons.feathericons.Paperclip
import compose.icons.feathericons.X

@Composable
fun ImagineAttachmentView(
    url: String,
    type: AttachmentType?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (type == AttachmentType.IMAGE) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpen(url) }
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CurlyColors.DarkSurface)
                .clickable { onOpen(url) }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                FeatherIcons.File,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Archivo adjunto",
                color = CurlyColors.DarkTextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                FeatherIcons.ExternalLink,
                contentDescription = "abrir",
                tint = CurlyColors.DarkTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ImagineAttachmentPicker(
    attachment: ImagineAttachment?,
    existingUrl: String?,
    errorMessage: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val hasFile = attachment != null || !existingUrl.isNullOrBlank()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CurlyColors.DarkSurface)
                .clickable(enabled = !hasFile, onClick = onPick)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                if (hasFile) FeatherIcons.File else FeatherIcons.Paperclip,
                contentDescription = null,
                tint = CurlyColors.DarkLavenderLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        attachment != null -> attachment.fileName
                        hasFile -> "Archivo adjunto actual"
                        else -> "Adjuntar archivo"
                    },
                    color = if (hasFile) CurlyColors.DarkTextPrimary else CurlyColors.DarkTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (attachment != null) {
                    Text(
                        text = formatImagineSize(attachment.sizeBytes),
                        color = CurlyColors.DarkTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
            if (hasFile) {
                Icon(
                    FeatherIcons.X,
                    contentDescription = "quitar",
                    tint = CurlyColors.DarkTextSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear)
                        .padding(5.dp)
                )
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(errorMessage, color = Color(0xFFF4B7A6), fontSize = 12.sp)
        }
    }
}

@Composable
internal fun ImagineAttachmentSelector(
    attachment: ImagineAttachment?,
    onAttachmentChange: (ImagineAttachment?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val picked = context.readImagineAttachment(uri)
            when {
                picked == null -> error = "No se pudo leer el archivo"
                picked.sizeBytes > IMAGINE_MAX_ATTACHMENT_BYTES -> error = "El archivo supera los 20 MB"
                else -> {
                    error = null
                    onAttachmentChange(picked)
                }
            }
        }
    }

    ImagineAttachmentPicker(
        attachment = attachment,
        existingUrl = null,
        errorMessage = error,
        onPick = { launcher.launch("*/*") },
        onClear = {
            error = null
            onAttachmentChange(null)
        },
        modifier = modifier
    )
}
