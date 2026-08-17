package com.goveye.app.ui.components.charts

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.stats.AttendanceTrend
import com.goveye.app.domain.stats.MonthlyVotingData
import com.goveye.app.domain.stats.RebellionTrend
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme

// Consistent colors — Aye = teal, No = orange, No-vote = gray
private val AyeColor = Color(0xFF00796B)
private val NoColor = Color(0xFFE65100)
private val NoVoteColor = Color(0xFF9E9E9E)
private val LineColor = Color(0xFF1976D2)

/**
 * Stacked bar chart showing monthly voting pattern (Aye/No/NoVote).
 * Uses consistent teal/orange/gray colors with a legend.
 */
@Composable
fun VotingBarChart(
    data: List<MonthlyVotingData>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data.map { it.ayeCount })
                series(data.map { it.noCount })
                series(data.map { it.noVoteCount })
            }
        }
    }

    val monthLabels = remember(data) {
        data.map { it.monthLabel }
    }

    val bottomAxisFormatter = remember(monthLabels) {
        CartesianValueFormatter { _, value, _ ->
            monthLabels.getOrElse(value.toInt()) { "" }
        }
    }

    Column(modifier = modifier) {
        ProvideVicoTheme(theme = rememberM3VicoTheme()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(fill = Fill(AyeColor), thickness = 12.dp),
                            rememberLineComponent(fill = Fill(NoColor), thickness = 12.dp),
                            rememberLineComponent(fill = Fill(NoVoteColor), thickness = 12.dp),
                        ),
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisFormatter,
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
            )
        }
        // Legend
        ChartLegend(
            items = listOf(
                "Ayes" to AyeColor,
                "Noes" to NoColor,
                "No vote" to NoVoteColor,
            ),
        )
    }
}

/**
 * Line chart showing monthly attendance rate.
 * Y-axis shows percentage values, X-axis shows month labels.
 */
@Composable
fun AttendanceLineChart(
    data: List<AttendanceTrend>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                series(data.map { (it.attendanceRate * 100).toInt() })
            }
        }
    }

    val monthLabels = remember(data) {
        data.map { it.monthLabel }
    }

    val bottomAxisFormatter = remember(monthLabels) {
        CartesianValueFormatter { _, value, _ ->
            monthLabels.getOrElse(value.toInt()) { "" }
        }
    }

    val startAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt()}%" }
    }

    ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(LineColor)),
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisFormatter,
                ),
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(8.dp),
        )
    }
}

/**
 * Line chart showing monthly rebellion rate.
 * Y-axis shows percentage values, X-axis shows month labels.
 */
@Composable
fun RebellionLineChart(
    data: List<RebellionTrend>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                series(data.map { (it.rebellionRate * 100).toInt() })
            }
        }
    }

    val monthLabels = remember(data) {
        data.map { it.monthLabel }
    }

    val bottomAxisFormatter = remember(monthLabels) {
        CartesianValueFormatter { _, value, _ ->
            monthLabels.getOrElse(value.toInt()) { "" }
        }
    }

    val startAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt()}%" }
    }

    ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(NoColor)),
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisFormatter,
                ),
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(8.dp),
        )
    }
}

@Composable
private fun ChartLegend(items: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
