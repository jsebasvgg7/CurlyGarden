package com.curlygarden.app.ui.screens.notifications

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun notificationText(kind: NotificationKind, name: String): String = when (kind) {
    NotificationKind.TALK_MESSAGE -> "$name te dejó una carta"
    NotificationKind.DRAW_TOPIC_RECEIVED -> "$name ya envió su tema. Te toca enviar el tuyo"
    NotificationKind.DRAW_READY_TO_DRAW -> "Ya pueden dibujar"
    NotificationKind.DRAW_REVEALED -> "Ya pueden ver los dibujos"
    NotificationKind.DRAW_NEXT_ROUND -> "$name quiere dibujar de nuevo"
    NotificationKind.PLAY_DAILY_ANSWERED -> "$name ya respondió el reto de hoy"
    NotificationKind.PLAY_SESSION_NEW -> "$name empezó una partida"
    NotificationKind.PLAY_GUESS_TURN -> "Ya puedes adivinar"
    NotificationKind.PLAY_RESULT -> "Ya hay resultado"
    NotificationKind.IMAGINE_CAPSULE_PENDING -> "Falta tu parte en una cápsula"
    NotificationKind.IMAGINE_CAPSULE_READY -> "Una cápsula ya se puede abrir"
    NotificationKind.IMAGINE_JOINED -> "$name se unió a algo tuyo"
    NotificationKind.MEMORY_NEW -> "$name guardó un recuerdo"
}

fun NotificationKind.usesActorName(): Boolean = when (this) {
    NotificationKind.DRAW_READY_TO_DRAW,
    NotificationKind.DRAW_REVEALED,
    NotificationKind.PLAY_GUESS_TURN,
    NotificationKind.PLAY_RESULT,
    NotificationKind.IMAGINE_CAPSULE_PENDING,
    NotificationKind.IMAGINE_CAPSULE_READY -> false
    else -> true
}

private val dayFormat = SimpleDateFormat("d MMM", Locale("es"))

fun formatNotificationAge(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    if (createdAt == Long.MAX_VALUE) return "ahora"
    val minutes = ((now - createdAt) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "ahora"
        minutes < 60L -> "hace $minutes min"
        minutes < 24L * 60L -> "hace ${minutes / 60L} h"
        minutes < 7L * 24L * 60L -> "hace ${minutes / (24L * 60L)} d"
        else -> dayFormat.format(Date(createdAt))
    }
}
