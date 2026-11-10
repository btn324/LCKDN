using System;
using System.Collections.Generic;
using System.Text.Json;

namespace Lckdn;

/** What the user chose to block. */
public sealed class Flags
{
    public bool BlockAds { get; set; } = true;
    public bool BlockShorts { get; set; } = true;
    public bool BlockReels { get; set; } = true;
    public bool BlockTikTok { get; set; } = false;
    public bool HideIp { get; set; } = true;
    public bool Quotes { get; set; } = true;

    public Flags Copy() => (Flags)MemberwiseClone();
}

/** Persistent service state. Only the service (SYSTEM) can write it. */
public sealed class LockState
{
    public bool Locked { get; set; }
    public DateTime LockedSinceUtc { get; set; }
    public Flags Flags { get; set; } = new();

    public string? Challenge { get; set; }
    public long CooldownStartTick { get; set; } = -1;
    public DateTime CooldownBootUtc { get; set; }

    /** Original DNS per adapter, keyed "v4:{guid}" / "v6:{guid}" → "name|servers" ("" servers = DHCP). */
    public Dictionary<string, string> SavedDns { get; set; } = new();
}

public sealed class Request
{
    public string Cmd { get; set; } = "status";
    public Flags? Flags { get; set; }
    public string? Text { get; set; }
}

public sealed class Status
{
    public bool Locked { get; set; }
    public DateTime LockedSinceUtc { get; set; }
    public Flags Flags { get; set; } = new();
    /** none | cooldown | ready */
    public string UnlockStage { get; set; } = "none";
    public long CooldownRemainingSec { get; set; }
    public long WindowRemainingSec { get; set; }
    public bool WarpInstalled { get; set; }
    public bool WarpConnected { get; set; }
    public bool Healthy { get; set; } = true;
    public string? Note { get; set; }
}

public sealed class Response
{
    public bool Ok { get; set; }
    public string? Error { get; set; }
    public Status? Status { get; set; }
    public string? Challenge { get; set; }

    public static Response Fail(string error) => new() { Ok = false, Error = error };
}

public static class Json
{
    public static readonly JsonSerializerOptions Options = new()
    {
        PropertyNameCaseInsensitive = true,
        WriteIndented = false
    };

    public static string Write<T>(T value) => JsonSerializer.Serialize(value, Options);
    public static T? Read<T>(string json) => JsonSerializer.Deserialize<T>(json, Options);
}
