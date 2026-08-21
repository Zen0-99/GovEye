package com.goveye.app.ui.screens.party

/**
 * Parses Wmatrix-format manifesto text into structured blocks for rendering.
 *
 * Wmatrix format:
 * - `<pb/>` = page break
 * - `&bullet;` = bullet point
 * - Lines are wrapped at ~60-70 chars with blank lines between each line
 * - Paragraphs separated by 2+ blank lines
 * - Headings are short lines, often title case or all caps
 */
sealed class ManifestoBlock {
    data class Heading(val text: String, val level: Int = 1) : ManifestoBlock()
    data class Paragraph(val text: String) : ManifestoBlock()
    data class Bullet(val text: String) : ManifestoBlock()
    data object PageBreak : ManifestoBlock()
}

object ManifestoParser {

    fun parse(rawText: String): List<ManifestoBlock> {
        val blocks = mutableListOf<ManifestoBlock>()

        // Split by page breaks first
        val pages = rawText.split("<pb/>")

        for ((pageIndex, page) in pages.withIndex()) {
            if (pageIndex > 0) {
                blocks.add(ManifestoBlock.PageBreak)
            }

            // Normalize: replace &bullet; with a marker, collapse multiple blank lines
            val normalized = page
                .replace("&bullet;", "•")
                .replace("\r\n", "\n")
                .replace("\r", "\n")

            // Split into lines and group into paragraphs (consecutive non-empty lines)
            val lines = normalized.split("\n").map { it.trim() }
            val paragraphs = mutableListOf<List<String>>()
            var currentParagraph = mutableListOf<String>()

            for (line in lines) {
                if (line.isBlank()) {
                    if (currentParagraph.isNotEmpty()) {
                        paragraphs.add(currentParagraph)
                        currentParagraph = mutableListOf()
                    }
                } else {
                    currentParagraph.add(line)
                }
            }
            if (currentParagraph.isNotEmpty()) {
                paragraphs.add(currentParagraph)
            }

            // Classify each paragraph
            for (paragraphLines in paragraphs) {
                val joined = paragraphLines.joinToString(" ")
                val firstLine = paragraphLines.first()

                // Bullet point
                if (firstLine.startsWith("•")) {
                    val bulletText = paragraphLines.joinToString(" ")
                        .removePrefix("•").trim()
                    blocks.add(ManifestoBlock.Bullet(bulletText))
                    continue
                }

                // Heading detection heuristics:
                // 1. Short text (< 80 chars) AND
                // 2. Either: all caps (with optional punctuation), or title case
                //    (each word capitalized), or starts with a number + dot
                val isShort = joined.length < 80
                val isAllCaps = firstLine.uppercase() == firstLine &&
                    firstLine.any { it.isLetter() } &&
                    firstLine.length < 60
                val isTitleCase = firstLine.split(" ").all { word ->
                    word.isEmpty() || word[0].isUpperCase() || !word[0].isLetter()
                } && firstLine.length < 60
                val isNumbered = Regex("^\\d+[.)]\\s+").containsMatchIn(firstLine)
                val isContentsEntry = firstLine.contains(".....") || firstLine.contains("....")

                if (isShort && (isAllCaps || (isTitleCase && paragraphLines.size <= 2)) && !isContentsEntry) {
                    val level = if (isAllCaps) 1 else 2
                    blocks.add(ManifestoBlock.Heading(joined, level))
                } else if (isNumbered && isShort) {
                    blocks.add(ManifestoBlock.Heading(joined, 2))
                } else {
                    blocks.add(ManifestoBlock.Paragraph(joined))
                }
            }
        }

        return blocks
    }
}
