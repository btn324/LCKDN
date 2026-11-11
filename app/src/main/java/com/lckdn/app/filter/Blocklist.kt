package com.lckdn.app.filter

import java.net.URLDecoder
import java.text.Normalizer

/**
 * Local rules applied before any DNS query leaves the phone, and the keyword engine the
 * screen guard uses to catch explicit searches and pages. The upstream resolvers
 * (AdGuard Family / Cloudflare for Families) add millions more adult and ad domains and
 * enforce SafeSearch on Google, Bing, DuckDuckGo and YouTube on top of this.
 */
object Blocklist {

    private val adultTlds = listOf(".xxx", ".porn", ".sex", ".adult", ".porno", ".cam", ".tube")

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
        "hentaihaven.xxx", "hentai2read.com", "rule34.paheal.net", "rule34video.com", "rule34.xxx",
        "playboy.com", "brazzers.com", "realitykings.com", "bangbros.com", "naughtyamerica.com",
        "adultfriendfinder.com", "ashleymadison.com", "fling.com", "jerkmate.com", "flirt4free.com",
        "streamate.com", "camster.com", "imlive.com", "slutroulette.com", "pornhd.com", "porn.com",
        "xvideos2.com", "hdporn.com", "tubegalore.com", "porngo.com", "pornhat.com",
        "fullporner.com", "hotmovies.com", "xmoviesforyou.com", "watchmdh.to", "spankwire.com",
        "keezmovies.com", "extremetube.com", "nuvid.com", "empflix.com", "pornhublive.com",
        "thumbzilla.com", "cams.com", "sextvx.com", "javmost.com", "supjav.com", "javguru.com",
        "av01.tv", "javbangers.com", "iwank.tv", "porzo.com", "booloo.com", "nudez.com",
        "leakedzone.com", "internetchicks.com", "influencersgonewild.com", "thotslife.com",
        "pichunter.com", "metart.com", "yespornplease.com", "cliphunter.com",
        "boobpedia.com", "babepedia.com", "indexxx.com", "theporndude.com", "pornmd.com",
        // hentai / doujin boards and more tubes
        "e-hentai.org", "exhentai.org", "hitomi.la", "hentaifox.com", "hentaihere.com",
        "hanime.tv", "hentaicity.com", "asmhentai.com", "3hentai.net", "tsumino.com", "pururin.to",
        "multporn.net", "porncomix.info", "8muses.com", "comicsarmy.com", "myreadingmanga.info",
        "simply-hentai.com", "furaffinity.net", "u18chan.com", "rule34hentai.net",
        "camwhores.tv", "camwhoresbay.com", "recurbate.com", "cambro.tv", "nudostar.com",
        "nudostar.tv", "dirtyship.com", "thotslife.com", "fapodrop.com", "fapello.su",
        "leakedzone.com", "faponic.com", "hotleak.vip", "porn3dx.com", "sex3dcartoon.com",
        "yespornplease.net", "pornhub.org", "spankbang.party", "javtiful.com", "highporn.net",
        "watchporn.to", "freeones.com", "babepedia.org", "thenip.net", "nsfw.xxx", "nsfwalbum.com",
        // Adult ad networks (pop-unders and redirect ads)
        "exoclick.com", "exosrv.com", "exdynsrv.com", "juicyads.com", "trafficjunky.com",
        "trafficjunky.net", "trafficstars.com", "tsyndicate.com", "plugrush.com",
        "ero-advertising.com", "adxxx.com", "trafficfactory.biz", "realsrv.com", "magsrv.com"
    )

    /**
     * Explicit terms across many languages, as lowercase, accent-stripped substrings.
     * Chosen to be specific enough that they rarely appear in ordinary text; the user has
     * chosen aggressive blocking over the occasional false positive.
     */
    private val adultKeywords = listOf(
        // English
        "porn", "porno", "xxx", "xvideo", "xnxx", "xhamster", "hentai", "nsfw", "onlyfans",
        "fansly", "chaturbate", "redtube", "youporn", "brazzers", "bangbros", "spankbang",
        "stripchat", "bongacams", "livejasmin", "camsoda", "myfreecams", "eporner", "rule34",
        "nhentai", "motherless", "fapello", "thothub", "camgirl", "camwhore", "sexcam", "milf",
        "blowjob", "handjob", "deepthroat", "cumshot", "creampie", "gangbang", "bukkake", "footjob",
        "pussy", "boobs", "titties", "tits", "titty", "busty", "cleavage", "nipples", "nudes",
        "erotic", "erotica", "fetish", "bdsm", "jerkoff", "masturbat", "redgifs", "hanime",
        "gonewild", "deepnude", "undress", "escort", "camsex", "sexting", "nudify", "upskirt",
        "cameltoe", "softcore", "lewd", "ecchi", "ahegao", "futanari", "doujin", "cumming",
        "orgasm", "dildo", "blowbang", "titjob", "fap ", "coom", "bigtits", "bigboobs", "hugetits",
        "sideboob", "underboob", "braless", "topless", "nipslip", "nudegirl", "nakedgirl",
        "camwhore", "recurbate", "nudostar", "dirtyship", "leaked nudes", "onlyfans leak",
        "sexvideo", "sexphoto", "xxxvideo", "pornvideo", "pornpic", "sexpic", "nudepic",
        // Hungarian (accent-stripped forms included)
        "szexvideo", "szexfilm", "szexkep", "meztelen", "pucer", "cici", "cicis", "cicik", "csocs",
        "csocsos", "mellbimbo", "punci", "pina", "fasz", "dugas", "dugni", "kuras", "kurva", "ribanc",
        "maszturbal", "merevedes", "orgazmus", "szopas", "fasza", "geci", "didko", "pornofilm",
        "pornokep", "pornovideo", "cickok", "szexi no",
        // German
        "nackt", "titten", "ficken", "muschi", "schwanz", "geile", "nackte", "nutte",
        // French
        "baise", "baiser", "salope", "nichons", "enculer", "branlette", "levrette",
        // Spanish / Portuguese
        "desnuda", "desnudo", "tetas", "cono", "coño", "follar", "corrida", "mamada", "buceta",
        "peitos", "foder", "gozada", "putaria", "safada", "xota", "siririca", "verga",
        // Italian
        "nuda", "tette", "figa", "scopare", "troia", "pompino", "sborra",
        // Russian / Ukrainian (translit)
        "siski", "pizda", "trah", "eblja", "minet", "golaya", "porevo", "porno",
        // Polish / Czech / Slovak / Romanian
        "cipka", "cycki", "ruchanie", "kurwa", "prsa", "futai", "muie", "pula", "sani goi",
        // Dutch / Scandinavian
        "naakt", "tieten", "neuken", "naken", "fitta",
        // Turkish
        "sikis", "sikin", "gotten", "ciplak", "amcik",
        // Hindi / Urdu (translit), Indonesian, Tagalog
        "chudai", "nangi", "gaand", "bhabhi", "bokep", "ngentot", "memek", "telanjang", "kantot",
        "hubad", "pekpek",
        // common latinized Japanese/Korean
        "mujun", "yadong"
    )

    /** Longer words that innocently contain a blocked substring. */
    private val keywordExceptions = listOf(
        "essex", "sussex", "middlesex", "wessex", "sexton", "sextant", "sexagen", "sexagesima",
        "homosexual", "heterosexual", "bisexual", "asexual", "sexism", "sexist", "unisex",
        "analysis", "analytic", "analog", "analyt", "canal", "banal", "scandal",
        "therapist", "assassin", "assistant", "assembly", "assess", "class", "pass", "grass",
        "cumberland", "cumulative", "accumulate", "document", "circumstance", "scunthorpe",
        "cockpit", "cocktail", "peacock", "hancock", "shuttlecock", "dickens", "cummings",
        "title", "competition", "constitution", "institute", "latitude", "appetite", "nudge",
        // Hungarian everyday words that contain a blocked fragment
        "faszen", "faszenet", "dugasz", "dugaszol", "dugaszt"
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
        "croxyproxy.com", "croxyproxy.rocks", "hide.me", "hidester.com", "proxysite.com",
        "kproxy.com", "hidemyass.com", "4everproxy.com", "blockaway.net", "proxyium.com",
        "proxyfree.com", "genmirror.com", "unblockit.cam", "whateverorigin.org", "12ft.io"
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
        val d = fold(domain.trimEnd('.'))
        if (adultTlds.any { d.endsWith(it) }) return true
        if (matchesSuffix(d, adultDomains)) return true
        return containsAdultKeyword(d)
    }

    /** Text from a browser address bar: a URL, a bare host, or a search query. */
    fun isExplicitText(raw: String): Boolean {
        val text = fold(decode(raw))
        val host = hostOf(text)
        if (host != null && isAdultDomain(host)) return true
        return containsAdultKeyword(text)
    }

    /** Only the host part of what's in the address bar. Used when strict mode is off. */
    fun isExplicitHost(raw: String): Boolean {
        val host = hostOf(fold(raw)) ?: return false
        return isAdultDomain(host)
    }

    /**
     * A larger blob of on-screen text (page title + visible labels). Used by the screen guard
     * to catch explicit pages and image results even on sites not in the domain list.
     */
    fun isExplicitContent(raw: String): Boolean = containsAdultKeyword(fold(decode(raw)))

    private fun decode(raw: String): String =
        try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (e: Exception) {
            raw
        }

    /** Lowercase, strip accents, and collapse common letter/number substitutions. */
    private fun fold(input: String): String {
        val lowered = input.lowercase()
        val stripped = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        val sb = StringBuilder(stripped.length)
        for (c in stripped) {
            sb.append(
                when (c) {
                    '0' -> 'o'
                    '1' -> 'i'
                    '3' -> 'e'
                    '4' -> 'a'
                    '5' -> 's'
                    '7' -> 't'
                    '@' -> 'a'
                    '$' -> 's'
                    else -> c
                }
            )
        }
        return sb.toString()
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

    private fun containsAdultKeyword(folded: String): Boolean {
        for (kw in adultKeywords) {
            var idx = folded.indexOf(kw)
            while (idx >= 0) {
                if (!isExcused(folded, idx, kw.length)) return true
                idx = folded.indexOf(kw, idx + 1)
            }
        }
        return false
    }

    /** True if the match at [start] sits inside a longer innocent word. */
    private fun isExcused(text: String, start: Int, len: Int): Boolean {
        for (ex in keywordExceptions) {
            var i = text.indexOf(ex)
            while (i >= 0) {
                if (start >= i && start + len <= i + ex.length) return true
                i = text.indexOf(ex, i + 1)
            }
        }
        return false
    }
}
