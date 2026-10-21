package com.curlygarden.app.ui.screens.imagine

import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.popIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Archive
import compose.icons.feathericons.Package
import compose.icons.feathericons.X

@Composable
fun CapsuleSealedContent(
    capsule: ImagineCapsule,
    myUid: String,
    myEntry: CapsuleEntry?,
    onOpenAttachment: (String) -> Unit,
    onContribute: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(
            FeatherIcons.Archive,
            contentDescription = null,
            tint = CurlyColors.DarkTextPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Se abre el ${formatImagineUnlock(capsule.unlockDate)}",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 12.sp
        )
        sharedStatus(capsule, myUid)?.let { status ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = status,
                color = CurlyColors.DarkTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
        if (onContribute != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onContribute,
                shape = RoundedCornerShape(50),
                color = CurlyColors.DarkLavenderPrimary
            ) {
                Text(
                    text = "Aportar mi parte",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
        if (myEntry != null) {
            Spacer(Modifier.height(14.dp))
            EntryBlock(
                label = "Tu parte",
                entry = myEntry,
                profile = null,
                onOpenAttachment = onOpenAttachment,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CapsuleReadyContent(
    capsule: ImagineCapsule,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(
            FeatherIcons.Package,
            contentDescription = null,
            tint = CurlyColors.DarkLavenderLight,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Ya se puede abrir",
            color = CurlyColors.DarkTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Se abrió el ${formatImagineUnlock(capsule.unlockDate)}",
            color = CurlyColors.DarkTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun CapsuleOpenedContent(
    capsule: ImagineCapsule,
    entries: CapsuleEntries?,
    myProfile: ImagineProfile,
    partnerProfile: ImagineProfile?,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (entries == null) {
            Text(
                text = "Cargando…",
                color = CurlyColors.DarkTextMuted,
                fontSize = 13.sp
            )
            return@Column
        }
        val parts = listOfNotNull(
            entries.mine?.let { it to myProfile },
            entries.partner?.let { it to partnerProfile }
        ).sortedBy { it.first.createdAt }
        parts.forEachIndexed { index, (entry, profile) ->
            EntryBlock(
                label = profile?.name?.takeIf { it.isNotBlank() } ?: "El otro",
                entry = entry,
                profile = profile,
                onOpenAttachment = onOpenAttachment,
                modifier = Modifier
                    .entrance(index + 2)
                    .fillMaxWidth()
            )
        }
        val missing = missingMessage(capsule, myProfile, partnerProfile)
        if (missing != null) {
            Text(
                text = missing,
                color = CurlyColors.DarkTextSecondary,
                fontSize = 12.sp
            )
        } else if (parts.isEmpty()) {
            Text(
                text = "Esta cápsula no tiene contenido",
                color = CurlyColors.DarkTextMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun CapsuleRevealDialog(
    capsule: ImagineCapsule,
    entries: CapsuleEntries?,
    myProfile: ImagineProfile,
    partnerProfile: ImagineProfile?,
    onOpenAttachment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = CurlyColors.DarkSurfaceElevated,
            modifier = Modifier
                .popIn()
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = capsule.title,
                            color = CurlyColors.DarkTextPrimary,
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Se abrió el ${formatImagineUnlock(capsule.unlockDate)}",
                            color = CurlyColors.DarkTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = CurlyColors.DarkSurface,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                FeatherIcons.X,
                                contentDescription = "cerrar",
                                tint = CurlyColors.DarkTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CapsuleOpenedContent(
                        capsule = capsule,
                        entries = entries,
                        myProfile = myProfile,
                        partnerProfile = partnerProfile,
                        onOpenAttachment = onOpenAttachment
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryBlock(
    label: String,
    entry: CapsuleEntry,
    profile: ImagineProfile?,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CurlyColors.DarkSurface, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (profile != null) {
                ImagineAvatar(profile = profile, size = 28.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = CurlyColors.DarkLavenderLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.text,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        val url = entry.attachmentUrl
        if (!url.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            ImagineAttachmentView(
                url = url,
                type = entry.attachmentType,
                onOpen = onOpenAttachment,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun sharedStatus(capsule: ImagineCapsule, myUid: String): String? {
    if (capsule.mode != CapsuleMode.SHARED) return null
    val mine = myUid in capsule.contributors
    val partner = capsule.contributors.any { it != myUid }
    return when {
        mine && partner -> "Los dos ya escribieron su parte"
        mine -> "Falta la parte del otro"
        else -> "Aún falta tu parte"
    }
}

private fun missingMessage(
    capsule: ImagineCapsule,
    myProfile: ImagineProfile,
    partnerProfile: ImagineProfile?
): String? {
    if (capsule.mode != CapsuleMode.SHARED) return null
    val mine = myProfile.uid in capsule.contributors
    val partner = capsule.contributors.any { it != myProfile.uid }
    return when {
        !mine -> "Tú no alcanzaste a escribir"
        !partner -> "${partnerProfile?.name?.takeIf { it.isNotBlank() } ?: "El otro"} no alcanzó a escribir"
        else -> null
    }
}
