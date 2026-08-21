package com.goveye.app.ui.components.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.stats.TraitBar
import com.goveye.app.ui.theme.padding
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TraitRadarChart(traitBars: List<TraitBar>, modifier: Modifier = Modifier) {
    if (traitBars.isEmpty()) return

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().padding(horizontal = MaterialTheme.padding.medium)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium)
        ) {
            Text(
                text = "Trait Percentiles",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Radar chart canvas
            val chartSize = 200.dp
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartSize)
                    .padding(8.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = minOf(center.x, center.y) * 0.8f
                val axes = traitBars.size

                // Draw grid rings
                for (ring in 1..4) {
                    val ringRadius = radius * ring / 4f
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 1f)
                    )
                }

                // Draw axes
                for (i in 0 until axes) {
                    val angle = (Math.PI * 2 * i / axes - Math.PI / 2).toFloat()
                    val end = Offset(
                        center.x + cos(angle) * radius,
                        center.y + sin(angle) * radius
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = center,
                        end = end,
                        strokeWidth = 1f
                    )
                }

                // Draw data polygon
                val points = traitBars.mapIndexed { i, bar ->
                    val angle = (Math.PI * 2 * i / axes - Math.PI / 2).toFloat()
                    val r = radius * bar.percentile / 100f
                    Offset(
                        center.x + cos(angle) * r,
                        center.y + sin(angle) * r
                    )
                }

                // Fill polygon
                if (points.size >= 3) {
                    for (i in 0 until points.size) {
                        val next = points[(i + 1) % points.size]
                        drawLine(
                            color = primaryColor,
                            start = points[i],
                            end = next,
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // Trait labels with percentile values
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                traitBars.forEach { bar ->
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(
                            text = "${bar.percentile}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = bar.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
