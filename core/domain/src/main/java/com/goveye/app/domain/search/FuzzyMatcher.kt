package com.goveye.app.domain.search

/**
 * Lightweight Levenshtein distance + fuzzy matching utilities.
 *
 * Used as a final fallback when FTS and LIKE search return no results,
 * to catch typos and partial name fragments (e.g. "Hamiltton" → "Hamilton").
 */
object FuzzyMatcher {

    /**
     * Compute the Levenshtein edit distance between [a] and [b].
     * Uses the standard dynamic-programming approach with O(n*m) time
     * and O(min(n,m)) space.
     */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val s = if (a.length <= b.length) a else b // shorter
        val l = if (a.length <= b.length) b else a // longer

        val prev = IntArray(s.length + 1) { it }
        val curr = IntArray(s.length + 1)

        for (j in 1..l.length) {
            curr[0] = j
            for (i in 1..s.length) {
                val cost = if (s[i - 1] == l[j - 1]) 0 else 1
                curr[i] = minOf(
                    prev[i] + 1, // deletion
                    curr[i - 1] + 1, // insertion
                    prev[i - 1] + cost // substitution
                )
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return prev[s.length]
    }

    /**
     * Check if [query] fuzzy-matches [target] (case-insensitive).
     *
     * A match is accepted if:
     * - The Levenshtein distance is within [maxDistance], OR
     * - [query] is a substring of [target] (catches partial matches), OR
     * - [query] matches any word in [target] with edit distance ≤ [maxDistance]
     *   (catches "Hamiltton" → "Hamilton" when the full name is "Hamilton, Ms Diane")
     *
     * @param query The user's search query (single token, already lowercased)
     * @param target The text to match against (already lowercased)
     * @param maxDistance Maximum edit distance (default 2 — catches 1-2 char typos)
     */
    fun matches(query: String, target: String, maxDistance: Int = 2): Boolean {
        if (query.isBlank()) return false
        val q = query.lowercase()
        val t = target.lowercase()

        // Substring match (LIKE equivalent)
        if (t.contains(q)) return true

        // Full-string Levenshtein (good for short names)
        if (q.length >= 3 && levenshtein(q, t) <= maxDistance) return true

        // Per-word Levenshtein (catches "Hamiltton" → "Hamilton" in "Hamilton, Ms Diane")
        val words = t.split(Regex("[\\s,]+"))
        for (word in words) {
            if (word.length >= 3 && levenshtein(q, word) <= maxDistance) return true
            // Also check prefix — "Hamil" should match "Hamilton"
            if (word.startsWith(q) || q.startsWith(word)) return true
        }
        return false
    }

    /**
     * Score how well [query] matches [target] — lower is better.
     * Returns [Int.MAX_VALUE] if no match.
     *
     * Ranking tiers (lower = better match):
     * - 0: Exact full-string match
     * - 1-49: Full name starts with query (prefix of entire string)
     * - 50-99: First word starts with query (word prefix, first word)
     * - 100-199: Any word starts with query (word prefix, later words)
     * - 200-299: Query is substring inside a word (position-based)
     * - 300-399: Query is substring of full string (not at word boundary)
     * - 400+: Fuzzy Levenshtein match (400 + edit distance)
     */
    fun score(query: String, target: String): Int {
        val q = query.lowercase()
        val t = target.lowercase()

        // Tier 0: Exact match
        if (q == t) return 0

        // Tier 1-49: Full string prefix
        if (t.startsWith(q)) return 1 + (t.length - q.length).coerceAtMost(48)

        val words = t.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        var bestScore = Int.MAX_VALUE

        for ((index, word) in words.withIndex()) {
            // Tier 50-99 / 100-199: Word prefix match
            if (word.startsWith(q)) {
                val score = if (index == 0) {
                    50 + (word.length - q.length).coerceAtMost(49)
                } else {
                    100 + index * 10 + (word.length - q.length).coerceAtMost(89)
                }
                if (score < bestScore) bestScore = score
            }

            // Tier 200-299: Substring inside a word
            val wordIdx = word.indexOf(q)
            if (wordIdx >= 0) {
                val score = 200 + index * 10 + wordIdx.coerceAtMost(89)
                if (score < bestScore) bestScore = score
            }

            // Tier 400+: Levenshtein fallback
            if (word.length >= 3) {
                val d = levenshtein(q, word)
                val score = 400 + d
                if (score < bestScore) bestScore = score
            }
        }

        // Tier 300-399: Substring of full string but not caught by word-level
        if (bestScore == Int.MAX_VALUE) {
            val fullIdx = t.indexOf(q)
            if (fullIdx >= 0) return 300 + fullIdx.coerceAtMost(99)
        }

        return bestScore
    }
}
