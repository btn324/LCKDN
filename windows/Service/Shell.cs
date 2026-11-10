using System;
using System.Diagnostics;
using System.IO;

namespace Lckdn.Service;

public static class Shell
{
    /** Runs a console program hidden and returns (exit code, combined output). */
    public static (int Code, string Output) Run(string exe, string args, int timeoutMs = 15000)
    {
        try
        {
            var psi = new ProcessStartInfo(exe, args)
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            using var p = Process.Start(psi);
            if (p == null) return (-1, "");
            var stdout = p.StandardOutput.ReadToEndAsync();
            var stderr = p.StandardError.ReadToEndAsync();
            if (!p.WaitForExit(timeoutMs))
            {
                try { p.Kill(true); } catch { }
                return (-2, "timeout");
            }
            return (p.ExitCode, stdout.Result + stderr.Result);
        }
        catch (Exception e)
        {
            return (-1, e.Message);
        }
    }

    public static string System32(string exe) => Path.Combine(Environment.SystemDirectory, exe);
}

public static class Log
{
    private static readonly object Gate = new();

    public static void Write(string message)
    {
        try
        {
            lock (Gate)
            {
                var file = new FileInfo(Paths.LogFile);
                if (file.Exists && file.Length > 512 * 1024) file.Delete();
                File.AppendAllText(Paths.LogFile, $"{DateTime.Now:yyyy-MM-dd HH:mm:ss} {message}{Environment.NewLine}");
            }
        }
        catch
        {
        }
    }
}
