using System;
using System.Linq;
using System.Threading;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Lckdn.Service;
using Lckdn.Ui;

namespace Lckdn;

public static class Program
{
    [STAThread]
    public static int Main(string[] args)
    {
        if (args.Contains("--service"))
        {
            var builder = Host.CreateApplicationBuilder(args);
            builder.Services.AddWindowsService(o => o.ServiceName = Paths.ServiceName);
            builder.Services.AddHostedService<LockService>();
            builder.Build().Run();
            return 0;
        }
        if (args.Contains("--install")) return Installer.Install();
        if (args.Contains("--uninstall")) return Installer.Uninstall();

        // One window per user session; a second launch just brings the first to the front.
        using var mutex = new Mutex(true, @"Local\LCKDN.UI", out bool first);
        using var showSignal = new EventWaitHandle(false, EventResetMode.AutoReset, @"Local\LCKDN.UI.Show");
        if (!first)
        {
            showSignal.Set();
            return 0;
        }

        var app = new LckdnApp();
        var window = new MainWindow(startInTray: args.Contains("--tray"), showSignal);
        return app.Run(window);
    }
}
