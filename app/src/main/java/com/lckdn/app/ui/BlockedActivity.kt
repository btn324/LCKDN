package com.lckdn.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.lckdn.app.Quotes
import com.lckdn.app.R

class BlockedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REASON = "reason"
        /** Small diagnostic line (which check fired and why), to report false positives. */
        const val EXTRA_DETAIL = "detail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)
        bind(intent)
        findViewById<android.view.View>(R.id.home).setOnClickListener { goHome() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        bind(intent)
    }

    private fun bind(intent: Intent?) {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: getString(R.string.page_blocked)
        val detail = intent?.getStringExtra(EXTRA_DETAIL).orEmpty()
        findViewById<TextView>(R.id.reason).text =
            if (detail.isBlank()) reason else "$reason\n\n($detail)"
        val q = Quotes.random()
        findViewById<TextView>(R.id.quote).text = "“${q.text}”"
        findViewById<TextView>(R.id.quoteAuthor).text = q.author.uppercase()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
