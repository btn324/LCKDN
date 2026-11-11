using System.Windows.Forms;

namespace Lckdn;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        // Single instance.
        using var mutex = new Mutex(true, @"Local\LCKDN.Windows", out bool first);
        if (!first) return;

        ApplicationConfiguration.Initialize();
        Application.SetHighDpiMode(HighDpiMode.PerMonitorV2);
        Application.Run(new MainForm());
    }
}
