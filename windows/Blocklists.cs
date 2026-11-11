namespace Lckdn;

/// <summary>
/// The domains and SafeSearch mappings LCKDN writes into the Windows hosts file.
/// Everything here is reversible: locking adds a marked block, unlocking removes it.
/// </summary>
public static class Blocklists
{
    /// <summary>Adult sites and adult/intrusive ad networks. Sent to 0.0.0.0 (nowhere).</summary>
    public static readonly string[] Adult =
    {
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com", "youporn.com",
        "spankbang.com", "onlyfans.com", "fansly.com", "chaturbate.com", "stripchat.com",
        "bongacams.com", "livejasmin.com", "cam4.com", "camsoda.com", "myfreecams.com",
        "eporner.com", "tnaflix.com", "beeg.com", "txxx.com", "hqporner.com", "porntrex.com",
        "motherless.com", "nhentai.net", "hanime.tv", "literotica.com", "imagefap.com",
        "fapello.com", "coomer.su", "kemono.su", "sxyprn.com", "daftsex.com", "4tube.com",
        "fuq.com", "tube8.com", "drtuber.com", "sunporno.com", "hclips.com", "upornia.com",
        "youjizz.com", "ixxx.com", "manyvids.com", "pornpics.com", "sex.com", "redgifs.com",
        "scrolller.com", "gelbooru.com", "rule34.xxx", "rule34video.com", "e-hentai.org",
        "exhentai.org", "hitomi.la", "hentaifox.com", "hentaihaven.xxx", "missav.com", "jable.tv",
        "javhd.com", "erome.com", "simpcity.su", "fikfap.com", "playboy.com", "brazzers.com",
        "realitykings.com", "bangbros.com", "naughtyamerica.com", "adultfriendfinder.com",
        "jerkmate.com", "flirt4free.com", "streamate.com", "camwhores.tv", "recurbate.com",
        "nudostar.com", "dirtyship.com", "thothub.to", "leakedzone.com", "porn.com", "porn300.com",
        "pornone.com", "javmost.com", "supjav.com", "spankwire.com", "keezmovies.com",
        "extremetube.com", "nuvid.com", "thumbzilla.com", "yespornplease.com", "cliphunter.com",
        // adult ad / pop-up networks
        "exoclick.com", "juicyads.com", "trafficjunky.com", "trafficstars.com", "plugrush.com",
        "ero-advertising.com", "realsrv.com", "magsrv.com", "exdynsrv.com"
    };

    public static readonly string[] Ads =
    {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "adcash.com",
        "hilltopads.net", "clickadu.com", "onclickads.net", "adnxs.com", "taboola.com",
        "outbrain.com", "mgid.com", "revcontent.com", "zedo.com", "criteo.com", "pubmatic.com",
        "rubiconproject.com", "openx.net", "smartadserver.com", "moatads.com", "adcolony.com",
        "monetag.com", "galaksion.com", "admaven.com", "ad-maven.com", "popmyads.com"
    };

    /// <summary>Short-video feeds blocked at the host level (whole feed, not the whole app/site).</summary>
    public static readonly string[] ShortFeeds =
    {
        // TikTok (whole site) and known reels/shorts api hosts
        "tiktok.com", "www.tiktok.com", "vm.tiktok.com",
        // Instagram reels delivery / fbcdn reels are hard to isolate; TikTok is the clean one here.
    };

    /// <summary>Reddit / X / Telegram web, blocked only if the user turns BlockSocial on.</summary>
    public static readonly string[] Social =
    {
        "reddit.com", "redd.it", "twitter.com", "x.com", "t.co", "web.telegram.org", "t.me"
    };

    /// <summary>
    /// SafeSearch / Restricted Mode via hosts: point each search/host at its "forced-safe" address.
    /// These IPs are published by Google/Microsoft for exactly this purpose. They rarely change,
    /// but if a provider moves them, update here.
    /// </summary>
    public static readonly (string Ip, string[] Hosts)[] SafeSearch =
    {
        // Google forced SafeSearch (forcesafesearch.google.com = 216.239.38.120)
        ("216.239.38.120", new[]
        {
            "www.google.com", "google.com", "www.google.hu", "google.hu", "www.google.co.uk",
            "google.co.uk", "www.google.de", "google.de", "www.google.at", "google.at",
            "www.google.ca", "google.ca", "www.google.fr", "google.fr", "www.google.es", "google.es",
            "www.google.it", "google.it", "www.google.nl", "google.nl", "www.google.pl", "google.pl",
            "www.google.ro", "google.ro", "www.google.sk", "google.sk", "www.google.cz", "google.cz"
        }),
        // YouTube Restricted Mode (restrict.youtube.com = 216.239.38.120)
        ("216.239.38.120", new[]
        {
            "www.youtube.com", "m.youtube.com", "youtube.com", "youtubei.googleapis.com",
            "youtube.googleapis.com", "www.youtube-nocookie.com"
        }),
        // Bing strict (strict.bing.com = 204.79.197.220)
        ("204.79.197.220", new[] { "www.bing.com", "bing.com" }),
        // DuckDuckGo safe (safe.duckduckgo.com = 20.43.219.69)
        ("20.43.219.69", new[] { "duckduckgo.com", "www.duckduckgo.com" })
    };

    // Cloudflare for Families (adult + malware filtering DNS).
    public static readonly string[] FamilyDnsV4 = { "1.1.1.3", "1.0.0.3" };

    public static IEnumerable<string> ZeroHosts(Flags f)
    {
        if (f.BlockAdult) foreach (var d in Adult) yield return d;
        if (f.BlockAds) foreach (var d in Ads) yield return d;
        if (f.BlockShortFeeds) foreach (var d in ShortFeeds) yield return d;
        if (f.BlockSocial) foreach (var d in Social) yield return d;
    }
}
