package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.ui.components.CategoryDonutChart
import me.robiul.hisabi.ui.components.IncomeExpenseVicoChart
import me.robiul.hisabi.ui.theme.AccentGreen
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.theme.SemanticTransfer
import me.robiul.hisabi.ui.viewmodel.AnalyticsGranularity
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.Formatters

@Composable
fun InsightsScreen(
    state: LedgerUiState,
    onSelectGranularity: (AnalyticsGranularity) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenLoans: () -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances

    val periodTitle = when (state.insightsGranularity) {
        AnalyticsGranularity.DAILY -> "Last 7 Days"
        AnalyticsGranularity.WEEKLY -> Formatters.formatBillingMonthLabel(state.insightsMonth.year, state.insightsMonth.monthValue)
        AnalyticsGranularity.MONTHLY -> Formatters.formatBillingMonthLabel(state.insightsMonth.year, state.insightsMonth.monthValue)
        AnalyticsGranularity.YEARLY -> "Year ${state.insightsMonth.year}"
    }

    val diffExpense = state.insightsExpenseCents - state.insightsPrevExpenseCents
    val changePct = if (state.insightsPrevExpenseCents > 0L) {
        ((diffExpense * 100L) / state.insightsPrevExpenseCents).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column {
                Text("Insights & Reports", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Income vs Expense analytics, savings & exports",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnalyticsGranularity.entries.forEach { gran ->
                    FilterChip(
                        selected = state.insightsGranularity == gran,
                        onClick = { onSelectGranularity(gran) },
                        label = { Text(gran.label) },
                        modifier = Modifier.testTag("insights_granularity_${gran.name.lowercase()}"),
                    )
                }
            }
        }

        if (state.insightsGranularity != AnalyticsGranularity.DAILY) {
            item {
                val navShape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(navShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, navShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val delta = if (state.insightsGranularity == AnalyticsGranularity.YEARLY) -12L else -1L
                    IconButton(onClick = { onShiftMonth(delta) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous period")
                    }
                    Text(text = periodTitle, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onShiftMonth(-delta) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next period")
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InsightMetricTile(
                        label = "Total Income",
                        value = Formatters.formatMoney(state.insightsIncomeCents, currency, hideBalances),
                        accent = SemanticIncome,
                        modifier = Modifier.weight(1f),
                    )
                    InsightMetricTile(
                        label = "Total Expense",
                        value = Formatters.formatMoney(state.insightsExpenseCents, currency, hideBalances),
                        accent = SemanticExpense,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InsightMetricTile(
                        label = "Net Cash Flow",
                        value = Formatters.formatMoney(
                            state.insightsNetCashflowCents,
                            currency,
                            hideBalances,
                            showPlus = state.insightsNetCashflowCents > 0L,
                        ),
                        accent = if (state.insightsNetCashflowCents >= 0L) AccentGreen else SemanticExpense,
                        modifier = Modifier.weight(1f),
                    )
                    InsightMetricTile(
                        label = "Avg Daily Spend",
                        value = Formatters.formatMoney(state.insightsAverageDailyExpenseCents, currency, hideBalances),
                        accent = ChampagneGold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InsightMetricTile(
                        label = "Savings Rate",
                        value = "${state.insightsSavingsRatePercent}%",
                        accent = SemanticIncome,
                        modifier = Modifier.weight(1f),
                    )
                    val topCat = state.insightsCategoryBreakdown.firstOrNull()
                    InsightMetricTile(
                        label = "Highest Category",
                        value = topCat?.let { "${it.name} (${it.percentInt}%)" } ?: "None",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (state.insightsPrevExpenseCents > 0L) {
                    val sign = if (changePct > 0) "+" else ""
                    val cmpColor = if (changePct > 0) SemanticExpense else SemanticIncome
                    val cmpShape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cmpShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cmpShape)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "vs. Previous Period Expense",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        Text(
                            text = "$sign$changePct% (${Formatters.formatMoney(diffExpense, currency, hideBalances, showPlus = diffExpense > 0)})",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = cmpColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        item {
            val cardShape = RoundedCornerShape(22.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Income vs Expense",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendDot(color = AccentGreen, label = "Income")
                        LegendDot(color = SemanticExpense, label = "Expense")
                    }
                }
                IncomeExpenseVicoChart(
                    points = state.insightsChartPoints,
                    showIncomeSeries = true,
                )
            }
        }

        item {
            val donutShape = RoundedCornerShape(22.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(donutShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, donutShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryDonutChart(
                    breakdown = state.insightsCategoryBreakdown,
                    totalExpenseCents = state.insightsExpenseCents,
                    currencySymbol = currency,
                    hideBalances = hideBalances,
                )
                if (state.insightsCategoryBreakdown.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.insightsCategoryBreakdown.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex)),
                                    )
                                    Text(
                                        text = "${cat.name} (${cat.count})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = "${cat.percentInt}% • ${Formatters.formatMoney(cat.totalCents, currency, hideBalances)}",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Planning Tools", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlanningTile(
                        title = "Budgets",
                        subtitle = "${state.budgetStatuses.size} active",
                        icon = Icons.Rounded.PieChart,
                        accent = ChampagneGold,
                        onClick = onOpenBudgets,
                        modifier = Modifier.weight(1f).testTag("insights_budgets_tile"),
                    )
                    PlanningTile(
                        title = "Savings Goals",
                        subtitle = "${state.goalStatuses.size} goals",
                        icon = Icons.Rounded.Savings,
                        accent = AccentGreen,
                        onClick = onOpenGoals,
                        modifier = Modifier.weight(1f).testTag("insights_goals_tile"),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlanningTile(
                        title = "Recurring",
                        subtitle = "${state.recurringRules.size} rules",
                        icon = Icons.Rounded.Repeat,
                        accent = SemanticTransfer,
                        onClick = onOpenRecurring,
                        modifier = Modifier.weight(1f).testTag("insights_recurring_tile"),
                    )
                    PlanningTile(
                        title = "Categories",
                        subtitle = "${state.categories.size} total",
                        icon = Icons.Rounded.Category,
                        accent = ChampagneGold,
                        onClick = onOpenCategories,
                        modifier = Modifier.weight(1f).testTag("insights_categories_tile"),
                    )
                }
                val activeLoanCount = state.loans.count { it.remainingCents > 0L }
                PlanningTile(
                    title = "Loans & Borrowings",
                    subtitle = "$activeLoanCount open receivables & payables",
                    icon = Icons.Rounded.Handshake,
                    accent = ChampagneGold,
                    onClick = onOpenLoans,
                    modifier = Modifier.fillMaxWidth().testTag("insights_loans_tile"),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("insights_export_pdf_button"),
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("insights_export_csv_button"),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun InsightMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlanningTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
