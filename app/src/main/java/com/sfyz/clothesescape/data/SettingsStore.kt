package com.sfyz.clothesescape.data

import android.content.Context
import com.sfyz.clothesescape.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("ce_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state.asStateFlow()

    private fun read(): Settings = Settings(
        slotCount = prefs.getInt(KEY_SLOTS, 6).coerceIn(SLOT_MIN, SLOT_MAX),
        defaultDays = prefs.getInt(KEY_DAYS, 2).coerceIn(1, 14),
        defaultHour = prefs.getInt(KEY_HOUR, 20).coerceIn(0, 23),
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM),
        reduceMotion = prefs.getBoolean(KEY_MOTION, false)
    )

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        prefs.edit()
            .putInt(KEY_SLOTS, next.slotCount.coerceIn(SLOT_MIN, SLOT_MAX))
            .putInt(KEY_DAYS, next.defaultDays.coerceIn(1, 14))
            .putInt(KEY_HOUR, next.defaultHour.coerceIn(0, 23))
            .putString(KEY_THEME, next.themeMode.name)
            .putBoolean(KEY_MOTION, next.reduceMotion)
            .apply()
        _state.value = next
    }

    private companion object {
        const val KEY_SLOTS = "slots"
        const val KEY_DAYS = "days"
        const val KEY_HOUR = "hour"
        const val KEY_THEME = "theme"
        const val KEY_MOTION = "motion"
    }
}
