using System;
using System.IO;

namespace Lckdn;

public static class Paths
{
    public const string ServiceName = "LCKDN";
    public const string PipeName = "LCKDN.Control";

    public static string InstallDir =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "LCKDN");

    public static string InstalledExe => Path.Combine(InstallDir, "LCKDN.exe");

    public static string DataDir =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "LCKDN");

    public static string StateFile => Path.Combine(DataDir, "state.json");
    public static string LogFile => Path.Combine(DataDir, "service.log");

    public static string HostsFile =>
        Path.Combine(Environment.SystemDirectory, "drivers", "etc", "hosts");

    public static string WarpCli =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles),
            "Cloudflare", "Cloudflare WARP", "warp-cli.exe");
}
