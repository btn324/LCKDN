package com.lckdn.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** Posts a motivational quote at 8:00, 13:00 and 20:00 each day. */
class QuoteReceiver : BroadcastReceiver() {

    companion object {
        private val HOURS = listOf(8, 13, 20)
        private const val ACTION = "com.lckdn.app.action.QUOTE"

        private fun pendingIntent(c: Context): PendingIntent =
            PendingIntent.getBroadcast(
                c, 10,
                Intent(c, QuoteReceiver::class.java).setAction(ACTION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        /** Schedules the next quote, or cancels if quotes are off. Safe to call repeatedly. */
        fun schedule(c: Context) {
            val am = c.getSystemService(AlarmManager::class.java) ?: return
            val pi = pendingIntent(c)
            if (!Prefs.flag(c, Prefs.QUOTES)) {
                am.cancel(pi)
                return
            }
            am.set(AlarmManager.RTC_WAKEUP, nextTime(), pi) // inexact, battery friendly
        }

        private fun atHour(base: Calendar, hour: Int, addDays: Int = 0): Calendar =
            (base.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, addDays)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        private fun nextTime(): Long {
            val now = Calendar.getInstance()
            for (h in HOURS) {
                val t = atHour(now, h)
                if (t.after(now)) return t.timeInMillis
            }
            return atHour(now, HOURS.first(), addDays = 1).timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION && Prefs.flag(context, Prefs.QUOTES)) Notifications.quote(context)
        schedule(context)
    }
}
