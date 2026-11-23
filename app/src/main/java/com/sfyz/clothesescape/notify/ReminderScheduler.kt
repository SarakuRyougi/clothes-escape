package com.sfyz.clothesescape.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sfyz.clothesescape.data.LaundryRepository

object ReminderScheduler {

    private fun pendingIntent(context: Context, recordId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_RECORD_ID, recordId)
        }
        return PendingIntent.getBroadcast(
            context,
            recordId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    fun schedule(context: Context, recordId: Long, atMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, recordId)
        val at = if (atMillis <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 1_500L
        } else {
            atMillis
        }
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (t: SecurityException) {
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }

    fun cancel(context: Context, recordId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { am.cancel(pendingIntent(context, recordId)) }
    }

    /** Re-arms every active record; call after boot, package update or data change. */
    fun resync(context: Context) {
        runCatching {
            val repo = LaundryRepository(context)
            val records = repo.loadAll()
            val now = System.currentTimeMillis()
            records.forEach { record ->
                if (record.active && record.reminderAt > now) {
                    schedule(context, record.id, record.reminderAt)
                } else if (!record.active) {
                    cancel(context, record.id)
                }
            }
            // Anything already due but still hanging gets a notification right away.
            records.filter { it.active && it.reminderAt <= now }
                .forEach { Notifier.showDue(context, it) }
        }
    }
}
