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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.goveye.app.ui.components.VoteColors

// Theme-aware vote colors — Aye = teal, No = orange, No-vote = gray
private val AyeColor @Composable get() = VoteColors.aye
private val NoColor @Composable get() = VoteColors.no
private val NoVoteColor @Composable get() = VoteColors.noVote
private val LineColor = Color(0xFF1976D2)

/**
 * Stacked bar chart showing voting pattern (Aye/No/NoVote) in a card.
 * Title and legend are on the same row. Animation is disabled.
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

    val periodLabels = remember(data) {
        data.map { it.monthLabel }
    }

    val bottomAxisFormatter = remember(periodLabels) {
        CartesianValueFormatter { _, value, _ ->
            periodLabels.getOrElse(value.toInt()) { "" }
        }
    }

    ChartCard(modifier = modifier) {
        // Title + legend on same row
        ChartHeaderWithLegend(
            title = "Voting Pattern",
            legendItems = listOf(
                "Ayes" to AyeColor,
                "Noes" to NoColor,
                "No vote" to NoVoteColor,
            ),
        )
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
                animationSpec = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
            )
        }
    }
}

/**
 * Line chart showing attendance rate in a card.
 * Y-axis shows percentage values, X-axis shows period labels.
 * Animation is disabled.
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

    val periodLabels = remember(data) {
        data.map { it.monthLabel }
    }

    val bottomAxisFormatter = remember(periodLabels) {
        CartesianValueFormatter { _, value, _ ->
            periodLabels.getOrElse(value.toInt()) { "" }
        }
    }

    val startAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt()}%" }
    }

    ChartCard(modifier = modifier) {
        ChartHeader(title = "Attendance Rate")
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
                animationSpec = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(8.dp),
            )
        }
    }
}

/**
 * Line chart showing rebellion rate in a card.
 * Y-axis shows percentage values, X-axis shows period labels.
 * Animation is disabled.
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

    val periodLabels = remember(data) {
        data.map { it.monthLabel }
    }

    val bottomAxisFormatter = remember(periodLabels) {
        CartesianValueFormatter { _, value, _ ->
            periodLabels.getOrElse(value.toInt()) { "" }
        }
    }

    val startAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt()}%" }
    }

    ChartCard(modifier = modifier) {
        ChartHeader(title = "Rebellion Trend")
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
                animationSpec = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(8.dp),
            )
        }
    }
}

/**
 * Rounded card wrapper for chart sections.
 */
@Composable
fun ChartCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            content()
        }
    }
}

/**
 * Chart header with title only.
 */
@Composable
fun ChartHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/**
 * Chart header with title and inline legend on the same row.
 */
@Composable
fun ChartHeaderWithLegend(
    title: String,
    legendItems: List<Pair<String, Color>>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            legendItems.forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
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
}
