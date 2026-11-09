package com.lckdn.app.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lckdn.app.Prefs
import com.lckdn.app.Protection
import com.lckdn.app.QuoteReceiver
import com.lckdn.app.R

class MainActivity : AppCompatActivity() {

    private lateinit var lockButton: LockButtonView
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var summary: TextView
    private lateinit var footer: TextView
    private lateinit var warning: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 15_000)
        }
    }

    private val setupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && !Prefs.isLocked(this) && Protection.isReady(this)) {
                confirmLock()
            }
        }

    private val vpnLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Protection.ensureRunning(this)
                handler.postDelayed({ render() }, 900)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lockButton = findViewById(R.id.lockButton)
        statusTitle = findViewById(R.id.statusTitle)
        statusSub = findViewById(R.id.statusSub)
        summary = findViewById(R.id.summary)
        footer = findViewById(R.id.footer)
        warning = findViewById(R.id.warning)

        lockButton.setOnClickListener { onLockPressed() }
        warning.setOnClickListener { restore() }
        footer.setOnClickListener {
            if (Prefs.isLocked(this)) startActivity(Intent(this, UnlockActivity::class.java))
        }
        findViewById<android.view.View>(R.id.settingsButton).setOnClickListener {
            setupLauncher.launch(Intent(this, SetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Protection.ensureRunning(this)
        QuoteReceiver.schedule(this)
        handler.removeCallbacksAndMessages(null)
        handler.post(ticker)
        handler.postDelayed({ render() }, 1200)
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun onLockPressed() {
        if (Prefs.isLocked(this)) {
            lockButton.nudge()
            Toast.makeText(this, R.string.already_locked, Toast.LENGTH_SHORT).show()
            return
        }
        if (!Protection.isReady(this)) {
            setupLauncher.launch(Intent(this, SetupActivity::class.java))
            return
        }
        confirmLock()
    }

    private fun confirmLock() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lock_confirm_title)
            .setMessage(R.string.lock_confirm_body)
            .setPositiveButton(R.string.lock_confirm_yes) { _, _ ->
                Protection.lock(this)
                lockButton.setLocked(true, animate = true)
                render()
                handler.postDelayed({ render() }, 1500)
            }
            .setNegativeButton(R.string.lock_confirm_no, null)
            .show()
    }

    private fun restore() {
        val consent = try {
            VpnService.prepare(this)
        } catch (e: Exception) {
            null
        }
        when {
            consent != null -> vpnLauncher.launch(consent)
            !Protection.isAccessibilityEnabled(this) -> Protection.openAccessibilitySettings(this)
            else -> {
                Protection.ensureRunning(this)
                handler.postDelayed({ render() }, 900)
            }
        }
    }

    private fun render() {
        val locked = Prefs.isLocked(this)
        lockButton.setLocked(locked, animate = false)
        if (locked) {
            statusTitle.setText(R.string.status_on)
            statusSub.text = formatSince(System.currentTimeMillis() - Prefs.lockedSince(this))
            summary.text = Protection.summary(this)
            footer.setText(R.string.footer_on)
            footer.isClickable = true
        } else {
            statusTitle.setText(R.string.status_off)
            statusSub.setText(R.string.status_off_sub)
            summary.text = ""
            footer.setText(R.string.footer_off)
            footer.isClickable = false
        }
        warning.isVisible = locked && !Protection.isHealthy(this)
    }

    private fun formatSince(ms: Long): String {
        val minutes = (ms / 60_000).coerceAtLeast(0)
        val days = minutes / (60 * 24)
        val hours = (minutes / 60) % 24
        val mins = minutes % 60
        return when {
            days > 0 -> "for ${days}d ${hours}h"
            hours > 0 -> "for ${hours}h ${mins}m"
            mins > 0 -> "for ${mins}m"
            else -> "Just now"
        }
    }
}
