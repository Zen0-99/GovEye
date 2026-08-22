package com.goveye.app.ui.screens.party

/**
 * Parses Wmatrix-format manifesto text into structured blocks for rendering.
 *
 * Wmatrix format characteristics:
 * - `<pb/>` = page break
 * - `&bullet;` = bullet point (may be on its own line or with content)
 * - Lines are wrapped at ~60-70 chars with blank lines between each wrapped line
 * - Some manifestos (Labour, Conservative) use 2+ blank lines to separate real
 *   paragraphs, with single blank lines acting as line-wrap markers within a
 *   paragraph.
 * - Other manifestos (Reform UK) put a single blank line between EVERY line,
 *   providing no reliable paragraph boundary signal at all.
 *
 * Detection strategy:
 * 1. Pages with 2+ consecutive blank lines → use those as paragraph boundaries;
 *    single blank lines are treated as line wraps and joined.
 * 2. Pages with only single blank lines → collapse all blank lines, join all text
 *    into one stream, then split into paragraphs by sentence boundaries
 *    (period/!/? followed by a capital letter).
 * 3. Structural elements (headings, bullets, contents/glossary entries) are
 *    detected per-line in both modes and act as paragraph boundaries.
 * 4. Contents/glossary entries with dotted leaders (....) are stripped and
 *    rendered as bullet/list items.
 */
sealed class ManifestoBlock {
    data class Heading(val text: String, val level: Int = 1) : ManifestoBlock()
    data class Paragraph(val text: String) : ManifestoBlock()
    data class Bullet(val text: String) : ManifestoBlock()
    data object PageBreak : ManifestoBlock()
}

object ManifestoParser {

    /** Matches a sentence boundary: . ! or ? followed by whitespace and a capital letter. */
    private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+(?=[A-Z])")

    /** Matches 4+ consecutive dots used as dotted leaders in contents/glossary entries. */
    private val DOTTED_LEADERS = Regex("\\.{4,}")

    /** Matches a numbered heading prefix like "1." or "3)" at the start of a line. */
    private val NUMBERED_PREFIX = Regex("^\\d+[.)]\\s+")

    fun parse(rawText: String): List<ManifestoBlock> {
        val blocks = mutableListOf<ManifestoBlock>()
        val pages = rawText.split("<pb/>")

        for ((pageIndex, page) in pages.withIndex()) {
            if (pageIndex > 0) {
                blocks.add(ManifestoBlock.PageBreak)
            }

            val normalized = page
                .replace("&bullet;", "•")
                .replace("\r\n", "\n")
                .replace("\r", "\n")

            val lines = normalized.split("\n").map { it.trim() }
            val hasHardBreaks = maxConsecutiveBlankLines(lines) >= 2

            processPage(lines, hasHardBreaks, blocks)
        }

        return blocks
    }

    /**
     * Processes all lines in a single page, classifying each line and accumulating
     * body text / bullet text / heading text into blocks.
     */
    private fun processPage(lines: List<String>, hasHardBreaks: Boolean, blocks: MutableList<ManifestoBlock>) {
        val bodyBuffer = mutableListOf<String>()
        val bulletBuffer = mutableListOf<String>()
        var headingText: String? = null
        var pendingBulletMarker = false
        var inBullet = false
        var blankCount = 0

        fun flushBody() {
            if (bodyBuffer.isEmpty()) return
            val joined = bodyBuffer.joinToString(" ")
            bodyBuffer.clear()

            if (hasHardBreaks) {
                blocks.add(ManifestoBlock.Paragraph(joined))
            } else {
                // No hard-break signal — split the accumulated text by sentence
                // boundaries so each sentence becomes its own paragraph.
                SENTENCE_BOUNDARY.split(joined)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { blocks.add(ManifestoBlock.Paragraph(it)) }
            }
        }

        fun flushBullet() {
            inBullet = false
            pendingBulletMarker = false
            if (bulletBuffer.isEmpty()) return
            val joined = bulletBuffer.joinToString(" ")
            bulletBuffer.clear()
            blocks.add(ManifestoBlock.Bullet(joined))
        }

        fun flushHeading() {
            val text = headingText ?: return
            headingText = null
            val level = if (text.uppercase() == text && text.any { it.isLetter() }) 1 else 2
            blocks.add(ManifestoBlock.Heading(text, level))
        }

        fun flushAll() {
            flushHeading()
            flushBullet()
            flushBody()
        }

        for (line in lines) {
            if (line.isBlank()) {
                blankCount++
                continue
            }

            // In hard-break mode, 2+ consecutive blank lines signal a paragraph
            // boundary — flush everything accumulated so far.
            if (blankCount >= 2 && hasHardBreaks) {
                flushAll()
            }
            blankCount = 0

            // --- Bullet marker on its own line (Conservative contents style) ---
            if (line == "•") {
                flushAll()
                pendingBulletMarker = true
                inBullet = true
                continue
            }

            // --- Bullet with content on the same line (Labour style) ---
            if (line.startsWith("•")) {
                flushAll()
                val content = line.removePrefix("•").trim()
                if (content.isNotEmpty()) {
                    bulletBuffer.add(content)
                }
                inBullet = true
                pendingBulletMarker = false
                continue
            }

            // --- Contents / glossary entry with dotted leaders ---
            if (DOTTED_LEADERS.containsMatchIn(line)) {
                flushAll()
                blocks.add(ManifestoBlock.Bullet(cleanContentsEntry(line)))
                continue
            }

            // --- Heading detection ---
            if (isHeading(line)) {
                flushAll()
                headingText = line
                continue
            }

            // --- Heading continuation check ---
            // If we're accumulating a heading, a short following line that doesn't
            // end with sentence punctuation may be a continuation (e.g. a heading
            // wrapped across two lines like "Britain Needs Reform" / "and Reform UK
            // Needs You").
            if (headingText != null && isHeadingContinuation(line)) {
                headingText = "$headingText $line"
                continue
            }

            // Not a continuation — flush the heading and fall through.
            if (headingText != null) {
                flushHeading()
            }

            // --- Pending bullet marker: this line is the bullet's content ---
            if (pendingBulletMarker) {
                bulletBuffer.add(line)
                pendingBulletMarker = false
                continue
            }

            // --- Inside a bullet: keep accumulating content lines ---
            if (inBullet) {
                bulletBuffer.add(line)
                continue
            }

            // --- Body text ---
            bodyBuffer.add(line)
        }

        flushAll()
    }

    // ------------------------------------------------------------------
    //  Heuristics
    // ------------------------------------------------------------------

    /**
     * Returns true if a line looks like a heading:
     * - Short enough (< 80 chars)
     * - Contains at least one letter
     * - Does NOT end with sentence-ending punctuation
     * - Has no dotted leaders or bullet markers
     * - Is all-caps, title-case, or starts with a numbered prefix (1. / 3))
     */
    private fun isHeading(line: String): Boolean {
        if (line.length > 80) return false
        if (!line.any { it.isLetter() }) return false
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) return false
        if (DOTTED_LEADERS.containsMatchIn(line)) return false
        if (line.startsWith("•")) return false

        val isAllCaps = line.uppercase() == line
        val isTitleCase = isTitleCase(line)
        val isNumbered = NUMBERED_PREFIX.containsMatchIn(line)

        return isAllCaps || (isTitleCase && line.length < 60) || isNumbered
    }

    /**
     * Returns true if a line could be a continuation of a heading:
     * - Short, no sentence-ending punctuation, no dotted leaders/bullets
     * - Starts with a lowercase word (e.g. "and Reform UK Needs You"), OR
     *   is itself title-case / all-caps
     */
    private fun isHeadingContinuation(line: String): Boolean {
        if (line.length > 80) return false
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) return false
        if (DOTTED_LEADERS.containsMatchIn(line)) return false
        if (line.startsWith("•")) return false
        if (!line.any { it.isLetter() }) return false

        val firstWord = line.split(" ").firstOrNull() ?: ""
        if (firstWord.isNotEmpty() && firstWord[0].isLowerCase()) return true

        return isTitleCase(line) || (line.uppercase() == line && line.any { it.isLetter() })
    }

    /** Checks whether every word in the line starts with an uppercase letter. */
    private fun isTitleCase(line: String): Boolean = line.split(" ").all { word ->
        word.isEmpty() || word[0].isUpperCase() || !word[0].isLetter()
    }

    /**
     * Strips dotted leaders from a contents/glossary entry and collapses
     * whitespace, preserving the title and trailing page number.
     * e.g. "Foreword...........3" → "Foreword 3"
     */
    private fun cleanContentsEntry(line: String): String = DOTTED_LEADERS.replace(line, " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Returns the maximum number of consecutive blank lines in [lines]. */
    private fun maxConsecutiveBlankLines(lines: List<String>): Int {
        var maxBlank = 0
        var current = 0
        for (line in lines) {
            if (line.isBlank()) {
                current++
                maxBlank = maxOf(maxBlank, current)
            } else {
                current = 0
            }
        }
        return maxBlank
    }
}
