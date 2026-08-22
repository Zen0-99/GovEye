package com.goveye.app.data.repo

import android.util.Log
import com.goveye.app.data.local.dao.ManifestoDao
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.domain.search.FtsQuerySanitizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestoRepository @Inject constructor(private val manifestoDao: ManifestoDao) {

    companion object {
        private const val TAG = "GovEye/Manifesto"
        private const val SNIPPET_RADIUS = 60
        private const val MAX_SNIPPETS = 50
    }

    suspend fun getManifesto(partyId: Int): PartyManifestoEntity? = manifestoDao.getManifesto(partyId)

    suspend fun getManifestoText(partyId: Int): String? = manifestoDao.getManifestoText(partyId)

    /**
     * Search a party's manifesto for the given query.
     *
     * Uses a two-phase approach (Odysseus Vault pattern):
     * 1. FTS4 MATCH to quickly check if the manifesto contains the terms at all
     * 2. If match found, LIKE search in the full text to find ALL occurrences
     *    and generate snippets around each one
     *
     * This returns multiple results (one per occurrence) instead of the single
     * snippet that FTS4's snippet() function returns per row.
     */
    suspend fun searchManifesto(partyId: Int, query: String): List<ManifestoSearchResult> {
        val sanitized = FtsQuerySanitizer.sanitize(query) ?: return emptyList()

        // Phase 1: FTS4 fast check — does this party's manifesto contain the terms?
        val ftsHits = try {
            manifestoDao.searchManifestoFts4(partyId, sanitized)
        } catch (e: Exception) {
            Log.e(TAG, "FTS4 search failed: ${e.message}")
            emptyList()
        }

        if (ftsHits.isEmpty()) {
            // FTS4 says no match — but FTS4 tokenization may differ from our
            // LIKE search. Try LIKE search as a fallback (Odysseus pattern).
            return likeSearchManifesto(partyId, query)
        }

        // Phase 2: LIKE search for all occurrences with snippet generation
        return likeSearchManifesto(partyId, query)
    }

    /**
     * LIKE-based search that finds ALL occurrences of each search term in the
     * manifesto text and generates snippets around each one.
     *
     * This is the fallback when FTS4 doesn't match (different tokenization)
     * and the primary source of multiple snippets when FTS4 does match.
     */
    private suspend fun likeSearchManifesto(partyId: Int, query: String): List<ManifestoSearchResult> {
        val manifestoText = manifestoDao.getManifestoText(partyId) ?: return emptyList()

        // Extract individual search terms (split on whitespace, strip punctuation)
        val terms = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("^[^\\w]+|[^\\w]+$"), "") }
            .filter { it.length >= 2 }
            .distinct()

        if (terms.isEmpty()) return emptyList()

        // Find all positions where any term matches (case-insensitive)
        val lowerText = manifestoText.lowercase()
        val positions = mutableListOf<Int>()

        for (term in terms) {
            val lowerTerm = term.lowercase()
            var idx = lowerText.indexOf(lowerTerm)
            while (idx >= 0 && positions.size < MAX_SNIPPETS * 3) {
                positions.add(idx)
                idx = lowerText.indexOf(lowerTerm, idx + 1)
            }
        }

        if (positions.isEmpty()) return emptyList()

        // Sort positions and deduplicate nearby ones (avoid overlapping snippets)
        positions.sort()
        val dedupedPositions = mutableListOf<Int>()
        var lastPos = -SNIPPET_RADIUS * 3
        for (pos in positions) {
            if (pos - lastPos >= SNIPPET_RADIUS) {
                dedupedPositions.add(pos)
                lastPos = pos
            }
        }

        // Generate snippets around each position with highlighting
        val results = dedupedPositions.take(MAX_SNIPPETS).map { pos ->
            val start = (pos - SNIPPET_RADIUS).coerceAtLeast(0)
            val end = (pos + SNIPPET_RADIUS).coerceAtMost(manifestoText.length)
            var snippet = manifestoText.substring(start, end)
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (start > 0) snippet = "...$snippet"
            if (end < manifestoText.length) snippet = "$snippet..."

            // Highlight all terms in the snippet
            var highlighted = snippet
            for (term in terms.sortedByDescending { it.length }) {
                highlighted = highlightTerm(highlighted, term)
            }

            ManifestoSearchResult(
                partyId = partyId,
                snippetText = highlighted,
                searchRank = 0f
            )
        }

        Log.i(TAG, "LIKE search for partyId=$partyId query='$query': ${results.size} snippets")
        return results
    }

    /**
     * Highlight a term in a snippet by wrapping it in <b> tags (case-insensitive).
     */
    private fun highlightTerm(snippet: String, term: String): String {
        val regex = Regex(Regex.escape(term), RegexOption.IGNORE_CASE)
        return regex.replace(snippet) { match ->
            "<b>${match.value}</b>"
        }
    }
}
