package com.sfyz.clothesescape

import android.app.Application
import com.sfyz.clothesescape.notify.Notifier
import com.sfyz.clothesescape.notify.ReminderScheduler

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
        val appContext = applicationContext
        Thread {
            runCatching { ReminderScheduler.resync(appContext) }
        }.start()
    }
}
