package com.curlygarden.app.ui.screens.memories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Image
import compose.icons.feathericons.Plus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val MemoriesInk = Color(0xFF26215C)

private val CollageHeight = 216.dp
private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("es"))

private class CollageSlot(val x: Float, val y: Float, val width: Float, val rotation: Float)

private val CollageLayouts: Map<Int, List<CollageSlot>> = mapOf(
    1 to listOf(CollageSlot(0.25f, 14f, 0.50f, -2f)),
    2 to listOf(
        CollageSlot(0.08f, 30f, 0.44f, -4f),
        CollageSlot(0.46f, 24f, 0.44f, 3f)
    ),
    3 to listOf(
        CollageSlot(0.06f, 28f, 0.40f, -4f),
        CollageSlot(0.36f, 44f, 0.36f, 2.5f),
        CollageSlot(0.62f, 14f, 0.32f, 4f)
    ),
    4 to listOf(
        CollageSlot(0.04f, 26f, 0.38f, -4f),
        CollageSlot(0.30f, 40f, 0.36f, 2.5f),
        CollageSlot(0.60f, 10f, 0.34f, 4f),
        CollageSlot(0.66f, 62f, 0.30f, -3f)
    )
)

internal fun formatMemoryDate(timestamp: Long): String =
    if (timestamp == Long.MAX_VALUE) "guardando…" else dateFormat.format(Date(timestamp)).replace(".", "")

internal fun memoriesCountLabel(count: Int): String =
    if (count == 1) "1 recuerdo" else "$count recuerdos"

@Composable
internal fun MemoriesDepthCard(
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val rim = BorderStroke(1.dp, lerp(color, Color.Black, 0.35f))
    val surfaceModifier = modifier.fillMaxWidth()

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = color,
            border = rim,
            modifier = surfaceModifier,
            content = content
        )
    } else {
        Surface(
            shape = shape,
            color = color,
            border = rim,
            modifier = surfaceModifier,
            content = content
        )
    }
}

@Composable
internal fun MemoriesAvatar(profile: MemoriesProfile?, size: Dp, modifier: Modifier = Modifier) {
    val photo = profile?.photoUrl.orEmpty()
    val initial = profile?.name?.trim()?.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(CurlyColors.DarkLavenderPrimary),
        contentAlignment = Alignment.Center
    ) {
        if (photo.isNotBlank()) {
            AsyncImage(
                model = photo,
                contentDescription = profile?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                initial,
                color = Color.White,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun MemoriesAlbumCard(
    album: Album,
    photos: List<Memory>,
    count: Int,
    creator: MemoriesProfile?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.97f, enabled = onClick != null)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CollageHeight)
                .background(CurlyColors.DarkSurface, cardShape)
                .border(1.dp, MemoriesOutline, cardShape)
                .clip(cardShape)
        ) {
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .size(72.dp)
                        .background(CurlyColors.DarkSurfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FeatherIcons.Image,
                        contentDescription = null,
                        tint = CurlyColors.DarkLavenderLight.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                MemoriesCollage(photos)
            }

            MemoriesDepthCard(
                color = albumColor(album.colorIndex),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    MemoriesRingButton(
                        icon = FeatherIcons.Plus,
                        contentDescription = "Agregar recuerdo",
                        onClick = onAdd,
                        size = 44.dp,
                        color = MemoriesInk,
                        iconTint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            album.name,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemoriesAvatar(creator, 20.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "${formatMemoryDate(album.createdAt)} · ${memoriesCountLabel(count)}",
                color = CurlyColors.DarkLavenderLight,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MemoriesCollage(photos: List<Memory>) {
    val slots = CollageLayouts.getValue(photos.size.coerceIn(1, MEMORIES_COLLAGE_MAX))
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        slots.forEachIndexed { index, slot ->
            val photoWidth = maxWidth * slot.width
            CollagePhoto(
                url = photos[index].photoUrl,
                modifier = Modifier
                    .offset(x = maxWidth * slot.x, y = slot.y.dp)
                    .size(width = photoWidth, height = photoWidth * 0.72f)
                    .graphicsLayer { rotationZ = slot.rotation }
            )
        }
    }
}

@Composable
private fun CollagePhoto(url: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun MemoriesEmptyIllustration(modifier: Modifier = Modifier) {
    val baseColor = albumColor(2)
    val baseShape = RoundedCornerShape(28.dp)
    Box(modifier = modifier.size(width = 280.dp, height = 250.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(190.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
                    .background(lerp(baseColor, Color.Black, 0.3f), baseShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .background(baseColor, baseShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), baseShape)
            )
        }
        PlaceholderPhoto(
            tint = albumColor(0),
            modifier = Modifier
                .offset(x = 22.dp, y = 74.dp)
                .size(width = 100.dp, height = 116.dp)
                .graphicsLayer { rotationZ = -12f }
        )
        PlaceholderPhoto(
            tint = albumColor(1),
            modifier = Modifier
                .offset(x = 104.dp, y = 16.dp)
                .size(width = 104.dp, height = 122.dp)
                .graphicsLayer { rotationZ = 7f }
        )
        PlaceholderPhoto(
            tint = albumColor(3),
            modifier = Modifier
                .offset(x = 112.dp, y = 118.dp)
                .size(width = 112.dp, height = 94.dp)
                .graphicsLayer { rotationZ = -5f }
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            sparkle(Offset(size.width * 0.10f, size.height * 0.18f), size.width * 0.045f, CurlyColors.DarkLavenderLight)
            sparkle(Offset(size.width * 0.92f, size.height * 0.46f), size.width * 0.035f, Color.White)
            sparkle(Offset(size.width * 0.06f, size.height * 0.80f), size.width * 0.03f, Color.White)
            sparkle(Offset(size.width * 0.80f, size.height * 0.92f), size.width * 0.04f, CurlyColors.DarkLavenderLight)
            heart(Offset(size.width * 0.88f, size.height * 0.12f), size.width * 0.035f, albumColor(0))
        }
    }
}

@Composable
private fun PlaceholderPhoto(tint: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(4.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(tint)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = size.width * 0.12f,
                center = Offset(size.width * 0.7f, size.height * 0.28f)
            )
            val hills = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * 0.7f)
                lineTo(size.width * 0.32f, size.height * 0.48f)
                lineTo(size.width * 0.6f, size.height * 0.78f)
                lineTo(size.width * 0.8f, size.height * 0.62f)
                lineTo(size.width, size.height * 0.78f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(hills, Color.Black.copy(alpha = 0.18f))
        }
    }
}

private fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticBezierTo(center.x, center.y, center.x + radius, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y + radius)
        quadraticBezierTo(center.x, center.y, center.x - radius, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.heart(center: Offset, unit: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + unit * 0.9f)
        cubicTo(
            center.x - unit * 1.6f, center.y - unit * 0.1f,
            center.x - unit * 0.7f, center.y - unit * 1.2f,
            center.x, center.y - unit * 0.4f
        )
        cubicTo(
            center.x + unit * 0.7f, center.y - unit * 1.2f,
            center.x + unit * 1.6f, center.y - unit * 0.1f,
            center.x, center.y + unit * 0.9f
        )
        close()
    }
    drawPath(path, color)
}

@Composable
internal fun MemoriesHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    trailing: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TalkCircleButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                FeatherIcons.ArrowLeft,
                contentDescription = "atras",
                tint = CurlyColors.DarkTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = CurlyColors.DarkTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = CurlyColors.DarkLavenderLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
            content = trailing
        )
    }
}

@Composable
internal fun MemoriesToast(message: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder),
        modifier = modifier
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            message,
            color = CurlyColors.DarkTextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
