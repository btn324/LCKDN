using System.Drawing;
using System.Windows.Forms;

namespace Lckdn;

public sealed class MainForm : Form
{
    private readonly AppState _state = AppState.Load();
    private readonly LockButton _button = new();
    private readonly Label _statusTitle = new();
    private readonly Label _statusSub = new();
    private readonly Label _summary = new();
    private readonly Label _footer = new();
    private readonly Label _quote = new();
    private readonly System.Windows.Forms.Timer _tick = new() { Interval = 10000 };

    public MainForm()
    {
        Text = "LCKDN";
        BackColor = Theme.Bg;
        ForeColor = Theme.Text;
        Font = new Font(Theme.Font, 10f);
        StartPosition = FormStartPosition.CenterScreen;
        MinimumSize = new Size(460, 640);
        Size = new Size(460, 700);

        // Header
        var brand = new Label
        {
            Text = "L C K D N", Font = new Font(Theme.Font, 16f, FontStyle.Bold),
            ForeColor = Theme.Text, AutoSize = true, Location = new Point(28, 20)
        };
        Controls.Add(brand);

        var settings = new Button
        {
            Text = "⚙", Font = new Font(Theme.Font, 14f), FlatStyle = FlatStyle.Flat,
            ForeColor = Theme.TextDim, BackColor = Theme.Bg, Size = new Size(40, 40),
            Cursor = Cursors.Hand
        };
        settings.FlatAppearance.BorderSize = 0;
        settings.Click += (_, _) => { using var s = new SettingsForm(_state); s.ShowDialog(this); Render(); };
        Controls.Add(settings);
        settings.Location = new Point(ClientSize.Width - 56, 20);
        settings.Anchor = AnchorStyles.Top | AnchorStyles.Right;

        // Lock button (centered)
        _button.BackColor = Theme.Bg;
        _button.Clicked += (_, _) => OnLockPressed();
        Controls.Add(_button);

        _statusTitle.Font = new Font(Theme.Font, 18f, FontStyle.Bold);
        _statusTitle.ForeColor = Theme.Text;
        _statusTitle.AutoSize = false;
        _statusTitle.TextAlign = ContentAlignment.MiddleCenter;
        Controls.Add(_statusTitle);

        _statusSub.ForeColor = Theme.TextDim;
        _statusSub.AutoSize = false;
        _statusSub.TextAlign = ContentAlignment.MiddleCenter;
        Controls.Add(_statusSub);

        _summary.ForeColor = Theme.TextFaint;
        _summary.AutoSize = false;
        _summary.TextAlign = ContentAlignment.MiddleCenter;
        _summary.Font = new Font(Theme.Font, 8.5f);
        Controls.Add(_summary);

        _quote.ForeColor = Theme.TextFaint;
        _quote.AutoSize = false;
        _quote.TextAlign = ContentAlignment.MiddleCenter;
        _quote.Font = new Font(Theme.Font, 9f, FontStyle.Italic);
        Controls.Add(_quote);

        _footer.ForeColor = Theme.TextFaint;
        _footer.AutoSize = false;
        _footer.TextAlign = ContentAlignment.MiddleCenter;
        _footer.Cursor = Cursors.Hand;
        _footer.Click += (_, _) => { if (_state.Locked) { using var u = new UnlockForm(_state); u.ShowDialog(this); if (u.Unlocked) DoUnlock(); Render(); } };
        Controls.Add(_footer);

        Resize += (_, _) => Layout2();
        Load += (_, _) => { ReconcileOnStartup(); Layout2(); Render(); };
        _tick.Tick += (_, _) => Render();
        _tick.Start();

        var q = Quotes.Random();
        _quote.Text = $"“{q.Text}”  — {q.Author}";
    }

    /// <summary>If we think we're locked but the hosts block is gone (manual edit), re-apply it.</summary>
    private void ReconcileOnStartup()
    {
        if (_state.Locked && !Blocker.IsApplied())
        {
            try { Blocker.Apply(_state); } catch { /* shown in status */ }
        }
    }

    private void Layout2()
    {
        int w = ClientSize.Width;
        int btn = 240;
        _button.Size = new Size(btn, btn);
        _button.Location = new Point((w - btn) / 2, 90);

        _statusTitle.SetBounds(0, 345, w, 30);
        _statusSub.SetBounds(0, 378, w, 24);
        _summary.SetBounds(24, 408, w - 48, 24);
        _quote.SetBounds(28, 470, w - 56, 60);
        _footer.SetBounds(0, ClientSize.Height - 54, w, 36);
        _footer.Anchor = AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
    }

    private void OnLockPressed()
    {
        if (_state.Locked)
        {
            MessageBox.Show(this, "You're locked in. Use the unlock challenge to turn it off.", "LCKDN",
                MessageBoxButtons.OK, MessageBoxIcon.Information);
            return;
        }

        var msg = "LCKDN will block adult content, ads and pop-ups, and force SafeSearch across your whole PC.\n\n" +
                  "Once you lock in, turning protection off means typing a 480-character code with no mistakes, " +
                  "then waiting 60 minutes.\n\nLock in?";
        if (MessageBox.Show(this, msg, "Lock in?", MessageBoxButtons.OKCancel, MessageBoxIcon.Question) != DialogResult.OK)
            return;

        try
        {
            _state.Locked = true;
            _state.LockedSinceUtc = DateTime.UtcNow;
            _state.CooldownStartUtc = null;
            _state.Save();
            Blocker.Apply(_state);
        }
        catch (Exception ex)
        {
            _state.Locked = false;
            _state.Save();
            MessageBox.Show(this, "Couldn't apply the block:\n" + ex.Message, "LCKDN",
                MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
        _button.SetLocked(_state.Locked);
        Render();
    }

    private void DoUnlock()
    {
        try
        {
            Blocker.Remove(_state);
            _state.Locked = false;
            _state.CooldownStartUtc = null;
            _state.Save();
        }
        catch (Exception ex)
        {
            MessageBox.Show(this, "Couldn't fully remove the block:\n" + ex.Message, "LCKDN",
                MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
        _button.SetLocked(false);
    }

    private void Render()
    {
        _button.SetLocked(_state.Locked);
        if (_state.Locked)
        {
            _statusTitle.Text = "Locked in";
            var since = DateTime.UtcNow - _state.LockedSinceUtc;
            _statusSub.Text = since.TotalHours >= 1
                ? $"for {(int)since.TotalHours}h {since.Minutes}m"
                : $"for {since.Minutes}m";
            _summary.Text = Summary();
            _footer.Text = "Request unlock";
            _footer.ForeColor = Theme.TextDim;
        }
        else
        {
            _statusTitle.Text = "Not protected";
            _statusSub.Text = "Tap the button to lock in";
            _summary.Text = "On-device filtering · Nothing you browse is collected";
            _footer.Text = Blocker.IsApplied() ? "Residual block found — open settings" : "";
            _footer.ForeColor = Theme.TextFaint;
        }
    }

    private string Summary()
    {
        var parts = new List<string> { "Adult content" };
        if (_state.Flags.SafeSearch) parts.Add("SafeSearch");
        if (_state.Flags.BlockAds) parts.Add("Ads");
        if (_state.Flags.BlockShortFeeds) parts.Add("TikTok");
        if (_state.Flags.BlockSocial) parts.Add("Reddit/X/Telegram");
        return string.Join("  ·  ", parts) + " blocked";
    }
}
