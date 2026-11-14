using System.Diagnostics;
using System.Text;
using Microsoft.Win32;

namespace Lckdn;

/// <summary>
/// Applies and removes the block. It only edits the Windows hosts file (a marked section) and,
/// optionally, the DNS servers on active adapters — both standard, fully reversible changes.
/// It does NOT run a service, hide itself, kill processes, or resist removal.
/// </summary>
public static class Blocker
{
    private const string Begin = "# === LCKDN BLOCKLIST BEGIN (managed by LCKDN — do not edit) ===";
    private const string End = "# === LCKDN BLOCKLIST END ===";

    private static string HostsPath =>
        Path.Combine(Environment.SystemDirectory, "drivers", "etc", "hosts");

    public static bool IsApplied()
    {
        try { return File.ReadAllText(HostsPath).Contains(Begin); }
        catch { return false; }
    }

    /// <summary>
    /// Writes the block into the hosts file. It deliberately does NOT change your system DNS —
    /// doing so made game/CDN downloads (Epic, Steam) pick a far-away server and crawl. All
    /// blocking and SafeSearch is done through the hosts file, which leaves downloads untouched.
    /// </summary>
    public static void Apply(AppState state)
    {
        WriteHosts(state.Flags);

        // Without this, Chrome/Edge/Firefox use their own encrypted DNS (DoH) and ignore the
        // hosts file entirely — so nothing would be blocked in the browser.
        DisableBrowserDoh();

        // If an older version switched DNS to a family resolver, put it back now.
        RestoreDns(state);
        FlushDns();

        if (!IsApplied())
            throw new InvalidOperationException(
                "The hosts file could not be updated. Make sure LCKDN is running as administrator.");
    }

    /// <summary>Removes everything LCKDN added.</summary>
    public static void Remove(AppState state)
    {
        RemoveHostsBlock();
        RestoreBrowserDoh();
        RestoreDns(state);
        FlushDns();
    }

    /// <summary>Heals machines where an older LCKDN left a family DNS set. Safe to call anytime.</summary>
    public static void RestoreOriginalDns(AppState state)
    {
        if (state.SavedDns.Count == 0) return;
        RestoreDns(state);
        FlushDns();
    }

    private static void WriteHosts(Flags f)
    {
        var lines = ReadHostsWithoutBlock();

        var block = new StringBuilder();
        block.AppendLine(Begin);
        block.AppendLine("# Blocks adult sites and ads, and forces SafeSearch. Remove by unlocking LCKDN.");

        foreach (var d in Blocklists.ZeroHosts(f).Distinct(StringComparer.OrdinalIgnoreCase))
        {
            block.AppendLine($"0.0.0.0 {d}");
            block.AppendLine($"0.0.0.0 www.{d}");
        }

        if (f.SafeSearch)
        {
            block.AppendLine("# SafeSearch / YouTube Restricted Mode");
            foreach (var (ip, hosts) in Blocklists.SafeSearch)
                foreach (var h in hosts)
                    block.AppendLine($"{ip} {h}");
        }

        block.AppendLine(End);

        var final = new StringBuilder();
        foreach (var l in lines) final.AppendLine(l);
        if (lines.Count > 0 && lines[^1].Length > 0) final.AppendLine();
        final.Append(block);

        WriteHostsFile(final.ToString());
    }

    private static void RemoveHostsBlock() => WriteHostsFile(string.Join(Environment.NewLine, ReadHostsWithoutBlock()) + Environment.NewLine);

    private static List<string> ReadHostsWithoutBlock()
    {
        var kept = new List<string>();
        if (!File.Exists(HostsPath)) return kept;
        bool inBlock = false;
        foreach (var raw in File.ReadAllLines(HostsPath))
        {
            var line = raw.TrimEnd();
            if (line == Begin) { inBlock = true; continue; }
            if (line == End) { inBlock = false; continue; }
            if (!inBlock) kept.Add(raw);
        }
        // Trim trailing blank lines.
        while (kept.Count > 0 && kept[^1].Trim().Length == 0) kept.RemoveAt(kept.Count - 1);
        return kept;
    }

    private static void WriteHostsFile(string content)
    {
        // Clear read-only / system attributes a previous tool may have set, write, then move on.
        try
        {
            var attr = File.GetAttributes(HostsPath);
            if (attr.HasFlag(FileAttributes.ReadOnly))
                File.SetAttributes(HostsPath, attr & ~FileAttributes.ReadOnly);
        }
        catch { /* best effort */ }

        File.WriteAllText(HostsPath, content, new UTF8Encoding(false));
    }

    // ---- Browser DoH policies -----------------------------------------------------------------

    // Chromium-family: SOFTWARE\Policies\<vendor>\<browser>\DnsOverHttpsMode = "off"
    private static readonly string[] ChromiumPolicyKeys =
    {
        @"SOFTWARE\Policies\Google\Chrome",
        @"SOFTWARE\Policies\Microsoft\Edge",
        @"SOFTWARE\Policies\BraveSoftware\Brave",
        @"SOFTWARE\Policies\Chromium\Chromium"
    };

    private const string FirefoxPolicyKey = @"SOFTWARE\Policies\Mozilla\Firefox\DNSOverHTTPS";

    private static void DisableBrowserDoh()
    {
        try
        {
            foreach (var key in ChromiumPolicyKeys)
                using (var k = Registry.LocalMachine.CreateSubKey(key))
                    k?.SetValue("DnsOverHttpsMode", "off", RegistryValueKind.String);

            using var ff = Registry.LocalMachine.CreateSubKey(FirefoxPolicyKey);
            ff?.SetValue("Enabled", 0, RegistryValueKind.DWord);
            ff?.SetValue("Locked", 1, RegistryValueKind.DWord);
        }
        catch { /* best effort; hosts still blocks apps that use the system resolver */ }
    }

    private static void RestoreBrowserDoh()
    {
        try
        {
            foreach (var key in ChromiumPolicyKeys)
                using (var k = Registry.LocalMachine.OpenSubKey(key, writable: true))
                    if (k?.GetValue("DnsOverHttpsMode") != null) k.DeleteValue("DnsOverHttpsMode", false);

            using var ff = Registry.LocalMachine.OpenSubKey(FirefoxPolicyKey, writable: true);
            if (ff != null)
            {
                if (ff.GetValue("Enabled") != null) ff.DeleteValue("Enabled", false);
                if (ff.GetValue("Locked") != null) ff.DeleteValue("Locked", false);
            }
        }
        catch { /* best effort */ }
    }

    // ---- DNS ----------------------------------------------------------------------------------

    private static void RestoreDns(AppState state)
    {
        foreach (var (alias, servers) in state.SavedDns)
        {
            if (servers.Length == 0)
            {
                RunNetsh($"interface ipv4 set dnsservers name=\"{alias}\" dhcp");
            }
            else
            {
                RunNetsh($"interface ipv4 set dnsservers name=\"{alias}\" static {servers[0]} primary");
                for (int i = 1; i < servers.Length; i++)
                    RunNetsh($"interface ipv4 add dnsservers name=\"{alias}\" {servers[i]} index={i + 1}");
            }
        }
        state.SavedDns.Clear();
        state.Save();
    }

    private static void RunNetsh(string args) => RunHidden("netsh.exe", args);

    private static void FlushDns() => RunHidden("ipconfig.exe", "/flushdns");

    private static void RunHidden(string exe, string args)
    {
        try
        {
            var psi = new ProcessStartInfo(Path.Combine(Environment.SystemDirectory, exe), args)
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            using var p = Process.Start(psi);
            p?.WaitForExit(15000);
        }
        catch { /* best effort; hosts file is the primary layer */ }
    }
}
