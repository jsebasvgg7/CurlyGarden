package com.curlygarden.app.ui.screens.home

import com.curlygarden.app.ui.animation.entrance
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.animation.pressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.curlygarden.app.R
import com.curlygarden.app.ui.components.CurlyToast
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground
import androidx.compose.material.icons.filled.Add
import java.time.LocalDate
import kotlinx.coroutines.delay
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text

@Composable
fun HomeScreen(
    onModuleClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    animateEntrance: Boolean = false,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLoadError by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loadFailed) {
        if (uiState.loadFailed) {
            showLoadError = true
            delay(2500)
            showLoadError = false
        }
    }

    val scrollState = rememberScrollState()
    Box(modifier = Modifier.curlyDarkBackground()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.42f,
                center = Offset(x = size.width * 1.1f, y = -size.height * 0.05f)
            )
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.48f,
                center = Offset(x = -size.width * 0.15f, y = size.height * 1.05f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .entrance(0, animateEntrance)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClickLabel = "ver perfil",
                                onClick = onProfileClick
                            )
                    ) {
                        AvatarCircle(
                            profile = uiState.myProfile,
                            fallbackColor = CurlyColors.DarkLavenderPrimary,
                            size = 48.dp
                        )
                        Box(Modifier.padding(start = 32.dp)) {
                            AvatarCircle(
                                profile = uiState.partnerProfile,
                                fallbackColor = CurlyColors.DarkLavenderLight,
                                size = 48.dp
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        val myFirstName = uiState.myProfile?.name
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?.substringBefore(" ")
                        Text(
                            if (myFirstName != null) "Hola, $myFirstName" else "Hola de nuevo",
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurlyColors.DarkTextPrimary
                        )
                        Text(
                            todayLabel(),
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            color = CurlyColors.DarkTextSecondary
                        )
                    }
                }
                IconButtonBadge(onClick = onNotificationsClick, badge = uiState.notificationBadge) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = if (uiState.notificationBadge > 0) {
                            "notificaciones, ${uiState.notificationBadge} pendientes"
                        } else {
                            "notificaciones"
                        },
                        tint = CurlyColors.DarkTextPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.entrance(1, animateEntrance)) {
                HomeModuleHeroCard(
                    onClick = { onModuleClick("dibujar") },
                    myProfile = uiState.myProfile,
                    partnerProfile = uiState.partnerProfile,
                    onInviteClick = onInviteClick
                )
            }

            Spacer(Modifier.height(16.dp))

            StreakWeekRow(
                completedDates = uiState.weekDaysCompleted,
                modifier = Modifier.entrance(2, animateEntrance)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Nuestros espacios",
                color = CurlyColors.DarkTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.entrance(3, animateEntrance)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .entrance(3, animateEntrance)
                    .height(232.dp)
            ) {
                PlaySpaceCard(
                    onClick = { onModuleClick("jugar") },
                    gameCount = uiState.gameCount,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ImagineSpaceCard(
                        onClick = { onModuleClick("imaginar") },
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxWidth()
                    )
                    ChatSpaceCard(
                        onClick = { onModuleClick("hablar") },
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.entrance(4, animateEntrance)) {
                TodaysMomentCard(
                    onClick = { onModuleClick("recuerdos") },
                    latestMemory = uiState.latestMemory
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        if (showLoadError) {
            CurlyToast(
                message = "No se pudo actualizar todo. Revisa tu conexión.",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun AvatarCircle(
    profile: HomeUserProfile?,
    fallbackColor: Color,
    size: Dp = 72.dp
) {
    Box(
        Modifier
            .size(size)
            .background(CurlyColors.DarkBackground, shape = CircleShape)
            .padding(3.dp)
            .background(fallbackColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val photoUrl = profile?.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initial = profile?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            Text(
                initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.33f).sp
            )
        }
    }
}

@Composable
private fun IconButtonBadge(
    onClick: () -> Unit,
    badge: Int = 0,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.size(44.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = CurlyColors.DarkSurfaceElevated,
            modifier = Modifier.pressScale(pressedScale = 0.9f).size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .background(Color(0xFFF4B7A6), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (badge > 9) "9+" else badge.toString(),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26215C)
                )
            }
        }
    }
}

private fun todayLabel(): String {
    val today = LocalDate.now()
    val month = today.month.getDisplayName(TextStyle.SHORT, Locale("es")).replaceFirstChar { it.uppercase() }
    return "hoy ${today.dayOfMonth} de $month"
}

@Composable
private fun StreakWeekRow(completedDates: Set<LocalDate>, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val startOfWeek = remember(today) { today.minusDays(((today.dayOfWeek.value % 7)).toLong()) }
    val days = remember(startOfWeek) { (0..6).map { startOfWeek.plusDays(it.toLong()) } }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { date ->
            val isToday = date == today
            val isCompleted = date in completedDates
            val isFuture = date.isAfter(today)

            val background = if (isCompleted) CurlyColors.DarkSurfaceElevated else CurlyColors.DarkSurface
            val textColor = when {
                isCompleted -> Color.White
                isFuture -> CurlyColors.DarkTextMuted
                else -> CurlyColors.DarkTextSecondary
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = background,
                border = if (isToday) BorderStroke(1.5.dp, CurlyColors.DarkLavenderPrimary) else null,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))
                            .take(2)
                            .uppercase(),
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            StreakDayDrop(day = date.dayOfMonth)
                        } else {
                            Text(
                                "${date.dayOfMonth}",
                                fontSize = 13.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakDayDrop(day: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w / 2f, h * 0.06f)
                cubicTo(
                    w * 1.05f, h * 0.5f,
                    w * 0.88f, h,
                    w / 2f, h
                )
                cubicTo(
                    w * 0.12f, h,
                    w * -0.05f, h * 0.5f,
                    w / 2f, h * 0.06f
                )
                close()
            }
            drawPath(path, color = Color(0xFF7F77DD))
        }
        Text(
            "$day",
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.offset(y = 3.dp)
        )
    }
}

@Composable
private fun HomeModuleHeroCard(
    onClick: () -> Unit,
    myProfile: HomeUserProfile?,
    partnerProfile: HomeUserProfile?,
    onInviteClick: () -> Unit = {}
) {
    val textDark = Color(0xFF26215C)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = CurlyColors.DarkLavenderLight,
        modifier = Modifier
            .pressScale(pressedScale = 0.98f)
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(
                    id = R.drawable.ill_draw_hero
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(width = 158.dp, height = 180.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .fillMaxWidth(0.6f)
            ) {
                Text(
                    "ACTIVIDAD DE HOY",
                    color = textDark.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dibujemos\njuntos...",
                    color = textDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 23.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Una pequeña aventura creativa te espera.",
                    color = textDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(100.dp)
                ) {
                    HeroAvatar(
                        profile = myProfile,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    HeroAvatar(
                        profile = partnerProfile,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 34.dp)
                    )
                    if (myProfile != null && partnerProfile == null) {
                        Surface(
                            onClick = onInviteClick,
                            shape = CircleShape,
                            color = textDark.copy(alpha = 0.18f),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 68.dp)
                                .size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "invitar",
                                    tint = textDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = textDark.copy(alpha = 0.14f),
                modifier = Modifier
                    .padding(14.dp)
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaySpaceCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gameCount: Int = 0
) {
    val bg = Color(0xFFEBC27A)
    val textDark = Color(0xFF4A2E02)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = bg,
        modifier = modifier.pressScale()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .fillMaxWidth(0.66f)
            ) {
                Text(
                    "JUEGO",
                    color = textDark.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Jugar",
                    color = textDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Reta, diviértete\ny descubre\nalgo nuevo.",
                    color = textDark.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Image(
                painter = painterResource(
                    id = R.drawable.ill_play_hero
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 32.dp)
                    .size(width = 100.dp, height = 112.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    if (gameCount == 1) "1 juego" else "$gameCount juegos",
                    color = textDark.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                Surface(
                    shape = CircleShape,
                    color = textDark.copy(alpha = 0.14f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = textDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagineSpaceCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = Color(0xFFF4B7A6)
    val textDark = Color(0xFF4A1B0C)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = bg,
        modifier = modifier.pressScale()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .fillMaxWidth(0.62f)
            ) {
                Text(
                    "IMAGINACIÓN",
                    color = textDark.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Imagina",
                    color = textDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Ideas que\nconstruimos\njuntos.",
                    color = textDark.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }

            Image(
                painter = painterResource(
                    id = R.drawable.ill_idea_hero
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 30.dp)
                    .size(width = 68.dp, height = 68.dp)
            )

            Surface(
                shape = CircleShape,
                color = textDark.copy(alpha = 0.14f),
                modifier = Modifier
                    .padding(12.dp)
                    .size(28.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textDark,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSpaceCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg = Color(0xFF9BBFE0)
    val textDark = Color(0xFF042C53)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = bg,
        modifier = modifier.pressScale()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
            ) {
                Text(
                    "HABLAR",
                    color = textDark.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    "Chat",
                    color = textDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    "Una nota para\nhoy.",
                    color = textDark.copy(alpha = 0.75f),
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                )
            }

            Image(
                painter = painterResource(
                    id = R.drawable.ill_chat_hero
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 2.dp, top = 2.dp)
                    .size(width = 58.dp, height = 54.dp)
            )

            Surface(
                shape = CircleShape,
                color = textDark.copy(alpha = 0.14f),
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textDark,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroAvatar(profile: HomeUserProfile?, modifier: Modifier = Modifier) {
    Box(
        Modifier
            .size(30.dp)
            .then(modifier)
            .clip(CircleShape)
            .background(CurlyColors.DarkLavenderPrimary),
        contentAlignment = Alignment.Center
    ) {
        val photoUrl = profile?.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initial = profile?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                initial,
                color = Color(0xFF26215C),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TodaysMomentCard(
    onClick: () -> Unit,
    latestMemory: HomeLatestMemory?
) {
    val title = latestMemory?.caption?.takeIf { it.isNotBlank() }
        ?: "Nuestros recuerdos"
    val subtitle = if (latestMemory != null) {
        "Toca para ver todos sus recuerdos"
    } else {
        "Revive los momentos que nos hacen únicos."
    }
    val textDark = Color(0xFF26215C)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFBFB9FD),
        modifier = Modifier.pressScale(pressedScale = 0.98f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "RECUERDOS",
                        color = textDark.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        title,
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = textDark.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(64.dp))
            }

            val memoryPhotoUrl = latestMemory?.photoUrl
            if (!memoryPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = memoryPhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 36.dp)
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Image(
                    painter = painterResource(
                        id = R.drawable.ill_memories_hero
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 22.dp)
                        .size(72.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = textDark.copy(alpha = 0.14f),
                modifier = Modifier
                    .padding(14.dp)
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}