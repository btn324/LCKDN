using System.Drawing;

namespace Lckdn;

/// <summary>The LCKDN dark palette, matching the Android app.</summary>
public static class Theme
{
    public static readonly Color Bg = ColorTranslator.FromHtml("#09090B");
    public static readonly Color Surface = ColorTranslator.FromHtml("#131316");
    public static readonly Color Surface2 = ColorTranslator.FromHtml("#1B1B20");
    public static readonly Color Stroke = ColorTranslator.FromHtml("#26262D");
    public static readonly Color Text = ColorTranslator.FromHtml("#F4F4F5");
    public static readonly Color TextDim = ColorTranslator.FromHtml("#8A8A94");
    public static readonly Color TextFaint = ColorTranslator.FromHtml("#55555E");
    public static readonly Color Accent = ColorTranslator.FromHtml("#34D399");
    public static readonly Color Danger = ColorTranslator.FromHtml("#F87171");

    public static readonly string Font = "Segoe UI";
}

public static class Quotes
{
    public static readonly (string Text, string Author)[] All =
    {
        ("We suffer more often in imagination than in reality.", "Seneca"),
        ("No man is free who is not master of himself.", "Epictetus"),
        ("You have power over your mind, not outside events.", "Marcus Aurelius"),
        ("What stands in the way becomes the way.", "Marcus Aurelius"),
        ("We are what we repeatedly do. Excellence is a habit.", "Will Durant"),
        ("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        ("The secret of getting ahead is getting started.", "Mark Twain"),
        ("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        ("The urge will pass. The regret won't. Let it pass.", "LCKDN"),
        ("Discipline is choosing what you want most over what you want now.", "LCKDN"),
        ("Close the tab. Go build the life you keep scrolling past.", "LCKDN"),
        ("You didn't come this far to only come this far.", "LCKDN"),
        ("Ten minutes of discomfort beats ten days of regret.", "LCKDN"),
        ("Focus is a muscle. Every rep counts.", "LCKDN"),
        ("Stay locked in.", "LCKDN")
    };

    public static (string Text, string Author) Random() => All[System.Random.Shared.Next(All.Length)];
}
