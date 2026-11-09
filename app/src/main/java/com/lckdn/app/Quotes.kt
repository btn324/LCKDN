package com.lckdn.app

import kotlin.random.Random

/** Short lines on discipline, self-control and growth. */
object Quotes {

    data class Quote(val text: String, val author: String) {
        override fun toString() = "“$text” — $author"
    }

    val all = listOf(
        Quote("We suffer more often in imagination than in reality.", "Seneca"),
        Quote("No man is free who is not master of himself.", "Epictetus"),
        Quote("You have power over your mind, not outside events. Realize this, and you will find strength.", "Marcus Aurelius"),
        Quote("The impediment to action advances action. What stands in the way becomes the way.", "Marcus Aurelius"),
        Quote("Waste no more time arguing what a good man should be. Be one.", "Marcus Aurelius"),
        Quote("First say to yourself what you would be; and then do what you have to do.", "Epictetus"),
        Quote("He who conquers himself is the mightiest warrior.", "Confucius"),
        Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        Quote("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant"),
        Quote("I count him braver who overcomes his desires than him who conquers his enemies.", "Aristotle"),
        Quote("Luck is what happens when preparation meets opportunity.", "Seneca"),
        Quote("Difficulties strengthen the mind, as labor does the body.", "Seneca"),
        Quote("Self-control is strength. Right thought is mastery. Calmness is power.", "James Allen"),
        Quote("A man is but the product of his thoughts. What he thinks, he becomes.", "Mahatma Gandhi"),
        Quote("Well done is better than well said.", "Benjamin Franklin"),
        Quote("Lost time is never found again.", "Benjamin Franklin"),
        Quote("Energy and persistence conquer all things.", "Benjamin Franklin"),
        Quote("The secret of getting ahead is getting started.", "Mark Twain"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        Quote("Nothing in the world is worth having or worth doing unless it means effort, pain, difficulty.", "Theodore Roosevelt"),
        Quote("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        Quote("What you do today can improve all your tomorrows.", "Ralph Marston"),
        Quote("The best time to plant a tree was 20 years ago. The second best time is now.", "Proverb"),
        Quote("Fall seven times, stand up eight.", "Japanese proverb"),
        Quote("A river cuts through rock not because of its power, but because of its persistence.", "Proverb"),
        Quote("The urge will pass. The regret won't. Let it pass.", "LCKDN"),
        Quote("Every time you say no to the urge, you vote for the person you want to become.", "LCKDN"),
        Quote("Your future self is watching. Make that person proud.", "LCKDN"),
        Quote("Comfort is the enemy of progress. Do the hard thing first.", "LCKDN"),
        Quote("Ten minutes of discomfort beats ten days of regret.", "LCKDN"),
        Quote("You didn't come this far to only come this far.", "LCKDN"),
        Quote("Small wins, every day. That is the whole secret.", "LCKDN"),
        Quote("Put the phone down. Go build the life you keep scrolling past.", "LCKDN"),
        Quote("Discipline is choosing what you want most over what you want now.", "LCKDN"),
        Quote("Focus is a muscle. Every rep counts.", "LCKDN"),
        Quote("The streak is the reward. Protect it.", "LCKDN"),
        Quote("Boredom is where your best ideas are waiting. Sit with it.", "LCKDN"),
        Quote("You are not your urges. You are the one who decides.", "LCKDN"),
        Quote("Move your body, clear your mind. Go for a walk.", "LCKDN"),
        Quote("Stay locked in.", "LCKDN")
    )

    fun random(): Quote = all[Random.nextInt(all.size)]

    /** The same quote for everyone within one time slot, so the status notification changes slowly. */
    fun forSlot(slot: Long): Quote = all[(slot % all.size).toInt()]
}
