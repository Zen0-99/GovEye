package com.goveye.app.ui.components.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
 * FotMob-style pentagon/radar chart for trait visualization.
 * 5 axes: Rebellion, Participation, Questions, Speeches, Committees.
 * The MP's percentile values form a filled polygon.
 */
@Composable
fun TraitRadarChart(traitBars: List<TraitBar>, modifier: Modifier = Modifier) {
    if (traitBars.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Traits vs Peers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Percentile rank (0-100) among same-house MPs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val labelTextSize = with(density) { 11.dp.toPx() }
            val paint = remember(labelColor, labelTextSize) {
                android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = labelTextSize
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = min(centerX, centerY) * 0.65f
                val sides = traitBars.size
                val angleStep = (2 * PI / sides).toFloat()
                val startAngle = (-PI / 2).toFloat()

                // Grid rings
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
                    drawPath(ringPath, color = gridColor, style = Stroke(width = 1f))
                }

                // Axis lines
                for (i in 0 until sides) {
                    val angle = startAngle + i * angleStep
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)
                    drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1f)
                }

                // MP percentile polygon
                val mpPath = Path()
                for (i in traitBars.indices) {
                    val angle = startAngle + i * angleStep
                    val value = traitBars[i].percentile / 100f
                    val r = radius * value
                    val x = centerX + r * cos(angle)
                    val y = centerY + r * sin(angle)
                    if (i == 0) mpPath.moveTo(x, y) else mpPath.lineTo(x, y)
                }
                mpPath.close()
                drawPath(mpPath, color = primaryColor.copy(alpha = 0.25f))
                drawPath(mpPath, color = primaryColor, style = Stroke(width = 2.5f))

                // Dots at vertices + labels
                for (i in traitBars.indices) {
                    val angle = startAngle + i * angleStep
                    val value = traitBars[i].percentile / 100f
                    val r = radius * value
                    val dotX = centerX + r * cos(angle)
                    val dotY = centerY + r * sin(angle)
                    drawCircle(primaryColor, 5f, Offset(dotX, dotY))

                    // Label outside the chart
                    val labelRadius = radius + 36f
                    val labelX = centerX + labelRadius * cos(angle)
                    val labelY = centerY + labelRadius * sin(angle)
                    drawContext.canvas.nativeCanvas.drawText(
                        traitBars[i].label,
                        labelX,
                        labelY + labelTextSize / 3f,
                        paint
                    )
                    // Percentile value below label
                    val valuePaint = android.graphics.Paint(paint).apply {
                        style = android.graphics.Paint.Style.FILL
                        isFakeBoldText = true
                        color = primaryColor.toArgb()
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${traitBars[i].percentile}%",
                        labelX,
                        labelY + labelTextSize * 1.5f,
                        valuePaint
                    )
                }
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = primaryColor, label = "MP percentile")
                LegendDot(color = gridColor, label = "25 / 50 / 75 / 100%")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
