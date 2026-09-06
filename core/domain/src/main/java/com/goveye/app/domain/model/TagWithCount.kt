package com.goveye.app.domain.model

/**
 * A tag with its mention count ([hitCount]) from the source text.
 * Tags are ordered by [hitCount] descending — the top tags represent
 * the most prominent topics in a division, publication, statement, or legislation.
 *
 * Used in the feed to show topic pills with counts, helping users understand
 * the subject matter of items with opaque or legalistic titles.
 */
data class TagWithCount(val tag: String, val hitCount: Int)
