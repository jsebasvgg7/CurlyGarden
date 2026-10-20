package com.curlygarden.app.ui.screens.imagine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.File

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
}
