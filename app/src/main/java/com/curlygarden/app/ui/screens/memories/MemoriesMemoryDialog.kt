package com.curlygarden.app.ui.screens.memories

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.curlygarden.app.ui.screens.imagine.ImagineDatePickerDialog
import com.curlygarden.app.ui.screens.imagine.ImagineField
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.Check
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Image
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val MemoryDialogErrorColor = Color(0xFFF4B7A6)

@Composable
internal fun MemoriesMemoryDialog(
    albums: List<Album>,
    initialAlbumId: String?,
    isWorking: Boolean,
    error: String?,
    onSave: (photo: MemoryPhoto, caption: String, dateMillis: Long, albumId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var photo by remember { mutableStateOf<MemoryPhoto?>(null) }
    var isReading by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var albumId by remember {
        mutableStateOf(initialAlbumId?.takeIf { id -> albums.any { it.id == id } })
    }
    var showDate by remember { mutableStateOf(false) }
    var showAlbums by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) {
            isReading = true
            readError = null
            scope.launch {
                runCatching { readMemoryPhoto(context, picked) }
                    .onSuccess {
                        uri = picked
                        photo = it
                    }
                    .onFailure { readError = "No se pudo leer la foto" }
                isReading = false
            }
        }
    }

    val busy = isWorking || isReading
    val selectedPhoto = photo
    val selectedAlbum = albums.firstOrNull { it.id == albumId }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    enabled = !busy,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CurlyColors.DarkSurfaceElevated, RoundedCornerShape(28.dp))
                    .border(1.dp, MemoriesOutline, RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    "Nuevo recuerdo",
                    color = CurlyColors.DarkTextPrimary,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                MemoryPhotoZone(
                    uri = uri,
                    isReading = isReading,
                    enabled = !busy,
                    onPick = { picker.launch("image/*") }
                )
                readError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color = MemoryDialogErrorColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                ImagineField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = "Escribe algo sobre este momento",
                    maxChars = MEMORIES_CAPTION_MAX,
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(Modifier.height(16.dp))
                MemoryChip(
                    icon = FeatherIcons.Calendar,
                    text = memoryDateLabel(date),
                    enabled = !busy,
                    onClick = { showDate = true }
                )
                Spacer(Modifier.height(10.dp))
                MemoryChip(
                    icon = FeatherIcons.Folder,
                    text = selectedAlbum?.name ?: "Sin álbum",
                    enabled = !busy,
                    dotColor = selectedAlbum?.let { albumColor(it.colorIndex) },
                    trailing = if (showAlbums) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                    onClick = { showAlbums = !showAlbums }
                )
                if (showAlbums) {
                    Spacer(Modifier.height(6.dp))
                    MemoryAlbumOption(
                        name = "Sin álbum",
                        dotColor = null,
                        selected = albumId == null,
                        onClick = {
                            albumId = null
                            showAlbums = false
                        }
                    )
                    albums.forEach { album ->
                        MemoryAlbumOption(
                            name = album.name,
                            dotColor = albumColor(album.colorIndex),
                            selected = album.id == albumId,
                            onClick = {
                                albumId = album.id
                                showAlbums = false
                            }
                        )
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        error,
                        color = MemoryDialogErrorColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(24.dp))
                MemoriesPrimaryButton(
                    text = "Guardar recuerdo",
                    onClick = {
                        selectedPhoto?.let {
                            onSave(it, caption, memoryDateMillis(date), albumId)
                        }
                    },
                    enabled = selectedPhoto != null && !isReading,
                    loading = isWorking
                )
            }
        }
    }

    if (showDate) {
        ImagineDatePickerDialog(
            initial = date,
            pastOnly = true,
            onDismiss = { showDate = false },
            onConfirm = {
                date = it
                showDate = false
            }
        )
    }
}

@Composable
private fun MemoryPhotoZone(
    uri: Uri?,
    isReading: Boolean,
    enabled: Boolean,
    onPick: () -> Unit
) {
    Surface(
        onClick = onPick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, MemoriesOutline),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "foto del recuerdo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        "Cambiar foto",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(CurlyColors.DarkSurfaceElevated, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            FeatherIcons.Image,
                            contentDescription = null,
                            tint = CurlyColors.DarkLavenderLight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Elegir foto",
                        color = CurlyColors.DarkLavenderLight,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (isReading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryChip(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    trailing: ImageVector? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, MemoriesOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(Modifier.size(18.dp).background(dotColor, CircleShape))
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = CurlyColors.DarkLavenderLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Icon(
                    trailing,
                    contentDescription = null,
                    tint = CurlyColors.DarkTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MemoryAlbumOption(
    name: String,
    dotColor: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(14.dp)
                .background(dotColor ?: CurlyColors.DarkTextMuted, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                FeatherIcons.Check,
                contentDescription = "elegido",
                tint = CurlyColors.DarkLavenderPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun memoryDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val pattern = if (date.year == today.year) "d 'de' MMM" else "d 'de' MMM yyyy"
    val text = date.format(DateTimeFormatter.ofPattern(pattern, Locale("es"))).replace(".", "")
    return if (date == today) "Hoy, $text" else text
}

private fun memoryDateMillis(date: LocalDate): Long =
    if (date == LocalDate.now()) {
        System.currentTimeMillis()
    } else {
        date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
