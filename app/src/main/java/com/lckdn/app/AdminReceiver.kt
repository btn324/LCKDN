package com.lckdn.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Device admin with no policies: its only job is to make uninstalling a two-step process. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        if (Prefs.isLocked(context)) {
            context.getString(R.string.admin_disable_locked)
        } else {
            context.getString(R.string.admin_disable_unlocked)
        }
}
