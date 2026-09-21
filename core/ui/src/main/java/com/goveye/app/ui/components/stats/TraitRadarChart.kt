package com.goveye.app.ui.components.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.goveye.app.ui.components.cardSurfaceColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Radar chart for trait visualization.
 * 5 axes: Loyalty, Participation, Questions, Speeches, Finance.
 * The polygon uses [traitDisplayPercent] — mpValue for rate-based traits
 * (Loyalty, Participation), percentile for count-based traits — so the
 * shape reflects the displayed percentages.
 *
 * Grid lines and axes use [onSurface] at a theme-aware alpha so they
 * are visible in both light and dark themes. Axis labels (trait name +
 * percentage) are drawn well outside the chart area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraitRadarChart(traitBars: List<TraitBar>, modifier: Modifier = Modifier) {
    if (traitBars.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val percentileColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    var showInfoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How Performance Breakdown works",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Each axis shows how this MP compares to their peers, scored 0–100%.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PerformanceBreakdownInfoItem(
                    title = "Loyalty",
                    description = "How often the MP votes with their party. 100% means they never rebel."
                )
                PerformanceBreakdownInfoItem(
                    title = "Participation",
                    description = "The share of votes the MP has taken part in since joining Parliament."
                )
                PerformanceBreakdownInfoItem(
                    title = "Questions",
                    description = "How many questions the MP asks compared to the average MP. " +
                        "100% means they ask about the same number."
                )
                PerformanceBreakdownInfoItem(
                    title = "Speeches",
                    description = "How many speeches the MP gives compared to the average MP."
                )
                PerformanceBreakdownInfoItem(
                    title = "Finance",
                    description = "How many financial interests and expenses the MP has declared " +
                        "compared to the average MP."
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardSurfaceColor()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Performance Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "How this MP compares to their peers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "How Performance Breakdown works",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Axis label sizing — larger than the chart's own scale so the
                // trait names and percentages stay legible at arm's length.
                val labelTextSize = with(density) { 14.dp.toPx() }
                val valueTextSize = with(density) { 15.dp.toPx() }
                // How far outside the polygon the labels sit.
                val labelOffsetPx = with(density) { 30.dp.toPx() }
                val paint = remember(labelColor, labelTextSize) {
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = labelTextSize
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                }
                val valuePaint = remember(percentileColor, valueTextSize) {
                    android.graphics.Paint().apply {
                        color = percentileColor.toArgb()
                        textSize = valueTextSize
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        style = android.graphics.Paint.Style.FILL
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        // Smaller polygon leaves room for the larger, further-out labels
                        val radius = min(centerX, centerY) * 0.44f
                        val sides = traitBars.size
                        val angleStep = (2 * PI / sides).toFloat()
                        val startAngle = (-PI / 2).toFloat()

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

                        for (i in 0 until sides) {
                            val angle = startAngle + i * angleStep
                            val x = centerX + radius * cos(angle)
                            val y = centerY + radius * sin(angle)
                            drawLine(gridColor, Offset(centerX, centerY), Offset(x, y), strokeWidth = 1.5f)
                        }

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

                        for (i in traitBars.indices) {
                            val angle = startAngle + i * angleStep
                            val value = traitDisplayPercent(traitBars[i]) / 100f
                            val r = minRadius + (radius - minRadius) * value
                            val dotX = centerX + r * cos(angle)
                            val dotY = centerY + r * sin(angle)
                            drawCircle(primaryColor, 5f, Offset(dotX, dotY))

                            val labelRadius = radius + labelOffsetPx
                            val labelX = centerX + labelRadius * cos(angle)
                            val labelY = centerY + labelRadius * sin(angle)
                            drawContext.canvas.nativeCanvas.drawText(
                                traitBars[i].label,
                                labelX,
                                labelY,
                                paint
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                "${traitDisplayPercent(traitBars[i]).toInt()}%",
                                labelX,
                                labelY + valueTextSize * 1.25f,
                                valuePaint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceBreakdownInfoItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
