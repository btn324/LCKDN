package com.lckdn.app.filter

import java.net.URLDecoder

/**
 * Local rules applied before any DNS query leaves the phone. The upstream resolvers
 * (AdGuard Family / Cloudflare for Families) add millions more adult and ad domains
 * and enforce SafeSearch on Google, Bing, DuckDuckGo and YouTube.
 */
object Blocklist {

    private val adultTlds = listOf(".xxx", ".porn", ".sex", ".adult")

    private val adultDomains = hashSetOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com", "youporn.com",
        "spankbang.com", "onlyfans.com", "fansly.com", "chaturbate.com", "stripchat.com",
        "bongacams.com", "livejasmin.com", "cam4.com", "camsoda.com", "myfreecams.com",
        "eporner.com", "tnaflix.com", "beeg.com", "txxx.com", "hqporner.com", "porntrex.com",
        "motherless.com", "e621.net", "nhentai.net", "hanime.tv", "f95zone.to", "literotica.com",
        "imagefap.com", "fapello.com", "coomer.su", "coomer.party", "kemono.su", "kemono.party",
        "thothub.to", "noodlemagazine.com", "sxyprn.com", "daftsex.com", "heavy-r.com", "4tube.com",
        "fuq.com", "tube8.com", "drtuber.com", "sunporno.com", "hclips.com", "upornia.com",
        "youjizz.com", "ixxx.com", "manyvids.com", "clips4sale.com", "pornpics.com", "sex.com",
        "redgifs.com", "scrolller.com", "gelbooru.com", "danbooru.donmai.us", "sankakucomplex.com",
        "8muses.com", "luscious.net", "missav.com", "missav.ws", "jable.tv", "javhd.com",
        "erome.com", "simpcity.su", "fikfap.com", "porngifs.com", "xgroovy.com", "ok.xxx",
        "porn300.com", "pornone.com", "xcafe.com", "anysex.com", "vjav.com", "javlibrary.com",
        "hentaihaven.xxx", "hentai2read.com", "rule34.paheal.net", "rule34video.com",
        "playboy.com", "brazzers.com", "realitykings.com", "bangbros.com", "naughtyamerica.com",
        "adultfriendfinder.com", "ashleymadison.com", "fling.com", "jerkmate.com", "flirt4free.com",
        "streamate.com", "camster.com", "imlive.com", "slutroulette.com",
        // Adult ad networks (pop-unders and redirect ads)
        "exoclick.com", "exosrv.com", "exdynsrv.com", "juicyads.com", "trafficjunky.com",
        "trafficjunky.net", "trafficstars.com", "tsyndicate.com", "plugrush.com",
        "ero-advertising.com", "adxxx.com", "trafficfactory.biz", "realsrv.com", "magsrv.com"
    )

    private val adultKeywords = listOf(
        "porn", "xxx", "xvideo", "xnxx", "xhamster", "hentai", "nsfw", "onlyfans", "fansly",
        "chaturbate", "redtube", "youporn", "brazzers", "bangbros", "spankbang", "stripchat",
        "bongacams", "livejasmin", "camsoda", "myfreecams", "eporner", "rule34", "nhentai",
        "motherless", "fapello", "thothub", "camgirl", "sexcam", "milf", "blowjob", "cumshot",
        "pussy", "boobs", "nudes", "erotic", "fetish", "bdsm", "jerkoff", "masturbat", "redgifs",
        "hanime", "gonewild", "deepnude", "undress"
    )

    /** Legitimate words that happen to contain "sex". */
    private val sexExceptions = listOf(
        "essex", "sussex", "middlesex", "wessex", "sexton", "sextant", "sexagen"
    )

    /** DNS-over-HTTPS / DoT endpoints and canaries that would let browsers bypass the filter. */
    private val bypassDomains = hashSetOf(
        "use-application-dns.net", "dns.google", "dns.google.com", "8888.google",
        "cloudflare-dns.com", "mozilla.cloudflare-dns.com", "chrome.cloudflare-dns.com",
        "1dot1dot1dot1.cloudflare-dns.com", "one.one.one.one", "security.cloudflare-dns.com",
        "doh.opendns.com", "doh.familyshield.opendns.com", "dns.quad9.net", "dns9.quad9.net",
        "dns10.quad9.net", "dns11.quad9.net", "dns.nextdns.io", "doh.cleanbrowsing.org",
        "doh.dns.sb", "dns.adguard.com", "dns.adguard-dns.com", "unfiltered.adguard-dns.com",
        "doh.mullvad.net", "dns.mullvad.net", "dns.controld.com", "freedns.controld.com",
        "doh.xfinity.com", "dns.twnic.tw", "dns.alidns.com", "doh.pub", "dns0.eu",
        // Proxy / unblocker sites
        "croxyproxy.com", "hide.me", "hidester.com", "proxysite.com", "kproxy.com",
        "hidemyass.com", "4everproxy.com", "blockaway.net", "proxyium.com"
    )

    private val adDomains = hashSetOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "adcash.com",
        "hilltopads.net", "clickadu.com", "onclickads.net", "onclkds.com", "adnxs.com",
        "taboola.com", "outbrain.com", "mgid.com", "revcontent.com", "zedo.com", "adform.net",
        "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com", "openx.net",
        "smartadserver.com", "advertising.com", "moatads.com", "amazon-adsystem.com",
        "adcolony.com", "popmyads.com", "a-ads.com", "richpush.co", "pushame.com",
        "evadav.com", "notix.io", "monetag.com", "galaksion.com", "admaven.com", "ad-maven.com"
    )

    fun isBlockedDomain(domain: String, blockAds: Boolean): Boolean {
        val d = domain.trimEnd('.').lowercase()
        if (d.isEmpty()) return false
        if (matchesSuffix(d, bypassDomains)) return true
        if (isAdultDomain(d)) return true
        if (blockAds && matchesSuffix(d, adDomains)) return true
        return false
    }

    fun isAdultDomain(domain: String): Boolean {
        val d = domain.trimEnd('.').lowercase()
        if (adultTlds.any { d.endsWith(it) }) return true
        if (matchesSuffix(d, adultDomains)) return true
        return containsAdultKeyword(d)
    }

    /** Text from a browser address bar: a URL, a bare host, or a search query. */
    fun isExplicitText(raw: String): Boolean {
        val text = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (e: Exception) {
            raw
        }.lowercase()
        val host = hostOf(text)
        if (host != null && isAdultDomain(host)) return true
        return containsAdultKeyword(text)
    }

    /** Only the host part of what's in the address bar. Used when strict mode is off. */
    fun isExplicitHost(raw: String): Boolean {
        val host = hostOf(raw.lowercase()) ?: return false
        return isAdultDomain(host)
    }

    private fun hostOf(text: String): String? {
        val t = text.trim().substringAfter("://")
        val host = t.substringBefore('/').substringBefore('?').substringBefore(':')
        return if (host.contains('.') && !host.contains(' ')) host else null
    }

    private fun matchesSuffix(domain: String, set: Set<String>): Boolean {
        var cur = domain
        while (true) {
            if (cur in set) return true
            val dot = cur.indexOf('.')
            if (dot < 0) return false
            cur = cur.substring(dot + 1)
        }
    }

    private fun containsAdultKeyword(text: String): Boolean {
        if (adultKeywords.any { text.contains(it) }) return true
        var idx = text.indexOf("sex")
        while (idx >= 0) {
            val excused = sexExceptions.any { ex ->
                val start = idx - ex.indexOf("sex")
                start >= 0 && text.startsWith(ex, start)
            }
            if (!excused) return true
            idx = text.indexOf("sex", idx + 1)
        }
        return false
    }
}
