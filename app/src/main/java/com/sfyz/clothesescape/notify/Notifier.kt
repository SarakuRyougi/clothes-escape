package com.sfyz.clothesescape.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sfyz.clothesescape.MainActivity
import com.sfyz.clothesescape.R
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.TimeUtils

object Notifier {

    const val CHANNEL_ID = "collect_v1"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_collect),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_collect_desc)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun showDue(context: Context, record: RecordUi) {
        if (!canNotify(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_RECORD_ID, record.id)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            record.id.toInt(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val collectIntent = Intent(context, CollectReceiver::class.java).apply {
            action = CollectReceiver.ACTION_COLLECT
            putExtra(CollectReceiver.EXTRA_RECORD_ID, record.id)
        }
        val collectPending = PendingIntent.getBroadcast(
            context,
            (record.id.toInt() * 31) + 7,
            collectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val count = if (record.items.isEmpty()) 1 else record.items.size
        val body = "${record.regionText}：${record.names}（$count 件）"
        val big = body + "\n已晾 " + TimeUtils.durationText(
            System.currentTimeMillis() - record.hungAt
        ) + "，快去收回来，别让它们跑掉。"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("衣服要逃跑了！")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "已收", collectPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(record.id.toInt(), notification)
        }
    }

    fun cancel(context: Context, recordId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(recordId.toInt())
        }
    }
}
