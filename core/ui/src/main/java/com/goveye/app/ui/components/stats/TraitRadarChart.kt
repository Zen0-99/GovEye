package com.goveye.app.ui.components.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.stats.TraitBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Radar chart for trait visualization.
 * 6 axes: Loyalty, Participation, Questions, Speeches, Committees, Finance.
 * The polygon uses [traitDisplayPercent] — mpValue for rate-based traits
 * (Loyalty, Participation), percentile for count-based traits — so the
 * shape reflects the displayed percentages.
 *
 * Grid lines and axes use [onSurface] at a theme-aware alpha so they
 * are visible in both light and dark themes. Axis labels (trait name +
 * percentage) are drawn well outside the chart area.
 */
@Composable
fun TraitRadarChart(traitBars: List<TraitBar>, modifier: Modifier = Modifier) {
    if (traitBars.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    // Use onSurface for grid/axis lines — visible in both light and dark
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    // Label text (trait name) uses onSurface — white in dark theme, dark in light theme
    val labelColor = MaterialTheme.colorScheme.onSurface
    // Percentage uses onSurfaceVariant — grayish in both themes
    val percentileColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Performance Breakdown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val labelTextSize = with(density) { 11.dp.toPx() }
                val paint = remember(labelColor, labelTextSize) {
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = labelTextSize
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                }
                val valuePaint = remember(percentileColor, labelTextSize) {
                    android.graphics.Paint(paint).apply {
                        style = android.graphics.Paint.Style.FILL
                        isFakeBoldText = true
                        color = percentileColor.toArgb()
                    }
                }

                // Box with fixed height so the Canvas can fill it
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        // Shrink radius to leave generous room for labels outside
                        val radius = min(centerX, centerY) * 0.5f
                        val sides = traitBars.size
                        val angleStep = (2 * PI / sides).toFloat()
                        val startAngle = (-PI / 2).toFloat()

                        // Grid rings — broader, brighter, theme-aware
                        for (ringPercent in listOf(0.25f, 0.5f, 0.75f, 1.0f)) {
                            val ringRadius = radius * ringPercent
                            val ringPath = Path()
                            for (i in 0 until sides) {
                                val angle = startAngle + i * angleStep
                                val x = centerX + ringRadius * cos(angle)
                                val y = centerY + ringRadius * sin(angle)
                                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                            }
                            ringPath.close()
                            drawPath(ringPath, color = gridColor, style = Stroke(width = 1.5f))
                        }

                        // Axis lines — broader, brighter, theme-aware
                        for (i in 0 until sides) {
                            val angle = startAngle + i * angleStep
                            val x = centerX + radius * cos(angle)
                            val y = centerY + radius * sin(angle)
                            drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1.5f)
                        }

                        // MP value polygon — uses traitDisplayPercent (mpValue for
                        // rate-based traits, percentile for count-based) so the
                        // polygon reflects the displayed percentage, not just rank.
                        // A minimum radius (15% of chart) ensures 0% values still
                        // produce a visible shape instead of a flat line to center.
                        val minRadius = radius * 0.15f
                        val mpPath = Path()
                        for (i in traitBars.indices) {
                            val angle = startAngle + i * angleStep
                            val value = traitDisplayPercent(traitBars[i]) / 100f
                            val r = minRadius + (radius - minRadius) * value
                            val x = centerX + r * cos(angle)
                            val y = centerY + r * sin(angle)
                            if (i == 0) mpPath.moveTo(x, y) else mpPath.lineTo(x, y)
                        }
                        mpPath.close()
                        drawPath(mpPath, color = primaryColor.copy(alpha = 0.2f))
                        drawPath(mpPath, color = primaryColor, style = Stroke(width = 2.5f))

                        // Dots at vertices + labels well outside the chart
                        for (i in traitBars.indices) {
                            val angle = startAngle + i * angleStep
                            val value = traitDisplayPercent(traitBars[i]) / 100f
                            val r = minRadius + (radius - minRadius) * value
                            val dotX = centerX + r * cos(angle)
                            val dotY = centerY + r * sin(angle)
                            drawCircle(primaryColor, 5f, Offset(dotX, dotY))

                            // Labels further from the chart — 68px beyond radius
                            val labelRadius = radius + 68f
                            val labelX = centerX + labelRadius * cos(angle)
                            val labelY = centerY + labelRadius * sin(angle)
                            drawContext.canvas.nativeCanvas.drawText(
                                traitBars[i].label,
                                labelX,
                                labelY + labelTextSize / 3f,
                                paint
                            )
                            // Show the display percentage (same as polygon fill)
                            drawContext.canvas.nativeCanvas.drawText(
                                "${traitDisplayPercent(traitBars[i]).toInt()}%",
                                labelX,
                                labelY + labelTextSize * 1.5f,
                                valuePaint
                            )
                        }
                    }
                }
            }
        }
    }
}
