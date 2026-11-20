package com.lckdn.app

import android.content.Context
import android.content.SharedPreferences

/** All persistent state. Writes use commit() so a crash can never lose the lock. */
object Prefs {
    private const val FILE = "lckdn"

    private const val LOCKED = "locked"
    private const val LOCKED_SINCE = "locked_since"
    private const val UNLOCK_START = "unlock_start_elapsed"
    private const val UNLOCK_BOOT = "unlock_boot_count"

    const val BLOCK_ADS = "block_ads"
    const val BLOCK_SHORTS = "block_shorts"
    const val BLOCK_REELS = "block_reels"
    const val BLOCK_TIKTOK = "block_tiktok"
    const val STRICT_BROWSER = "strict_browser"
    const val BLOCK_SOCIAL = "block_social"
    const val ALLOW_SHORTS_SCHOOL = "allow_shorts_school"
    const val QUOTES = "quotes"

    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isLocked(c: Context): Boolean = sp(c).getBoolean(LOCKED, false)

    fun lockedSince(c: Context): Long = sp(c).getLong(LOCKED_SINCE, 0L)

    fun setLocked(c: Context, locked: Boolean) {
        sp(c).edit()
            .putBoolean(LOCKED, locked)
            .putLong(LOCKED_SINCE, if (locked) System.currentTimeMillis() else 0L)
            .commit()
    }

    fun flag(c: Context, key: String): Boolean = sp(c).getBoolean(key, key != BLOCK_TIKTOK)

    fun setFlag(c: Context, key: String, value: Boolean) {
        sp(c).edit().putBoolean(key, value).commit()
    }

    private const val SETTINGS_GRANT = "settings_grant_until"

    /** LCKDN's setup screen is opening the "run in background" prompt: allow it for 30 s. */
    fun grantSettings(c: Context) {
        sp(c).edit().putLong(SETTINGS_GRANT, android.os.SystemClock.elapsedRealtime() + 30_000).commit()
    }

    fun settingsGrantActive(c: Context): Boolean =
        android.os.SystemClock.elapsedRealtime() < sp(c).getLong(SETTINGS_GRANT, 0L)

    /** Elapsed-realtime timestamp at which the unlock cooldown started, or -1. */
    fun cooldownStart(c: Context): Long = sp(c).getLong(UNLOCK_START, -1L)

    fun cooldownBoot(c: Context): Int = sp(c).getInt(UNLOCK_BOOT, -1)

    fun startCooldown(c: Context, elapsedRealtime: Long, bootCount: Int) {
        sp(c).edit().putLong(UNLOCK_START, elapsedRealtime).putInt(UNLOCK_BOOT, bootCount).commit()
    }

    fun clearUnlock(c: Context) {
        sp(c).edit().remove(UNLOCK_START).remove(UNLOCK_BOOT).commit()
    }
}
