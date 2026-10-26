package com.curlygarden.app.ui.screens.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.curlygarden.app.ui.theme.CurlyColors

private const val PUSH_PREFS = "push_prefs"
private const val KEY_PERMISSION_ASKED = "permission_asked"

private fun Context.markPermissionAsked() {
    getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_PERMISSION_ASKED, true)
        .apply()
}

@Composable
fun PushPermissionPrompt() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val asked = context.getSharedPreferences(PUSH_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_ASKED, false)
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        visible = !asked && !granted
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            context.markPermissionAsked()
            visible = false
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = CurlyColors.DarkSurfaceElevated,
        titleContentColor = CurlyColors.DarkTextPrimary,
        textContentColor = CurlyColors.DarkTextSecondary,
        title = {
            Text(
                "Activar avisos",
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Para avisarte cuando el otro te deje algo o sea tu turno, Android necesita tu permiso. Los avisos nunca muestran el contenido de lo que escriben.",
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.markPermissionAsked()
                    visible = false
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            ) {
                Text(
                    "Permitir",
                    color = CurlyColors.DarkLavenderLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    context.markPermissionAsked()
                    visible = false
                }
            ) {
                Text(
                    "Ahora no",
                    color = CurlyColors.DarkTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
