package com.curlygarden.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.curlygarden.app.profile.MyProfileData
import com.curlygarden.app.profile.ProfileUiState
import com.curlygarden.app.profile.ProfileViewModel
import com.curlygarden.app.profile.ProfileViewModelFactory
import com.curlygarden.app.ui.theme.CurlyColors
import com.curlygarden.app.ui.theme.curlyDarkBackground

@Composable
fun ProfileScreen(
    onBackClicked: () -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    onGoToOnboardingClicked: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConnectionModal by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.unlinked) {
        if (uiState.unlinked) {
            showConnectionModal = false
            viewModel.onUnlinkHandled()
        }
    }

    Box(modifier = Modifier.curlyDarkBackground()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.42f,
                center = Offset(x = -size.width * 0.12f, y = -size.height * 0.04f)
            )
            drawCircle(
                color = CurlyColors.DarkBlob,
                radius = size.width * 0.48f,
                center = Offset(x = size.width * 1.08f, y = size.height * 1.03f)
            )
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            ProfileHeader(onBackClicked = onBackClicked, onLogoutClicked = onLogoutClicked)

            Spacer(Modifier.height(20.dp))

            NameAndRole(uiState = uiState)

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProfileAvatar(profile = uiState.myProfile)
            }

            Spacer(Modifier.height(24.dp))

            LevelProgressCard(level = uiState.level, progress = uiState.levelProgress)

            Spacer(Modifier.height(16.dp))

            ConnectionStatusCard(
                partnerProfile = uiState.partnerProfile,
                onClick = {
                    if (uiState.partnerProfile != null) {
                        showConnectionModal = true
                    } else {
                        onGoToOnboardingClicked()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreakPill(
                    streakDays = uiState.streakDays,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.HourglassEmpty,
                    iconTint = CurlyColors.DarkLavenderLight,
                    value = "${uiState.daysLinked}",
                    label = "dias juntos"
                )
            }
        }
    }

    val currentPartner = uiState.partnerProfile
    if (showConnectionModal && currentPartner != null) {
        ConnectionStatusDialog(
            partnerProfile = currentPartner,
            isUnlinking = uiState.isUnlinking,
            errorMessage = uiState.unlinkError,
            onDismiss = {
                if (!uiState.isUnlinking) {
                    showConnectionModal = false
                    viewModel.dismissUnlinkError()
                }
            },
            onConfirmUnlink = viewModel::onUnlinkConfirmed
        )
    }
}

@Composable
private fun ProfileHeader(onBackClicked: () -> Unit, onLogoutClicked: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBackClicked,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "volver",
                    tint = Color.White
                )
            }
        }
        Surface(
            onClick = onLogoutClicked,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = "cerrar sesión",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun NameAndRole(uiState: ProfileUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            uiState.myProfile?.name?.trim()?.takeIf { it.isNotBlank() }
                ?.substringBefore(' ')
                ?: "Tu nombre",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Usuario",
            fontSize = 14.sp,
            color = CurlyColors.DarkLavenderLight
        )
    }
}

@Composable
private fun ProfileAvatar(profile: MyProfileData?) {
    Box(
        Modifier
            .size(112.dp)
            .background(CurlyColors.DarkSurfaceElevated, shape = CircleShape)
            .padding(4.dp)
            .background(CurlyColors.DarkLavenderPrimary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val photoUrl = profile?.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = profile?.name,
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
                fontSize = 36.sp
            )
        }
    }
}

@Composable
private fun LevelProgressCard(level: Int, progress: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Suma puntos jugando y sube de nivel. No te dejes alcanzar.",
                fontSize = 14.sp,
                color = CurlyColors.DarkLavenderLight
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CurlyColors.DarkBorder, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CurlyColors.DarkSurface,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = CurlyColors.DarkLavenderLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Lv. $level",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CurlyColors.DarkLavenderPrimary,
                    trackColor = CurlyColors.DarkSurface
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "$progress%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurlyColors.DarkLavenderPrimary
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(partnerProfile: MyProfileData?, onClick: () -> Unit) {
    val isLinked = partnerProfile != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = CurlyColors.DarkSurface,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = null,
                            tint = CurlyColors.DarkLavenderLight,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Estado de conexión",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isLinked) "Ya comparten su espacio" else "Comparte o agrega el código para vincularte",
                        fontSize = 12.sp,
                        color = CurlyColors.DarkLavenderLight
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = CurlyColors.DarkLavenderPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        partnerProfile?.name?.takeIf { it.isNotBlank() }
                            ?.let { "Vinculado con $it" }
                            ?: "Vincular ahora",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CurlyColors.DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                Text(
                    value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurlyColors.DarkTextPrimary,
                    lineHeight = 22.sp
                )
                Text(
                    label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurlyColors.DarkTextSecondary,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusDialog(
    partnerProfile: MyProfileData,
    isUnlinking: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmUnlink: () -> Unit
) {
    var confirmingUnlink by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CurlyColors.DarkSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Estado de Conexión",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(72.dp)
                        .background(CurlyColors.DarkSurface, shape = CircleShape)
                        .padding(3.dp)
                        .background(CurlyColors.PinkSoft.copy(alpha = 0.4f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val photoUrl = partnerProfile.photoUrl
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = partnerProfile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initial = partnerProfile.name.trim()
                            .firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                        Text(
                            initial,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    partnerProfile.name.takeIf { it.isNotBlank() } ?: "Tu pareja",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Vinculado",
                    fontSize = 13.sp,
                    color = CurlyColors.DarkLavenderLight
                )

                errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        color = CurlyColors.Pink,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (confirmingUnlink) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "¿Seguro que quieres desvincularse? Sus mensajes y recuerdos quedarán guardados, pero dejarán de compartir su espacio.",
                        fontSize = 12.sp,
                        color = CurlyColors.DarkLavenderLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (confirmingUnlink) {
                TextButton(
                    onClick = onConfirmUnlink,
                    enabled = !isUnlinking
                ) {
                    if (isUnlinking) {
                        CircularProgressIndicator(
                            color = CurlyColors.Pink,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Sí, desvincular", color = CurlyColors.Pink, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                TextButton(onClick = { confirmingUnlink = true }) {
                    Text("Desvincular", color = CurlyColors.Pink, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (confirmingUnlink) confirmingUnlink = false else onDismiss() },
                enabled = !isUnlinking
            ) {
                Text(
                    if (confirmingUnlink) "Cancelar" else "Cerrar",
                    color = CurlyColors.DarkLavenderLight
                )
            }
        }
    )
}

@Composable
private fun StreakPill(streakDays: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CurlyColors.DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = "racha",
                    tint = CurlyColors.DarkLavenderPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                    Text(
                        "$streakDays",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkTextPrimary,
                        lineHeight = 22.sp
                    )
                    Text(
                        "racha actual",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurlyColors.DarkTextSecondary,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}