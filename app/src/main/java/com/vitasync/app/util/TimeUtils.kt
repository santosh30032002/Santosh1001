package com.vitasync.app.util

import java.util.Calendar

object TimeUtils {
    /** Start of the current day (local time) in epoch millis. */
    fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Start of tomorrow (local time) in epoch millis. */
    fun startOfTomorrow(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Local wall-clock time, e.g. "9:41 AM". */
    fun formatClock(epochMillis: Long): String {
        val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(epochMillis))
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
