package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.robiul.hisabi.R
import me.robiul.hisabi.data.local.LoanStatus
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.ui.components.BankCardVisual
import me.robiul.hisabi.ui.components.BudgetStatusCard
import me.robiul.hisabi.ui.components.IncomeExpenseVicoChart
import me.robiul.hisabi.ui.components.TransactionRow
import me.robiul.hisabi.ui.theme.ChampagneGold
import me.robiul.hisabi.ui.theme.InterFontFamily
import me.robiul.hisabi.ui.theme.SemanticExpense
import me.robiul.hisabi.ui.theme.SemanticIncome
import me.robiul.hisabi.ui.theme.SemanticTransfer
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.ui.viewmodel.ReportingPeriod
import me.robiul.hisabi.util.Formatters
import kotlin.math.absoluteValue

@Composable
fun WalletHomeScreen(
    state: LedgerUiState,
    onToggleHideBalances: () -> Unit,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenAddTransaction: (TransactionType) -> Unit,
    onOpenAddAccount: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAccountDetail: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenLoans: () -> Unit,
    onClickTransaction: (TransactionEntity) -> Unit,
) {
    val currency = state.preferences.currencySymbol
    val hideBalances = state.preferences.hideBalances
    val datePattern = state.preferences.dateFormatPattern
    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val categoryMap = remember(state.categories) { state.categories.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Minimal Luxury Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Hisabi",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${state.activeAccounts.size} active cards • Offline Vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        IconButton(
                            onClick = onToggleHideBalances,
                            modifier = Modifier.testTag("home_toggle_hide_balance"),
                        ) {
                            Icon(
                                imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle balance visibility",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // Prominent Horizontally Swipeable Virtual Debit Card Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "My Cards & Wallets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Row {
                        TextButton(onClick = onOpenAddAccount) { Text("+ Add", maxLines = 1, softWrap = false) }
                        if (state.accounts.isNotEmpty()) {
                            TextButton(onClick = onOpenAccounts) { Text("Manage", maxLines = 1, softWrap = false) }
                        }
                    }
                }

                if (state.activeAccounts.isEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        EmptyWalletCard(onAddAccount = onOpenAddAccount)
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { state.activeAccounts.size })
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        val account = state.activeAccounts[page]
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = 1f - (pageOffset.coerceIn(0f, 1f) * 0.04f)
                        val alpha = 1f - (pageOffset.coerceIn(0f, 1f) * 0.22f)
                        BankCardVisual(
                            account = account,
                            currencySymbol = currency,
                            hideBalances = hideBalances,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                            onClick = { onOpenAccountDetail(account.id) },
                        )
                    }

                    if (state.activeAccounts.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(state.activeAccounts.size) { index ->
                                val selected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(5.dp)
                                        .width(if (selected) 20.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Total Portfolio Balance Summary Card
        item {
            PortfolioSummaryCard(
                totalAvailableCents = state.totalLiquidAccountBalanceCents,
                netWorthCents = state.netWorthCents,
                creditDebtCents = state.totalCreditDebtCents,
                savingsGoalsCents = state.totalSavingsGoalsCents,
                currencySymbol = currency,
                hideBalances = hideBalances,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // Receivables & Payables Overview Card (separate from Income/Expense)
        item {
            val overdueCount = remember(state.loans) {
                state.loans.count { it.status == LoanStatus.OVERDUE }
            }
            LoansDashboardCard(
                receivablesCents = state.loanSummary.outstandingReceivablesCents,
                payablesCents = state.loanSummary.outstandingPayablesCents,
                overdueCount = overdueCount,
                currencySymbol = currency,
                hideBalances = hideBalances,
                onClick = onOpenLoans,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // Reporting Period Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportingPeriod.entries.forEach { period ->
                    val selected = state.dashboardPeriod == period
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.testTag("period_chip_${period.name.lowercase()}"),
                    )
                }
            }
        }

        // Compact Income, Expense & Net Cash Flow Summary
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CashflowSummaryCard(
                        title = "Income",
                        amountCents = state.periodIncomeCents,
                        currencySymbol = currency,
                        hideBalances = hideBalances,
                        accentColor = SemanticIncome,
                        icon = Icons.Rounded.ArrowDownward,
                        modifier = Modifier.weight(1f),
                    )
                    CashflowSummaryCard(
                        title = "Expenses",
                        amountCents = state.periodExpenseCents,
                        currencySymbol = currency,
                        hideBalances = hideBalances,
                        accentColor = SemanticExpense,
                        icon = Icons.Rounded.ArrowUpward,
                        modifier = Modifier.weight(1f),
                    )
                }

                NetCashflowBanner(
                    periodLabel = state.dashboardPeriod.label,
                    netCents = state.periodNetCashflowCents,
                    currencySymbol = currency,
                    hideBalances = hideBalances,
                )
            }
        }

        // Refined Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickActionButton(
                        label = "Expense",
                        icon = Icons.Rounded.ArrowUpward,
                        tint = SemanticExpense,
                        onClick = { onOpenAddTransaction(TransactionType.EXPENSE) },
                        testTag = "quick_add_expense",
                    )
                    QuickActionButton(
                        label = "Income",
                        icon = Icons.Rounded.ArrowDownward,
                        tint = SemanticIncome,
                        onClick = { onOpenAddTransaction(TransactionType.INCOME) },
                        testTag = "quick_add_income",
                    )
                    QuickActionButton(
                        label = "Transfer",
                        icon = Icons.Rounded.SwapHoriz,
                        tint = ChampagneGold,
                        onClick = { onOpenAddTransaction(TransactionType.TRANSFER) },
                        testTag = "quick_add_transfer",
                    )
                    QuickActionButton(
                        label = "Budgets",
                        icon = Icons.Rounded.PieChart,
                        tint = ChampagneGold,
                        onClick = onOpenBudgets,
                        testTag = "quick_open_budgets",
                    )
                    QuickActionButton(
                        label = "Goals",
                        icon = Icons.Rounded.Savings,
                        tint = SemanticIncome,
                        onClick = onOpenGoals,
                        testTag = "quick_open_goals",
                    )
                    QuickActionButton(
                        label = "Recurring",
                        icon = Icons.Rounded.Repeat,
                        tint = SemanticTransfer,
                        onClick = onOpenRecurring,
                        testTag = "quick_open_recurring",
                    )
                    QuickActionButton(
                        label = "Loans",
                        icon = Icons.Rounded.Handshake,
                        tint = ChampagneGold,
                        onClick = onOpenLoans,
                        testTag = "quick_open_loans",
                    )
                }
            }
        }

        // Minimal Cashflow Chart
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "${state.dashboardPeriod.label} Cashflow",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IncomeExpenseVicoChart(
                    points = state.dashboardChartPoints,
                    showIncomeSeries = true,
                )
            }
        }

        // Clean Expense Category Breakdown
        if (state.periodCategoryBreakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Spending Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    state.periodCategoryBreakdown.take(5).forEach { cat ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${cat.name} • ${cat.percentInt}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = Formatters.formatMoney(cat.totalCents, currency, hideBalances),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(cat.percentInt.coerceIn(0, 100) / 100f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(cat.colorHex)),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Budget Progress
        if (state.budgetStatuses.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Budget Progress",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        TextButton(onClick = onOpenBudgets) { Text("All Budgets") }
                    }
                    state.budgetStatuses.take(3).forEach { status ->
                        BudgetStatusCard(
                            status = status,
                            currencySymbol = currency,
                            hideBalances = hideBalances,
                        )
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onOpenActivity) { Text("See All") }
            }
        }

        if (state.transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No transactions recorded yet. Tap + to record an expense, income, or transfer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.transactions.take(6), key = { it.id }) { tx ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    TransactionRow(
                        transaction = tx,
                        accountMap = accountMap,
                        categoryMap = categoryMap,
                        currencySymbol = currency,
                        datePattern = datePattern,
                        hideBalances = hideBalances,
                        onClick = { onClickTransaction(tx) },
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PortfolioSummaryCard(
    totalAvailableCents: Long,
    netWorthCents: Long,
    creditDebtCents: Long,
    savingsGoalsCents: Long,
    currencySymbol: String,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TOTAL AVAILABLE BALANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = Formatters.formatMoney(totalAvailableCents, currencySymbol, hideBalances),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PortfolioStat(
                label = "Net Worth",
                value = Formatters.formatMoney(netWorthCents, currencySymbol, hideBalances),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            PortfolioStat(
                label = "Credit Payable",
                value = Formatters.formatMoney(creditDebtCents, currencySymbol, hideBalances),
                valueColor = if (creditDebtCents > 0L) SemanticExpense else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            PortfolioStat(
                label = "Goals Saved",
                value = Formatters.formatMoney(savingsGoalsCents, currencySymbol, hideBalances),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PortfolioStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            fontSize = 13.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoansDashboardCard(
    receivablesCents: Long,
    payablesCents: Long,
    overdueCount: Int,
    currencySymbol: String,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("home_loans_summary_card"),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = "Loans and Borrowings",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Loans & Borrowings",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (overdueCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SemanticExpense.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "$overdueCount Overdue",
                                style = MaterialTheme.typography.labelSmall,
                                color = SemanticExpense,
                            )
                        }
                    }
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneGold,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Receivables (Lent)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatters.formatMoney(receivablesCents, currencySymbol, hideBalances),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (receivablesCents > 0L) SemanticIncome else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Payables (Borrowed)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatters.formatMoney(payablesCents, currencySymbol, hideBalances),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (payablesCents > 0L) SemanticExpense else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun CashflowSummaryCard(
    title: String,
    amountCents: Long,
    currencySymbol: String,
    hideBalances: Boolean,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(15.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = Formatters.formatMoney(amountCents, currencySymbol, hideBalances),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NetCashflowBanner(
    periodLabel: String,
    netCents: Long,
    currencySymbol: String,
    hideBalances: Boolean,
) {
    val positive = netCents >= 0L
    val color = if (positive) SemanticIncome else SemanticExpense
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Net Cash Flow ($periodLabel)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Text(
            text = Formatters.formatMoney(netCents, currencySymbol, hideBalances, showPlus = positive),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .width(86.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyWalletCard(onAddAccount: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_wallet),
            contentDescription = "Luxury matte-black card illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(18.dp)),
        )
        Text(
            text = "Add your first Cash, Bank, or MFS card",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Create bKash, Nagad, Rocket, Upay, Bank, or Cash cards to track your balances privately offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onAddAccount,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("empty_add_account_button"),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Account / Card", fontWeight = FontWeight.SemiBold)
        }
    }
}
