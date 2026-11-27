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
        // Reddit-NSFW mirrors / image rippers, image boards and leak sites
        "redgifs.com", "gifdeliverynetwork.com", "gfycatporn.com", "scrolller.com", "nsfwreddit.com",
        "redditporn.com", "reddxxx.com", "redditlist.com", "nsfwbase.com", "nsfwcrave.com",
        "rule34.us", "rule34.world", "xbooru.com", "realbooru.com", "hypnohub.net", "yande.re",
        "konachan.com", "konachan.net", "e-hentai.org", "hentai-foundry.com", "pixiv.moe",
        "fapdungeon.com", "fapsafari.com", "fapnado.com", "fapopedia.net", "nudogram.com",
        "onlyfans-leaks.com", "leakimedia.com", "socialmediagirls.com", "forums.socialmediagirls.com",
        "phun.org", "planetsuzy.org", "vipergirls.to", "imx.to", "pixhost.to", "imagetwist.com",
        "imagebam.com", "turboimagehost.com", "porn-images-xxx.com", "sexygirlspics.com",
        "pornpics.de", "elitebabes.com", "hegre.com", "femjoy.com", "x-art.com", "nubiles.net",
        "joymii.com", "zishy.com", "nakedgirls.xxx", "nudepics.co", "girlsofdesire.org",
        "sexyandfunny.com", "boobieblog.com", "drunkenstepfather.com", "fapcat.com", "xasiat.com",
        "tiktits.com", "tikporn.tv", "nsfwtiktok.com", "xxxtik.com", "fikfap.com", "jizzbunker.com",
        "porntn.com", "ixxx.com", "pornoxo.com", "lobstertube.com", "pornburst.xxx", "xozilla.com",
        "letsjerk.tv", "pornky.com", "4kporn.xxx", "povaddict.com", "sexu.com", "redwap.me",
        "chikiporn.com", "pornerbros.com", "alohatube.com", "porndig.com", "pornwhite.com",
        "omegle.fm", "chatroulette.com", "camgirlvideos.org", "mrdeepfakes.com", "deepfakeporn.net",
        "clothoff.io", "undress.app", "nudify.online", "deepnude.cc", "pornpen.ai", "candy.ai",
        "crushon.ai", "spicychat.ai", "janitorai.com",
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
        "nude", "naked", "nudity", "18+", "over18", "over 18", "nsfl", "lingerie", "bikini",
        "thong", "see through", "seethrough", "slutty", "slut", "whore", "horny", "sexy", "hot girl",
        "hotgirl", "e-girl", "egirl", "feet pics", "feetpics", "pawg", "realgirls", "collegesluts",
        "holdthemoan", "cumsluts", "boobies", "petite gonewild", "asstastic", "girlsfinishingthejob",
        "legalteens", "gwcouples", "nsfw_gif", "porninfifteenseconds", "celebnsfw", "onlyfansgirls",
        "sex", "fuck", "cock", "dick pic", "dickpic", "penis", "vagina", "clit", "anal", "nsfw gif",
        "strip", "stripper", "striptease", "camshow", "lap dance", "hookup", "sugar daddy", "deepfake",
        "undressed", "barely legal", "jailbait", "teen sex", "stepsis", "stepmom", "joi ",
        // Hungarian (accent-stripped forms included)
        "szexvideo", "szexfilm", "szexkep", "meztelen", "pucer", "cici", "cicis", "cicik", "csocs",
        "csocsos", "mellbimbo", "punci", "pina", "fasz", "dugas", "dugni", "kuras", "kurva", "ribanc",
        "maszturbal", "merevedes", "orgazmus", "szopas", "fasza", "geci", "didko", "segg", "pornofilm",
        "pornokep", "pornovideo", "cickok", "szexi no", "dekolt", "szexi csaj", "bugyis", "popsi",
        "booty", "thicc", "twerk", "bigass", "nagy fenek", "harisnyas", "szex",
        "meztelenul", "csupasz", "fehernemu", "tanga", "bugyi", "melltarto", "kebel", "keblek",
        "basz", "baszas", "kurvak", "lotyo", "prosti", "sztriptiz", "erotikus", "szexualis",
        "nudi", "akt foto", "aktfoto", "18 even feluli", "felnott tartalom", "kenyes tartalom",
        // German
        "nackt", "titten", "ficken", "muschi", "schwanz", "geile", "nackte", "nutte",
        // French
        "baise", "baiser", "salope", "nichons", "enculer", "branlette", "levrette",
        // Spanish / Portuguese
        "desnuda", "desnudo", "tetas", "cono", "coño", "follar", "corrida", "mamada", "buceta",
        "peitos", "foder", "gozada", "putaria", "safada", "xota", "siririca", "verga",
        // Italian
        "nuda", "figa", "scopare", "troia", "pompino", "sborra",
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
    ).map { fold(it) }

    /**
     * Terms matched only as a whole word, because as a substring they collide with innocent words.
     * In Hungarian, "mellek" (breasts) and "mellék-" (side / auxiliary, as in melléklet, mellékhatás)
     * fold to the same letters — so "mellek" is blocked only when it stands alone, not as a prefix.
     */
    private val boundaryKeywords = listOf(
        "mellek", "mell", "mellei", "mellem", "melleit", "melleid", "cicije", "breasts", "boob",
        "ass", "butt", "fenek", "popo"
    ).map { fold(it) }

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
        "faszen", "faszenet", "dugasz", "dugaszol", "dugaszt",
        // English words containing sex / cock / anal / strip / nude / dick / slut / clit / basz / kefir
        "sextet", "sexton", "susse", "cockroach", "cockatoo", "cockney", "hitchcock", "woodcock",
        "kanal", "analiz", "analitik", "analfabet", "analys", "cocker", "babcock", "thorny", "petits", "strip mall", "stripe",
        "striped", "stripes", "airstrip", "comic strip", "filmstrip", "outstrip", "gaza strip",
        "nudel", "denude", "dickinson", "benedict", "dickens", "predict", "verdict", "addict",
        "edict", "contradict", "dictat", "diction", "dictionar", "jurisdict", "indict", "valedict",
        "slutsk", "clitheroe", "nudelsupp"
    ).map { fold(it) }

    /**
     * Innocent words that are excused ONLY when they stand alone as a whole word (not glued to
     * more letters or digits), unlike [keywordExceptions], which excuse any longer word that
     * merely contains them. "fasza" is everyday Hungarian slang for "great / cool"; it matched
     * both "fasz" and "fasza" and so tripped the two-hit rule on its own. Inflected vulgar
     * forms such as "faszat" or "faszan" are NOT excused and still count.
     */
    private val wholeWordExceptions = listOf("fasza").map { fold(it) }

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
        "proxyfree.com", "genmirror.com", "unblockit.cam", "whateverorigin.org", "12ft.io",
        // Page caches / translators that re-serve a blocked page under another domain
        "translate.goog", "archive.ph", "archive.is", "archive.today", "archive.li", "archive.vn",
        "archive.md", "web.archive.org", "ghostarchive.org", "cachedview.nl", "removeddit.com",
        "reveddit.com", "unddit.com", "camas.unddit.com", "pullpush.io", "api.pullpush.io"
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

    /**
     * Hungarian school system (e-KRÉTA). Its login links carry long random codes and its
     * school lists contain place names that can accidentally contain a blocked fragment, so
     * its own domains are never treated as adult. Exact domains only, nothing else.
     */
    private val trustedSchoolDomains = hashSetOf("e-kreta.hu", "ekreta.hu", "kreta.hu")

    /** True if the address / host belongs to the trusted school system above. */
    fun isTrustedHost(raw: String): Boolean {
        val host = hostOf(fold(raw)) ?: return false
        return matchesSuffix(host, trustedSchoolDomains)
    }

    fun isAdultDomain(domain: String): Boolean {
        val d = fold(domain.trimEnd('.'))
        if (matchesSuffix(d, trustedSchoolDomains)) return false
        if (adultTlds.any { d.endsWith(it) }) return true
        if (matchesSuffix(d, adultDomains)) return true
        return containsAdultKeyword(d)
    }

    /** Text from a browser address bar: a URL, a bare host, or a search query. */
    /**
     * Terms that are explicit in another language but everyday words in Hungarian ("tette" =
     * did / put; Italian = breasts). They count only in searches and addresses, never in page
     * text, where the Hungarian word is everywhere.
     */
    private val searchOnlyKeywords = listOf("tette").map { fold(it) }

    fun isExplicitText(raw: String): Boolean {
        val text = fold(decode(raw))
        val host = hostOf(text)
        if (host != null && matchesSuffix(host, trustedSchoolDomains)) return false
        if (host != null && isAdultDomain(host)) return true
        return containsAdultKeyword(text) || searchOnlyKeywords.any { hasKeyword(text, it) }
    }

    /** Only the host part of what's in the address bar. Used when strict mode is off. */
    fun isExplicitHost(raw: String): Boolean {
        val host = hostOf(fold(raw)) ?: return false
        return isAdultDomain(host)
    }

    /** Sites blocked outright when "Block Reddit, X & Telegram" is on. */
    private val socialHosts = setOf(
        "reddit.com", "redd.it", "redditmedia.com", "redditstatic.com",
        "twitter.com", "x.com", "t.co", "twimg.com",
        "telegram.org", "telegram.me", "t.me", "telegram.dog", "telega.one"
    )

    private val redditHosts = setOf(
        "reddit.com", "redd.it", "redditmedia.com", "reddituploads.com", "redditstatic.com"
    )

    fun isRedditHost(raw: String): Boolean {
        val host = hostOf(fold(raw)) ?: return false
        return matchesSuffix(host, redditHosts)
    }

    /**
     * Reddit's own NSFW markers (post badge, the 18+ community gate, over18 links and the
     * "show NSFW results" search toggle). Any of them on a Reddit page means the page is closed.
     */
    private val redditNsfwMarkers = listOf(
        "nsfw", "over18", "over_18", "include_over_18", "18+", "mature content", "mature community",
        "you must be 18", "are you over 18", "adult content", "this community is for adults",
        "i'm over 18", "im over 18", "yes, i'm over 18", "continue to nsfw", "view nsfw",
        "show nsfw", "explicit", "spoiler nsfw"
    ).map { fold(it) }

    fun isRedditNsfw(raw: String): Boolean {
        val f = fold(decode(raw))
        return redditNsfwMarkers.any { f.contains(it) }
    }

    /** Image / video search pages that SafeSearch DNS cannot lock (Yandex, Baidu, Sogou...). */
    fun isUnsafeMediaSearch(raw: String): Boolean {
        val f = fold(decode(raw))
        val host = hostOf(f) ?: return false
        val path = f.substringAfter(host, "")
        val yandex = host.startsWith("yandex.") || host.contains(".yandex.") || host.startsWith("ya.ru")
        if (yandex && (path.startsWith("/images") || path.startsWith("/video"))) return true
        if (host.startsWith("image.baidu.") || host.startsWith("pic.sogou.") || host.startsWith("image.so.")) return true
        // Google / Bing / DDG with SafeSearch explicitly switched off in the URL.
        if (f.contains("safe=off") || f.contains("safesearch=off") || f.contains("kp=-2") ||
            f.contains("adlt=off")
        ) return true
        return false
    }

    fun isSocialHost(raw: String): Boolean {
        val host = hostOf(fold(raw)) ?: return false
        return matchesSuffix(host, socialHosts)
    }

    /**
     * Unambiguous porn terms: strong enough that one occurrence anywhere means the page is adult.
     * Everything else in [adultKeywords] only counts toward the two-hit rule in [isExplicitContent],
     * so a single ambiguous word in a comment or menu no longer triggers a black-out.
     */
    private val strongCore = listOf(
        "porn", "xxx", "hentai", "onlyfans", "chaturbate", "pornhub", "xvideos", "xnxx", "xhamster",
        "redtube", "youporn", "brazzers", "bangbros", "spankbang", "stripchat", "bongacams",
        "livejasmin", "myfreecams", "eporner", "nhentai", "blowjob", "handjob", "deepthroat",
        "cumshot", "creampie", "gangbang", "bukkake", "masturbat", "deepnude", "camwhore",
        "sexvideo", "xxxvideo", "pornvideo", "pornpic", "nudepic", "bigtits", "bigboobs", "hugetits",
        "szexvideo", "szexfilm", "szexkep", "pornofilm", "pornokep", "pornovideo", "meztelen",
        "mellbimbo", "maszturbal", "bokep", "ngentot", "chudai", "buceta",
        "nsfw", "gonewild", "nudes", "nudity", "rule34", "redgifs",
        "over18", "sexually explicit", "adult content", "mature content", "sensitive content",
        "nsfl", "pawg", "realgirls", "collegesluts", "holdthemoan", "cumsluts", "boobies",
        "pussy", "boobs", "titties", "dildo", "orgasm", "erotic", "erotica", "fetish",
        "bdsm", "slut", "whore", "jerkoff", "lewd", "ecchi", "ahegao", "futanari",
        "deepfake", "undress", "nudify", "topless", "nipples", "nipslip", "upskirt", "cameltoe",
        "onlyfans leak", "leaked nudes", "felnott tartalom", "kenyes tartalom", "pucer", "punci",
        "szopas", "dugas", "baszas", "csocs"
    ).map { fold(it) }

    /**
     * A larger blob of on-screen text (page title + visible labels). Used by the screen guard to
     * catch explicit pages and image results even on unknown sites. To avoid false black-outs it
     * fires only on a strong term, or on two or more distinct adult keywords.
     */
    fun isExplicitContent(raw: String): Boolean {
        val f = fold(decode(raw))
        if (strongCore.any { hasKeyword(f, it) }) return true
        var hits = 0
        for (kw in adultKeywords) {
            if (hasKeyword(f, kw)) {
                hits++
                if (hits >= 2) return true
            }
        }
        for (w in boundaryKeywords) {
            if (hasWord(f, w)) {
                hits++
                if (hits >= 2) return true
            }
        }
        return false
    }

    /**
     * Diagnostics for the block screen: which terms made [isExplicitContent] /
     * [isExplicitText] fire, so false positives can be found and fixed precisely.
     */
    fun explain(raw: String): String {
        val f = fold(decode(raw))
        val found = ArrayList<String>()
        hostOf(f)?.let { h ->
            if (adultTlds.any { h.endsWith(it) }) found.add("tld")
            if (matchesSuffix(h, adultDomains)) found.add("domain:$h")
        }
        strongCore.filter { hasKeyword(f, it) }.forEach { found.add("!$it") }
        adultKeywords.filter { hasKeyword(f, it) }.forEach { found.add(it) }
        boundaryKeywords.filter { hasWord(f, it) }.forEach { found.add("[$it]") }
        return found.distinct().take(6).joinToString(", ").ifEmpty { "-" }
    }

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

    private fun containsAdultKeyword(folded: String): Boolean =
        adultKeywords.any { hasKeyword(folded, it) } || boundaryKeywords.any { hasWord(folded, it) }

    /** True if [kw] occurs in [folded] at a position not inside an innocent word. */
    private fun hasKeyword(folded: String, kw: String): Boolean {
        var idx = folded.indexOf(kw)
        while (idx >= 0) {
            if (!isExcused(folded, idx, kw.length)) return true
            idx = folded.indexOf(kw, idx + 1)
        }
        return false
    }

    /** True if [word] appears as a standalone word (not glued to more letters/digits). */
    private fun hasWord(folded: String, word: String): Boolean {
        var idx = folded.indexOf(word)
        while (idx >= 0) {
            val before = if (idx == 0) ' ' else folded[idx - 1]
            val afterPos = idx + word.length
            val after = if (afterPos >= folded.length) ' ' else folded[afterPos]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            idx = folded.indexOf(word, idx + 1)
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
        for (ex in wholeWordExceptions) {
            var i = text.indexOf(ex)
            while (i >= 0) {
                val end = i + ex.length
                val before = if (i == 0) ' ' else text[i - 1]
                val after = if (end >= text.length) ' ' else text[end]
                if (!before.isLetterOrDigit() && !after.isLetterOrDigit() &&
                    start >= i && start + len <= end
                ) return true
                i = text.indexOf(ex, i + 1)
            }
        }
        return false
    }
}
