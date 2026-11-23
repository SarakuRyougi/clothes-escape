package com.sfyz.clothesescape.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    private val dateFmt = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val fullFmt = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    private val weekFmt = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)

    /** "N 天后 M 点" expressed as an absolute epoch millis. */
    fun dueAt(nowMillis: Long, days: Int, hour: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val target = now.toLocalDate().plusDays(days.toLong()).atTime(hour, 0)
        return target.atZone(zone).toInstant().toEpochMilli()
    }

    fun remainingText(now: Long, due: Long): String {
        val diff = due - now
        return if (diff <= 0) "超时 ${durationText(-diff)}" else "还有 ${durationText(diff)}"
    }

    fun durationText(ms: Long): String {
        val totalMinutes = ms / 60_000L
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days} 天 ${hours} 小时"
            hours > 0 -> "${hours} 小时 ${minutes} 分"
            else -> "${minutes} 分钟"
        }
    }

    fun clock(hour: Int): String = String.format(Locale.CHINA, "%02d:00", hour)

    fun full(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(fullFmt)

    fun date(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(dateFmt)

    fun weekday(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(weekFmt)

    fun time(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(timeFmt)

    fun dayKey(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()

    fun monthStart(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
}
