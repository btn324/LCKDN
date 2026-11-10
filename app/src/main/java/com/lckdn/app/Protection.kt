package com.lckdn.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import com.lckdn.app.guard.GuardAccessibilityService
import com.lckdn.app.vpn.BlockerVpnService

object Protection {

    fun isVpnPrepared(c: Context): Boolean =
        try {
            VpnService.prepare(c) == null
        } catch (e: Exception) {
            false
        }

    fun isAccessibilityEnabled(c: Context): Boolean {
        val enabled = Settings.Secure.getString(
            c.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(c, GuardAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun adminComponent(c: Context) = ComponentName(c, AdminReceiver::class.java)

    fun isAdminActive(c: Context): Boolean {
        val dpm = c.getSystemService(DevicePolicyManager::class.java) ?: return false
        return dpm.isAdminActive(adminComponent(c))
    }

    fun adminIntent(c: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(c))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                c.getString(R.string.admin_explanation)
            )

    /** Required permissions are in place. */
    fun isReady(c: Context): Boolean = isVpnPrepared(c) && isAccessibilityEnabled(c)

    /** Locked and every layer is actually running. */
    fun isHealthy(c: Context): Boolean = BlockerVpnService.running && isAccessibilityEnabled(c)

    fun lock(c: Context) {
        Prefs.setLocked(c, true)
        Prefs.clearUnlock(c)
        ensureRunning(c)
    }

    fun release(c: Context) {
        Prefs.setLocked(c, false)
        Prefs.clearUnlock(c)
        BlockerVpnService.stop(c)
        Notifications.clearInterrupted(c)
    }

    /** Idempotent: brings the filter back if it should be running. */
    fun ensureRunning(c: Context) {
        if (!Prefs.isLocked(c)) return
        if (!isVpnPrepared(c)) {
            Notifications.interrupted(c)
            return
        }
        if (!BlockerVpnService.running) BlockerVpnService.start(c)
        if (isAccessibilityEnabled(c)) Notifications.clearInterrupted(c) else Notifications.interrupted(c)
    }

    fun openAccessibilitySettings(c: Context) {
        c.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openVpnSettings(c: Context) {
        try {
            c.startActivity(Intent("android.settings.VPN_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                c.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
    }

    fun summary(c: Context): String {
        val parts = mutableListOf("Adult content")
        if (Prefs.flag(c, Prefs.BLOCK_ADS)) parts += "Ads & pop-ups"
        if (Prefs.flag(c, Prefs.BLOCK_SHORTS)) parts += "Shorts"
        if (Prefs.flag(c, Prefs.BLOCK_REELS)) parts += "Reels"
        if (Prefs.flag(c, Prefs.BLOCK_TIKTOK)) parts += "TikTok"
        return parts.joinToString("  ·  ") + " blocked"
    }
}
