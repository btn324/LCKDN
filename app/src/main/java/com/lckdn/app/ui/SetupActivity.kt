package com.lckdn.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.lckdn.app.Notifications
import com.lckdn.app.Prefs
import com.lckdn.app.Protection
import com.lckdn.app.QuoteReceiver
import com.lckdn.app.R

class SetupActivity : AppCompatActivity() {

    private lateinit var steps: LinearLayout
    private lateinit var toggles: LinearLayout
    private lateinit var continueButton: MaterialButton

    private val refreshOnResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        steps = findViewById(R.id.steps)
        toggles = findViewById(R.id.toggles)
        continueButton = findViewById(R.id.continueButton)
        findViewById<android.view.View>(R.id.back).setOnClickListener { finish() }
        continueButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
        buildToggles()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        steps.removeAllViews()

        addStep(
            "Content filter",
            "Blocks adult sites, ads and pop-ups in every app and browser with a private on-device DNS filter.",
            required = true,
            done = Protection.isVpnPrepared(this)
        ) {
            val intent = try {
                VpnService.prepare(this)
            } catch (e: Exception) {
                null
            }
            if (intent != null) refreshOnResult.launch(intent) else refresh()
        }

        addStep(
            "Screen guard",
            "Blocks Shorts and Reels, stops explicit searches, and protects LCKDN while you're locked in. " +
                "Open Installed apps → LCKDN → turn it on. If the switch is greyed out: Settings → Apps → LCKDN → ⋮ → Allow restricted settings.",
            required = true,
            done = Protection.isAccessibilityEnabled(this)
        ) { Protection.openAccessibilitySettings(this) }

        addStep(
            "Uninstall protection",
            "Prevents LCKDN from being removed while you're locked in.",
            required = false,
            done = Protection.isAdminActive(this)
        ) { refreshOnResult.launch(Protection.adminIntent(this)) }

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            addStep(
                "Notifications",
                "Alerts you if protection is ever interrupted.",
                required = false,
                done = granted
            ) { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }

        val pm = getSystemService(PowerManager::class.java)
        addStep(
            "Run in background",
            "Stops your phone from putting the filter to sleep.",
            required = false,
            done = pm?.isIgnoringBatteryOptimizations(packageName) == true
        ) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        addStep(
            "Always-on VPN",
            "Strongest setting. Tap ⚙ next to LCKDN → turn on Always-on VPN and Block connections without VPN.",
            required = false,
            done = null,
            actionLabel = "Open"
        ) { startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }

        val locked = Prefs.isLocked(this)
        val ready = Protection.isReady(this)
        continueButton.isEnabled = locked || ready
        continueButton.text = when {
            locked -> "Done"
            ready -> "Continue"
            else -> "Complete the required steps"
        }
    }

    private fun addStep(
        title: String,
        desc: String,
        required: Boolean,
        done: Boolean?,
        actionLabel: String = "Allow",
        onAction: () -> Unit
    ) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_step, steps, false)
        row.findViewById<TextView>(R.id.title).text = title
        row.findViewById<TextView>(R.id.badge).text = if (required) "REQUIRED" else "RECOMMENDED"
        row.findViewById<TextView>(R.id.desc).text = desc
        val action = row.findViewById<MaterialButton>(R.id.action)
        if (done == true) {
            action.text = "Done"
            action.isEnabled = false
        } else {
            action.text = actionLabel
            action.setOnClickListener {
                try {
                    onAction()
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't open that screen on this phone.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        steps.addView(row)
    }

    private fun buildToggles() {
        addToggle(
            "Adult content",
            "Always on. Blocks porn and explicit sites and adult ad networks, forces SafeSearch and YouTube Restricted Mode, and closes any YouTube video with an explicit title.",
            key = null
        )
        addToggle("Ads & pop-ups", "Blocks ad, pop-up and redirect networks across apps and browsers.", Prefs.BLOCK_ADS)
        addToggle("YouTube Shorts", "Hides the Shorts tab and backs out of any Short. The rest of YouTube keeps working.", Prefs.BLOCK_SHORTS)
        addToggle("Reels (Instagram + Facebook)", "Hides the Reels tab and backs out of any Reel in Instagram and Facebook. Feeds, Stories and DMs keep working.", Prefs.BLOCK_REELS)
        addToggle("Hide Instagram suggested posts", "Covers “Suggested for you” posts in the home feed so you mostly see people you follow. Tip: Instagram’s own “Following” feed does this perfectly.", Prefs.HIDE_IG_SUGGESTED)
        addToggle("TikTok", "Closes TikTok while you're locked in.", Prefs.BLOCK_TIKTOK)
        addToggle(
            "Block Reddit, X & Telegram",
            "Closes these apps and their websites completely. They are full of image NSFW the on-screen filter can't read, so this blocks them outright.",
            Prefs.BLOCK_SOCIAL
        )
        addToggle(
            "Strict browser filter",
            "Also blocks explicit words in addresses and searches, not just known sites.",
            Prefs.STRICT_BROWSER
        )
        addToggle(
            "Daily motivation",
            "A powerful quote at 8:00, 13:00 and 20:00, plus a fresh one in the Locked in notification every 30 minutes.",
            Prefs.QUOTES
        )
    }

    private fun addToggle(title: String, desc: String, key: String?) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_toggle, toggles, false)
        row.findViewById<TextView>(R.id.title).text = title
        row.findViewById<TextView>(R.id.desc).text = desc
        val toggle = row.findViewById<MaterialSwitch>(R.id.toggle)
        if (key == null) {
            toggle.isChecked = true
            toggle.isEnabled = false
        } else {
            toggle.isChecked = Prefs.flag(this, key)
            toggle.setOnCheckedChangeListener { button, checked ->
                // Quotes aren't protection, so they can always be switched.
                if (!checked && Prefs.isLocked(this) && key != Prefs.QUOTES) {
                    button.isChecked = true
                    Toast.makeText(this, R.string.loosen_blocked, Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                Prefs.setFlag(this, key, checked)
                if (key == Prefs.QUOTES) {
                    QuoteReceiver.schedule(this)
                    Notifications.refreshStatusIfDue(this, force = true)
                }
            }
        }
        toggles.addView(row)
    }
}
