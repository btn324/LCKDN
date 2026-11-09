package com.lckdn.app.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lckdn.app.Prefs
import com.lckdn.app.Protection
import com.lckdn.app.R
import java.security.SecureRandom

/**
 * The only way out:
 *  1. type 480 random characters with zero mistakes (pasting resets),
 *  2. wait 60 minutes (a reboot restarts the timer),
 *  3. confirm within a 10-minute window.
 */
class UnlockActivity : AppCompatActivity() {

    private companion object {
        const val COOLDOWN_MS = 60 * 60 * 1000L
        const val WINDOW_MS = 10 * 60 * 1000L
        const val GROUPS = 80
        const val GROUP_LEN = 5
        const val ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        const val INTRO = 0
        const val TYPING = 1
        const val COOLDOWN = 2
        const val READY = 3
    }

    private lateinit var stageIntro: View
    private lateinit var stageTyping: View
    private lateinit var stageCooldown: View
    private lateinit var stageReady: View
    private lateinit var introNote: TextView
    private lateinit var challengeView: TextView
    private lateinit var input: EditText
    private lateinit var progress: TextView
    private lateinit var typingMessage: TextView
    private lateinit var timer: TextView
    private lateinit var cooldownNote: TextView
    private lateinit var readyTimer: TextView

    private var stage = INTRO
    private var challenge = ""
    private var ignoreChanges = false
    private var pasteDetected = false

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            refreshCooldown()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.isLocked(this)) {
            finish()
            return
        }
        setContentView(R.layout.activity_unlock)

        stageIntro = findViewById(R.id.stageIntro)
        stageTyping = findViewById(R.id.stageTyping)
        stageCooldown = findViewById(R.id.stageCooldown)
        stageReady = findViewById(R.id.stageReady)
        introNote = findViewById(R.id.introNote)
        challengeView = findViewById(R.id.challenge)
        input = findViewById(R.id.input)
        progress = findViewById(R.id.progress)
        typingMessage = findViewById(R.id.typingMessage)
        timer = findViewById(R.id.timer)
        cooldownNote = findViewById(R.id.cooldownNote)
        readyTimer = findViewById(R.id.readyTimer)

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.stayButton).setOnClickListener { finish() }
        findViewById<View>(R.id.beginButton).setOnClickListener { beginTyping() }
        findViewById<View>(R.id.cancelButton).setOnClickListener { stayLocked() }
        findViewById<View>(R.id.keepButton).setOnClickListener { stayLocked() }
        findViewById<View>(R.id.turnOffButton).setOnClickListener { confirmTurnOff() }

        setUpInput()
    }

    override fun onResume() {
        super.onResume()
        if (!Prefs.isLocked(this)) {
            finish()
            return
        }
        if (stage != TYPING) {
            show(if (Prefs.cooldownStart(this) >= 0) COOLDOWN else INTRO)
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onStop() {
        // Leaving the screen mid-challenge throws away progress.
        if (stage == TYPING) show(INTRO)
        super.onStop()
    }

    private fun show(s: Int) {
        stage = s
        stageIntro.isVisible = s == INTRO
        stageTyping.isVisible = s == TYPING
        stageCooldown.isVisible = s == COOLDOWN
        stageReady.isVisible = s == READY
        if (s != TYPING) hideKeyboard()
    }

    // ---- Stage 1: typing -------------------------------------------------------------------

    private fun beginTyping() {
        introNote.isVisible = false
        challenge = generateChallenge()
        typingMessage.text = ""
        clearInput()
        renderChallenge(0)
        show(TYPING)
        input.requestFocus()
        getSystemService(InputMethodManager::class.java)?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun generateChallenge(): String {
        val rnd = SecureRandom()
        return (0 until GROUPS).joinToString(" ") {
            (0 until GROUP_LEN).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.joinToString("")
        }
    }

    private fun setUpInput() {
        val blockActions = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
        input.customSelectionActionModeCallback = blockActions
        input.customInsertionActionModeCallback = blockActions
        input.isLongClickable = false
        input.setTextIsSelectable(false)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!ignoreChanges && count - before > 2) pasteDetected = true
            }

            override fun afterTextChanged(s: Editable?) {
                if (ignoreChanges || stage != TYPING) return
                val typed = s?.toString().orEmpty()
                if (pasteDetected) {
                    fail("Pasting isn't allowed. Start over.")
                    return
                }
                if (!challenge.startsWith(typed)) {
                    fail("Mistake at character ${typed.length}. Start over.")
                    return
                }
                typingMessage.text = ""
                renderChallenge(typed.length)
                if (typed.length == challenge.length) onTypingComplete()
            }
        })
    }

    private fun fail(message: String) {
        pasteDetected = false
        clearInput()
        renderChallenge(0)
        typingMessage.text = message
        ObjectAnimator.ofFloat(input, "translationX", 0f, 18f, -14f, 10f, -6f, 0f)
            .setDuration(380).start()
    }

    private fun clearInput() {
        ignoreChanges = true
        input.setText("")
        ignoreChanges = false
        pasteDetected = false
    }

    private fun renderChallenge(done: Int) {
        val span = SpannableString(challenge)
        span.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent)),
            0, done, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        challengeView.text = span
        progress.text = "$done / ${challenge.length}"
    }

    private fun onTypingComplete() {
        clearInput()
        Prefs.startCooldown(this, SystemClock.elapsedRealtime(), bootCount())
        cooldownNote.text = ""
        show(COOLDOWN)
        refreshCooldown()
    }

    // ---- Stage 2 & 3: cooldown and confirmation window --------------------------------------

    private fun bootCount(): Int =
        Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)

    private fun refreshCooldown() {
        if (stage == TYPING) return
        val start = Prefs.cooldownStart(this)
        if (start < 0) {
            if (stage != INTRO) show(INTRO)
            return
        }
        val boot = bootCount()
        if (boot != Prefs.cooldownBoot(this) || SystemClock.elapsedRealtime() < start) {
            Prefs.startCooldown(this, SystemClock.elapsedRealtime(), boot)
            cooldownNote.text = "Your phone restarted, so the timer started over."
        }
        val elapsed = SystemClock.elapsedRealtime() - Prefs.cooldownStart(this)
        when {
            elapsed < COOLDOWN_MS -> {
                if (stage != COOLDOWN) show(COOLDOWN)
                timer.text = clock(COOLDOWN_MS - elapsed)
            }
            elapsed < COOLDOWN_MS + WINDOW_MS -> {
                if (stage != READY) show(READY)
                readyTimer.text = "This option disappears in ${clock(COOLDOWN_MS + WINDOW_MS - elapsed)}."
            }
            else -> {
                Prefs.clearUnlock(this)
                show(INTRO)
                introNote.text = "Your unlock window expired, so protection stayed on. That's a win."
                introNote.isVisible = true
            }
        }
    }

    private fun clock(ms: Long): String {
        val total = (ms + 999) / 1000
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private fun stayLocked() {
        Prefs.clearUnlock(this)
        Toast.makeText(this, "Good call. Still locked in.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmTurnOff() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Turn off LCKDN?")
            .setMessage("All blocking stops immediately. You can lock in again any time.")
            .setPositiveButton("Turn off") { _, _ ->
                Protection.release(this)
                Toast.makeText(this, "Protection is off.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Keep me locked in") { _, _ -> stayLocked() }
            .show()
    }

    private fun hideKeyboard() {
        if (!::input.isInitialized) return
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0)
    }
}
