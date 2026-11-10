using System;

namespace Lckdn;

public static class Quotes
{
    public sealed record Quote(string Text, string Author);

    public static readonly Quote[] All =
    {
        new("We suffer more often in imagination than in reality.", "Seneca"),
        new("No man is free who is not master of himself.", "Epictetus"),
        new("You have power over your mind, not outside events. Realize this, and you will find strength.", "Marcus Aurelius"),
        new("The impediment to action advances action. What stands in the way becomes the way.", "Marcus Aurelius"),
        new("Waste no more time arguing what a good man should be. Be one.", "Marcus Aurelius"),
        new("First say to yourself what you would be; and then do what you have to do.", "Epictetus"),
        new("He who conquers himself is the mightiest warrior.", "Confucius"),
        new("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        new("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant"),
        new("I count him braver who overcomes his desires than him who conquers his enemies.", "Aristotle"),
        new("Luck is what happens when preparation meets opportunity.", "Seneca"),
        new("Difficulties strengthen the mind, as labor does the body.", "Seneca"),
        new("Self-control is strength. Right thought is mastery. Calmness is power.", "James Allen"),
        new("A man is but the product of his thoughts. What he thinks, he becomes.", "Mahatma Gandhi"),
        new("Well done is better than well said.", "Benjamin Franklin"),
        new("Lost time is never found again.", "Benjamin Franklin"),
        new("Energy and persistence conquer all things.", "Benjamin Franklin"),
        new("The secret of getting ahead is getting started.", "Mark Twain"),
        new("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        new("Nothing in the world is worth having or worth doing unless it means effort, pain, difficulty.", "Theodore Roosevelt"),
        new("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        new("What you do today can improve all your tomorrows.", "Ralph Marston"),
        new("The best time to plant a tree was 20 years ago. The second best time is now.", "Proverb"),
        new("Fall seven times, stand up eight.", "Japanese proverb"),
        new("A river cuts through rock not because of its power, but because of its persistence.", "Proverb"),
        new("The urge will pass. The regret won't. Let it pass.", "LCKDN"),
        new("Every time you say no to the urge, you vote for the person you want to become.", "LCKDN"),
        new("Your future self is watching. Make that person proud.", "LCKDN"),
        new("Comfort is the enemy of progress. Do the hard thing first.", "LCKDN"),
        new("Ten minutes of discomfort beats ten days of regret.", "LCKDN"),
        new("You didn't come this far to only come this far.", "LCKDN"),
        new("Small wins, every day. That is the whole secret.", "LCKDN"),
        new("Close the tab. Go build the life you keep scrolling past.", "LCKDN"),
        new("Discipline is choosing what you want most over what you want now.", "LCKDN"),
        new("Focus is a muscle. Every rep counts.", "LCKDN"),
        new("The streak is the reward. Protect it.", "LCKDN"),
        new("Boredom is where your best ideas are waiting. Sit with it.", "LCKDN"),
        new("You are not your urges. You are the one who decides.", "LCKDN"),
        new("Stand up, stretch, drink water. Then get back to work.", "LCKDN"),
        new("Stay locked in.", "LCKDN")
    };

    public static Quote Random() => All[System.Random.Shared.Next(All.Length)];

    /** Same quote for a whole 30-minute slot. */
    public static Quote ForNow() => All[(int)(DateTime.UtcNow.Ticks / TimeSpan.FromMinutes(30).Ticks % All.Length)];
}
