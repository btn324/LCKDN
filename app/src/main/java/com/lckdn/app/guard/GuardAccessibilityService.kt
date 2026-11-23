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

        /**
         * Apps that create or switch to another user / space / work profile. LCKDN's filter only
         * runs in the main user, so everything there would be unfiltered. Closed on sight.
         * com.miui.securitycore hosts Xiaomi Second space and Dual apps.
         */
        val SPACE_APPS = setOf(
            "com.miui.securitycore", "com.miui.securityspace", "com.miui.dualspace",
            "com.oasisfeng.island", "com.oasisfeng.island.fdroid", "com.oasisfeng.insular",
            "net.typeblog.shelter", "com.lbe.parallel.intl", "com.parallel.space.lite",
            "com.parallel.space.pro", "com.excelliance.multiaccounts", "com.ludashi.dualspace",
            "com.samsung.knox.securefolder", "com.samsung.android.knox.containercore",
            "com.huawei.privatespace", "com.coloros.childrenspace", "com.oplus.securityspace"
        )

        /** Settings screens for second space, extra users, guests and work / private profiles. */
        val SPACE_PAGES = listOf(
            "second space", "create second space", "switch to second space", "go to second space",
            "dual apps", "app twin", "clone app", "parallel apps", "secure folder", "private space",
            "multiple users", "add user", "add guest", "switch to guest", "guest mode", "new user",
            "add new user", "work profile", "set up work profile",
            "második tér", "masodik ter", "második terület", "masodik terulet",
            "kettős alkalmazás", "kettos alkalmazas", "dupla alkalmazás", "dupla alkalmazas",
            "alkalmazásklón", "alkalmazasklon", "privát tér", "privat ter", "biztonságos mappa",
            "több felhasználó", "tobb felhasznalo", "felhasználó hozzáadása", "felhasznalo hozzaadasa",
            "vendég hozzáadása", "vendeg hozzaadasa", "váltás vendégre", "valtas vendegre",
            "vendég mód", "vendeg mod", "új felhasználó", "uj felhasznalo", "munkahelyi profil"
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
            "com.huawei.systemmanager", "com.android.settings.overlay",
            // Autostart / background / battery managers (Xiaomi, Samsung, Oppo, Vivo, Huawei)
            "com.miui.powerkeeper", "com.miui.permcenter", "com.miui.notification", "com.xiaomi.misettings",
            "com.miui.securityadd", "com.miui.battery", "com.samsung.android.sm.devicesecurity",
            "com.samsung.android.sm", "com.samsung.android.sm_cn", "com.oplus.battery",
            "com.coloros.oppoguardelf", "com.oplus.startupapp", "com.coloros.startupapp",
            "com.vivo.abe", "com.iqoo.powersaving", "com.huawei.powergenie"
        )

        /**
         * Pages that let an app be stopped from starting or running in the background (Xiaomi
         * "Autostart", battery saver / restrictions, "pause app activity"...). Turning these off
         * for LCKDN kills the filter, so while locked they can only be used to turn it back ON.
         */
        val BACKGROUND_PAGES = listOf(
            "autostart", "auto-start", "auto start", "auto-launch", "auto launch", "startup",
            "run in background", "background activity", "background running", "background usage",
            "background restriction", "battery saver", "battery optimi", "battery usage",
            "app battery", "no restrictions", "restricted", "unrestricted", "optimized",
            "pause app activity", "manage app if unused", "sleeping apps", "deep sleeping",
            "app launch", "power management",
            // Notification settings: with LCKDN's notifications off, the phone may kill it.
            "notification", "show notifications", "allow notifications", "értesítés", "ertesites",
            "automatikus indítás", "automatikus inditas", "automatikus indulás", "automatikus indulas",
            "háttér", "hatter", "akkumulátor", "akkumulator", "energiatakarék", "energiatakarek",
            "korlátozás", "korlatozas", "korlátlan", "korlatlan", "optimalizál", "optimalizal",
            "alkalmazástevékenység", "szüneteltet", "szuneteltet", "indítás", "inditas"
        )

        /**
         * Battery-saver / power-saving modes (Xiaomi "Ultra battery saver", Samsung "Maximum power
         * saving", stock "Battery Saver"...). They freeze background apps and can switch off the
         * VPN and accessibility service, which lets the filter be bypassed. Matched on any
         * settings page, whether or not LCKDN is mentioned on it.
         */
        val POWER_SAVE_TERMS = listOf(
            "ultra battery", "ultra power", "extreme battery", "extreme power", "super battery",
            "super power saving", "maximum power saving", "max power saving", "battery saver",
            "power saving mode", "power saver", "power-saving", "low power mode",
            // Hungarian (accented and plain forms)
            "ultra akkumul", "akkumulátorkímélő", "akkumulatorkimelo", "akkumulátor-takarékos",
            "akkumulator-takarekos", "energiatakarékos", "energiatakarekos", "takarékos mód",
            "takarekos mod"
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

        val FIREFOX = setOf(
            "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix",
            "org.mozilla.focus", "org.mozilla.klar"
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
    /** Covers only LCKDN's own row on autostart / battery lists, so other apps stay settable. */
    private var rowCover: TabCover? = null
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
        rowCover = TabCover(this)
        scanBrowsers()
        handler.removeCallbacks(watchdog)
        handler.post(watchdog)
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        cover?.hide()
        censor?.hide()
        rowCover?.hide()
        super.onDestroy()
    }

    override fun onInterrupt() {
        cover?.hide()
        rowCover?.hide()
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
        val rowCovered = rowCover?.coveredPackage
        if (rowCovered != null && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val front = rootInActiveWindow?.packageName?.toString()
            if (front != null && front != rowCovered) rowCover?.hide()
        }

        if (pkg == packageName) return
        if (pkg == SYSTEM_UI) {
            checkShadeTiles()
            checkNotificationShade()
            return
        }
        if (!isInteresting(pkg)) {
            // Any other app: if it is showing a web page (an in-app browser / WebView), read it.
            checkInAppWebView(pkg)
            return
        }

        // Uninstall / force-stop / deactivate screens must be reacted to on EVERY event with no
        // cooldown, otherwise they can be won by spamming the screen open faster than the guard.
        val isTamper = pkg in SETTINGS || pkg in BYPASS_APPS || pkg in SPACE_APPS || pkg == VPN_DIALOGS

        val now = SystemClock.uptimeMillis()
        val frequent = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        if (!isTamper && frequent) {
            // Browsers (Firefox above all) slow down and crash if their page tree is read too
            // often, so they are checked less frequently than other apps.
            val gap = when {
                pkg in FIREFOX -> 1000L
                isBrowser(pkg) -> 600L
                else -> 150L
            }
            if (now - (lastCheck[pkg] ?: 0L) < gap) return
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
        if (pkg in SPACE_APPS) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return act(GLOBAL_ACTION_HOME, getString(R.string.space_blocked))
        }
        if (pkg in TIKTOK && Prefs.flag(this, Prefs.BLOCK_TIKTOK)) {
            return act(GLOBAL_ACTION_HOME, ge 
