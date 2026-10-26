package com.curlygarden.app.ui.screens.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.curlygarden.app.push.ElevenReminder
import com.curlygarden.app.ui.animation.pressScale
import com.curlygarden.app.ui.screens.talk.TalkBackground
import com.curlygarden.app.ui.screens.talk.TalkCircleButton
import com.curlygarden.app.ui.theme.CurlyColors
import kotlinx.coroutines.delay

@Composable
fun NotificationsSettingsScreen(
    state: NotificationsUiState,
    viewModel: NotificationsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val error by viewModel.errorMessage.collectAsState()
    var systemEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var elevenOn by remember { mutableStateOf(ElevenReminder.isEnabled(context)) }
    var elevenBlocked by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                elevenOn = ElevenReminder.isEnabled(context)
                if (ElevenReminder.canScheduleExact(context)) elevenBlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(error) {
        if (error != null) {
            delay(2500)
            viewModel.dismissError()
        }
    }

    val pushOn = state.settings.pushEnabled

    TalkBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            SettingsHeader(onBack = onBack)
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error != null) {
                    item(key = "error") {
                        Text(
                            error.orEmpty(),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SettingsDot
                        )
                    }
                }
                if (!systemEnabled) {
                    item(key = "system") {
                        SystemDisabledCard(onOpen = { openAppNotificationSettings(context) })
                    }
                }
                item(key = "general") {
                    SettingsCard {
                        ToggleRow(
                            title = "Avisos en el teléfono",
                            subtitle = "Apágalos para ver todo solo dentro de la app",
                            checked = pushOn,
                            enabled = true,
                            onChange = viewModel::setPushEnabled
                        )
                    }
                }
                NotificationModule.entries.forEach { module ->
                    item(key = "module_${module.key}") {
                        ModuleSection(
                            module = module,
                            settings = state.settings,
                            enabled = pushOn,
                            onChange = viewModel::setKindEnabled
                        )
                    }
                }
                item(key = "reminders") {
                    RemindersSection(
                        elevenOn = elevenOn,
                        blocked = elevenBlocked,
                        onChange = { on ->
                            if (on) {
                                val zone = state.session?.timezone ?: DEFAULT_TIMEZONE
                                if (ElevenReminder.enable(context, zone)) {
                                    elevenOn = true
                                    elevenBlocked = false
                                } else {
                                    elevenBlocked = true
                                    ElevenReminder.openExactAlarmSettings(context)
                                }
                            } else {
                                ElevenReminder.disable(context)
                                elevenOn = false
                            }
                        }
                    )
                }
            }
        }
    }
}

private val SettingsDot = Color(0xFFF4B7A6)

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TalkCircleButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver", tint = CurlyColors.DarkTextPrimary)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            "Ajustes de avisos",
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = CurlyColors.DarkTextPrimary
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurface,
        border = BorderStroke(1.dp, CurlyColors.DarkBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SystemDisabledCard(onOpen: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CurlyColors.DarkSurfaceElevated,
        border = BorderStroke(1.dp, SettingsDot.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Los avisos están desactivados en el sistema",
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurlyColors.DarkTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Mientras sigan apagados solo verás lo nuevo dentro de la app.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = CurlyColors.DarkTextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onOpen,
                shape = RoundedCornerShape(20.dp),
                color = CurlyColors.DarkLavenderPrimary,
                modifier = Modifier
                    .height(40.dp)
                    .pressScale(pressedScale = 0.95f)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Abrir configuración",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleSection(
    module: NotificationModule,
    settings: NotificationSettings,
    enabled: Boolean,
    onChange: (NotificationKind, Boolean) -> Unit
) {
    val kinds = NotificationKind.forModule(module)
    Column {
        Row(
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(module.color), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                module.label,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurlyColors.DarkTextPrimary
            )
        }
        SettingsCard {
            kinds.forEachIndexed { index, kind ->
                if (index > 0) RowDivider()
                ToggleRow(
                    title = kind.label,
                    subtitle = null,
                    checked = settings.isPushEnabled(kind),
                    enabled = enabled,
                    onChange = { onChange(kind, it) }
                )
            }
        }
    }
}

@Composable
private fun RemindersSection(
    elevenOn: Boolean,
    blocked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(CurlyColors.DarkLavenderLight, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Recordatorios",
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurlyColors.DarkTextPrimary
            )
        }
        SettingsCard {
            ToggleRow(
                title = "11:11",
                subtitle = "Aviso diario a las 23:11 para pedir tu deseo. Solo en este teléfono",
                checked = elevenOn,
                enabled = true,
                onChange = onChange
            )
        }
        if (blocked) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Permite las alarmas y recordatorios en la configuración que se abrió, vuelve y activa el 11:11 de nuevo.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = SettingsDot,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(CurlyColors.DarkBorder.copy(alpha = 0.4f))
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = CurlyColors.DarkTextPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = CurlyColors.DarkTextSecondary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CurlyColors.DarkLavenderPrimary,
                uncheckedThumbColor = CurlyColors.DarkTextMuted,
                uncheckedTrackColor = CurlyColors.DarkSurfaceElevated,
                uncheckedBorderColor = CurlyColors.DarkBorder
            )
        )
    }
}
