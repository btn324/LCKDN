package com.lckdn.app.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.lckdn.app.Notifications
import com.lckdn.app.Prefs
import com.lckdn.app.Protection
import com.lckdn.app.R
import com.lckdn.app.filter.Blocklist
import com.lckdn.app.ui.BlockedActivity

/**
 * Screen-level guard. Only acts while locked in. It:
 *  - backs out of YouTube Shorts and Instagram Reels (the rest of each app keeps working)
 *  - closes TikTok (optional) and filter-bypassing apps such as Tor Browser
 *  - clears explicit searches and leaves explicit pages in browsers
 *  - stops LCKDN from being force-stopped, uninstalled, deactivated or replaced by another VPN
 * Nothing it reads is stored or sent anywhere.
 */
class GuardAccessibilityService : AccessibilityService() {

    private companion object {
        const val YOUTUBE = "com.google.android.youtube"
        const val INSTAGRAM = "com.instagram.android"
        const val VPN_DIALOGS = "com.android.vpndialogs"

        /** Facebook / Messenger: Reels live in these. */
        val FACEBOOK = setOf("com.facebook.katana", "com.facebook.lite", "com.facebook.orca")

        /**
         * Social / content apps where explicit posts, captions and NSFW feeds appear. In these we
         * clear explicit searches, close explicit posts by their visible text, and back out of
         * "sensitive / 18+" age gates (which sit in front of explicit media).
         */
        /** Apps the user chose to block completely (sent home on sight while locked). */
        val SOCIAL_APPS = setOf(
            "com.reddit.frontpage", "com.reddit.scribe",
            "ml.docilealligator.infinityforreddit", "com.rubenmayayo.reddit", "me.ccrama.redditslide",
            "com.laurencedawson.reddit_sync", "com.andrewshu.android.reddit",
            "com.twitter.android", "com.twitter.android.lite",
            "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram",
            "org.telegram.plus", "nekox.messenger", "org.telegram.messenger.beta"
        )

        val FEED_APPS = setOf(
            "com.reddit.frontpage", "com.reddit.scribe",           // Reddit
            "ml.docilealligator.infinityforreddit", "com.rubenmayayo.reddit", "me.ccrama.redditslide",
            "com.twitter.android", "com.twitter.android.lite",     // X / Twitter
            "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", // Telegram
            "com.tumblr", "com.snapchat.android", "com.pinterest", "com.discord",
            "com.bsky.app", "com.zhiliaoapp.musically.go", "com.vkontakte.android", "com.imgur.mobile",
            "com.ninegag.android.app", "br.com.ifunny", "mobi.ifunny", "kik.android", "com.quora.android",
            "com.lemon.lvoverseas", "com.bereal.ft", "com.tellonym.android", "com.yubo.app",
            "com.azarlive.android", "com.wildec.meet24", "com.tinder", "com.bumble.app",
            "org.joinmastodon.android", "com.mastodon.android", "com.deviantart.android.damobile",
            "com.patreon.android", "com.fansly.app", "com.vsco.cam", "com.weheartit"
        )

        /** Age / content gates that precede explicit media. Matched case-insensitively. */
        val FEED_GATES = listOf(
            "sexually explicit", "contains nudity", "mature content", "sensitive content",
            "adult content", "nsfw content", "18+ content", "over 18 to view", "contains sensitive",
            "r/nsfw", "/r/nsfw", "marked as 18+", "view sensitive", "nsfw",
            "you must be 18", "are you over 18", "i'm over 18", "over 18 only", "age-restricted",
            "age restricted", "explicit content", "graphic content", "this community is for adults",
            "felnőtt tartalom", "kényes tartalom", "18 éven felüli", "érzékeny tartalom"
        )

        val YT_SHORTS_IDS = listOf(
            "reel_recycler", "reel_player_page_container", "reel_watch_player", "reel_watch_fragment_root",
            "reel_player_underlay", "reel_progress_bar", "shorts_player", "shorts_container",
            "shorts_video_cell", "reel_player_overlay", "reel_dyn_container"
        ).map { "$YOUTUBE:id/$it" }

        /** The regular video player (not Shorts). */
        val YT_PLAYER_IDS = listOf(
            "player_view", "watch_player", "player_fragment_container", "watch_player_fragment"
        ).map { "$YOUTUBE:id/$it" }

        /** Where the current video's title is rendered. */
        val YT_TITLE_IDS = listOf(
            "title", "watch_title", "expandable_title", "video_title", "player_video_title"
        ).map { "$YOUTUBE:id/$it" }

        val YT_SEARCH_IDS = listOf("search_edit_text", "big_header_search_edit_text").map { "$YOUTUBE:id/$it" }

        // Instagram calls Reels "clips" internally. (Its "reel_viewer" is the STORIES viewer, so it
        // must NOT be matched here, or Stories would be blocked too.)
        val IG_REELS_IDS = listOf(
            "clips_viewer_view_pager", "clips_viewer_root", "root_clips_layout",
            "clips_video_container", "clips_swipe_refresh_container", "clips_viewer_media_container",
            "clips_tab_root"
        ).map { "$INSTAGRAM:id/$it" }

        /** Bottom-bar tab buttons to hide. */
        val YT_TAB_IDS = listOf("$YOUTUBE:id/shorts_pivot_item", "$YOUTUBE:id/pivot_shorts")
        val IG_TAB_IDS = listOf("$INSTAGRAM:id/clips_tab")

        val TIKTOK = setOf(
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme", "com.zhiliaoapp.musically.go"
        )

        /** Apps that route around the DNS filter. */
        val BYPASS_APPS = setOf(
            "org.torproject.torbrowser", "org.torproject.torbrowser_alpha",
            "org.torproject.android", "info.guardianproject.orfox",
            // Cloud-rendered / built-in-proxy browsers: pages load on a remote server, so the
            // phone's DNS filter never sees them.
            "com.cloudmosa.puffin", "com.cloudmosa.puffinFree", "com.cloudmosa.puffinIncognito",
            "com.cloudmosa.puffinTV", "com.aloha.browser", "com.aloha.browser.lite"
        )

        /** System surfaces that never show web pages; skipped by the in-app WebView check. */
        val NON_WEB = setOf(
            "com.android.systemui", "com.google.android.inputmethod.latin", "com.android.vending",
            "com.google.android.gms", "com.android.phone", "com.google.android.dialer",
            "com.android.server.telecom", "com.miui.home", "com.mi.android.globallauncher",
            "com.google.android.apps.nexuslauncher", "com.sec.android.app.launcher"
        )

        val SETTINGS = setOf(
            "com.android.settings", "com.android.settings.intelligence",
            "com.google.android.packageinstaller", "com.android.packageinstaller",
            "com.google.android.permissioncontroller", "com.android.permissioncontroller",
            // Xiaomi / POCO HyperOS & MIUI
            "com.miui.securitycenter", "com.miui.packageinstaller",
            "com.xiaomi.market", "com.miui.cleanmaster", "com.miui.guardprovider",
            // Samsung, Oppo/Realme/OnePlus, Vivo, Huawei
            "com.samsung.android.lool", "com.samsung.android.packageinstaller",
            "com.coloros.safecenter", "com.oplus.safecenter", "com.coloros.phonemanager",
            "com.iqoo.secure", "com.vivo.permissionmanager", "com.vivo.appfilter",
            "com.huawei.systemmanager", "com.android.settings.overlay"
        )

        val DANGER = listOf(
            // English
            "force stop", "uninstall", "deactivate", "turn off", "clear storage", "clear data",
            "disconnect", "forget", "always-on", "stop lckdn", "use lckdn", "lckdn shortcut",
            "disable", "remove app", "app info",
            // Hungarian (accented and plain forms)
            "eltávolít", "eltavolit", "törlés", "torles", "letilt", "kikapcsol", "leállít",
            "leallit", "kényszerített leállítás", "kenyszeritett leallitas", "kényszerleállítás",
            "adatok törlése", "adatok torlese", "tárhely törlése", "deaktivál", "deaktival",
            "alkalmazás eltávolítása", "eltávolítása"
        )

        val BROWSERS: Map<String, List<String>> = buildMap {
            listOf(
                "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
                "com.brave.browser", "com.brave.browser_beta", "com.microsoft.emmx",
                "com.vivaldi.browser", "com.kiwibrowser.browser", "com.yandex.browser"
            ).forEach { put(it, listOf("url_bar", "search_box_text")) }
            listOf("org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix", "org.mozilla.focus")
                .forEach { put(it, listOf("mozac_browser_toolbar_url_view", "url_bar_title", "display_url", "mozac_browser_toolbar_edit_url_view")) }
            put("com.sec.android.app.sbrowser", listOf("location_bar_edit_text", "custom_tab_toolbar_url_bar_text"))
            put("com.sec.android.app.sbrowser.beta", listOf("location_bar_edit_text"))
            put("com.opera.browser", listOf("url_field"))
            put("com.opera.mini.native", listOf("url_field"))
            put("com.opera.gx", listOf("url_field"))
            put("com.duckduckgo.mobile.android", listOf("omnibarTextInput"))
            put("com.mi.globalbrowser", listOf("url"))
            put("com.UCMobile.intl", listOf("titlebar_url"))
            put("com.google.android.googlequicksearchbox", listOf("search_box", "googleapp_search_box"))
            // Read by page content only (their address-bar ids aren't known).
            put("com.android.browser", listOf("url", "url_bar", "title"))
            put("com.google.android.apps.searchlite", listOf("search_box"))
            put("com.ecosia.android", listOf("url_bar"))
            put("org.mozilla.klar", listOf("display_url", "mozac_browser_toolbar_url_view"))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var cover: TabCover? = null
    private var censor: CensorOverlay? = null
    private val lastCheck = HashMap<String, Long>()
    private var lastAction = 0L

    /** Installed web browsers that LCKDN can't read. They are closed while locked in. */
    @Volatile private var otherBrowsers: Set<String> = emptySet()
    private var lastBrowserScan = 0L

    private val watchdog = object : Runnable {
        override fun run() {
            Protection.ensureRunning(this@GuardAccessibilityService)
            Notifications.refreshStatusIfDue(this@GuardAccessibilityService)
            if (SystemClock.uptimeMillis() - lastBrowserScan > 60_000) scanBrowsers()
            handler.postDelayed(this, 10_000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        cover = TabCover(this)
        censor = CensorOverlay(this)
        scanBrowsers()
        handler.removeCallbacks(watchdog)
        handler.post(watchdog)
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        cover?.hide()
        censor?.hide()
        super.onDestroy()
    }

    override fun onInterrupt() {
        cover?.hide()
        censor?.hide()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!Prefs.isLocked(this)) {
            cover?.hide()
            return
        }
        val pkg = event.packageName?.toString() ?: return

        // Another app came to the front: take the tab cover down.
        val covered = cover?.coveredPackage
        if (covered != null && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val front = rootInActiveWindow?.packageName?.toString()
            if (front != null && front != covered) cover?.hide()
        }

        if (pkg == packageName) return
        if (!isInteresting(pkg)) {
            // Any other app: if it is showing a web page (an in-app browser / WebView), read it.
            checkInAppWebView(pkg)
            return
        }

        // Uninstall / force-stop / deactivate screens must be reacted to on EVERY event with no
        // cooldown, otherwise they can be won by spamming the screen open faster than the guard.
        val isTamper = pkg in SETTINGS || pkg in BYPASS_APPS || pkg == VPN_DIALOGS

        val now = SystemClock.uptimeMillis()
        val frequent = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        if (!isTamper && frequent) {
            if (now - (lastCheck[pkg] ?: 0L) < 150) return
            lastCheck[pkg] = now
        }

        // Tab-cover must never be able to throw past this point, or it would stop the
        // Shorts / Reels blocking below from ever running.
        if (pkg == YOUTUBE || pkg == INSTAGRAM || pkg in FACEBOOK) {
            try {
                updateTabCover()
            } catch (_: Exception) {
                cover?.hide()
            }
        }
        if (!isTamper && now - lastAction < 600) return

        // Whole-app blocks don't need the view tree.
        if (pkg in BYPASS_APPS) return act(GLOBAL_ACTION_HOME, getString(R.string.app_blocked))
        if (pkg in TIKTOK && Prefs.flag(this, Prefs.BLOCK_TIKTOK)) {
            return act(GLOBAL_ACTION_HOME, getString(R.string.app_blocked))
        }
        if (pkg in SOCIAL_APPS && Prefs.flag(this, Prefs.BLOCK_SOCIAL)) {
            return act(GLOBAL_ACTION_HOME, getString(R.string.app_blocked))
        }

        val root = rootInActiveWindow ?: return
        val rootPkg = root.packageName?.toString() ?: pkg
        try {
            when {
                rootPkg == YOUTUBE -> {
                    if (Prefs.flag(this, Prefs.BLOCK_SHORTS) && !shortsReelsAllowedNow() &&
                        (hasVisibleId(root, YT_SHORTS_IDS) || isImmersiveShortVideo(root, "short"))
                    ) {
                        act(GLOBAL_ACTION_BACK, getString(R.string.shorts_blocked))
                    } else {
                        checkYouTube(root)
                    }
                }

                rootPkg == INSTAGRAM && Prefs.flag(this, Prefs.BLOCK_REELS) && !shortsReelsAllowedNow() ->
                    // clips_* ids only. No "reel" content fallback here, since Instagram Stories
                    // are also internally "reels" and must stay usable.
                    if (hasVisibleId(root, IG_REELS_IDS)) {
                        act(GLOBAL_ACTION_BACK, getString(R.string.reels_blocked))
                    } else {
                        checkFeedApp(root)
                    }

                rootPkg == INSTAGRAM -> checkFeedApp(root)

                rootPkg in FACEBOOK && Prefs.flag(this, Prefs.BLOCK_REELS) -> {
                    if (!shortsReelsAllowedNow() && isImmersiveReel(root)) act(GLOBAL_ACTION_BACK, getString(R.string.reels_blocked))
                    else checkFeedApp(root)
                }

                rootPkg in FACEBOOK -> checkFeedApp(root)

                rootPkg in FEED_APPS -> checkFeedApp(root)

                isBrowser(rootPkg) -> checkBrowser(root, rootPkg)

                rootPkg == VPN_DIALOGS -> {
                    if (!collectText(root).contains("lckdn")) act(GLOBAL_ACTION_BACK, getString(R.string.vpn_blocked))
                }

                rootPkg in SETTINGS -> checkTamper(root)
            }
        } catch (_: Exception) {
            // The view tree can change under us; just wait for the next event.
        }
    }

    /** Covers the Shorts / Reels tab button while that app is on screen. */
    private fun updateTabCover() {
        val c = cover ?: return
        val root = rootInActiveWindow ?: return
        val front = root.packageName?.toString() ?: return
        val (label, ids, dark, enabled) = when {
            front == YOUTUBE -> TabSpec("Shorts", YT_TAB_IDS, 0xFF0F0F0F.toInt(), Prefs.flag(this, Prefs.BLOCK_SHORTS) && !shortsReelsAllowedNow())
            front == INSTAGRAM -> TabSpec("Reels", IG_TAB_IDS, 0xFF000000.toInt(), Prefs.flag(this, Prefs.BLOCK_REELS) && !shortsReelsAllowedNow())
            front in FACEBOOK -> TabSpec("Reels", emptyList(), 0xFF18191A.toInt(), Prefs.flag(this, Prefs.BLOCK_REELS) && !shortsReelsAllowedNow())
            else -> return
        }
        if (!enabled) {
            c.hide()
            return
        }
        val bounds = findTabBounds(root, label, ids)
        if (bounds == null) c.hide() else c.show(front, bounds, dark)
    }

    private data class TabSpec(val label: String, val ids: List<String>, val dark: Int, val enabled: Boolean)

    /** Bounds of a bottom-bar tab: by view id first, then by its label/description. */
    private fun findTabBounds(root: AccessibilityNodeInfo, label: String, ids: List<String>): Rect? {
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val candidates = ids.flatMap { root.findAccessibilityNodeInfosByViewId(it).orEmpty() } +
            root.findAccessibilityNodeInfosByText(label).orEmpty()
        for (node in candidates) {
            if (!node.isVisibleToUser) continue
            val matchesLabel = node.viewIdResourceName in ids ||
                node.text?.toString().equals(label, ignoreCase = true) ||
                node.contentDescription?.toString()?.startsWith(label, ignoreCase = true) == true
            if (!matchesLabel) continue
            var target: AccessibilityNodeInfo = node
            var hops = 0
            while (!target.isClickable && hops < 3) {
                target = target.parent ?: break
                hops++
            }
            val r = Rect()
            target.getBoundsInScreen(r)
            val inBottomBar = r.centerY() > screenH * 0.78f
            val tabSized = r.width() in 1 until screenW / 2 && r.height() in 1 until screenH / 6
            if (inBottomBar && tabSized) return r
        }
        return null
    }

    private fun isInteresting(pkg: String) =
        pkg == YOUTUBE || pkg == INSTAGRAM || pkg == VPN_DIALOGS || pkg in TIKTOK ||
            pkg in FACEBOOK || pkg in FEED_APPS || pkg in SOCIAL_APPS || pkg in BYPASS_APPS ||
            pkg in SETTINGS || isBrowser(pkg)

    /** Known browsers, every app that opens web links like a browser, and anything named "browser". */
    private fun isBrowser(pkg: String) =
        pkg in BROWSERS || pkg in otherBrowsers || pkg.contains("browser", ignoreCase = true)

    /**
     * Reddit, X, Telegram and other feed apps: clear explicit searches, close explicit posts by
     * their visible text, and back out of "sensitive / 18+" gates. Image-only content with no
     * text cannot be read by accessibility, so this is a strong filter, not a perfect one.
     */
    private fun checkFeedApp(root: AccessibilityNodeInfo) {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused?.isEditable == true) {
            val typed = focused.text?.toString()
            if (typed != null && Blocklist.isExplicitText(typed)) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                toast(getString(R.string.search_blocked))
                lastAction = SystemClock.uptimeMillis()
                return
            }
        }
        val text = collectText(root, limit = 1200)
        if (Blocklist.isExplicitContent(text) || FEED_GATES.any { text.contains(it) }) {
            blockPage(getString(R.string.content_blocked))
        }
    }

    /**
     * Facebook hides its view ids, so a Reel is recognised by its full-screen immersive video:
     * a tall node (most of the screen) whose description mentions a reel or its overlay controls.
     */
    private fun isImmersiveReel(root: AccessibilityNodeInfo): Boolean = isImmersiveShortVideo(root, "reel")

    /**
     * A full-screen vertical short-video player, recognised by a tall node (most of the screen)
     * whose description mentions the feed name ([keyword] = "reel" or "short"). Used as a fallback
     * when the app's view ids change, and for apps (Facebook) that hide their ids entirely.
     */
    private fun isImmersiveShortVideo(root: AccessibilityNodeInfo, keyword: String): Boolean {
        val screenH = resources.displayMetrics.heightPixels
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var seen = 0
        var tallVideo = false
        var words = 0
        val rect = Rect()
        while (stack.isNotEmpty() && seen < 500) {
            val node = stack.removeLast()
            seen++
            val desc = (node.contentDescription?.toString() ?: node.text?.toString())?.lowercase()
            if (desc != null && node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val tall = rect.height() > screenH * 0.6f
                if (tall && (desc.contains(keyword) || (node.className?.contains("TextureView") == true))) tallVideo = true
                if (desc.contains(keyword)) words++
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.addLast(it) }
        }
        return tallVideo && words >= 1
    }

    /**
     * On a real school day (Mon–Fri, not a public holiday or school break) between 07:00 and 14:10,
     * Shorts and Reels are allowed, if the user turned that exception on. On weekends, holidays,
     * breaks (autumn / winter / spring / summer) and outside those hours they stay blocked — and
     * everything else (porn, etc.) stays blocked at all times.
     */
    private fun shortsReelsAllowedNow(): Boolean {
        if (!Prefs.flag(this, Prefs.ALLOW_SHORTS_SCHOOL)) return false
        if (!com.lckdn.app.Holidays.isSchoolDay(java.time.LocalDate.now())) return false
        val now = java.time.LocalTime.now()
        return !now.isBefore(java.time.LocalTime.of(7, 0)) && !now.isAfter(java.time.LocalTime.of(14, 10))
    }

    private fun hasVisibleId(root: AccessibilityNodeInfo, ids: List<String>): Boolean =
        ids.any { id ->
            root.findAccessibilityNodeInfosByViewId(id)?.any { it.isVisibleToUser } == true
        }

    /** Clears explicit YouTube searches and closes any video whose title is explicit. */
    private fun checkYouTube(root: AccessibilityNodeInfo) {
        // Explicit search query: clear it in place.
        for (id in YT_SEARCH_IDS) {
            for (node in root.findAccessibilityNodeInfosByViewId(id).orEmpty()) {
                val text = node.text?.toString()?.takeIf { it.isNotBlank() } ?: continue
                if (!Blocklist.isExplicitText(text)) continue
                if (node.isEditable) {
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    }
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    toast(getString(R.string.search_blocked))
                    lastAction = SystemClock.uptimeMillis()
                } else {
                    act(GLOBAL_ACTION_BACK, getString(R.string.search_blocked))
                }
                return
            }
        }
        // Watching a regular video: if its title is explicit, leave the player.
        if (hasVisibleId(root, YT_PLAYER_IDS)) {
            val title = firstTextOf(root, YT_TITLE_IDS)
            if (title != null && Blocklist.isExplicitText(title)) {
                blockPage(getString(R.string.video_blocked))
            }
        }
    }

    private fun firstTextOf(root: AccessibilityNodeInfo, ids: List<String>): String? {
        for (id in ids) {
            for (node in root.findAccessibilityNodeInfosByViewId(id).orEmpty()) {
                if (!node.isVisibleToUser) continue
                val text = (node.text ?: node.contentDescription)?.toString()?.takeIf { it.isNotBlank() }
                if (text != null) return text
            }
        }
        return null
    }

    private fun checkBrowser(root: AccessibilityNodeInfo, pkg: String) {
        // Strict mode is always on while locked: words in addresses and searches count, not only
        // known sites, and the rendered page itself is read.
        val blockSocial = Prefs.flag(this, Prefs.BLOCK_SOCIAL)
        var url: String? = null
        for (node in addressBarNodes(root, pkg)) {
            val text = node.text?.toString()?.takeIf { it.isNotBlank() } ?: continue
            if (url == null) url = text
            if (blockSocial && Blocklist.isSocialHost(text)) return blockPage(getString(R.string.page_blocked))
            if (Blocklist.isUnsafeMediaSearch(text)) return blockPage(getString(R.string.page_blocked))
            if (!Blocklist.isExplicitText(text)) continue
            if (node.isFocused && node.isEditable) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                toast(getString(R.string.search_blocked))
                lastAction = SystemClock.uptimeMillis()
            } else {
                blockPage(getString(R.string.page_blocked))
            }
            return
        }
        // Read the rendered page (title, visible text, image alt text), so explicit pages, image
        // results and NSFW posts are closed even on sites the domain list doesn't know.
        val page = collectText(root, limit = 1200)
        val onReddit = (url != null && Blocklist.isRedditHost(url)) || page.contains("reddit")
        if (onReddit && Blocklist.isRedditNsfw((url ?: "") + "\n" + page)) {
            return blockPage(getString(R.string.page_blocked))
        }
        if (Blocklist.isExplicitContent(page) || FEED_GATES.any { page.contains(it) }) {
            blockPage(getString(R.string.page_blocked))
        }
    }

    /**
     * The address bar (and search box) of any browser: by its known view id first, then by
     * looking for an editable field or a view whose id names a URL / address / omnibox, so
     * browsers LCKDN has never seen are read too.
     */
    private fun addressBarNodes(root: AccessibilityNodeInfo, pkg: String): List<AccessibilityNodeInfo> {
        val out = ArrayList<AccessibilityNodeInfo>()
        for (id in BROWSERS[pkg].orEmpty()) {
            root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")?.let { out.addAll(it) }
        }
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var seen = 0
        while (stack.isNotEmpty() && seen < 300) {
            val node = stack.removeLast()
            seen++
            val id = node.viewIdResourceName?.lowercase()
            val looksLikeBar = id != null && ADDRESS_ID_HINTS.any { id.contains(it) }
            if ((looksLikeBar || node.isEditable) && !node.text.isNullOrBlank()) out.add(node)
            // Don't descend into the web page itself here; that is read separately.
            if (node.className?.contains("WebView") == true) continue
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.addLast(it) }
        }
        return out
    }

    private val ADDRESS_ID_HINTS = listOf(
        "url", "address", "omnibox", "location", "search_box", "searchbox", "search_edit",
        "search_src", "titlebar", "title_bar", "toolbar_text", "query"
    )

    /**
     * In-app browsers (links opened inside Gmail, Messenger, Discord, Snapchat, news apps...)
     * render in a WebView. If any app shows one, its page is read and closed when explicit.
     */
    private fun checkInAppWebView(pkg: String) {
        if (pkg in NON_WEB || pkg.contains("launcher") || pkg.contains("inputmethod") ||
            pkg.contains("keyboard")
        ) return
        val now = SystemClock.uptimeMillis()
        if (now - (lastCheck[pkg] ?: 0L) < 300 || now - lastAction < 600) return
        lastCheck[pkg] = now
        try {
            val root = rootInActiveWindow ?: return
            if (root.packageName?.toString() != pkg) return
            val web = findWebView(root) ?: return
            val text = collectText(web, limit = 1000)
            if (Blocklist.isExplicitContent(text) || FEED_GATES.any { text.contains(it) } ||
                (text.contains("reddit") && Blocklist.isRedditNsfw(text))
            ) {
                blockPage(getString(R.string.page_blocked))
            }
        } catch (_: Exception) {
        }
    }

    private fun findWebView(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var seen = 0
        while (stack.isNotEmpty() && seen < 400) {
            val node = stack.removeLast()
            seen++
            if (node.className?.contains("WebView") == true && node.isVisibleToUser) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.addLast(it) }
        }
        return null
    }

    /**
     * Every installed app that opens any web address is a browser, and is filtered like one.
     */
    private fun scanBrowsers() {
        lastBrowserScan = SystemClock.uptimeMillis()
        try {
            val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://lckdn-probe.example/"))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            val found = packageManager.queryIntentActivities(probe, PackageManager.MATCH_ALL)
                .mapNotNull { it.activityInfo?.packageName }
                .filter {
                    it != packageName && it !in BROWSERS && it !in SETTINGS && it != "android" &&
                        it != YOUTUBE && it != INSTAGRAM && it !in FACEBOOK && it !in FEED_APPS &&
                        !it.contains("intentresolver") && !it.contains("resolver")
                }
                .toSet()
            otherBrowsers = found
        } catch (_: Exception) {
        }
    }

    private fun checkTamper(root: AccessibilityNodeInfo) {
        val text = collectText(root)
        val aboutUs = text.contains("lckdn")
        if (aboutUs && DANGER.any { text.contains(it) }) {
            // Dismiss the dialog and leave to the home screen, immediately.
            lastAction = SystemClock.uptimeMillis()
            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)
            toast(getString(R.string.tamper_blocked))
            return
        }
        // A custom Private DNS hostname would route lookups around the filter.
        if (text.contains("private dns provider hostname") || text.contains("private dns hostname")) {
            act(GLOBAL_ACTION_BACK, getString(R.string.tamper_blocked))
        }
    }

    /** Lower-cased text + content descriptions of the visible tree (bounded). */
    private fun collectText(root: AccessibilityNodeInfo, limit: Int = 600): String {
        val sb = StringBuilder()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var seen = 0
        while (stack.isNotEmpty() && seen < limit) {
            val node = stack.removeLast()
            seen++
            node.text?.let { sb.append(it).append('\n') }
            node.contentDescription?.let { sb.append(it).append('\n') }
            for (i in 0 until node.childCount) node.getChild(i)?.let { stack.addLast(it) }
        }
        return sb.toString().lowercase()
    }

    private fun act(action: Int, message: String) {
        lastAction = SystemClock.uptimeMillis()
        performGlobalAction(action)
        toast(message)
    }

    /** Black the screen out first (so nothing can be seen), then leave the page. */
    private fun blockPage(reason: String) {
        lastAction = SystemClock.uptimeMillis()
        censor?.flash()
        performGlobalAction(GLOBAL_ACTION_BACK)
        showBlocked(reason)
    }

    private fun showBlocked(reason: String) {
        lastAction = SystemClock.uptimeMillis()
        censor?.flash()   // instantly black out the screen before anything else can be seen
        try {
            startActivity(
                Intent(this, BlockedActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(BlockedActivity.EXTRA_REASON, reason)
            )
        } catch (_: Exception) {
            toast(reason)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
