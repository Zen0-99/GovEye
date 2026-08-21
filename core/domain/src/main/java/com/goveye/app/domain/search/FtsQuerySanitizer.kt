package com.goveye.app.domain.search

/**
 * Sanitizes a raw user query into safe SQLite FTS MATCH syntax.
 *
 * Strips FTS operators, quotes phrases, and produces safe MATCH syntax.
 * Returns null if the query has no valid tokens (caller should skip the search).
 *
 * Port of Odysseus Vault `_sanitize_fts_query()` pattern.
 */
object FtsQuerySanitizer {
    private val tokenRegex = Regex(""""([^"]+)"|[\w][\w._-]*""")

    fun sanitize(query: String): String? {
        val tokens = mutableListOf<String>()
        for (match in tokenRegex.findAll(query)) {
            val phrase = match.groupValues[1]
            if (phrase.isNotEmpty()) {
                tokens.add("\"${phrase.replace("\"", "\"\"")}\"")
            } else {
                val token = match.value.trim('.', '_', '-')
                if (token.isNotEmpty()) {
                    tokens.add(if (token.any { it in "._-" }) "\"$token\"" else token)
                }
            }
        }
        return if (tokens.isEmpty()) null else tokens.joinToString(" ")
    }
}
