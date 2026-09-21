package com.goveye.app.ui.components.stats

import com.goveye.app.domain.stats.TraitBar

/**
 * Returns the percentage value to display and use for bar/radar fills.
 *
 * Rate-based traits (Loyalty, Participation) use the actual mpValue (0-100).
 * Count-based traits (Questions, Speeches, Finance) use the normalized
 * rate-based score stored in [TraitBar.percentile] (0-100, clamped).
 *
 * Questions/Speeches/Finance: score = (mpRate / avgRate) * 100, where rate = count / yearsServed.
 * An MP at the average rate scores 100%.
 */
fun traitDisplayPercent(trait: TraitBar): Float = when (trait.label) {
    "Loyalty", "Participation" -> trait.mpValue.coerceIn(0f, 100f)
    else -> trait.percentile.toFloat().coerceIn(0f, 100f)
}
