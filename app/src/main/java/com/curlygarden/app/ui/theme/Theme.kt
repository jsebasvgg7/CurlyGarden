package com.curlygarden.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier

object CurlyColors {

    val Pink = Color(0xFFD4537E)
    val Blue = Color(0xFF378ADD)
    val Purple = Color(0xFF7F77DD)
    val Coral = Color(0xFFD85A30)
    val TextDark = Color(0xFF2B2A4C)
    val TextGray = Color(0xFF6B6980)

    val PinkCard = Color(0xFFFBE0EC)
    val LavenderCard = Color(0xFFE6E1FA)
    val MintCard = Color(0xFFDFF3E8)
    val SalmonCard = Color(0xFFFBE1DE)

    val PinkAccent = Color(0xFFD4537E)
    val LavenderAccent = Color(0xFF7F77DD)
    val MintAccent = Color(0xFF4CAF7D)
    val SalmonAccent = Color(0xFFD85A30)

    val PinkSoft = Color(0xFFF4C0D1)
    val BlueSoft = Color(0xFFB5D4F4)
    val PurpleSoft = Color(0xFFCECBF6)
    val CoralSoft = Color(0xFFF5C4B3)

    val ScreenBackground = Color(0xFFF6F4FC)

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFBEAF0),
            Color(0xFFE6F1FB),
            Color(0xFFEEEDFE)
        )
    )

    val DarkBackground = Color(0xFF12101F)
    val DarkLavenderPrimary = Color(0xFF7F77DD)
    val DarkLavenderLight = Color(0xFFB4AEEA)
    val DarkBlob = Color(0xFF1E1B33)

    val DarkSurface = Color(0xFF1E1B33)
    val DarkSurfaceElevated = Color(0xFF262242)
    val DarkBorder = Color(0xFF322D54)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFFB4AEEA)
    val DarkTextMuted = Color(0xFF6E6A96)
}

fun Modifier.curlyBackground(): Modifier = this
    .fillMaxSize()
    .background(CurlyColors.backgroundGradient)

fun Modifier.curlySoftBackground(): Modifier = this
    .fillMaxSize()
    .background(CurlyColors.ScreenBackground)

fun Modifier.curlyDarkBackground(): Modifier = this
    .fillMaxSize()
    .background(CurlyColors.DarkBackground)