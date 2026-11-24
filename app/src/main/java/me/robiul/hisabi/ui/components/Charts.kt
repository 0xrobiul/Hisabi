package me.robiul.hisabi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.ui.theme.AccentGreen
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.viewmodel.BudgetStatus
import me.robiul.hisabi.ui.viewmodel.CategorySpendSummary
import me.robiul.hisabi.ui.viewmodel.PeriodChartPoint
import me.robiul.hisabi.util.Formatters
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

@Composable
fun IncomeExpenseVicoChart(
    points: List<PeriodChartPoint>,
    showIncomeSeries: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(points) { points.map { it.label } }

    LaunchedEffect(points, showIncomeSeries) {
        if (points.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    if (showIncomeSeries) {
                        series(points.map { (it.incomeCents / 100L).toInt().coerceAtLeast(0) })
                    }
                    series(points.map { (it.expenseCents / 100L).toInt().coerceAtLeast(0) })
                }
            }
        }
    }

    if (points.isEmpty() || points.all { it.incomeCents == 0L && it.expenseCents == 0L }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No cashflow activity in this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val incomeColumn = rememberLineComponent(
        fill = fill(AccentGreen),
        thickness = 7.dp,
        shape = CorneredShape.rounded(topLeftPercent = 45, topRightPercent = 45),
    )
    val expenseColumn = rememberLineComponent(
        fill = fill(SemanticExpense),
        thickness = 7.dp,
        shape = CorneredShape.rounded(topLeftPercent = 45, topRightPercent = 45),
    )

    val columns = if (showIncomeSeries) listOf(incomeColumn, expenseColumn) else listOf(expenseColumn)
    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val idx = value.toInt()
            labels.getOrNull(idx) ?: ""
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomFormatter,
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}

@Composable
fun CategoryDonutChart(
    breakdown: List<CategorySpendSummary>,
    totalExpenseCents: Long,
    currencySymbol: String,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    var animTarget by remember(breakdown) { mutableFloatStateOf(0f) }
    LaunchedEffect(breakdown) { animTarget = 1f }
    val sweepProgress by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 650),
        label = "donut_sweep",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier.size(196.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokePx = 18.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )

            if (breakdown.isNotEmpty() && totalExpenseCents > 0L) {
                var startAngle = -90f
                val gapDegrees = if (breakdown.size > 1) 3.5f else 0f

                breakdown.forEach { item ->
                    val fraction = ((item.totalCents * 1000L) / totalExpenseCents).toFloat() / 1000f
                    val rawSweep = (fraction * 360f) * sweepProgress
                    val drawSweep = (rawSweep - gapDegrees).coerceAtLeast(1.5f)
                    drawArc(
                        color = Color(item.colorHex),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    startAngle += rawSweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL SPENT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hideBalances) "$currencySymbol••••" else Formatters.formatCompactMoney(totalExpenseCents, currencySymbol),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun BudgetStatusCard(
    status: BudgetStatus,
    currencySymbol: String,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    val pct = status.utilizationPercent
    val barColor = when {
        pct >= 100 -> SemanticExpense
        pct >= status.budget.alertThresholdPercent -> ChampagneGold
        else -> AccentGreen
    }
    val fillRatio = pct.coerceIn(0, 100) / 100f
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Column {
                    Text(
                        text = status.budget.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${status.budget.periodType.label} • ${status.categoryName ?: "All Expenses"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "$pct%",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = barColor,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillRatio)
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Spent ${Formatters.formatMoney(status.spentCents, currencySymbol, hideBalances)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val remText = if (status.remainingCents >= 0L) {
                "Left ${Formatters.formatMoney(status.remainingCents, currencySymbol, hideBalances)}"
            } else {
                "Over ${Formatters.formatMoney(-status.remainingCents, currencySymbol, hideBalances)}"
            }
            Text(
                text = "$remText / ${Formatters.formatCompactMoney(status.budget.limitCents, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (status.remainingCents < 0L) SemanticExpense else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
