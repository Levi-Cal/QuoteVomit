package com.aliah.quotevomit

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * One loading-screen card.
 * label  = the little tag on top, like "TIP" or "USELESS FACT"
 * text   = the main thing
 * author = who said it (blank for tips and facts)
 * source = where it came from, shown in the footer
 */
data class Quote(
    val text: String,
    val author: String,
    val fromWeb: Boolean,
    val label: String = "QUOTE",
    val source: String = "the stash"
)

/**
 * Loading-screen randomizer. Each refresh rolls a random category:
 * Wikiquote quote, useless fact, Wikipedia "did you know", fake game tip, or a rude old quote.
 * Anything that fails (offline, junk page) falls back to the built-in tips and quotes.
 */
object QuoteRepository {

    private const val PREFS = "quote_prefs"
    private const val UA = "QuoteVomit/1.0 (personal hobby Android widget)"

    private const val WIKIQUOTE_API =
        "https://en.wikiquote.org/w/api.php?action=query&format=json&formatversion=2" +
            "&generator=random&grnnamespace=0&grnlimit=1&grnfilterredir=nonredirects" +
            "&prop=revisions&rvprop=content&rvslots=main"
    private const val WIKIPEDIA_RANDOM = "https://en.wikipedia.org/api/rest_v1/page/random/summary"
    private const val USELESS_FACT = "https://uselessfacts.jsph.pl/api/v2/facts/random?language=en"

    // Wikiquote sections we skip: quotes ABOUT someone, fake quotes, link lists, etc.
    private val skipSections = Regex(
        "about|misattributed|disputed|attributed|see also|external|references|cast|sources",
        RegexOption.IGNORE_CASE
    )

    /** Rude, petty, and unhinged lines from history. All centuries old. Add your own! */
    private val rudeStash = listOf(
        "What, you egg! Young fry of treachery!" to "William Shakespeare, Macbeth",
        "Thou cream-faced loon!" to "William Shakespeare, Macbeth",
        "Thou art a boil, a plague-sore." to "William Shakespeare, King Lear",
        "Thou art as fat as butter." to "William Shakespeare, Henry IV Part 1",
        "Away, you starvelling, you elf-skin, you dried neat's-tongue!" to "William Shakespeare, Henry IV Part 1",
        "I do desire we may be better strangers." to "William Shakespeare, As You Like It",
        "I'll beat thee, but I would infect my hands." to "William Shakespeare, Timon of Athens",
        "Methink'st thou art a general offence, and every man should beat thee." to "William Shakespeare, All's Well That Ends Well",
        "Lord, what fools these mortals be!" to "William Shakespeare, A Midsummer Night's Dream",
        "Exit, pursued by a bear." to "William Shakespeare, The Winter's Tale (stage direction)",
        "Stand a little out of my sun." to "Diogenes, to Alexander the Great",
        "Three may keep a secret, if two of them are dead." to "Benjamin Franklin, Poor Richard's Almanack",
        "Fish and visitors stink in three days." to "Benjamin Franklin, Poor Richard's Almanack",
        "Man is the only animal that blushes. Or needs to." to "Mark Twain, Following the Equator",
        "Get your facts first, and then you can distort them as you please." to "Mark Twain",
        "The report of my death was an exaggeration." to "Mark Twain",
        "Some cause happiness wherever they go; others whenever they go." to "Oscar Wilde (attributed)",
        "He has no enemies, but is intensely disliked by his friends." to "Oscar Wilde, on George Bernard Shaw",
        "Either that wallpaper goes, or I do." to "Oscar Wilde (attributed, on his deathbed)",
        "Et tu, Brute?" to "William Shakespeare, Julius Caesar"
    )

    /** Fake loading-screen tips. */
    private val tips = listOf(
        "Drinking water increases your health. Probably.",
        "Touching grass restores +10 sanity.",
        "Coffee grants a temporary speed boost. Crash damage is applied later.",
        "Sleeping is a valid strategy. Respawn is guaranteed.",
        "Pressing the elevator button harder does not make it arrive faster.",
        "You cannot outrun a deadline. You can only face it.",
        "Group projects have a hidden stat: one member does 90% of the work.",
        "Reading the instructions first unlocks the rare 'knowing what to do' achievement.",
        "Some doors are push. Read the sign.",
        "There is no shame in replaying the tutorial.",
        "Dishes left overnight respawn as a stronger version.",
        "Ctrl+Z works on code. It does not work on texts you already sent.",
        "'It works on my machine' is not a valid defense.",
        "Most bugs reveal themselves the moment after you submit.",
        "Always back up your database before running DELETE without a WHERE.",
        "Save often. Especially your thesis.",
        "NPCs who say 'we should hang out sometime' rarely spawn again.",
        "If you're lost, walk in a confident direction. Nobody will question it.",
        "The semicolon you forgot is still out there. Somewhere.",
        "Your phone battery regenerates slowly while plugged in. Patience is key.",
        "Closing unused tabs reduces your laptop's fan noise by 40%. Source: trust me.",
        "Enemies are weak to sleep. So are you.",
        "Snacks restore health but reduce your wallet's durability.",
        "Yes, you can skip this. No, you won't."
    )

    // ---- rolling a random card ----

    private fun fromRudeStash(): Quote {
        val (text, author) = rudeStash.random()
        return Quote(text, author, false, label = "QUOTE", source = "the stash")
    }

    private fun fromTips(): Quote =
        Quote(tips.random(), "", false, label = "TIP", source = "the loading screen")

    private fun offlineFallback(): Quote =
        if ((0 until 2).random() == 0) fromTips() else fromRudeStash()

    /** Picks a category, tries it, and falls back to offline content if it fails. */
    fun roll(): Quote {
        val roll = (0 until 100).random()
        val result = try {
            when {
                roll < 30 -> fetchWikiquote()       // 30% random quote from anywhere in history
                roll < 50 -> fetchUselessFact()     // 20% useless fact
                roll < 70 -> fetchWikipedia()       // 20% random Wikipedia trivia
                roll < 88 -> fromTips()             // 18% fake game tip
                else -> fromRudeStash()             // 12% rude old quote
            }
        } catch (_: Exception) {
            null
        }
        return result ?: offlineFallback()
    }

    // ---- web sources ----

    private fun getJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", UA)
        conn.setRequestProperty("Accept", "application/json")
        return try {
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchUselessFact(): Quote? {
        val text = getJson(USELESS_FACT).optString("text").trim()
        if (text.length !in 10..260) return null
        return Quote(text, "", true, label = "USELESS FACT", source = "uselessfacts")
    }

    private fun fetchWikipedia(): Quote? {
        repeat(3) {
            val page = getJson(WIKIPEDIA_RANDOM)
            val title = page.optString("title")
            val extract = page.optString("extract").replace(Regex("\\s+"), " ").trim()
            // Keep just the first sentence so it fits on the widget
            val end = Regex("(?<=[a-z0-9)\\]])\\. ").find(extract, 20)?.range?.first
            val sentence = if (end != null) extract.substring(0, end + 1) else extract
            if (sentence.length in 30..240) {
                return Quote(sentence, "", true, label = "DID YOU KNOW", source = "Wikipedia: $title")
            }
        }
        return null
    }

    private fun fetchWikiquote(): Quote? {
        repeat(5) {
            val page = getJson(WIKIQUOTE_API)
                .getJSONObject("query")
                .getJSONArray("pages")
                .getJSONObject(0)
            val title = page.getString("title")
            if (title.startsWith("List of") || title.contains("disambiguation")) return@repeat

            val content = page.getJSONArray("revisions")
                .getJSONObject(0)
                .getJSONObject("slots")
                .getJSONObject("main")
                .getString("content")
            if (content.contains("{{disambig", ignoreCase = true)) return@repeat

            val candidates = mutableListOf<String>()
            var skipping = false
            for (line in content.lines()) {
                val heading = Regex("^==+\\s*(.*?)\\s*==+\\s*$").find(line)
                if (heading != null) {
                    skipping = skipSections.containsMatchIn(heading.groupValues[1])
                    continue
                }
                if (skipping) continue
                // A single "*" is a quote; "**" lines are just where it came from
                if (line.startsWith("*") && !line.startsWith("**") && !line.startsWith("*:")) {
                    val cleaned = clean(line)
                    if (looksUsable(cleaned)) candidates += cleaned
                }
            }
            if (candidates.isNotEmpty()) {
                return Quote(candidates.random(), title, true, label = "QUOTE", source = "Wikiquote")
            }
        }
        return null
    }

    /** Strips wiki markup like [[links]], '''bold''', {{templates}} and <ref> tags. */
    private fun clean(raw: String): String {
        var s = raw.removePrefix("*")
        s = s.replace(Regex("<ref[^>]*/>"), "")
        s = s.replace(Regex("<ref[^>]*>.*?</ref>"), "")
        repeat(3) { s = s.replace(Regex("\\{\\{[^{}]*\\}\\}"), "") }
        s = s.replace(Regex("\\[\\[(?:[^|\\]]*\\|)?([^\\]]+)\\]\\]"), "\$1")
        s = s.replace(Regex("\\[https?://\\S+\\s([^\\]]+)\\]"), "\$1")
        s = s.replace(Regex("'{2,}"), "")
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        s = s.replace(Regex("<[^>]+>"), "")
        s = s.replace("&nbsp;", " ").replace("&quot;", "\"").replace("&amp;", "&")
            .replace("&mdash;", "—").replace("&ndash;", "–")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    private fun looksUsable(s: String): Boolean =
        s.length in 15..240 &&
            s.any { it.isLetter() } &&
            !s.contains("http") &&
            !s.contains("[[") && !s.contains("{{") && !s.contains("|")

    // ---- saving the current card so the widget can show it ----

    fun save(context: Context, q: Quote) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("text", q.text)
            .putString("author", q.author)
            .putBoolean("web", q.fromWeb)
            .putString("label", q.label)
            .putString("source", q.source)
            .apply()
    }

    fun load(context: Context): Quote {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = p.getString("text", null)
            ?: return Quote("What, you egg! Young fry of treachery!", "William Shakespeare, Macbeth", false)
        return Quote(
            text,
            p.getString("author", "") ?: "",
            p.getBoolean("web", false),
            p.getString("label", "QUOTE") ?: "QUOTE",
            p.getString("source", "the stash") ?: "the stash"
        )
    }

    /** Rolls a new card and a new pretty background, and saves both. */
    fun refresh(context: Context): Quote {
        val q = roll()
        save(context, q)
        BackgroundRepository.refresh(context)
        return q
    }
}
