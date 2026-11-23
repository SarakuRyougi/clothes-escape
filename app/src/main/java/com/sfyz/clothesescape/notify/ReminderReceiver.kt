package com.sfyz.clothesescape.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sfyz.clothesescape.data.LaundryRepository

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (id <= 0L) return
        val record = runCatching {
            LaundryRepository(context).loadAll().firstOrNull { it.id == id }
        }.getOrNull() ?: return
        if (!record.active) return
        Notifier.showDue(context, record)
    }

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }
}
