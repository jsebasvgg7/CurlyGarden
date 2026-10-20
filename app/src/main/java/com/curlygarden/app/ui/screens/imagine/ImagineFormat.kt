package com.curlygarden.app.ui.screens.imagine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val createdFormat = SimpleDateFormat("d 'de' MMM. HH:mm", Locale("es"))
private val unlockFormat = SimpleDateFormat("d 'de' MMM. yyyy", Locale("es"))

fun formatImagineCreated(timestamp: Long): String =
    if (timestamp == Long.MAX_VALUE) "guardando…" else createdFormat.format(Date(timestamp))

fun formatImagineUnlock(timestamp: Long): String = unlockFormat.format(Date(timestamp))
