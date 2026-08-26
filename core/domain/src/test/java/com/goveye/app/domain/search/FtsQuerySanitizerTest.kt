package com.goveye.app.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQuerySanitizerTest {
    @Test
    fun emptyString_returnsNull() {
        assertNull(FtsQuerySanitizer.sanitize(""))
    }

    @Test
    fun singleWord_returnsWithPrefixWildcard() {
        assertEquals("housing*", FtsQuerySanitizer.sanitize("housing"))
    }

    @Test
    fun multipleWords_returnsSpaceSeparatedWithWildcards() {
        assertEquals("climate* change*", FtsQuerySanitizer.sanitize("climate change"))
    }

    @Test
    fun quotedPhrase_returnsQuotedWithWildcard() {
        assertEquals("\"net zero\"*", FtsQuerySanitizer.sanitize("\"net zero\""))
    }

    @Test
    fun ftsOperators_strippedOrSanitized() {
        // OR is treated as a regular token since it matches the word pattern
        val result = FtsQuerySanitizer.sanitize("housing OR education")
        assertEquals("housing* OR* education*", result)
    }

    @Test
    fun specialCharactersOnly_returnsNull() {
        assertNull(FtsQuerySanitizer.sanitize("!@#"))
    }

    @Test
    fun mixedPhraseAndWord_returnsBothWithWildcards() {
        assertEquals("\"net zero\"* housing*", FtsQuerySanitizer.sanitize("\"net zero\" housing"))
    }

    @Test
    fun wildcardStripped() {
        // * is not part of the word pattern, so it gets stripped, then * is re-added
        val result = FtsQuerySanitizer.sanitize("hous*")
        assertEquals("hous*", result)
    }
}
