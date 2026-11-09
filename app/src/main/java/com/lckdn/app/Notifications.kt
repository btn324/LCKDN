package com.lckdn.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lckdn.app.ui.MainActivity
import com.lckdn.app.vpn.BlockerVpnService

object Notifications {
    private const val CHANNEL_STATUS = "status_v2"
    private const val CHANNEL_ALERTS = "alerts"
    private const val CHANNEL_MOTIVATION = "motivation"
    const val ID_STATUS = 1
    private const val ID_ALERT = 2
    private const val ID_QUOTE = 3

    /** The status notification shows a new quote every 30 minutes. */
    private const val STATUS_SLOT_MS = 30 * 60 * 1000L

    @Volatile
    private var lastStatusSlot = -1L

    fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.deleteNotificationChannel("status") // 1.0 channel was too quiet to show quotes
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Protection status", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Protection alerts", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MOTIVATION, "Daily motivation", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Quotes to keep you focused on your goals" }
        )
    }

    private fun openApp(c: Context): PendingIntent =
        PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun currentSlot() = System.currentTimeMillis() / STATUS_SLOT_MS

    fun status(c: Context): Notification {
        ensureChannels(c)
        val slot = currentSlot()
        lastStatusSlot = slot
        val builder = NotificationCompat.Builder(c, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Locked in")
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp(c))
        if (Prefs.flag(c, Prefs.QUOTES)) {
            val quote = Quotes.forSlot(slot).toString()
            builder.setContentText(quote).setStyle(NotificationCompat.BigTextStyle().bigText(quote))
        } else {
            builder.setContentText("Explicit content and distractions are blocked.")
        }
        return builder.build()
    }

    /** Swaps in the next quote when its 30-minute slot has passed. Cheap to call often. */
    fun refreshStatusIfDue(c: Context, force: Boolean = false) {
        if (!BlockerVpnService.running) return
        if (!force && currentSlot() == lastStatusSlot) return
        try {
            NotificationManagerCompat.from(c).notify(ID_STATUS, status(c))
        } catch (_: SecurityException) {
        }
    }

    fun quote(c: Context) {
        ensureChannels(c)
        val nmc = NotificationManagerCompat.from(c)
        if (!nmc.areNotificationsEnabled()) return
        val q = Quotes.random()
        val n = NotificationCompat.Builder(c, CHANNEL_MOTIVATION)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(q.author)
            .setContentText("“${q.text}”")
            .setStyle(NotificationCompat.BigTextStyle().bigText("“${q.text}”"))
            .setAutoCancel(true)
            .setContentIntent(openApp(c))
            .build()
        try {
            nmc.notify(ID_QUOTE, n)
        } catch (_: SecurityException) {
        }
    }

    fun interrupted(c: Context) {
        ensureChannels(c)
        val nmc = NotificationManagerCompat.from(c)
        if (!nmc.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(c, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Protection interrupted")
            .setContentText("Tap to restore LCKDN.")
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(c))
            .build()
        try {
            nmc.notify(ID_ALERT, n)
        } catch (_: SecurityException) {
        }
    }

    fun clearInterrupted(c: Context) {
        NotificationManagerCompat.from(c).cancel(ID_ALERT)
    }
}
