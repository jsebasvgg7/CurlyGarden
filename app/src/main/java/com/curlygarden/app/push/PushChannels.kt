package com.curlygarden.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.curlygarden.app.ui.screens.notifications.NotificationModule

object PushChannels {

    const val REMINDERS_CHANNEL = "reminders"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        NotificationModule.entries.forEach { module ->
            manager.createNotificationChannel(
                NotificationChannel(
                    module.key,
                    module.label,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDERS_CHANNEL,
                "Recordatorios",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
}
