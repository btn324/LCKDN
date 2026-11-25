package com.lckdn.app.guard

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * A solid panel drawn over another app's tab button (YouTube Shorts, Instagram Reels).
 * It matches the tab bar colour so the button looks gone, and it swallows every tap.
 * Accessibility overlays need no extra permission.
 */
class TabCover(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private var view: View? = null
    private val shown = Rect()
    private var shownInvisible = false

    /** Package whose tab is currently covered, or null. */
    var coveredPackage: String? = null
        private set

    @SuppressLint("ClickableViewAccessibility")
    fun show(pkg: String, bounds: Rect, darkColor: Int, invisible: Boolean = false) {
        if (bounds.isEmpty) {
            hide()
            return
        }
        // Switching between a visible and an invisible panel needs a fresh window.
        if (view != null && shownInvisible != invisible) hide()
        val color = when {
            invisible -> Color.TRANSPARENT
            isNight() -> darkColor
            else -> LIGHT
        }
        val current = view
        if (current != null && coveredPackage == pkg && shown == bounds) return

        val lp = WindowManager.LayoutParams(
            bounds.width(),
            bounds.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            if (invisible) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top
        }

        try {
            if (current == null) {
                val v = View(service).apply {
                    setBackgroundColor(color)
                    setOnTouchListener { _, _ -> true } // eat every tap
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                wm.addView(v, lp)
                view = v
            } else {
                current.setBackgroundColor(color)
                wm.updateViewLayout(current, lp)
            }
            shown.set(bounds)
            shownInvisible = invisible
            coveredPackage = pkg
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
        shownInvisible = false
        coveredPackage = null
    }

    private fun isNight(): Boolean =
        (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private companion object {
        const val LIGHT = 0xFFFFFFFF.toInt()
    }
}
