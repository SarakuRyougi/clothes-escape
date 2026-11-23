package com.sfyz.clothesescape.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sfyz.clothesescape.data.LaundryRepository

/** Handles the "已收" action on the reminder notification. */
class CollectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COLLECT) return
        val id = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (id <= 0L) return
        val appContext = context.applicationContext
        Thread {
            runCatching { LaundryRepository(appContext).collectAll(id) }
            Notifier.cancel(appContext, id)
            ReminderScheduler.cancel(appContext, id)
        }.start()
    }

    companion object {
        const val ACTION_COLLECT = "com.sfyz.clothesescape.COLLECT"
        const val EXTRA_RECORD_ID = "record_id"
    }
}
