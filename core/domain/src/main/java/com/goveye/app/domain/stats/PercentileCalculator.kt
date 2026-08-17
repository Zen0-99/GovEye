package com.goveye.app.domain.stats

/**
 * Computes percentile rank of a value among peer values.
 * Pure function.
 */
object PercentileCalculator {
    /**
     * @param value The value to rank
     * @param peerValues All peers' values (including the value being ranked)
     * @return Percentile rank (0-100), where 100 = highest
     */
    fun computePercentile(value: Float, peerValues: List<Float>): Int {
        if (peerValues.isEmpty()) return 50
        val below = peerValues.count { it < value }
        val equal = peerValues.count { it == value }
        // Average rank for ties
        val percentile = (below + equal / 2f) / peerValues.size * 100f
        return percentile.toInt().coerceIn(0, 100)
    }

    /**
     * @param value The value to rank
     * @param peerValues All peers' values (including the value being ranked)
     * @return Percentile rank (0-100)
     */
    fun computePercentile(value: Int, peerValues: List<Int>): Int =
        computePercentile(value.toFloat(), peerValues.map { it.toFloat() })
}
