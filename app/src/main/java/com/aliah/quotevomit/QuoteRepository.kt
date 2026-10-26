package com.aliah.quotevomit

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Quote(val text: String, val author: String, val fromWeb: Boolean)

/**
 * Gets quotes. Tries a random Wikiquote page first, and falls back to a
 * built-in stash of weird old quotes if you're offline or the page is junk.
 */
object QuoteRepository {

    private const val PREFS = "quote_prefs"
    private const val API =
        "https://en.wikiquote.org/w/api.php?action=query&format=json&formatversion=2" +
            "&generator=random&grnnamespace=0&grnlimit=1&grnfilterredir=nonredirects" +
            "&prop=revisions&rvprop=content&rvslots=main"

    // Sections we don't want: quotes ABOUT the person, fake quotes, link lists, etc.
    private val skipSections = Regex(
        "about|misattributed|disputed|attributed|see also|external|references|cast|sources",
        RegexOption.IGNORE_CASE
    )

    /**
     * Built-in stash of rude, petty, and unhinged lines from history.
     * Everything here is centuries old, so it's free to use. Add your own!
     */
    private val stash = listOf(
        Quote("What, you egg! Young fry of treachery!", "William Shakespeare, Macbeth", false),
        Quote("Thou cream-faced loon!", "William Shakespeare, Macbeth", false),
        Quote("Thou art a boil, a plague-sore.", "William Shakespeare, King Lear", false),
        Quote("Thou art as fat as butter.", "William Shakespeare, Henry IV Part 1", false),
        Quote("Away, you starvelling, you elf-skin, you dried neat's-tongue!", "William Shakespeare, Henry IV Part 1", false),
        Quote("I do desire we may be better strangers.", "William Shakespeare, As You Like It", false),
        Quote("I'll beat thee, but I would infect my hands.", "William Shakespeare, Timon of Athens", false),
        Quote("Methink'st thou art a general offence, and every man should beat thee.", "William Shakespeare, All's Well That Ends Well", false),
        Quote("Lord, what fools these mortals be!", "William Shakespeare, A Midsummer Night's Dream", false),
        Quote("Exit, pursued by a bear.", "William Shakespeare, The Winter's Tale (stage direction)", false),
        Quote("Stand a little out of my sun.", "Diogenes, to Alexander the Great", false),
        Quote("Three may keep a secret, if two of them are dead.", "Benjamin Franklin, Poor Richard's Almanack", false),
        Quote("Fish and visitors stink in three days.", "Benjamin Franklin, Poor Richard's Almanack", false),
        Quote("Man is the only animal that blushes. Or needs to.", "Mark Twain, Following the Equator", false),
        Quote("Get your facts first, and then you can distort them as you please.", "Mark Twain", false),
        Quote("The report of my death was an exaggeration.", "Mark Twain", false),
        Quote("Some cause happiness wherever they go; others whenever they go.", "Oscar Wilde (attributed)", false),
        Quote("He has no enemies, but is intensely disliked by his friends.", "Oscar Wilde, on George Bernard Shaw", false),
        Quote("Either that wallpaper goes, or I do.", "Oscar Wilde (attributed, on his deathbed)", false),
        Quote("Et tu, Brute?", "William Shakespeare, Julius Caesar", false)
    )

    fun randomFromStash(): Quote = stash.random()

    /** Tries a few random Wikiquote pages; returns null if none gave a usable quote. */
    fun fetchFromWikiquote(attempts: Int = 5): Quote? {
        repeat(attempts) {
            try {
                val q = fetchOnce()
                if (q != null) return q
            } catch (_: Exception) {
                return null // probably offline, don't keep hammering
            }
        }
        return null
    }

    private fun fetchOnce(): Quote? {
        val conn = URL(API).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        // Wikimedia asks apps to identify themselves
        conn.setRequestProperty("User-Agent", "QuoteVomit/1.0 (personal hobby Android widget)")
        val body = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }

        val page = JSONObject(body)
            .getJSONObject("query")
            .getJSONArray("pages")
            .getJSONObject(0)
        val title = page.getString("title")
        if (title.startsWith("List of") || title.contains("disambiguation")) return null

        val content = page.getJSONArray("revisions")
            .getJSONObject(0)
            .getJSONObject("slots")
            .getJSONObject("main")
            .getString("content")
        if (content.contains("{{disambig", ignoreCase = true)) return null

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
        if (candidates.isEmpty()) return null
        return Quote(candidates.random(), title, true)
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

    // ---- saving the current quote so the widget can show it ----

    fun save(context: Context, q: Quote) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("text", q.text)
            .putString("author", q.author)
            .putBoolean("web", q.fromWeb)
            .apply()
    }

    fun load(context: Context): Quote {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = p.getString("text", null) ?: return stash.first()
        return Quote(text, p.getString("author", "Unknown") ?: "Unknown", p.getBoolean("web", false))
    }

    /**
     * Gets a new quote and a new pretty background, and saves both.
     * Roughly 1 in 3 quotes comes from the rude stash; the rest are pure Wikiquote chaos.
     */
    fun refresh(context: Context): Quote {
        val q = if ((0 until 3).random() == 0) {
            randomFromStash()
        } else {
            fetchFromWikiquote() ?: randomFromStash()
        }
        save(context, q)
        BackgroundRepository.refresh(context)
        return q
    }
}
