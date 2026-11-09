package com.lckdn.app.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.lckdn.app.R
import kotlin.math.min

/** The big round button on the home screen. Draws a padlock that closes when locked. */
class LockButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity

    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val textColor = ContextCompat.getColor(context, R.color.text)
    private val dimColor = ContextCompat.getColor(context, R.color.text_dim)
    private val strokeColor = ContextCompat.getColor(context, R.color.stroke)
    private val surface = ContextCompat.getColor(context, R.color.surface)
    private val surface2 = ContextCompat.getColor(context, R.color.surface2)
    private val bg = ContextCompat.getColor(context, R.color.bg)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * dp; color = strokeColor
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3 * dp; strokeCap = Paint.Cap.ROUND; color = accent
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.5f * dp; color = accent
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 12.5f * sp
        letterSpacing = 0.28f
    }

    private val arcRect = RectF()
    private val bodyRect = RectF()
    private val shackleRect = RectF()
    private val shacklePath = Path()

    private var lockedFraction = 0f
    private var pulse = 0f
    private var isLocked = false
    private var transition: ValueAnimator? = null

    private val pulseAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { pulse = it.animatedValue as Float; invalidate() }
    }

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Lock in"
    }

    fun setLocked(locked: Boolean, animate: Boolean) {
        if (locked == isLocked) return
        isLocked = locked
        transition?.cancel()
        val target = if (locked) 1f else 0f
        if (animate) {
            transition = ValueAnimator.ofFloat(lockedFraction, target).apply {
                duration = 750
                interpolator = DecelerateInterpolator(1.6f)
                addUpdateListener { lockedFraction = it.animatedValue as Float; invalidate() }
                start()
            }
        } else {
            lockedFraction = target
            invalidate()
        }
        if (locked && isAttachedToWindow) pulseAnim.start() else if (!locked) pulseAnim.cancel()
        contentDescription = if (locked) "Locked" else "Lock in"
    }

    /** Small shake when pressed while already locked. */
    fun nudge() {
        ObjectAnimator.ofFloat(this, "translationX", 0f, 10 * dp, -8 * dp, 6 * dp, -3 * dp, 0f)
            .setDuration(420).start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isLocked) pulseAnim.start()
    }

    override fun onDetachedFromWindow() {
        pulseAnim.cancel()
        transition?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val r = radius()
        fillPaint.shader = RadialGradient(
            w / 2f, h / 2f - r * 0.25f, r * 0.95f, surface2, surface, Shader.TileMode.CLAMP
        )
    }

    private fun radius() = min(width, height) / 2f - 4 * dp

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = radius()
        val f = lockedFraction

        // Breathing ripple when locked.
        if (f > 0f) {
            val rr = r * (0.86f + 0.14f * pulse)
            ripplePaint.alpha = (f * 110 * (1f - pulse)).toInt()
            canvas.drawCircle(cx, cy, rr, ripplePaint)
        }

        // Ring.
        val ringR = r * 0.86f
        canvas.drawCircle(cx, cy, ringR, trackPaint)
        if (f > 0f) {
            arcRect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            canvas.drawArc(arcRect, -90f, 360f * f, false, activePaint)
        }

        // Disc.
        canvas.drawCircle(cx, cy, r * 0.78f, fillPaint)

        // Padlock.
        val iconColor = ColorUtils.blendARGB(textColor, accent, f)
        iconPaint.color = iconColor
        shacklePaint.color = iconColor
        shacklePaint.strokeWidth = r * 0.05f

        val bodyW = r * 0.34f
        val bodyH = r * 0.26f
        val bodyTop = cy - r * 0.10f
        bodyRect.set(cx - bodyW / 2, bodyTop, cx + bodyW / 2, bodyTop + bodyH)

        val shackleW = bodyW * 0.62f
        val lift = (1f - f) * r * 0.09f
        val shackleTop = bodyTop - r * 0.20f - lift
        shackleRect.set(cx - shackleW / 2, shackleTop, cx + shackleW / 2, shackleTop + shackleW)
        shacklePath.reset()
        shacklePath.moveTo(cx - shackleW / 2, bodyTop + 1)
        shacklePath.lineTo(cx - shackleW / 2, shackleTop + shackleW / 2)
        shacklePath.arcTo(shackleRect, 180f, 180f, false)
        shacklePath.lineTo(cx + shackleW / 2, bodyTop - lift * 1.2f + 1)
        canvas.drawPath(shacklePath, shacklePaint)

        canvas.drawRoundRect(bodyRect, r * 0.05f, r * 0.05f, iconPaint)
        val holeY = bodyTop + bodyH * 0.42f
        canvas.drawCircle(cx, holeY, r * 0.03f, holePaint)
        canvas.drawRect(cx - r * 0.011f, holeY, cx + r * 0.011f, holeY + bodyH * 0.28f, holePaint)

        // Label.
        labelPaint.color = ColorUtils.blendARGB(dimColor, accent, f)
        canvas.drawText(if (isLocked) "LOCKED" else "LOCK IN", cx, cy + r * 0.40f, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).start()
                return true
            }
            MotionEvent.ACTION_UP -> {
                animate().scaleX(1f).scaleY(1f).setDuration(220).start()
                if (event.x in 0f..width.toFloat() && event.y in 0f..height.toFloat()) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(220).start()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}
