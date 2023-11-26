package li.songe.gkd.util

import li.songe.gkd.R
import li.songe.gkd.app
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatTimeAgo(timestamp: Long): String {
    val currentTime = System.currentTimeMillis()
    val timeDifference = currentTime - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference)
    val hours = TimeUnit.MILLISECONDS.toHours(timeDifference)
    val days = TimeUnit.MILLISECONDS.toDays(timeDifference)
    val weeks = days / 7
    val months = (days / 30)
    val years = (days / 365)
    return when {
        years > 0 -> app.getString(R.string.time_ago_year, years)
        months > 0 -> app.getString(R.string.time_ago_month, months)
        weeks > 0 -> app.getString(R.string.time_ago_week, weeks)
        days > 0 -> app.getString(R.string.time_ago_day, days)
        hours > 0 -> app.getString(R.string.time_ago_hour, hours)
        minutes > 0 -> app.getString(R.string.time_ago_minute, minutes)
        else -> app.getString(R.string.time_ago_just_now)
    }
}

private val formatDateMap = mutableMapOf<String, SimpleDateFormat>()

fun Long.format(formatStr: String): String {
    var df = formatDateMap[formatStr]
    if (df == null) {
        df = SimpleDateFormat(formatStr, Locale.getDefault())
        formatDateMap[formatStr] = df
    }
    return df.format(this)
}

fun useThrottle(interval: Long = 1000L): (fn: () -> Unit) -> Unit {
    var lastTriggerTime = 0L
    return { fn ->
        val t = System.currentTimeMillis()
        if (t - lastTriggerTime > interval) {
            lastTriggerTime = t
            fn()
        }
    }
}