package com.curlygarden.app.push

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import java.time.ZoneId
import java.time.ZonedDateTime

object ElevenReminder {

    private const val PREFS = "push_prefs"
    private const val KEY_ENABLED = "eleven_enabled"
    private const val KEY_ZONE = "eleven_zone"
    private const val REQUEST_CODE = 1111
    private const val HOUR = 23
    private const val MINUTE = 11

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager(context)?.canScheduleExactAlarms() == true

    fun enable(context: Context, zoneId: String): Boolean {
        if (!canScheduleExact(context)) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_ZONE, zoneId)
            .apply()
        return schedule(context)
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, false)
            .apply()
        alarmManager(context)?.cancel(pendingIntent(context))
    }

    fun reschedule(context: Context) {
        if (!isEnabled(context)) return
        if (!canScheduleExact(context)) return
        schedule(context)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return
        }
    }

    private fun schedule(context: Context): Boolean {
        val manager = alarmManager(context) ?: return false
        val zone = storedZone(context)
        val trigger = nextTrigger(zone)
        return try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context))
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun nextTrigger(zone: ZoneId): Long {
        val now = ZonedDateTime.now(zone)
        var target = now.withHour(HOUR).withMinute(MINUTE).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    private fun storedZone(context: Context): ZoneId {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ZONE, null) ?: DEFAULT_TIMEZONE
        return runCatching { ZoneId.of(raw) }.getOrElse { ZoneId.of(DEFAULT_TIMEZONE) }
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ElevenReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
