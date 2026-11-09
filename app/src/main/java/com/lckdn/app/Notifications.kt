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

object Notifications {
    private const val CHANNEL_STATUS = "status"
    private const val CHANNEL_ALERTS = "alerts"
    const val ID_STATUS = 1
    private const val ID_ALERT = 2

    fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Protection status", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Protection alerts", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun openApp(c: Context): PendingIntent =
        PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun status(c: Context): Notification {
        ensureChannels(c)
        return NotificationCompat.Builder(c, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Locked in")
            .setContentText("Explicit content and distractions are blocked.")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openApp(c))
            .build()
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
