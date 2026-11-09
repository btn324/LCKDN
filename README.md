# LCKDN

**One button. Locked in.**

LCKDN is an Android app that blocks porn and explicit content, ads and pop-ups, YouTube Shorts and Instagram Reels across your whole phone with a single tap. Once you lock in, there is no off switch. Getting out takes so much effort that it isn't worth it.

## What it blocks

| | How |
|---|---|
| **Adult content** (always on) | An on-device DNS filter blocks known adult sites, adult ad networks and explicit domains in every app and browser. Lookups go to AdGuard Family DNS, which blocks millions more adult and ad domains and enforces SafeSearch on Google, Bing and DuckDuckGo plus YouTube Restricted Mode. Cloudflare for Families is the fallback. |
| **Ads & pop-ups** | Ad, pop-under and redirect networks are blocked at the DNS level. |
| **YouTube Shorts** | The Shorts tab button is hidden and can't be tapped, and any Short that still opens is closed. The rest of YouTube works normally. |
| **Instagram Reels** | The Reels tab button is hidden and can't be tapped, and any Reel that still opens is closed. Feed, Stories and DMs work normally. |
| **TikTok** (optional) | Closed while you're locked in. |
| **Explicit searches** | Explicit words typed in browser address bars are cleared, and explicit pages are closed. |
| **Daily motivation** | A quote at 8:00, 13:00 and 20:00, and a fresh one in the Locked in notification every 30 minutes. |
| **Bypass routes** | DNS-over-HTTPS endpoints, proxy sites, Tor Browser, other VPNs and custom Private DNS are blocked. |

## Getting out is hard on purpose

While you're locked in, LCKDN stops itself from being force-stopped, uninstalled, deactivated, or replaced by another VPN. The only way out is in the app:

1. **Type 480 random characters** with zero mistakes. One wrong key resets you, and pasting is detected.
2. **Wait 60 minutes.** Restarting your phone restarts the timer.
3. **Confirm within 10 minutes**, or protection stays on.

## Install

1. Download `LCKDN.apk` from the [latest release](../../releases/latest) on your phone and open it. Allow installing from your browser if Android asks.
2. Open LCKDN and tap the button. Setup walks you through the permissions:
   - **Content filter**: approve the VPN request. It's a local filter; your traffic isn't sent to a VPN server.
   - **Screen guard**: turn on LCKDN under Accessibility → Installed apps. On Android 13+, if the switch is greyed out, go to Settings → Apps → LCKDN → ⋮ → **Allow restricted settings** first.
   - Recommended: uninstall protection, notifications, background running, and **Always-on VPN + Block connections without VPN**.
3. Tap **Lock in**.

## Privacy

Everything runs on the phone. LCKDN has no account, no analytics and no server. Only domain-name lookups leave the device, going to the family DNS resolver. Screen content read by the accessibility service is checked in memory and never stored or sent.

## Limits

No app can make a phone fully tamper-proof without a factory reset or managed-device setup. Safe mode (which disables all third-party apps), a computer with ADB, or a factory reset can still remove LCKDN. Apps that use their own encrypted DNS or a built-in VPN can get around DNS filtering. LCKDN blocks the common ones, but the list can't cover everything. Shorts and Reels detection relies on the apps' current screen layout and may need updating when those apps change.

## Build

```bash
gradle assembleRelease
```

Requires JDK 17 and the Android SDK (compileSdk 35). GitHub Actions builds every push and publishes the APK for `v*` tags.
