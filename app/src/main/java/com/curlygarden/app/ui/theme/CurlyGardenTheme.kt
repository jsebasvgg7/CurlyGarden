package com.curlygarden.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.curlygarden.app.R

private val CurlyLightColors = lightColorScheme(
    primary = CurlyColors.Pink,
    secondary = CurlyColors.Purple,
    tertiary = CurlyColors.Blue,
    background = CurlyColors.ScreenBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = CurlyColors.TextDark,
    onSurface = CurlyColors.TextDark
)

private val CurlyDarkColors = darkColorScheme(
    primary = CurlyColors.DarkLavenderPrimary,
    secondary = CurlyColors.DarkLavenderLight,
    tertiary = CurlyColors.DarkLavenderPrimary,
    background = CurlyColors.DarkBackground,
    surface = CurlyColors.DarkSurface,
    surfaceVariant = CurlyColors.DarkSurfaceElevated,
    onPrimary = Color.White,
    onBackground = CurlyColors.DarkTextPrimary,
    onSurface = CurlyColors.DarkTextPrimary,
    onSurfaceVariant = CurlyColors.DarkTextSecondary,
    outline = CurlyColors.DarkBorder
)

val CurlyFontFamily = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold)
)

private val CurlyTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = CurlyFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = CurlyFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = CurlyFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = CurlyFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = CurlyFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = CurlyFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = CurlyFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = CurlyFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = CurlyFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = CurlyFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = CurlyFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = CurlyFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = CurlyFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = CurlyFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = CurlyFontFamily)
    )
}

enum class CurlyStatusBarStyle { Light, Dark }

@Composable
fun CurlyGardenTheme(
    statusBarStyle: CurlyStatusBarStyle = CurlyStatusBarStyle.Light,
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            when (statusBarStyle) {
                CurlyStatusBarStyle.Light -> {
                    window.statusBarColor = CurlyColors.ScreenBackground.toArgb()
                    window.navigationBarColor = Color.White.toArgb()
                    controller.isAppearanceLightStatusBars = true
                    controller.isAppearanceLightNavigationBars = true
                }
                CurlyStatusBarStyle.Dark -> {
                    window.statusBarColor = CurlyColors.DarkBackground.toArgb()
                    window.navigationBarColor = CurlyColors.DarkBackground.toArgb()
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    val colorScheme = when (statusBarStyle) {
        CurlyStatusBarStyle.Light -> CurlyLightColors
        CurlyStatusBarStyle.Dark -> CurlyDarkColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CurlyTypography,
        content = content
    )
}