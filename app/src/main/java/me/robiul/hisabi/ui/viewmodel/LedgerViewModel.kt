package me.robiul.hisabi.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.BudgetEntity
import me.robiul.hisabi.data.local.BudgetPeriod
import me.robiul.hisabi.data.local.CardNetwork
import me.robiul.hisabi.data.local.CategoryEntity
import me.robiul.hisabi.data.local.CategoryType
import me.robiul.hisabi.data.local.LoanRecordEntity
import me.robiul.hisabi.data.local.LoanRepaymentEntity
import me.robiul.hisabi.data.local.LoanStatus
import me.robiul.hisabi.data.local.PreferencesManager
import me.robiul.hisabi.data.local.RecurringFrequency
import me.robiul.hisabi.data.local.RecurringRuleEntity
import me.robiul.hisabi.data.local.SavingsGoalEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.data.local.UserPreferences
import me.robiul.hisabi.data.repository.LedgerRepository
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.LoanCalculations
import me.robiul.hisabi.util.LoanSummaryTotals
import me.robiul.hisabi.util.PdfReportGenerator
import me.robiul.hisabi.util.PinSecurity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

enum class ReportingPeriod(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
}

enum class AnalyticsGranularity(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

data class CategorySpendSummary(
    val categoryId: Long?,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val totalCents: Long,
    val percentInt: Int,
    val count: Int,
)

data class PeriodChartPoint(
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
)

data class BudgetStatus(
    val budget: BudgetEntity,
    val categoryName: String?,
    val spentCents: Long,
    val remainingCents: Long,
    val utilizationPercent: Int,
)

data class GoalStatus(
    val goal: SavingsGoalEntity,
    val remainingCents: Long,
    val progressPercent: Int,
)

data class LoanStatusItem(
    val loan: LoanRecordEntity,
    val status: LoanStatus,
    val remainingCents: Long,
    val progressPercent: Int,
    val repayments: List<LoanRepaymentEntity>,
)

data class LedgerUiState(
    val isLoading: Boolean = true,
    val accounts: List<AccountEntity> = emptyList(),
    val activeAccounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val goalStatuses: List<GoalStatus> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val loans: List<LoanStatusItem> = emptyList(),
    val loanSummary: LoanSummaryTotals = LoanSummaryTotals(),
    val preferences: UserPreferences = UserPreferences(),

    // Account & Net Wealth Balances (strict integer paisa)
    val totalLiquidAccountBalanceCents: Long = 0L,
    val totalCreditDebtCents: Long = 0L,
    val totalSavingsGoalsCents: Long = 0L,
    val netWorthCents: Long = 0L,

    // Dashboard Period Stats
    val dashboardPeriod: ReportingPeriod = ReportingPeriod.THIS_MONTH,
    val periodIncomeCents: Long = 0L,
    val periodExpenseCents: Long = 0L,
    val periodNetCashflowCents: Long = 0L,
    val periodCategoryBreakdown: List<CategorySpendSummary> = emptyList(),
    val dashboardChartPoints: List<PeriodChartPoint> = emptyList(),

    // Insights Deep Analytics
    val insightsGranularity: AnalyticsGranularity = AnalyticsGranularity.MONTHLY,
    val insightsMonth: YearMonth = YearMonth.now(),
    val insightsIncomeCents: Long = 0L,
    val insightsExpenseCents: Long = 0L,
    val insightsNetCashflowCents: Long = 0L,
    val insightsPrevExpenseCents: Long = 0L,
    val insightsAverageDailyExpenseCents: Long = 0L,
    val insightsSavingsRatePercent: Int = 0,
    val insightsChartPoints: List<PeriodChartPoint> = emptyList(),
    val insightsCategoryBreakdown: List<CategorySpendSummary> = emptyList(),
)

private data class CoreDataBundle(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<SavingsGoalEntity>,
    val rules: List<RecurringRuleEntity>,
    val loans: List<LoanRecordEntity> = emptyList(),
    val loanRepayments: List<LoanRepaymentEntity> = emptyList(),
)

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val prefsManager: PreferencesManager,
) : ViewModel() {

    private val dashboardPeriodFlow = MutableStateFlow(ReportingPeriod.THIS_MONTH)
    private val insightsGranularityFlow = MutableStateFlow(AnalyticsGranularity.MONTHLY)
    private val insightsMonthFlow = MutableStateFlow(YearMonth.now())

    // Runtime-only lock state (never persisted/restored across process death).
    // Always starts false so a configured PIN requires verification on every launch.
    var isSessionUnlocked by mutableStateOf(false)
        private set

    fun unlockSession() {
        isSessionUnlocked = true
    }

    fun lockSession() {
        isSessionUnlocked = false
    }

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
            repository.processDueRecurringRules(System.currentTimeMillis())
            repository.checkLoanDueReminders()
        }
    }

    private val coreDataFlow = combine(
        repository.allAccounts,
        repository.allCategories,
        repository.allTransactions,
        repository.allBudgets,
        repository.allSavingsGoals,
    ) { accounts, categories, txs, budgets, goals ->
        CoreDataBundle(accounts, categories, txs, budgets, goals, emptyList())
    }.combine(repository.allRecurringRules) { bundle, rules ->
        bundle.copy(rules = rules)
    }.combine(repository.allLoans) { bundle, loans ->
        bundle.copy(loans = loans)
    }.combine(repository.allLoanRepayments) { bundle, repayments ->
        bundle.copy(loanRepayments = repayments)
    }

    val uiState: StateFlow<LedgerUiState> = combine(
        coreDataFlow,
        prefsManager.preferencesFlow,
        dashboardPeriodFlow,
        insightsGranularityFlow,
        insightsMonthFlow,
    ) { bundle, prefs, dashPeriod, granularity, insightsYm ->
        val accounts = bundle.accounts
        val activeAccounts = accounts.filter { !it.isArchived }
        val categories = bundle.categories
        val categoryMap = categories.associateBy { it.id }
        val txs = bundle.transactions
        val budgets = bundle.budgets
        val goals = bundle.goals
        val rules = bundle.rules

        // Strict integer balances
        val totalLiquid = activeAccounts
            .filter { it.accountType != AccountType.CREDIT_CARD }
            .sumOf { it.currentBalanceCents }
        val totalCreditDebt = activeAccounts
            .filter { it.accountType == AccountType.CREDIT_CARD }
            .sumOf { it.currentBalanceCents }
        val totalGoalsSaved = goals.sumOf { it.savedAmountCents }
        val netWorth = totalLiquid + totalGoalsSaved - totalCreditDebt

        // Dashboard Period Bounds
        val nowDate = LocalDate.now()
        val (dashStart, dashEnd) = periodBounds(dashPeriod, nowDate, prefs.firstDayOfMonth)
        val dashTxs = txs.filter { it.timestamp in dashStart until dashEnd }
        val dashIncome = dashTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
        val dashExpense = dashTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents } +
            dashTxs.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeCents }
        val dashNet = dashIncome - dashExpense
        val dashBreakdown = buildCategoryBreakdown(dashTxs, categoryMap)
        val dashChart = buildDailyPoints(dashTxs, dashStart, dashEnd)

        // Budgets
        val budgetStatuses = budgets.filter { it.isActive }.map { b ->
            val (bStart, bEnd) = when (b.periodType) {
                BudgetPeriod.MONTHLY -> Formatters.billingMonthBounds(nowDate.year, nowDate.monthValue, prefs.firstDayOfMonth)
                BudgetPeriod.WEEKLY -> Formatters.currentWeekBounds()
                BudgetPeriod.CUSTOM -> b.startDateMs to b.endDateMs
            }
            val spent = txs.filter { tx ->
                tx.type == TransactionType.EXPENSE &&
                    tx.timestamp in bStart until bEnd &&
                    (b.categoryId == null || tx.categoryId == b.categoryId)
            }.sumOf { it.amountCents + it.feeCents }
            val remaining = b.limitCents - spent
            val util = if (b.limitCents > 0L) ((spent * 100L) / b.limitCents).toInt() else 0
            BudgetStatus(
                budget = b,
                categoryName = b.categoryId?.let { categoryMap[it]?.name },
                spentCents = spent,
                remainingCents = remaining,
                utilizationPercent = util,
            )
        }

        // Goals
        val goalStatuses = goals.map { g ->
            val rem = (g.targetAmountCents - g.savedAmountCents).coerceAtLeast(0L)
            val pct = if (g.targetAmountCents > 0L) {
                ((g.savedAmountCents * 100L) / g.targetAmountCents).toInt().coerceIn(0, 100)
            } else 0
            GoalStatus(goal = g, remainingCents = rem, progressPercent = pct)
        }

        // Loans & Borrowings (strictly separate Receivables & Payables)
        val repaymentsByLoan = bundle.loanRepayments.groupBy { it.loanId }
        val loanStatusItems = bundle.loans.map { loan ->
            val loanReps = repaymentsByLoan[loan.id].orEmpty()
            val totalRepaid = if (loanReps.isNotEmpty()) loanReps.sumOf { it.amountCents } else loan.repaidAmountCents
            val effectiveLoan = if (totalRepaid != loan.repaidAmountCents) {
                loan.copy(repaidAmountCents = totalRepaid)
            } else {
                loan
            }
            val remaining = LoanCalculations.computeRemainingCents(effectiveLoan.principalAmountCents, totalRepaid)
            val status = LoanCalculations.computeStatus(
                principalCents = effectiveLoan.principalAmountCents,
                repaidCents = totalRepaid,
                dueDateMs = effectiveLoan.dueDateMs,
                today = nowDate,
            )
            val progress = LoanCalculations.computeProgressPercent(effectiveLoan.principalAmountCents, totalRepaid)
            LoanStatusItem(
                loan = effectiveLoan,
                status = status,
                remainingCents = remaining,
                progressPercent = progress,
                repayments = loanReps,
            )
        }
        val loanSummary = LoanCalculations.computePortfolioSummary(loanStatusItems.map { it.loan })

        // Insights Deep Analytics
        val (inStart, inEnd, prevStart, prevEnd) = insightsBounds(granularity, insightsYm, nowDate, prefs.firstDayOfMonth)
        val inTxs = txs.filter { it.timestamp in inStart until inEnd }
        val prevTxs = txs.filter { it.timestamp in prevStart until prevEnd }

        val inIncome = inTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
        val inExpense = inTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents } +
            inTxs.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeCents }
        val prevExpense = prevTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents } +
            prevTxs.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeCents }

        val daysInRange = ChronoUnit.DAYS.between(
            Formatters.toLocalDate(inStart),
            Formatters.toLocalDate((inEnd - 1L).coerceAtLeast(inStart)).plusDays(1),
        ).coerceAtLeast(1L)
        val avgDailyExpense = inExpense / daysInRange
        val savingsRate = if (inIncome > 0L) {
            (((inIncome - inExpense) * 100L) / inIncome).toInt().coerceIn(-999, 100)
        } else 0

        val insightsChart = when (granularity) {
            AnalyticsGranularity.DAILY -> buildDailyPoints(inTxs, inStart, inEnd)
            AnalyticsGranularity.WEEKLY -> buildWeeklyPoints(inTxs, insightsYm)
            AnalyticsGranularity.MONTHLY -> buildDailyPoints(inTxs, inStart, inEnd)
            AnalyticsGranularity.YEARLY -> buildMonthlyPoints(txs, insightsYm.year, prefs.firstDayOfMonth)
        }

        LedgerUiState(
            isLoading = false,
            accounts = accounts,
            activeAccounts = activeAccounts,
            categories = categories,
            expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
            incomeCategories = categories.filter { it.type == CategoryType.INCOME },
            transactions = txs,
            budgets = budgets,
            budgetStatuses = budgetStatuses,
            savingsGoals = goals,
            goalStatuses = goalStatuses,
            recurringRules = rules,
            loans = loanStatusItems,
            loanSummary = loanSummary,
            preferences = prefs,
            totalLiquidAccountBalanceCents = totalLiquid,
            totalCreditDebtCents = totalCreditDebt,
            totalSavingsGoalsCents = totalGoalsSaved,
            netWorthCents = netWorth,
            dashboardPeriod = dashPeriod,
            periodIncomeCents = dashIncome,
            periodExpenseCents = dashExpense,
            periodNetCashflowCents = dashNet,
            periodCategoryBreakdown = dashBreakdown,
            dashboardChartPoints = dashChart,
            insightsGranularity = granularity,
            insightsMonth = insightsYm,
            insightsIncomeCents = inIncome,
            insightsExpenseCents = inExpense,
            insightsNetCashflowCents = inIncome - inExpense,
            insightsPrevExpenseCents = prevExpense,
            insightsAverageDailyExpenseCents = avgDailyExpense,
            insightsSavingsRatePercent = savingsRate,
            insightsChartPoints = insightsChart,
            insightsCategoryBreakdown = buildCategoryBreakdown(inTxs, categoryMap),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerUiState(),
    )

    private fun periodBounds(
        period: ReportingPeriod,
        now: LocalDate,
        firstDayOfMonth: Int,
    ): Pair<Long, Long> = when (period) {
        ReportingPeriod.THIS_WEEK -> Formatters.currentWeekBounds()
        ReportingPeriod.THIS_MONTH -> Formatters.billingMonthBounds(now.year, now.monthValue, firstDayOfMonth)
        ReportingPeriod.LAST_MONTH -> {
            val prev = YearMonth.from(now).minusMonths(1)
            Formatters.billingMonthBounds(prev.year, prev.monthValue, firstDayOfMonth)
        }
        ReportingPeriod.THIS_YEAR -> Formatters.currentYearBounds(now.year)
    }

    private data class FourLongs(val a: Long, val b: Long, val c: Long, val d: Long)

    private fun insightsBounds(
        granularity: AnalyticsGranularity,
        ym: YearMonth,
        now: LocalDate,
        firstDayOfMonth: Int,
    ): FourLongs = when (granularity) {
        AnalyticsGranularity.DAILY -> {
            val zone = ZoneId.systemDefault()
            val start = now.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val prevStart = now.minusDays(13).atStartOfDay(zone).toInstant().toEpochMilli()
            FourLongs(start, end, prevStart, start)
        }
        AnalyticsGranularity.WEEKLY -> {
            val zone = ZoneId.systemDefault()
            val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val prevYm = ym.minusMonths(1)
            val prevStart = prevYm.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            FourLongs(start, end, prevStart, start)
        }
        AnalyticsGranularity.MONTHLY -> {
            val cur = Formatters.billingMonthBounds(ym.year, ym.monthValue, firstDayOfMonth)
            val prevYm = ym.minusMonths(1)
            val prev = Formatters.billingMonthBounds(prevYm.year, prevYm.monthValue, firstDayOfMonth)
            FourLongs(cur.first, cur.second, prev.first, prev.second)
        }
        AnalyticsGranularity.YEARLY -> {
            val cur = Formatters.currentYearBounds(ym.year)
            val prev = Formatters.currentYearBounds(ym.year - 1)
            FourLongs(cur.first, cur.second, prev.first, prev.second)
        }
    }

    private fun buildCategoryBreakdown(
        txs: List<TransactionEntity>,
        categoryMap: Map<Long, CategoryEntity>,
    ): List<CategorySpendSummary> {
        val expenses = txs.filter { it.type == TransactionType.EXPENSE }
        val total = expenses.sumOf { it.amountCents + it.feeCents }.coerceAtLeast(1L)
        return expenses
            .groupBy { it.categoryId ?: -1L }
            .map { (catId, group) ->
                val cat = categoryMap[catId]
                val sum = group.sumOf { it.amountCents + it.feeCents }
                CategorySpendSummary(
                    categoryId = if (catId > 0L) catId else null,
                    name = cat?.name ?: group.first().categoryNameSnapshot.ifBlank { "Others" },
                    iconKey = cat?.iconKey ?: "others",
                    colorHex = cat?.colorHex ?: 0xFF94A3B8,
                    totalCents = sum,
                    percentInt = ((sum * 100L) / total).toInt().coerceIn(0, 100),
                    count = group.size,
                )
            }
            .sortedByDescending { it.totalCents }
    }

    private fun buildDailyPoints(
        txs: List<TransactionEntity>,
        startMs: Long,
        endMs: Long,
    ): List<PeriodChartPoint> {
        val startDate = Formatters.toLocalDate(startMs)
        val endDate = Formatters.toLocalDate((endMs - 1L).coerceAtLeast(startMs))
        val dayCount = ChronoUnit.DAYS.between(startDate, endDate).toInt().coerceIn(0, 31)
        val byDate = txs.groupBy { Formatters.toLocalDate(it.timestamp) }

        return (0..dayCount).map { offset ->
            val d = startDate.plusDays(offset.toLong())
            val dayTxs = byDate[d].orEmpty()
            val inc = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
            val exp = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents } +
                dayTxs.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeCents }
            PeriodChartPoint(
                label = d.dayOfMonth.toString(),
                incomeCents = inc,
                expenseCents = exp,
            )
        }
    }

    private fun buildWeeklyPoints(txs: List<TransactionEntity>, ym: YearMonth): List<PeriodChartPoint> {
        val zone = ZoneId.systemDefault()
        return (1..4).map { week ->
            val startDay = (week - 1) * 7 + 1
            val endDay = if (week == 4) ym.lengthOfMonth() else (week * 7)
            val s = ym.atDay(startDay).atStartOfDay(zone).toInstant().toEpochMilli()
            val e = ym.atDay(endDay).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val wTxs = txs.filter { it.timestamp in s until e }
            PeriodChartPoint(
                label = "W$week",
                incomeCents = wTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = wTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents } +
                    wTxs.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeCents },
            )
        }
    }

    private fun buildMonthlyPoints(
        allTxs: List<TransactionEntity>,
        year: Int,
        firstDayOfMonth: Int,
    ): List<PeriodChartPoint> {
        val fmt = DateTimeFormatter.ofPattern("MMM", Locale.US)
        return (1..12).map { month ->
            val (s, e) = Formatters.billingMonthBounds(year, month, firstDayOfMonth)
            val mTxs = allTxs.filter { it.timestamp in s until e }
            PeriodChartPoint(
                label = YearMonth.of(year, month).format(fmt),
                incomeCents = mTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = mTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents + it.feeCents } +
                    mTxs.filter { it.type == TransactionType.TRANSFER }.sumOf { it.feeCents },
            )
        }
    }

    // --- UI Actions ---
    fun setDashboardPeriod(period: ReportingPeriod) {
        dashboardPeriodFlow.value = period
    }

    fun setInsightsGranularity(granularity: AnalyticsGranularity) {
        insightsGranularityFlow.value = granularity
    }

    fun shiftInsightsMonth(delta: Long) {
        insightsMonthFlow.value = insightsMonthFlow.value.plusMonths(delta)
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.saveAccount(account)
            _events.emit(if (account.id == 0L) "Account created" else "Account updated")
        }
    }

    fun setAccountArchived(accountId: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.setAccountArchived(accountId, archived)
            _events.emit(if (archived) "Account archived" else "Account restored")
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            _events.emit("Account deleted")
        }
    }

    fun adjustAccountBalance(accountId: Long, targetBalanceCents: Long, note: String) {
        viewModelScope.launch {
            repository.adjustAccountBalance(accountId, targetBalanceCents, note)
            _events.emit("Balance adjusted & logged")
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.saveCategory(category)
            _events.emit(if (category.id == 0L) "Category added" else "Category updated")
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            _events.emit("Category removed")
        }
    }

    fun saveTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            runCatching {
                if (tx.id == 0L) repository.recordTransaction(tx)
                else repository.updateTransaction(tx)
            }.onSuccess {
                _events.emit(if (tx.id == 0L) "${tx.type.label} saved" else "${tx.type.label} updated")
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to save transaction")
            }
        }
    }

    fun duplicateTransaction(id: Long) {
        viewModelScope.launch {
            repository.duplicateTransaction(id)
            _events.emit("Transaction duplicated")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _events.emit("Transaction deleted & balance restored")
        }
    }

    suspend fun copyReceiptImage(uri: Uri): String? = repository.copyReceiptToInternalStorage(uri)

    fun saveBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.saveBudget(budget)
            repository.checkBudgetAlerts()
            _events.emit("Budget saved")
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            _events.emit("Budget deleted")
        }
    }

    fun saveGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveGoal(goal)
            _events.emit("Savings goal saved")
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            _events.emit("Savings goal removed")
        }
    }

    fun contributeToGoal(goalId: Long, accountId: Long, amountCents: Long, note: String) {
        viewModelScope.launch {
            runCatching {
                repository.contributeToGoal(goalId, accountId, amountCents, note)
            }.onSuccess {
                _events.emit("Contribution added to goal")
            }.onFailure { err ->
                _events.emit(err.message ?: "Could not add contribution")
            }
        }
    }

    fun withdrawFromGoal(goalId: Long, accountId: Long, amountCents: Long, note: String) {
        viewModelScope.launch {
            runCatching {
                repository.withdrawFromGoal(goalId, accountId, amountCents, note)
            }.onSuccess {
                _events.emit("Funds returned to account")
            }.onFailure { err ->
                _events.emit(err.message ?: "Could not withdraw")
            }
        }
    }

    fun saveRecurringRule(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repository.saveRecurringRule(rule)
            repository.processDueRecurringRules(System.currentTimeMillis())
            _events.emit("Recurring schedule saved")
        }
    }

    fun toggleRecurringPause(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repository.saveRecurringRule(rule.copy(isPaused = !rule.isPaused))
            _events.emit(if (rule.isPaused) "Recurring resumed" else "Recurring paused")
        }
    }

    fun skipNextRecurring(ruleId: Long) {
        viewModelScope.launch {
            repository.skipNextRecurringOccurrence(ruleId)
            _events.emit("Next occurrence skipped")
        }
    }

    fun deleteRecurringRule(ruleId: Long) {
        viewModelScope.launch {
            repository.deleteRecurringRule(ruleId)
            _events.emit("Recurring schedule deleted")
        }
    }

    fun processDueRecurringNow() {
        viewModelScope.launch {
            val count = repository.processDueRecurringRules(System.currentTimeMillis())
            _events.emit(
                if (count > 0) "Processed $count recurring item(s)"
                else "No recurring items due right now"
            )
        }
    }

    // --- Loans & Borrowings ---
    fun saveLoanRecord(loan: LoanRecordEntity) {
        viewModelScope.launch {
            runCatching {
                repository.saveLoanRecord(loan)
            }.onSuccess {
                repository.checkLoanDueReminders()
                _events.emit(if (loan.id == 0L) "Loan record added" else "Loan record updated")
            }.onFailure { err ->
                _events.emit(err.message ?: "Could not save loan record")
            }
        }
    }

    fun deleteLoanRecord(loanId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteLoanRecord(loanId)
            }.onSuccess {
                _events.emit("Loan record deleted")
            }.onFailure { err ->
                _events.emit(err.message ?: "Could not delete loan record")
            }
        }
    }

    fun saveLoanRepayment(repayment: LoanRepaymentEntity) {
        viewModelScope.launch {
            runCatching {
                if (repayment.id == 0L) repository.recordLoanRepayment(repayment)
                else repository.updateLoanRepayment(repayment)
            }.onSuccess {
                _events.emit(if (repayment.id == 0L) "Repayment recorded" else "Repayment updated")
            }.onFailure { err ->
                _events.emit(err.message ?: "Could not save repayment")
            }
        }
    }

    fun deleteLoanRepayment(repaymentId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteLoanRepayment(repaymentId)
            }.onSuccess {
                _events.emit("Repayment deleted & balance updated")
            }.onFailure { err ->
                _events.emit(err.message ?: "Could not delete repayment")
            }
        }
    }

    // --- Preferences & Security ---
    fun toggleHideBalances() {
        viewModelScope.launch {
            prefsManager.setHideBalances(!uiState.value.preferences.hideBalances)
        }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { prefsManager.setCurrencySymbol(symbol) }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { prefsManager.setDarkThemePreference(mode) }
    }

    fun setDateFormat(pattern: String) {
        viewModelScope.launch { prefsManager.setDateFormatPattern(pattern) }
    }

    fun setFirstDayOfMonth(day: Int) {
        viewModelScope.launch { prefsManager.setFirstDayOfMonth(day) }
    }

    fun setDefaultAccountId(id: Long?) {
        viewModelScope.launch { prefsManager.setDefaultAccountId(id) }
    }

    fun setDefaultExpenseCategoryId(id: Long?) {
        viewModelScope.launch { prefsManager.setDefaultExpenseCategoryId(id) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setNotificationsEnabled(enabled) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setBudgetAlertsEnabled(enabled) }
    }

    fun setRecurringRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setRecurringRemindersEnabled(enabled) }
    }

    fun setPin(pin: String?) {
        if (!pin.isNullOrBlank()) {
            // Keep the current foreground Settings session unlocked right after user creates/updates their PIN;
            // any exit or background transition will relock immediately.
            isSessionUnlocked = true
        } else {
            isSessionUnlocked = false
        }
        viewModelScope.launch {
            prefsManager.setPinHash(pin?.let { PinSecurity.hashPin(it) })
            if (pin == null) prefsManager.setBiometricEnabled(false)
            _events.emit(if (pin == null) "PIN lock disabled" else "PIN lock updated")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.setBiometricEnabled(enabled)
            _events.emit(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled")
        }
    }

    // --- Exports & Backups ---
    suspend fun generateCsv(): String = repository.exportCsv(uiState.value.preferences.currencySymbol)

    suspend fun generateBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            repository.importBackupJson(json)
                .onSuccess { (accs, txs) ->
                    _events.emit("Restored $accs accounts & $txs transactions")
                }
                .onFailure {
                    _events.emit("Invalid backup file")
                }
        }
    }

    suspend fun writePdfReport(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val state = uiState.value
        val periodLabel = when (state.insightsGranularity) {
            AnalyticsGranularity.DAILY -> "Last 7 Days"
            AnalyticsGranularity.WEEKLY -> Formatters.formatBillingMonthLabel(state.insightsMonth.year, state.insightsMonth.monthValue) + " (Weekly)"
            AnalyticsGranularity.MONTHLY -> Formatters.formatBillingMonthLabel(state.insightsMonth.year, state.insightsMonth.monthValue)
            AnalyticsGranularity.YEARLY -> "Year ${state.insightsMonth.year}"
        }
        val (s, e, _, _) = insightsBounds(
            state.insightsGranularity,
            state.insightsMonth,
            LocalDate.now(),
            state.preferences.firstDayOfMonth,
        )
        val periodTxs = state.transactions.filter { it.timestamp in s until e }

        PdfReportGenerator.writeFinancialReportPdf(
            outputStream = outputStream,
            periodLabel = periodLabel,
            currencySymbol = state.preferences.currencySymbol,
            datePattern = state.preferences.dateFormatPattern,
            totalLiquidCents = state.totalLiquidAccountBalanceCents,
            totalCreditDebtCents = state.totalCreditDebtCents,
            incomeCents = state.insightsIncomeCents,
            expenseCents = state.insightsExpenseCents,
            categoryBreakdown = state.insightsCategoryBreakdown.map { it.name to it.totalCents },
            accounts = state.accounts,
            transactions = periodTxs,
        )
    }
}
