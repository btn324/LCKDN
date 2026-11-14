using System.Drawing;
using System.Windows.Forms;

namespace Lckdn;

/// <summary>Choose what to block. While locked, protections can't be loosened.</summary>
public sealed class SettingsForm : Form
{
    private readonly AppState _state;
    private readonly bool _locked;

    public SettingsForm(AppState state)
    {
        _state = state;
        _locked = state.Locked;

        Text = "LCKDN — Protection settings";
        BackColor = Theme.Bg;
        ForeColor = Theme.Text;
        Font = new Font(Theme.Font, 10f);
        StartPosition = FormStartPosition.CenterParent;
        Size = new Size(560, 520);
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;

        var title = new Label
        {
            Text = "Protection", Font = new Font(Theme.Font, 20f, FontStyle.Bold),
            ForeColor = Theme.Text, AutoSize = true, Location = new Point(24, 20)
        };
        Controls.Add(title);

        var flow = new FlowLayoutPanel
        {
            Location = new Point(24, 70), Size = new Size(500, 380),
            FlowDirection = FlowDirection.TopDown, WrapContents = false, AutoScroll = true
        };
        Controls.Add(flow);

        flow.Controls.Add(Toggle("Adult content", "Porn and explicit sites, plus adult ad networks. Always on.",
            _state.Flags.BlockAdult, v => _state.Flags.BlockAdult = v, alwaysOn: true));
        flow.Controls.Add(Toggle("SafeSearch & YouTube Restricted",
            "Forces SafeSearch on Google, Bing and DuckDuckGo, and Restricted Mode on YouTube — this also filters images.",
            _state.Flags.SafeSearch, v => _state.Flags.SafeSearch = v));
        flow.Controls.Add(Toggle("Ads & pop-ups", "Blocks ad, pop-up and redirect networks.",
            _state.Flags.BlockAds, v => _state.Flags.BlockAds = v));
        flow.Controls.Add(Toggle("TikTok & short feeds", "Blocks TikTok at the network level.",
            _state.Flags.BlockShortFeeds, v => _state.Flags.BlockShortFeeds = v));
        flow.Controls.Add(Toggle("Block Reddit, X & Telegram (web)", "Blocks these sites in every browser.",
            _state.Flags.BlockSocial, v => _state.Flags.BlockSocial = v));

        var ok = new Button
        {
            Text = "Done", Size = new Size(500, 44), Location = new Point(24, 456),
            FlatStyle = FlatStyle.Flat, BackColor = Theme.Text, ForeColor = Theme.Bg,
            Font = new Font(Theme.Font, 11f, FontStyle.Bold), Cursor = Cursors.Hand
        };
        ok.FlatAppearance.BorderSize = 0;
        ok.Click += (_, _) =>
        {
            _state.Save();
            if (_state.Locked) Blocker.Apply(_state); // re-apply with any (tightened) changes
            Close();
        };
        Controls.Add(ok);
    }

    private Panel Toggle(string title, string desc, bool value, Action<bool> set, bool alwaysOn = false)
    {
        var p = new Panel { Size = new Size(476, 76), BackColor = Theme.Surface, Margin = new Padding(0, 0, 0, 10) };
        var t = new Label { Text = title, ForeColor = Theme.Text, Font = new Font(Theme.Font, 11f, FontStyle.Bold), AutoSize = true, Location = new Point(14, 10) };
        var d = new Label { Text = desc, ForeColor = Theme.TextDim, AutoSize = false, Size = new Size(380, 36), Location = new Point(14, 34), Font = new Font(Theme.Font, 8.5f) };
        var cb = new CheckBox { Checked = value || alwaysOn, Location = new Point(430, 26), AutoSize = true };
        cb.FlatStyle = FlatStyle.Flat;

        if (alwaysOn)
        {
            cb.Enabled = false;
        }
        else
        {
            cb.CheckedChanged += (_, _) =>
            {
                // While locked, you can tighten (turn more on) but not loosen (turn off).
                if (_locked && !cb.Checked)
                {
                    cb.Checked = true;
                    MessageBox.Show(this, "You can't loosen protection while you're locked in.", "LCKDN",
                        MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }
                set(cb.Checked);
            };
        }

        p.Controls.Add(t);
        p.Controls.Add(d);
        p.Controls.Add(cb);
        return p;
    }
}
