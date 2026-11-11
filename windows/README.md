# LCKDN for Windows

A desktop companion to the LCKDN Android app. Same idea — one button, locked in — built in **C# / WinForms / .NET 10**.

## What it does

When you lock in, LCKDN edits the Windows **hosts file** (a marked, clearly-labelled section) and optionally switches your **DNS** to Cloudflare for Families. That blocks, across every browser and app on the PC:

- **Adult sites** and adult/intrusive ad & pop-up networks
- **Ads & pop-ups**
- **SafeSearch** forced on Google, Bing and DuckDuckGo, and **YouTube Restricted Mode** — this is what filters images, not just sites
- **TikTok** (optional), and **Reddit / X / Telegram** websites (optional)

Turning it off uses the same friction as the phone: type a **480-character code** with no mistakes (pasting is blocked), wait **60 minutes**, then confirm within 10.

## What it deliberately does NOT do

This app is a **reversible** blocker, not a lockdown enforcer. It does **not**:

- run a hidden background service, or hide itself
- block Task Manager or system settings
- kill other programs
- make itself undeletable or resist removal

Those behaviours are indistinguishable from malware/stalkerware, so LCKDN for Windows doesn't do them. An administrator can always undo everything — the unlock challenge is friction to help you pause, not a trap. If you ever need to remove the block manually, open `C:\Windows\System32\drivers\etc\hosts` as administrator and delete the section between `# === LCKDN BLOCKLIST BEGIN ===` and `# === LCKDN BLOCKLIST END ===`, then run `ipconfig /flushdns`.

## Install & run

1. Download `LCKDN.exe` from the release and run it. It asks for **administrator** rights (needed to edit the hosts file and DNS) — approve the UAC prompt.
2. Click the big button to lock in. Use the ⚙ settings to choose what to block.

No .NET install needed — the exe is self-contained.

## Build from source

```bash
cd windows
dotnet publish -c Release -r win-x64 -o publish
```

Requires the .NET 10 SDK. Output: `publish/LCKDN.exe`.

## Note on SafeSearch IPs

SafeSearch/Restricted Mode is enforced by pointing Google/Bing/YouTube at their published "forced-safe" addresses in the hosts file. Those IPs are stable but provider-controlled; if a provider moves one, update `Blocklists.cs`.
