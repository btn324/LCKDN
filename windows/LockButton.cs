using System.Drawing;
using System.Drawing.Drawing2D;
using System.Windows.Forms;

namespace Lckdn;

/// <summary>The big round lock button. Draws a padlock that closes and glows green when locked.</summary>
public sealed class LockButton : Control
{
    private bool _locked;
    private float _frac;       // 0 = open, 1 = closed
    private float _pulse;      // 0..1 breathing
    private bool _pressed;
    private readonly System.Windows.Forms.Timer _anim;

    public event EventHandler? Clicked;

    public LockButton()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                 ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                 ControlStyles.SupportsTransparentBackColor, true);
        Size = new Size(260, 260);
        Cursor = Cursors.Hand;
        _anim = new System.Windows.Forms.Timer { Interval = 33 };
        _anim.Tick += (_, _) => Animate();
        _anim.Start();
    }

    public void SetLocked(bool locked)
    {
        _locked = locked;
        Invalidate();
    }

    private void Animate()
    {
        float target = _locked ? 1f : 0f;
        _frac += (target - _frac) * 0.18f;
        if (Math.Abs(target - _frac) < 0.002f) _frac = target;
        if (_locked)
        {
            _pulse += 0.02f;
            if (_pulse > 1f) _pulse -= 1f;
        }
        Invalidate();
    }

    protected override void OnMouseDown(MouseEventArgs e) { _pressed = true; Invalidate(); base.OnMouseDown(e); }

    protected override void OnMouseUp(MouseEventArgs e)
    {
        bool was = _pressed;
        _pressed = false;
        Invalidate();
        if (was && ClientRectangle.Contains(e.Location)) Clicked?.Invoke(this, EventArgs.Empty);
        base.OnMouseUp(e);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.Clear(BackColor);

        float size = Math.Min(Width, Height);
        float cx = Width / 2f, cy = Height / 2f;
        float r = size / 2f - 8f;
        if (_pressed) r *= 0.96f;
        float f = _frac;

        Color iconColor = Blend(Theme.Text, Theme.Accent, f);

        // breathing ring
        if (f > 0f)
        {
            float rr = r * (0.9f + 0.08f * _pulse);
            int alpha = (int)(70 * f * (1f - _pulse));
            using var rp = new Pen(Color.FromArgb(Math.Clamp(alpha, 0, 255), Theme.Accent), 2f);
            g.DrawEllipse(rp, cx - rr, cy - rr, rr * 2, rr * 2);
        }

        // track ring
        float ringR = r * 0.86f;
        using (var track = new Pen(Theme.Stroke, 2.5f))
            g.DrawEllipse(track, cx - ringR, cy - ringR, ringR * 2, ringR * 2);
        if (f > 0f)
        {
            using var arc = new Pen(Theme.Accent, 3.5f) { StartCap = LineCap.Round, EndCap = LineCap.Round };
            g.DrawArc(arc, cx - ringR, cy - ringR, ringR * 2, ringR * 2, -90, 360 * f);
        }

        // disc
        using (var disc = new SolidBrush(Theme.Surface2))
            g.FillEllipse(disc, cx - r * 0.78f, cy - r * 0.78f, r * 1.56f, r * 1.56f);

        // padlock body
        float bodyW = r * 0.42f, bodyH = r * 0.32f;
        float bodyTop = cy - r * 0.08f;
        var body = new RectangleF(cx - bodyW / 2, bodyTop, bodyW, bodyH);
        using (var bodyBrush = new SolidBrush(iconColor))
            FillRound(g, bodyBrush, body, r * 0.06f);

        // shackle
        float shW = bodyW * 0.62f;
        float lift = (1f - f) * r * 0.10f;
        float shTop = bodyTop - r * 0.22f - lift;
        using (var sh = new Pen(iconColor, r * 0.06f) { StartCap = LineCap.Round, EndCap = LineCap.Round })
        {
            using var path = new GraphicsPath();
            path.AddLine(cx - shW / 2, bodyTop + 1, cx - shW / 2, shTop + shW / 2);
            path.AddArc(cx - shW / 2, shTop, shW, shW, 180, 180);
            path.AddLine(cx + shW / 2, shTop + shW / 2, cx + shW / 2, bodyTop - lift * 1.2f + 1);
            g.DrawPath(sh, path);
        }

        // keyhole
        using (var hole = new SolidBrush(Theme.Bg))
        {
            float hy = bodyTop + bodyH * 0.40f;
            g.FillEllipse(hole, cx - r * 0.035f, hy - r * 0.035f, r * 0.07f, r * 0.07f);
            g.FillRectangle(hole, cx - r * 0.012f, hy, r * 0.024f, bodyH * 0.32f);
        }

        // label
        string label = _locked ? "LOCKED" : "LOCK IN";
        using var font = new Font(Theme.Font, size * 0.052f, FontStyle.Bold);
        var ts = g.MeasureString(label, font);
        using var lb = new SolidBrush(Blend(Theme.TextDim, Theme.Accent, f));
        g.DrawString(label, font, lb, cx - ts.Width / 2, cy + r * 0.42f);
    }

    private static void FillRound(Graphics g, Brush b, RectangleF rect, float radius)
    {
        using var path = new GraphicsPath();
        float d = radius * 2;
        path.AddArc(rect.Left, rect.Top, d, d, 180, 90);
        path.AddArc(rect.Right - d, rect.Top, d, d, 270, 90);
        path.AddArc(rect.Right - d, rect.Bottom - d, d, d, 0, 90);
        path.AddArc(rect.Left, rect.Bottom - d, d, d, 90, 90);
        path.CloseFigure();
        g.FillPath(b, path);
    }

    private static Color Blend(Color a, Color b, float t)
    {
        t = Math.Clamp(t, 0f, 1f);
        return Color.FromArgb(
            (int)(a.R + (b.R - a.R) * t),
            (int)(a.G + (b.G - a.G) * t),
            (int)(a.B + (b.B - a.B) * t));
    }
}
