using System.Text.Json;

namespace Lckdn;

/// <summary>What the user chose to block. All on by default except where noted.</summary>
public sealed class Flags
{
    public bool BlockAdult { get; set; } = true;   // always on in practice
    public bool BlockAds { get; set; } = true;
    public bool SafeSearch { get; set; } = true;   // Google/Bing/DuckDuckGo + YouTube Restricted
    public bool BlockShortFeeds { get; set; } = true; // youtube shorts / instagram reels / tiktok hosts
    public bool BlockSocial { get; set; } = false;  // reddit / x / telegram web, off by default
    // Kept for back-compat with old saved state; no longer used (switching DNS broke game downloads).
    public bool FamilyDns { get; set; } = false;
}

/// <summary>Persistent app state, stored in ProgramData so it survives restarts.</summary>
public sealed class AppState
{
    public bool Locked { get; set; }
    public DateTime LockedSinceUtc { get; set; }
    public Flags Flags { get; set; } = new();

    // Unlock challenge progress
    public DateTime? CooldownStartUtc { get; set; }

    // DNS we replaced, so we can restore it exactly on unlock. Key: interface alias.
    public Dictionary<string, string[]> SavedDns { get; set; } = new();

    private static readonly JsonSerializerOptions JsonOpts = new() { WriteIndented = true };

    public static string Dir =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "LCKDN");

    public static string FilePath => Path.Combine(Dir, "state.json");

    public static AppState Load()
    {
        try
        {
            if (File.Exists(FilePath))
                return JsonSerializer.Deserialize<AppState>(File.ReadAllText(FilePath)) ?? new AppState();
        }
        catch
        {
            // Corrupt or unreadable: start clean rather than crash.
        }
        return new AppState();
    }

    public void Save()
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllText(FilePath, JsonSerializer.Serialize(this, JsonOpts));
    }
}
