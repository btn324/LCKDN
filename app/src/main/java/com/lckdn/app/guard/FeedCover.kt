package com.lckdn.app.guard

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * A visual-only panel over a region of another app (e.g. an Instagram "Suggested" post). Unlike
 * [TabCover], it does NOT eat touches — it passes them through (FLAG_NOT_TOUCHABLE) so the user can
 * still scroll the feed; it only hides the content underneath. Accessibility overlays need no extra
 * permission.
 */
class FeedCover(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private var view: View? = null
    private val shown = Rect()

    fun show(bounds: Rect, darkColor: Int) {
        if (bounds.isEmpty || bounds.height() < 2) {
            hide()
            return
        }
        val color = if (isNight()) darkColor else 0xFFFFFFFF.toInt()
        if (view != null && shown == bounds) return

        val lp = WindowManager.LayoutParams(
            bounds.width(),
            bounds.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top
        }

        try {
            val current = view
            if (current == null) {
                val v = View(service).apply {
                    setBackgroundColor(color)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                wm.addView(v, lp)
                view = v
            } else {
                current.setBackgroundColor(color)
                wm.updateViewLayout(current, lp)
            }
            shown.set(bounds)
        } catch (_: Exception) {
            hide()
        }
    }

    fun hide() {
        view?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Exception) {
            }
        }
        view = null
        shown.setEmpty()
    }

    private fun isNight(): Boolean =
        (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
