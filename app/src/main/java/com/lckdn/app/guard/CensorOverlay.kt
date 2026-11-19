package com.lckdn.app.guard

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * A full-screen opaque panel that instantly blacks out ("satírozza ki") whatever is on screen
 * the moment the guard spots explicit content, so nothing flashes up before the guard backs out.
 * It removes itself automatically after a short moment. Accessibility overlays need no extra
 * permission and sit above every app window.
 */
class CensorOverlay(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null

    @SuppressLint("ClickableViewAccessibility")
    fun flash(durationMs: Long = 1500) {
        show()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ hide() }, durationMs)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun show() {
        if (view != null) return
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val panel = FrameLayout(service).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            setOnTouchListener { _, _ -> true } // swallow every touch underneath
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(
                TextView(service).apply {
                    text = "🔒"
                    setTextColor(Color.parseColor("#2A2A2E"))
                    textSize = 64f
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER }
            )
        }
        try {
            wm.addView(panel, lp)
            view = panel
        } catch (_: Exception) {
            view = null
        }
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        view?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Exception) {
            }
        }
        view = null
    }
}
