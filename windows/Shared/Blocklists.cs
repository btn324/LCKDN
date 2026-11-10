using System.Collections.Generic;

namespace Lckdn;

/** Same rules as the Android app. The family DNS resolver adds millions more on top. */
public static class Blocklists
{
    public static readonly string[] Adult =
    {
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
        "erome.com", "simpcity.su", "fikfap.com", "porngifs.com", "xgroovy.com",
        "porn300.com", "pornone.com", "xcafe.com", "anysex.com", "vjav.com", "javlibrary.com",
        "hentaihaven.xxx", "hentai2read.com", "rule34.xxx", "rule34.paheal.net", "rule34video.com",
        "playboy.com", "brazzers.com", "realitykings.com", "bangbros.com", "naughtyamerica.com",
        "adultfriendfinder.com", "ashleymadison.com", "fling.com", "jerkmate.com", "flirt4free.com",
        "streamate.com", "camster.com", "imlive.com", "slutroulette.com",
        "exoclick.com", "exosrv.com", "exdynsrv.com", "juicyads.com", "trafficjunky.com",
        "trafficjunky.net", "trafficstars.com", "tsyndicate.com", "plugrush.com",
        "ero-advertising.com", "adxxx.com", "trafficfactory.biz", "realsrv.com", "magsrv.com"
    };

    /** Encrypted-DNS endpoints, canaries and proxy sites that would route around the filter. */
    public static readonly string[] Bypass =
    {
        "use-application-dns.net", "dns.google", "dns.google.com", "8888.google",
        "cloudflare-dns.com", "mozilla.cloudflare-dns.com", "chrome.cloudflare-dns.com",
        "1dot1dot1dot1.cloudflare-dns.com", "one.one.one.one", "security.cloudflare-dns.com",
        "doh.opendns.com", "dns.quad9.net", "dns9.quad9.net", "dns10.quad9.net", "dns11.quad9.net",
        "dns.nextdns.io", "doh.cleanbrowsing.org", "doh.dns.sb", "dns.adguard.com",
        "dns.adguard-dns.com", "unfiltered.adguard-dns.com", "doh.mullvad.net", "dns.mullvad.net",
        "dns.controld.com", "freedns.controld.com", "doh.xfinity.com", "dns.alidns.com", "doh.pub", "dns0.eu",
        "croxyproxy.com", "hide.me", "hidester.com", "proxysite.com", "kproxy.com",
        "hidemyass.com", "4everproxy.com", "blockaway.net", "proxyium.com"
    };

    public static readonly string[] Ads =
    {
        "doubleclick.net", "googlesyndication.com", "pagead2.googlesyndication.com",
        "googleadservices.com", "adservice.google.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "adcash.com",
        "hilltopads.net", "clickadu.com", "onclickads.net", "onclkds.com", "adnxs.com",
        "taboola.com", "outbrain.com", "mgid.com", "revcontent.com", "zedo.com", "adform.net",
        "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com", "openx.net",
        "smartadserver.com", "advertising.com", "moatads.com", "amazon-adsystem.com",
        "adcolony.com", "popmyads.com", "a-ads.com", "richpush.co", "pushame.com",
        "evadav.com", "notix.io", "monetag.com", "galaksion.com", "admaven.com", "ad-maven.com"
    };

    /** Apps that tunnel around DNS filtering (process names without .exe). */
    public static readonly string[] BypassProcesses =
    {
        "tor", "opera", "opera_gx", "psiphon3", "psiphon", "ProtonVPN", "ProtonVPN.Client",
        "NordVPN", "nordvpn-service", "ExpressVPN", "expressvpn-ui", "Windscribe", "WindscribeService",
        "hotspotshield", "hsscp", "TunnelBear", "openvpn-gui", "openvpn", "wireguard",
        "CyberGhost", "Surfshark", "pia-client", "hide.me", "ultrasurf", "lantern"
    };

    public static IEnumerable<string> UrlBlocks(Flags f)
    {
        if (f.BlockShorts)
        {
            yield return "youtube.com/shorts";
        }
        if (f.BlockReels)
        {
            yield return "instagram.com/reels";
            yield return "instagram.com/reel/";
        }
        if (f.BlockTikTok) yield return "tiktok.com";
        foreach (var d in Adult) yield return d;
        foreach (var d in Bypass) yield return d;
    }
}
