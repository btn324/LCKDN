using System.Drawing;
using System.Security.Cryptography;
using System.Text;
using System.Windows.Forms;

namespace Lckdn;

/// <summary>
/// The unlock challenge: type 480 random characters with no mistakes (pasting is blocked),
/// then wait a 60-minute cooldown, then confirm. Friction by design — but always possible,
/// because this app is meant to help, not to trap.
/// </summary>
public sealed class UnlockForm : Form
{
    private const int ChallengeLen = 480;
    private static readonly TimeSpan Cooldown = TimeSpan.FromMinutes(60);
    private static readonly TimeSpan Window = TimeSpan.FromMinutes(10);
    private const string Alphabet = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private readonly AppState _state;
    public bool Unlocked { get; private set; }

    private readonly Label _title = new();
    private readonly Label _info = new();
    private readonly Panel _stage = new();
    private readonly System.Windows.Forms.Timer _timer = new() { Interval = 1000 };

    private string _challenge = "";

    public UnlockForm(AppState state)
    {
        _state = state;
        Text = "LCKDN — Unlock";
        BackColor = Theme.Bg;
        ForeColor = Theme.Text;
        Font = new Font(Theme.Font, 10f);
        StartPosition = FormStartPosition.CenterParent;
        Size = new Size(620, 640);
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;

        _title.Text = "Unlock";
        _title.Font = new Font(Theme.Font, 22f, FontStyle.Bold);
        _title.ForeColor = Theme.Text;
        _title.AutoSize = true;
        _title.Location = new Point(28, 24);
        Controls.Add(_title);

        _info.ForeColor = Theme.TextDim;
        _info.AutoSize = false;
        _info.Size = new Size(560, 48);
        _info.Location = new Point(30, 72);
        Controls.Add(_info);

        _stage.BackColor = Theme.Bg;
        _stage.Location = new Point(24, 120);
        _stage.Size = new Size(568, 480);
        Controls.Add(_stage);

        _timer.Tick += (_, _) => UpdateStage();
        _timer.Start();

        ShowStage();
    }

    private void UpdateStage()
    {
        if (_state.CooldownStartUtc.HasValue)
        {
            var elapsed = DateTime.UtcNow - _state.CooldownStartUtc.Value;
            if (elapsed < Cooldown) ShowCooldown();
            else if (elapsed < Cooldown + Window) ShowReady();
            else { _state.CooldownStartUtc = null; _state.Save(); ShowIntro("Your unlock window expired, so protection stayed on. That's a win."); }
        }
    }

    private void ShowStage()
    {
        if (_state.CooldownStartUtc.HasValue) UpdateStage();
        else ShowIntro(null);
    }

    private void Clear() => _stage.Controls.Clear();

    private void ShowIntro(string? note)
    {
        Clear();
        _info.Text = "Turning off LCKDN takes real effort. That is the point.";
        int y = 0;
        if (note != null)
        {
            var n = Card(note, Theme.Accent);
            n.Location = new Point(0, y);
            _stage.Controls.Add(n);
            y += n.Height + 16;
        }

        var steps = Card(
            "1 · Type a 480-character code by hand. One mistake and you start over; pasting is blocked.\n\n" +
            "2 · Wait 60 minutes.\n\n" +
            "3 · Confirm within 10 minutes, or protection stays on.", Theme.TextDim);
        steps.Location = new Point(0, y);
        _stage.Controls.Add(steps);
        y += steps.Height + 20;

        var stay = Primary("Stay locked in");
        stay.Location = new Point(0, y);
        stay.Click += (_, _) => Close();
        _stage.Controls.Add(stay);

        var begin = new LinkLabel
        {
            Text = "Start unlock challenge",
            LinkColor = Theme.TextDim,
            ActiveLinkColor = Theme.Text,
            AutoSize = true,
            Location = new Point(2, y + 56)
        };
        begin.Click += (_, _) => ShowTyping();
        _stage.Controls.Add(begin);
    }

    private void ShowTyping()
    {
        Clear();
        _info.Text = "Type the code exactly, including spaces.";
        _challenge = Generate();

        var challengeBox = new TextBox
        {
            Multiline = true, ReadOnly = true, BorderStyle = BorderStyle.None,
            BackColor = Theme.Surface, ForeColor = Theme.TextDim,
            Font = new Font("Consolas", 10.5f), Text = _challenge,
            Location = new Point(0, 0), Size = new Size(568, 150),
            ScrollBars = ScrollBars.Vertical, TabStop = false, ShortcutsEnabled = false
        };
        _stage.Controls.Add(challengeBox);

        var input = new TextBox
        {
            Multiline = true, BorderStyle = BorderStyle.FixedSingle,
            BackColor = Theme.Surface2, ForeColor = Theme.Text,
            Font = new Font("Consolas", 10.5f),
            Location = new Point(0, 162), Size = new Size(568, 120),
            ShortcutsEnabled = false  // blocks Ctrl+V paste
        };
        var progress = new Label { ForeColor = Theme.TextFaint, AutoSize = true, Location = new Point(0, 292) };
        var msg = new Label { ForeColor = Theme.Danger, AutoSize = true, Location = new Point(0, 316) };

        input.TextChanged += (_, _) =>
        {
            var typed = input.Text;
            if (!_challenge.StartsWith(typed, StringComparison.Ordinal))
            {
                input.Text = "";
                msg.Text = "Mistake — start over.";
                return;
            }
            msg.Text = "";
            progress.Text = $"{typed.Length} / {_challenge.Length}";
            if (typed.Length == _challenge.Length)
            {
                _state.CooldownStartUtc = DateTime.UtcNow;
                _state.Save();
                ShowCooldown();
            }
        };
        // Block paste explicitly.
        input.KeyDown += (_, ev) => { if (ev.Control && ev.KeyCode == Keys.V) ev.SuppressKeyPress = true; };

        _stage.Controls.Add(progress);
        _stage.Controls.Add(msg);
        _stage.Controls.Add(input);
        input.Focus();
    }

    private void ShowCooldown()
    {
        if (_stage.Controls.Count == 1 && _stage.Controls[0] is Label l0 && l0.Name == "cooldown") { /* already */ }
        Clear();
        _info.Text = "Cooldown in progress. Protection stays on until it ends.";
        var remaining = Cooldown - (DateTime.UtcNow - (_state.CooldownStartUtc ?? DateTime.UtcNow));
        if (remaining < TimeSpan.Zero) remaining = TimeSpan.Zero;

        var timer = new Label
        {
            Name = "cooldown",
            Text = $"{(int)remaining.TotalMinutes:00}:{remaining.Seconds:00}",
            Font = new Font(Theme.Font, 48f, FontStyle.Regular),
            ForeColor = Theme.Text, AutoSize = true, Location = new Point(0, 20)
        };
        _stage.Controls.Add(timer);

        var tips = Card("While you wait: 20 push-ups, a glass of water, a ten-minute walk, message someone you care about. If you still want to unlock in an hour, you can.", Theme.TextDim);
        tips.Location = new Point(0, 120);
        _stage.Controls.Add(tips);

        var cancel = Primary("Cancel and stay locked in");
        cancel.Location = new Point(0, 260);
        cancel.Click += (_, _) => { _state.CooldownStartUtc = null; _state.Save(); Close(); };
        _stage.Controls.Add(cancel);
    }

    private void ShowReady()
    {
        Clear();
        _info.Text = "You made it through the cooldown.";
        var remaining = (Cooldown + Window) - (DateTime.UtcNow - (_state.CooldownStartUtc ?? DateTime.UtcNow));
        var note = new Label
        {
            Text = $"This option disappears in {(int)remaining.TotalMinutes:00}:{remaining.Seconds:00}.",
            ForeColor = Theme.TextFaint, AutoSize = true, Location = new Point(0, 8)
        };
        _stage.Controls.Add(note);

        var keep = Primary("Keep me locked in");
        keep.Location = new Point(0, 48);
        keep.Click += (_, _) => { _state.CooldownStartUtc = null; _state.Save(); Close(); };
        _stage.Controls.Add(keep);

        var off = new LinkLabel
        {
            Text = "Turn off protection", LinkColor = Theme.Danger, ActiveLinkColor = Theme.Danger,
            AutoSize = true, Location = new Point(2, 112)
        };
        off.Click += (_, _) =>
        {
            if (MessageBox.Show(this, "All blocking stops immediately. You can lock in again any time.",
                    "Turn off LCKDN?", MessageBoxButtons.OKCancel, MessageBoxIcon.Warning) == DialogResult.OK)
            {
                Unlocked = true;
                _state.CooldownStartUtc = null;
                Close();
            }
        };
        _stage.Controls.Add(off);
    }

    private static string Generate()
    {
        var sb = new StringBuilder(ChallengeLen + ChallengeLen / 5);
        var bytes = RandomNumberGenerator.GetBytes(ChallengeLen);
        for (int i = 0; i < ChallengeLen; i++)
        {
            if (i > 0 && i % 5 == 0) sb.Append(' ');
            sb.Append(Alphabet[bytes[i] % Alphabet.Length]);
        }
        return sb.ToString();
    }

    private static Panel Card(string text, Color color)
    {
        var p = new Panel { BackColor = Theme.Surface, Size = new Size(568, 0), Padding = new Padding(16) };
        var l = new Label { Text = text, ForeColor = color, AutoSize = false, Dock = DockStyle.Fill };
        using (var g = l.CreateGraphics())
        {
            var sz = g.MeasureString(text, new Font(Theme.Font, 10f), 536);
            p.Height = (int)sz.Height + 36;
        }
        p.Controls.Add(l);
        return p;
    }

    private static Button Primary(string text)
    {
        var b = new Button
        {
            Text = text, Size = new Size(568, 48), FlatStyle = FlatStyle.Flat,
            BackColor = Theme.Text, ForeColor = Theme.Bg, Font = new Font(Theme.Font, 11f, FontStyle.Bold),
            Cursor = Cursors.Hand
        };
        b.FlatAppearance.BorderSize = 0;
        return b;
    }

    protected override void OnFormClosed(FormClosedEventArgs e)
    {
        _timer.Stop();
        base.OnFormClosed(e);
    }
}
