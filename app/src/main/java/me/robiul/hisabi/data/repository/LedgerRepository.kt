package me.robiul.hisabi.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import me.robiul.hisabi.data.local.AccountDao
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.BudgetDao
import me.robiul.hisabi.data.local.BudgetEntity
import me.robiul.hisabi.data.local.BudgetPeriod
import me.robiul.hisabi.data.local.CardNetwork
import me.robiul.hisabi.data.local.CardVaultDatabase
import me.robiul.hisabi.data.local.CategoryDao
import me.robiul.hisabi.data.local.CategoryEntity
import me.robiul.hisabi.data.local.CategoryType
import me.robiul.hisabi.data.local.LoanDao
import me.robiul.hisabi.data.local.LoanDirection
import me.robiul.hisabi.data.local.LoanRecordEntity
import me.robiul.hisabi.data.local.LoanRepaymentEntity
import me.robiul.hisabi.data.local.PreferencesManager
import me.robiul.hisabi.data.local.RecurringFrequency
import me.robiul.hisabi.data.local.RecurringRuleDao
import me.robiul.hisabi.data.local.RecurringRuleEntity
import me.robiul.hisabi.data.local.RepaymentMethod
import me.robiul.hisabi.data.local.SavingsGoalDao
import me.robiul.hisabi.data.local.SavingsGoalEntity
import me.robiul.hisabi.data.local.TransactionDao
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.LoanCalculations
import me.robiul.hisabi.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID

class LedgerRepository(
    private val context: Context,
    private val database: CardVaultDatabase,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper,
    private val accountDao: AccountDao = database.accountDao(),
    private val categoryDao: CategoryDao = database.categoryDao(),
    private val transactionDao: TransactionDao = database.transactionDao(),
    private val budgetDao: BudgetDao = database.budgetDao(),
    private val savingsGoalDao: SavingsGoalDao = database.savingsGoalDao(),
    private val recurringRuleDao: RecurringRuleDao = database.recurringRuleDao(),
    private val loanDao: LoanDao = database.loanDao(),
) {
    val allAccounts: Flow<List<AccountEntity>> = accountDao.observeAllAccounts()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.observeAllCategories()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.observeAllBudgets()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.observeAllGoals()
    val allRecurringRules: Flow<List<RecurringRuleEntity>> = recurringRuleDao.observeAllRules()
    val allLoans: Flow<List<LoanRecordEntity>> = loanDao.observeAllLoans()
    val allLoanRepayments: Flow<List<LoanRepaymentEntity>> = loanDao.observeAllRepayments()

    suspend fun ensureDefaultCategories() {
        if (categoryDao.count() > 0) return
        val defaults = listOf(
            // Default Expense Categories (10)
            CategoryEntity(name = "Food", type = CategoryType.EXPENSE, iconKey = "food", colorHex = 0xFFFF7849, subcategoriesCsv = "Cha & Snacks,Lunch & Meal,Dining Out,Bazar & Groceries", isDefault = true, sortOrder = 1),
            CategoryEntity(name = "Transportation", type = CategoryType.EXPENSE, iconKey = "transport", colorHex = 0xFF38BDF8, subcategoriesCsv = "Rickshaw,Bus & Metro,CNG & Ride,Fuel", isDefault = true, sortOrder = 2),
            CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE, iconKey = "shopping", colorHex = 0xFFA78BFA, subcategoriesCsv = "Clothing,Electronics,Home Goods,Online Order", isDefault = true, sortOrder = 3),
            CategoryEntity(name = "Bills", type = CategoryType.EXPENSE, iconKey = "bills", colorHex = 0xFFF59E0B, subcategoriesCsv = "Electricity,Gas & Water,WiFi Internet,Mobile Recharge,MFS Cashout Fee", isDefault = true, sortOrder = 4),
            CategoryEntity(name = "Health", type = CategoryType.EXPENSE, iconKey = "health", colorHex = 0xFFF43F5E, subcategoriesCsv = "Medicine,Doctor Visit,Lab Test,Fitness", isDefault = true, sortOrder = 5),
            CategoryEntity(name = "Education", type = CategoryType.EXPENSE, iconKey = "education", colorHex = 0xFF6366F1, subcategoriesCsv = "Books,Course & Tuition,Exam Fee,Stationery", isDefault = true, sortOrder = 6),
            CategoryEntity(name = "Entertainment", type = CategoryType.EXPENSE, iconKey = "entertainment", colorHex = 0xFF8B5CF6, subcategoriesCsv = "Movies,Subscriptions,Hangout,Games", isDefault = true, sortOrder = 7),
            CategoryEntity(name = "Housing", type = CategoryType.EXPENSE, iconKey = "housing", colorHex = 0xFF14B8A6, subcategoriesCsv = "House Rent,Mess & Hostel,Maintenance,Furniture", isDefault = true, sortOrder = 8),
            CategoryEntity(name = "Personal", type = CategoryType.EXPENSE, iconKey = "personal", colorHex = 0xFFEC4899, subcategoriesCsv = "Grooming,Family Support,Gifts,Charity", isDefault = true, sortOrder = 9),
            CategoryEntity(name = "Others", type = CategoryType.EXPENSE, iconKey = "others", colorHex = 0xFF94A3B8, subcategoriesCsv = "Miscellaneous,Emergency", isDefault = true, sortOrder = 10),

            // Default Income Categories (6)
            CategoryEntity(name = "Salary", type = CategoryType.INCOME, iconKey = "salary", colorHex = 0xFF36C995, subcategoriesCsv = "Monthly Pay,Overtime,Allowance", isDefault = true, sortOrder = 1),
            CategoryEntity(name = "Freelancing", type = CategoryType.INCOME, iconKey = "freelance", colorHex = 0xFF10B981, subcategoriesCsv = "Client Project,Tuition,Consulting", isDefault = true, sortOrder = 2),
            CategoryEntity(name = "Business", type = CategoryType.INCOME, iconKey = "business", colorHex = 0xFF22D3EE, subcategoriesCsv = "Sales,Commission,Service", isDefault = true, sortOrder = 3),
            CategoryEntity(name = "Investment", type = CategoryType.INCOME, iconKey = "investment", colorHex = 0xFF4ADE80, subcategoriesCsv = "Profit,Dividend,DPS / FDR Return", isDefault = true, sortOrder = 4),
            CategoryEntity(name = "Gifts", type = CategoryType.INCOME, iconKey = "gifts", colorHex = 0xFFFBBF24, subcategoriesCsv = "Home Allowance,Bonus,Salami / Gift", isDefault = true, sortOrder = 5),
            CategoryEntity(name = "Other income", type = CategoryType.INCOME, iconKey = "wallet", colorHex = 0xFF60A5FA, subcategoriesCsv = "Cashback,Refund,Reimbursement", isDefault = true, sortOrder = 6),
        )
        categoryDao.insertCategories(defaults)
    }

    // --- Accounts ---
    suspend fun saveAccount(account: AccountEntity): Long {
        return if (account.id == 0L) {
            accountDao.insertAccount(account.copy(currentBalanceCents = account.openingBalanceCents))
        } else {
            val existing = accountDao.getAccountById(account.id)
            if (existing != null) {
                val openingDelta = account.openingBalanceCents - existing.openingBalanceCents
                val updated = account.copy(
                    currentBalanceCents = existing.currentBalanceCents + openingDelta
                )
                accountDao.updateAccount(updated)
                account.id
            } else {
                accountDao.insertAccount(account)
            }
        }
    }

    suspend fun setAccountArchived(accountId: Long, archived: Boolean) {
        accountDao.setArchived(accountId, archived)
    }

    suspend fun deleteAccount(accountId: Long) {
        database.withTransaction {
            loanDao.clearLoanAccountReferences(accountId)
            loanDao.clearRepaymentAccountReferences(accountId)
            transactionDao.deleteTransactionsForAccount(accountId)
            accountDao.deleteAccountById(accountId)
        }
    }

    suspend fun adjustAccountBalance(
        accountId: Long,
        targetBalanceCents: Long,
        note: String,
    ) {
        database.withTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction
            val delta = targetBalanceCents - account.currentBalanceCents
            if (delta == 0L) return@withTransaction

            accountDao.adjustBalance(accountId, delta)
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Balance Adjustment",
                    amountCents = delta, // signed delta stored explicitly for ADJUSTMENT
                    type = TransactionType.ADJUSTMENT,
                    categoryNameSnapshot = "Adjustment",
                    accountId = accountId,
                    timestamp = System.currentTimeMillis(),
                    description = "Manual reconciliation",
                    notes = note.trim(),
                )
            )
        }
    }

    // --- Categories ---
    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            categoryDao.insertCategory(category)
        } else {
            categoryDao.updateCategory(category)
            category.id
        }
    }

    suspend fun deleteCategory(categoryId: Long) {
        categoryDao.deleteCategoryById(categoryId)
    }

    // --- Transactions ---
    suspend fun recordTransaction(transaction: TransactionEntity): Long {
        if (transaction.type != TransactionType.ADJUSTMENT && transaction.amountCents <= 0L) {
            throw IllegalArgumentException("Transaction amount must be positive")
        }
        if (transaction.feeCents < 0L) {
            throw IllegalArgumentException("Fee / charge cannot be negative")
        }
        val insertedId = database.withTransaction {
            val fromAccount = accountDao.getAccountById(transaction.accountId)
                ?: throw IllegalStateException("Source account not found")
            val toAccount = transaction.toAccountId?.let { accountDao.getAccountById(it) }

            val id = transactionDao.insertTransaction(transaction)
            applyTransactionEffect(transaction, fromAccount, toAccount, revert = false)
            id
        }
        if (transaction.type == TransactionType.EXPENSE) {
            checkBudgetAlerts()
        }
        return insertedId
    }

    suspend fun updateTransaction(updated: TransactionEntity) {
        if (updated.type != TransactionType.ADJUSTMENT && updated.amountCents <= 0L) {
            throw IllegalArgumentException("Transaction amount must be positive")
        }
        if (updated.feeCents < 0L) {
            throw IllegalArgumentException("Fee / charge cannot be negative")
        }
        database.withTransaction {
            val oldTx = transactionDao.getTransactionById(updated.id) ?: return@withTransaction
            val oldFrom = accountDao.getAccountById(oldTx.accountId)
            val oldTo = oldTx.toAccountId?.let { accountDao.getAccountById(it) }
            applyTransactionEffect(oldTx, oldFrom, oldTo, revert = true)

            transactionDao.updateTransaction(updated)

            val newFrom = accountDao.getAccountById(updated.accountId)
            val newTo = updated.toAccountId?.let { accountDao.getAccountById(it) }
            applyTransactionEffect(updated, newFrom, newTo, revert = false)
        }
        if (updated.type == TransactionType.EXPENSE) {
            checkBudgetAlerts()
        }
    }

    suspend fun duplicateTransaction(transactionId: Long): Long? {
        val original = transactionDao.getTransactionById(transactionId) ?: return null
        val copy = original.copy(
            id = 0L,
            timestamp = System.currentTimeMillis(),
            recurringRuleId = null,
        )
        return recordTransaction(copy)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        database.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
            val fromAccount = accountDao.getAccountById(tx.accountId)
            val toAccount = tx.toAccountId?.let { accountDao.getAccountById(it) }
            applyTransactionEffect(tx, fromAccount, toAccount, revert = true)
            transactionDao.deleteTransactionById(transactionId)
        }
    }

    private suspend fun applyTransactionEffect(
        tx: TransactionEntity,
        fromAccount: AccountEntity?,
        toAccount: AccountEntity?,
        revert: Boolean,
    ) {
        val sign = if (revert) -1L else 1L
        val totalOutflow = tx.amountCents + tx.feeCents

        when (tx.type) {
            TransactionType.EXPENSE -> {
                fromAccount?.let { acc ->
                    val delta = if (acc.accountType == AccountType.CREDIT_CARD) {
                        totalOutflow * sign
                    } else {
                        -totalOutflow * sign
                    }
                    accountDao.adjustBalance(acc.id, delta)
                }
            }

            TransactionType.INCOME -> {
                fromAccount?.let { acc ->
                    val delta = if (acc.accountType == AccountType.CREDIT_CARD) {
                        -tx.amountCents * sign
                    } else {
                        tx.amountCents * sign
                    }
                    accountDao.adjustBalance(acc.id, delta)
                }
            }

            TransactionType.TRANSFER -> {
                fromAccount?.let { src ->
                    val srcDelta = if (src.accountType == AccountType.CREDIT_CARD) {
                        totalOutflow * sign
                    } else {
                        -totalOutflow * sign
                    }
                    accountDao.adjustBalance(src.id, srcDelta)
                }
                toAccount?.let { dst ->
                    val dstDelta = if (dst.accountType == AccountType.CREDIT_CARD) {
                        -tx.amountCents * sign
                    } else {
                        tx.amountCents * sign
                    }
                    accountDao.adjustBalance(dst.id, dstDelta)
                }
            }

            TransactionType.SAVINGS_CONTRIBUTION -> {
                fromAccount?.let { src ->
                    val srcDelta = if (src.accountType == AccountType.CREDIT_CARD) {
                        tx.amountCents * sign
                    } else {
                        -tx.amountCents * sign
                    }
                    accountDao.adjustBalance(src.id, srcDelta)
                }
                tx.savingsGoalId?.let { goalId ->
                    val goal = savingsGoalDao.getGoalById(goalId)
                    if (goal != null) {
                        val updatedSaved = (goal.savedAmountCents + tx.amountCents * sign).coerceAtLeast(0L)
                        savingsGoalDao.updateGoal(
                            goal.copy(
                                savedAmountCents = updatedSaved,
                                isCompleted = updatedSaved >= goal.targetAmountCents,
                            )
                        )
                    }
                }
            }

            TransactionType.ADJUSTMENT -> {
                fromAccount?.let { acc ->
                    accountDao.adjustBalance(acc.id, tx.amountCents * sign)
                }
            }
        }
    }

    // --- Budgets ---
    suspend fun saveBudget(budget: BudgetEntity): Long {
        return if (budget.id == 0L) budgetDao.insertBudget(budget)
        else {
            budgetDao.updateBudget(budget)
            budget.id
        }
    }

    suspend fun deleteBudget(budgetId: Long) {
        budgetDao.deleteBudgetById(budgetId)
    }

    suspend fun checkBudgetAlerts() {
        val prefs = preferencesManager.preferencesFlow.first()
        if (!prefs.notificationsEnabled || !prefs.budgetAlertsEnabled) return

        val budgets = budgetDao.getAllBudgetsOnce().filter { it.isActive && it.alertsEnabled && it.limitCents > 0L }
        if (budgets.isEmpty()) return

        val transactions = transactionDao.getAllTransactionsOnce()
        val now = LocalDate.now()

        for (budget in budgets) {
            val (startMs, endMs, periodKey) = when (budget.periodType) {
                BudgetPeriod.MONTHLY -> {
                    val bounds = Formatters.billingMonthBounds(now.year, now.monthValue, prefs.firstDayOfMonth)
                    Triple(bounds.first, bounds.second, "M_${now.year}_${now.monthValue}")
                }
                BudgetPeriod.WEEKLY -> {
                    val bounds = Formatters.currentWeekBounds()
                    Triple(bounds.first, bounds.second, "W_${bounds.first}")
                }
                BudgetPeriod.CUSTOM -> {
                    Triple(budget.startDateMs, budget.endDateMs, "C_${budget.id}_${budget.startDateMs}")
                }
            }
            if (budget.lastAlertPeriodKey == periodKey) continue

            val spent = transactions
                .filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                        tx.timestamp in startMs until endMs &&
                        (budget.categoryId == null || tx.categoryId == budget.categoryId)
                }
                .sumOf { it.amountCents + it.feeCents }

            val pct = ((spent * 100L) / budget.limitCents).toInt()
            if (pct >= budget.alertThresholdPercent) {
                budgetDao.markAlertSent(budget.id, periodKey)
                notificationHelper.showBudgetAlert(
                    title = "Budget Alert: ${budget.name}",
                    message = "You have used $pct% (${Formatters.formatMoney(spent, prefs.currencySymbol)}) of your ${Formatters.formatMoney(budget.limitCents, prefs.currencySymbol)} limit.",
                    notificationId = (2000 + budget.id).toInt(),
                )
            }
        }
    }

    // --- Savings Goals ---
    suspend fun saveGoal(goal: SavingsGoalEntity): Long {
        return if (goal.id == 0L) savingsGoalDao.insertGoal(goal)
        else {
            savingsGoalDao.updateGoal(goal)
            goal.id
        }
    }

    suspend fun deleteGoal(goalId: Long) {
        savingsGoalDao.deleteGoalById(goalId)
    }

    suspend fun contributeToGoal(
        goalId: Long,
        accountId: Long,
        amountCents: Long,
        note: String = "",
    ) {
        if (amountCents <= 0L) throw IllegalArgumentException("Contribution must be positive")
        database.withTransaction {
            val goal = savingsGoalDao.getGoalById(goalId) ?: throw IllegalStateException("Goal not found")
            val account = accountDao.getAccountById(accountId) ?: throw IllegalStateException("Account not found")

            val tx = TransactionEntity(
                title = "Goal: ${goal.name}",
                amountCents = amountCents,
                type = TransactionType.SAVINGS_CONTRIBUTION,
                categoryNameSnapshot = "Savings Goal",
                accountId = account.id,
                savingsGoalId = goal.id,
                timestamp = System.currentTimeMillis(),
                description = "Allocated to ${goal.name}",
                notes = note.trim(),
            )
            transactionDao.insertTransaction(tx)
            applyTransactionEffect(tx, account, null, revert = false)
        }
    }

    suspend fun withdrawFromGoal(
        goalId: Long,
        accountId: Long,
        amountCents: Long,
        note: String = "",
    ) {
        if (amountCents <= 0L) throw IllegalArgumentException("Withdrawal must be positive")
        database.withTransaction {
            val goal = savingsGoalDao.getGoalById(goalId) ?: throw IllegalStateException("Goal not found")
            val account = accountDao.getAccountById(accountId) ?: throw IllegalStateException("Account not found")
            val actualWithdraw = amountCents.coerceAtMost(goal.savedAmountCents)
            if (actualWithdraw <= 0L) return@withTransaction

            // Restore funds back to account and decrement goal without creating artificial income
            val deltaToAccount = if (account.accountType == AccountType.CREDIT_CARD) -actualWithdraw else actualWithdraw
            accountDao.adjustBalance(account.id, deltaToAccount)
            val newSaved = (goal.savedAmountCents - actualWithdraw).coerceAtLeast(0L)
            savingsGoalDao.updateGoal(
                goal.copy(
                    savedAmountCents = newSaved,
                    isCompleted = newSaved >= goal.targetAmountCents,
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Goal Withdrawal: ${goal.name}",
                    amountCents = actualWithdraw,
                    type = TransactionType.ADJUSTMENT,
                    categoryNameSnapshot = "Goal Withdraw",
                    accountId = account.id,
                    savingsGoalId = goal.id,
                    timestamp = System.currentTimeMillis(),
                    description = "Returned from ${goal.name}",
                    notes = note.trim(),
                )
            )
        }
    }

    // --- Recurring Transactions ---
    suspend fun saveRecurringRule(rule: RecurringRuleEntity): Long {
        return if (rule.id == 0L) recurringRuleDao.insertRule(rule)
        else {
            recurringRuleDao.updateRule(rule)
            rule.id
        }
    }

    suspend fun deleteRecurringRule(ruleId: Long) {
        recurringRuleDao.deleteRuleById(ruleId)
    }

    suspend fun skipNextRecurringOccurrence(ruleId: Long) {
        val rule = recurringRuleDao.getRuleById(ruleId) ?: return
        val next = computeNextDueDate(rule.nextDueDateMs, rule.frequency, rule.intervalCount)
        recurringRuleDao.updateRule(rule.copy(nextDueDateMs = next))
    }

    suspend fun processDueRecurringRules(nowMs: Long = System.currentTimeMillis()): Int {
        val prefs = preferencesManager.preferencesFlow.first()
        var processedCount = 0

        database.withTransaction {
            val rules = recurringRuleDao.getAllRulesOnce()
            for (rule in rules) {
                if (rule.isPaused) continue
                var currentRule = rule
                var guard = 0
                while (
                    currentRule.nextDueDateMs <= nowMs &&
                    (currentRule.endDateMs == null || currentRule.nextDueDateMs <= currentRule.endDateMs) &&
                    guard < 24
                ) {
                    val dueMs = currentRule.nextDueDateMs
                    if (currentRule.lastProcessedDateMs == dueMs) {
                        val advanced = computeNextDueDate(dueMs, currentRule.frequency, currentRule.intervalCount)
                        currentRule = currentRule.copy(nextDueDateMs = advanced)
                        recurringRuleDao.updateRule(currentRule)
                        guard++
                        continue
                    }

                    val acc = accountDao.getAccountById(currentRule.accountId) ?: break
                    val tx = TransactionEntity(
                        title = currentRule.title,
                        amountCents = currentRule.amountCents,
                        type = currentRule.type,
                        categoryId = currentRule.categoryId,
                        categoryNameSnapshot = currentRule.categoryNameSnapshot,
                        subcategoryName = currentRule.subcategoryName,
                        accountId = currentRule.accountId,
                        timestamp = dueMs,
                        description = "Recurring (${currentRule.frequency.label})",
                        notes = currentRule.notes,
                        recurringRuleId = currentRule.id,
                    )
                    transactionDao.insertTransaction(tx)
                    applyTransactionEffect(tx, acc, null, revert = false)

                    val nextDue = computeNextDueDate(dueMs, currentRule.frequency, currentRule.intervalCount)
                    currentRule = currentRule.copy(
                        lastProcessedDateMs = dueMs,
                        nextDueDateMs = nextDue,
                    )
                    recurringRuleDao.updateRule(currentRule)
                    processedCount++
                    guard++
                }
            }
        }

        if (processedCount > 0 && prefs.notificationsEnabled && prefs.recurringRemindersEnabled) {
            notificationHelper.showRecurringNotification(
                title = "Recurring Transactions Processed",
                message = "$processedCount scheduled transaction(s) recorded automatically.",
                notificationId = 3001,
            )
            checkBudgetAlerts()
        }
        return processedCount
    }

    private fun computeNextDueDate(
        fromMs: Long,
        frequency: RecurringFrequency,
        intervalCount: Int,
    ): Long {
        val count = intervalCount.coerceAtLeast(1).toLong()
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(fromMs).atZone(zone)
        val next = when (frequency) {
            RecurringFrequency.DAILY -> zdt.plusDays(count)
            RecurringFrequency.WEEKLY -> zdt.plusWeeks(count)
            RecurringFrequency.MONTHLY -> zdt.plusMonths(count)
            RecurringFrequency.YEARLY -> zdt.plusYears(count)
            RecurringFrequency.CUSTOM_DAYS -> zdt.plusDays(count)
        }
        return next.toInstant().toEpochMilli()
    }

    // --- Loans & Borrowings (Receivables & Payables — strictly isolated from Income/Expense totals) ---
    private suspend fun applyLoanPrincipalAccountEffect(
        accountId: Long?,
        direction: LoanDirection,
        amountCents: Long,
        revert: Boolean,
        requireAccountIfNonNull: Boolean,
    ) {
        if (accountId == null || amountCents <= 0L) return
        val acc = accountDao.getAccountById(accountId)
        if (acc == null) {
            if (requireAccountIfNonNull) throw IllegalStateException("Selected account not found")
            return
        }
        val delta = LoanCalculations.computeLoanPrincipalBalanceDelta(
            direction = direction,
            accountType = acc.accountType,
            amountCents = amountCents,
            revert = revert,
        )
        if (delta != 0L) {
            accountDao.adjustBalance(acc.id, delta)
        }
    }

    private suspend fun applyRepaymentAccountEffect(
        accountId: Long?,
        direction: LoanDirection,
        amountCents: Long,
        revert: Boolean,
        requireAccountIfNonNull: Boolean,
    ) {
        if (accountId == null || amountCents <= 0L) return
        val acc = accountDao.getAccountById(accountId)
        if (acc == null) {
            if (requireAccountIfNonNull) throw IllegalStateException("Selected account not found")
            return
        }
        val delta = LoanCalculations.computeRepaymentBalanceDelta(
            direction = direction,
            accountType = acc.accountType,
            amountCents = amountCents,
            revert = revert,
        )
        if (delta != 0L) {
            accountDao.adjustBalance(acc.id, delta)
        }
    }

    suspend fun saveLoanRecord(loan: LoanRecordEntity): Long {
        val cleanName = loan.personName.trim()
        if (cleanName.isEmpty()) throw IllegalArgumentException("Person's name is required")
        if (loan.principalAmountCents <= 0L) throw IllegalArgumentException("Loan amount must be positive")
        val now = System.currentTimeMillis()
        return if (loan.id == 0L) {
            database.withTransaction {
                applyLoanPrincipalAccountEffect(
                    accountId = loan.accountId,
                    direction = loan.direction,
                    amountCents = loan.principalAmountCents,
                    revert = false,
                    requireAccountIfNonNull = true,
                )
                loanDao.insertLoan(
                    loan.copy(
                        personName = cleanName,
                        purpose = loan.purpose.trim(),
                        notes = loan.notes.trim(),
                        repaidAmountCents = 0L,
                        lastReminderKey = "",
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        } else {
            database.withTransaction {
                val existing = loanDao.getLoanById(loan.id)
                val actualRepaid = loanDao.getTotalRepaidForLoan(loan.id)
                if (loan.principalAmountCents < actualRepaid) {
                    throw IllegalArgumentException("Amount cannot be less than already repaid total")
                }
                val existingReps = if (existing != null && existing.direction != loan.direction) {
                    loanDao.getRepaymentsForLoanOnce(loan.id)
                } else {
                    emptyList()
                }
                if (existing != null) {
                    existingReps.forEach { rep ->
                        applyRepaymentAccountEffect(
                            accountId = rep.accountId,
                            direction = existing.direction,
                            amountCents = rep.amountCents,
                            revert = true,
                            requireAccountIfNonNull = false,
                        )
                    }
                    applyLoanPrincipalAccountEffect(
                        accountId = existing.accountId,
                        direction = existing.direction,
                        amountCents = existing.principalAmountCents,
                        revert = true,
                        requireAccountIfNonNull = false,
                    )
                }
                applyLoanPrincipalAccountEffect(
                    accountId = loan.accountId,
                    direction = loan.direction,
                    amountCents = loan.principalAmountCents,
                    revert = false,
                    requireAccountIfNonNull = true,
                )
                if (existing != null && existing.direction != loan.direction) {
                    existingReps.forEach { rep ->
                        applyRepaymentAccountEffect(
                            accountId = rep.accountId,
                            direction = loan.direction,
                            amountCents = rep.amountCents,
                            revert = false,
                            requireAccountIfNonNull = false,
                        )
                    }
                }
                val resetReminder = existing == null ||
                    existing.dueDateMs != loan.dueDateMs ||
                    existing.reminderEnabled != loan.reminderEnabled
                loanDao.updateLoan(
                    loan.copy(
                        personName = cleanName,
                        purpose = loan.purpose.trim(),
                        notes = loan.notes.trim(),
                        repaidAmountCents = actualRepaid,
                        lastReminderKey = if (resetReminder) "" else existing.lastReminderKey,
                        updatedAt = now,
                    )
                )
                loan.id
            }
        }
    }

    suspend fun deleteLoanRecord(loanId: Long) {
        database.withTransaction {
            val existingLoan = loanDao.getLoanById(loanId)
            val repayments = loanDao.getRepaymentsForLoanOnce(loanId)
            if (existingLoan != null) {
                repayments.forEach { rep ->
                    applyRepaymentAccountEffect(
                        accountId = rep.accountId,
                        direction = existingLoan.direction,
                        amountCents = rep.amountCents,
                        revert = true,
                        requireAccountIfNonNull = false,
                    )
                }
                applyLoanPrincipalAccountEffect(
                    accountId = existingLoan.accountId,
                    direction = existingLoan.direction,
                    amountCents = existingLoan.principalAmountCents,
                    revert = true,
                    requireAccountIfNonNull = false,
                )
            }
            loanDao.deleteRepaymentsForLoan(loanId)
            loanDao.deleteLoanById(loanId)
        }
    }

    suspend fun recordLoanRepayment(repayment: LoanRepaymentEntity): Long {
        if (repayment.amountCents <= 0L) throw IllegalArgumentException("Repayment amount must be positive")
        return database.withTransaction {
            val loan = loanDao.getLoanById(repayment.loanId)
                ?: throw IllegalStateException("Loan record not found")
            val currentRepaid = loanDao.getTotalRepaidForLoan(loan.id)
            if (!LoanCalculations.validateRepaymentAmount(loan.principalAmountCents, currentRepaid, repayment.amountCents)) {
                throw IllegalArgumentException("Repayment cannot exceed remaining balance")
            }
            applyRepaymentAccountEffect(
                accountId = repayment.accountId,
                direction = loan.direction,
                amountCents = repayment.amountCents,
                revert = false,
                requireAccountIfNonNull = true,
            )
            val now = System.currentTimeMillis()
            val id = loanDao.insertRepayment(
                repayment.copy(
                    notes = repayment.notes.trim(),
                    createdAt = now,
                )
            )
            val updatedRepaid = loanDao.getTotalRepaidForLoan(loan.id)
            loanDao.updateLoan(loan.copy(repaidAmountCents = updatedRepaid, updatedAt = now))
            id
        }
    }

    suspend fun updateLoanRepayment(repayment: LoanRepaymentEntity) {
        if (repayment.amountCents <= 0L) throw IllegalArgumentException("Repayment amount must be positive")
        database.withTransaction {
            val existing = loanDao.getRepaymentById(repayment.id)
                ?: throw IllegalStateException("Repayment record not found")
            val loan = loanDao.getLoanById(existing.loanId)
                ?: throw IllegalStateException("Loan record not found")
            val currentRepaid = loanDao.getTotalRepaidForLoan(loan.id)
            if (!LoanCalculations.validateRepaymentAmount(
                    principalCents = loan.principalAmountCents,
                    currentRepaidCents = currentRepaid,
                    newRepaymentCents = repayment.amountCents,
                    existingRepaymentCents = existing.amountCents,
                )
            ) {
                throw IllegalArgumentException("Repayment cannot exceed remaining balance")
            }
            applyRepaymentAccountEffect(
                accountId = existing.accountId,
                direction = loan.direction,
                amountCents = existing.amountCents,
                revert = true,
                requireAccountIfNonNull = false,
            )
            applyRepaymentAccountEffect(
                accountId = repayment.accountId,
                direction = loan.direction,
                amountCents = repayment.amountCents,
                revert = false,
                requireAccountIfNonNull = true,
            )
            loanDao.updateRepayment(
                repayment.copy(
                    loanId = existing.loanId,
                    notes = repayment.notes.trim(),
                )
            )
            val updatedRepaid = loanDao.getTotalRepaidForLoan(loan.id)
            loanDao.updateLoan(loan.copy(repaidAmountCents = updatedRepaid, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteLoanRepayment(repaymentId: Long) {
        database.withTransaction {
            val existing = loanDao.getRepaymentById(repaymentId) ?: return@withTransaction
            val loan = loanDao.getLoanById(existing.loanId)
            if (loan != null) {
                applyRepaymentAccountEffect(
                    accountId = existing.accountId,
                    direction = loan.direction,
                    amountCents = existing.amountCents,
                    revert = true,
                    requireAccountIfNonNull = false,
                )
            }
            loanDao.deleteRepaymentById(repaymentId)
            if (loan == null) return@withTransaction
            val updatedRepaid = loanDao.getTotalRepaidForLoan(loan.id)
            loanDao.updateLoan(loan.copy(repaidAmountCents = updatedRepaid, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun checkLoanDueReminders(nowDate: LocalDate = LocalDate.now()) {
        val prefs = preferencesManager.preferencesFlow.first()
        if (!prefs.notificationsEnabled) return
        val loans = loanDao.getAllLoansOnce()
        for (loan in loans) {
            if (!loan.reminderEnabled) continue
            val dueMs = loan.dueDateMs ?: continue
            val remaining = LoanCalculations.computeRemainingCents(loan.principalAmountCents, loan.repaidAmountCents)
            if (remaining <= 0L) continue
            val dueDate = Formatters.toLocalDate(dueMs)
            val daysUntilDue = ChronoUnit.DAYS.between(nowDate, dueDate)
            val formattedRemaining = Formatters.formatMoney(remaining, prefs.currencySymbol)
            val roleLabel = if (loan.direction == LoanDirection.LENT) "Receivable from" else "Payable to"

            if (daysUntilDue < 0L) {
                val key = "OVERDUE_${loan.id}_$nowDate"
                if (loan.lastReminderKey != key) {
                    loanDao.markLoanReminderSent(loan.id, key)
                    notificationHelper.showLoanReminder(
                        title = "Overdue: $roleLabel ${loan.personName}",
                        message = "$formattedRemaining remaining (due ${Formatters.formatDateOnly(dueMs, prefs.dateFormatPattern)}).",
                        notificationId = (4000 + loan.id).toInt(),
                    )
                }
            } else if (daysUntilDue <= 1L) {
                val key = "DUE_${loan.id}_$dueDate"
                if (loan.lastReminderKey != key) {
                    loanDao.markLoanReminderSent(loan.id, key)
                    val whenLabel = if (daysUntilDue == 0L) "today" else "tomorrow"
                    notificationHelper.showLoanReminder(
                        title = "Repayment Due $whenLabel: ${loan.personName}",
                        message = "$roleLabel ${loan.personName}: $formattedRemaining remaining.",
                        notificationId = (4000 + loan.id).toInt(),
                    )
                }
            }
        }
    }

    // --- Receipt Photo Local Storage (Offline) ---
    suspend fun copyReceiptToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file.absolutePath
        }.getOrNull()
    }

    // --- Exports & Full Offline Backup/Restore ---
    suspend fun exportCsv(currencySymbol: String): String {
        val accounts = accountDao.getAllAccountsOnce().associateBy { it.id }
        val txs = transactionDao.getAllTransactionsOnce()
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("ID,Date,Type,Category,Subcategory,Title,Account,ToAccount,Amount,Fee,Currency,Tags,Notes")
        for (tx in txs) {
            val fromName = accounts[tx.accountId]?.name ?: "Deleted"
            val toName = tx.toAccountId?.let { accounts[it]?.name ?: "Deleted" } ?: ""
            sb.appendLine(
                listOf(
                    tx.id.toString(),
                    escapeCsv(dateFmt.format(Date(tx.timestamp))),
                    tx.type.name,
                    escapeCsv(tx.categoryNameSnapshot),
                    escapeCsv(tx.subcategoryName),
                    escapeCsv(tx.title),
                    escapeCsv(fromName),
                    escapeCsv(toName),
                    Formatters.centsToEditableString(tx.amountCents),
                    Formatters.centsToEditableString(tx.feeCents),
                    escapeCsv(currencySymbol),
                    escapeCsv(tx.tagsCsv),
                    escapeCsv(tx.notes),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    suspend fun exportBackupJson(): String {
        val accounts = accountDao.getAllAccountsOnce()
        val categories = categoryDao.getAllCategoriesOnce()
        val txs = transactionDao.getAllTransactionsOnce()
        val budgets = budgetDao.getAllBudgetsOnce()
        val goals = savingsGoalDao.getAllGoalsOnce()
        val rules = recurringRuleDao.getAllRulesOnce()
        val loans = loanDao.getAllLoansOnce()
        val loanRepayments = loanDao.getAllRepaymentsOnce()

        val root = JSONObject().apply {
            put("app", "CardVault")
            put("schemaVersion", 4)
            put("exportedAt", System.currentTimeMillis())

            put("accounts", JSONArray().apply {
                accounts.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("name", a.name)
                        put("accountType", a.accountType.name)
                        put("customTypeName", a.customTypeName)
                        put("network", a.network.name)
                        put("lastFourDigits", a.lastFourDigits)
                        put("openingBalanceCents", a.openingBalanceCents)
                        put("currentBalanceCents", a.currentBalanceCents)
                        put("creditLimitCents", a.creditLimitCents)
                        put("cardStyleIndex", a.cardStyleIndex)
                        put("iconKey", a.iconKey)
                        put("isArchived", a.isArchived)
                        put("isPinned", a.isPinned)
                        put("createdAt", a.createdAt)
                    })
                }
            })

            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("type", c.type.name)
                        put("iconKey", c.iconKey)
                        put("colorHex", c.colorHex)
                        put("subcategoriesCsv", c.subcategoriesCsv)
                        put("isDefault", c.isDefault)
                        put("sortOrder", c.sortOrder)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                txs.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("amountCents", t.amountCents)
                        put("feeCents", t.feeCents)
                        put("type", t.type.name)
                        if (t.categoryId != null) put("categoryId", t.categoryId)
                        put("categoryNameSnapshot", t.categoryNameSnapshot)
                        put("subcategoryName", t.subcategoryName)
                        put("accountId", t.accountId)
                        if (t.toAccountId != null) put("toAccountId", t.toAccountId)
                        if (t.savingsGoalId != null) put("savingsGoalId", t.savingsGoalId)
                        put("timestamp", t.timestamp)
                        put("description", t.description)
                        put("notes", t.notes)
                        if (t.receiptPath != null) put("receiptPath", t.receiptPath)
                        put("tagsCsv", t.tagsCsv)
                        if (t.recurringRuleId != null) put("recurringRuleId", t.recurringRuleId)
                    })
                }
            })

            put("budgets", JSONArray().apply {
                budgets.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("periodType", b.periodType.name)
                        if (b.categoryId != null) put("categoryId", b.categoryId)
                        put("limitCents", b.limitCents)
                        put("startDateMs", b.startDateMs)
                        put("endDateMs", b.endDateMs)
                        put("alertThresholdPercent", b.alertThresholdPercent)
                        put("alertsEnabled", b.alertsEnabled)
                        put("lastAlertPeriodKey", b.lastAlertPeriodKey)
                        put("isActive", b.isActive)
                        put("createdAt", b.createdAt)
                    })
                }
            })

            put("savingsGoals", JSONArray().apply {
                goals.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("targetAmountCents", g.targetAmountCents)
                        put("savedAmountCents", g.savedAmountCents)
                        put("targetDateMs", g.targetDateMs)
                        put("iconKey", g.iconKey)
                        put("colorHex", g.colorHex)
                        put("isCompleted", g.isCompleted)
                        put("createdAt", g.createdAt)
                    })
                }
            })

            put("recurringRules", JSONArray().apply {
                rules.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("amountCents", r.amountCents)
                        put("type", r.type.name)
                        if (r.categoryId != null) put("categoryId", r.categoryId)
                        put("categoryNameSnapshot", r.categoryNameSnapshot)
                        put("subcategoryName", r.subcategoryName)
                        put("accountId", r.accountId)
                        put("frequency", r.frequency.name)
                        put("intervalCount", r.intervalCount)
                        put("startDateMs", r.startDateMs)
                        if (r.endDateMs != null) put("endDateMs", r.endDateMs)
                        put("nextDueDateMs", r.nextDueDateMs)
                        if (r.lastProcessedDateMs != null) put("lastProcessedDateMs", r.lastProcessedDateMs)
                        put("isPaused", r.isPaused)
                        put("reminderEnabled", r.reminderEnabled)
                        put("notes", r.notes)
                        put("createdAt", r.createdAt)
                    })
                }
            })

            put("loans", JSONArray().apply {
                loans.forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id)
                        put("direction", l.direction.name)
                        put("personName", l.personName)
                        put("principalAmountCents", l.principalAmountCents)
                        put("repaidAmountCents", l.repaidAmountCents)
                        put("loanDateMs", l.loanDateMs)
                        if (l.dueDateMs != null) put("dueDateMs", l.dueDateMs)
                        if (l.accountId != null) put("accountId", l.accountId)
                        put("purpose", l.purpose)
                        put("notes", l.notes)
                        put("reminderEnabled", l.reminderEnabled)
                        put("lastReminderKey", l.lastReminderKey)
                        put("createdAt", l.createdAt)
                        put("updatedAt", l.updatedAt)
                    })
                }
            })

            put("loanRepayments", JSONArray().apply {
                loanRepayments.forEach { rep ->
                    put(JSONObject().apply {
                        put("id", rep.id)
                        put("loanId", rep.loanId)
                        put("amountCents", rep.amountCents)
                        put("timestamp", rep.timestamp)
                        put("method", rep.method.name)
                        if (rep.accountId != null) put("accountId", rep.accountId)
                        put("notes", rep.notes)
                        put("createdAt", rep.createdAt)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<Pair<Int, Int>> = runCatching {
        val root = JSONObject(jsonString)

        val accounts = buildList {
            val arr = root.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    AccountEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        accountType = runCatching { AccountType.valueOf(o.getString("accountType")) }.getOrDefault(AccountType.CUSTOM),
                        customTypeName = o.optString("customTypeName", ""),
                        network = runCatching { CardNetwork.valueOf(o.getString("network")) }.getOrDefault(CardNetwork.BANK),
                        lastFourDigits = o.optString("lastFourDigits", "0000"),
                        openingBalanceCents = o.optLong("openingBalanceCents", o.optLong("balanceCents", 0L)),
                        currentBalanceCents = o.optLong("currentBalanceCents", o.optLong("balanceCents", 0L)),
                        creditLimitCents = o.optLong("creditLimitCents", 0L),
                        cardStyleIndex = o.optInt("cardStyleIndex", 0),
                        iconKey = o.optString("iconKey", "wallet"),
                        isArchived = o.optBoolean("isArchived", false),
                        isPinned = o.optBoolean("isPinned", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val categories = buildList {
            val arr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    CategoryEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        type = runCatching { CategoryType.valueOf(o.getString("type")) }.getOrDefault(CategoryType.EXPENSE),
                        iconKey = o.optString("iconKey", "others"),
                        colorHex = o.optLong("colorHex", 0xFF36C995),
                        subcategoriesCsv = o.optString("subcategoriesCsv", ""),
                        isDefault = o.optBoolean("isDefault", false),
                        sortOrder = o.optInt("sortOrder", 0),
                    )
                )
            }
        }

        val txs = buildList {
            val arr = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    TransactionEntity(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        amountCents = o.getLong("amountCents"),
                        feeCents = o.optLong("feeCents", 0L),
                        type = runCatching { TransactionType.valueOf(o.getString("type")) }.getOrDefault(TransactionType.EXPENSE),
                        categoryId = if (o.has("categoryId") && !o.isNull("categoryId")) o.getLong("categoryId") else null,
                        categoryNameSnapshot = o.optString("categoryNameSnapshot", o.optString("category", "")),
                        subcategoryName = o.optString("subcategoryName", ""),
                        accountId = o.getLong("accountId"),
                        toAccountId = if (o.has("toAccountId") && !o.isNull("toAccountId")) o.getLong("toAccountId") else null,
                        savingsGoalId = if (o.has("savingsGoalId") && !o.isNull("savingsGoalId")) o.getLong("savingsGoalId") else null,
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        description = o.optString("description", ""),
                        notes = o.optString("notes", o.optString("note", "")),
                        receiptPath = if (o.has("receiptPath") && !o.isNull("receiptPath")) o.getString("receiptPath") else null,
                        tagsCsv = o.optString("tagsCsv", ""),
                        recurringRuleId = if (o.has("recurringRuleId") && !o.isNull("recurringRuleId")) o.getLong("recurringRuleId") else null,
                    )
                )
            }
        }

        val budgets = buildList {
            val arr = root.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    BudgetEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        periodType = runCatching { BudgetPeriod.valueOf(o.getString("periodType")) }.getOrDefault(BudgetPeriod.MONTHLY),
                        categoryId = if (o.has("categoryId") && !o.isNull("categoryId")) o.getLong("categoryId") else null,
                        limitCents = o.getLong("limitCents"),
                        startDateMs = o.optLong("startDateMs", 0L),
                        endDateMs = o.optLong("endDateMs", 0L),
                        alertThresholdPercent = o.optInt("alertThresholdPercent", 80),
                        alertsEnabled = o.optBoolean("alertsEnabled", true),
                        lastAlertPeriodKey = o.optString("lastAlertPeriodKey", ""),
                        isActive = o.optBoolean("isActive", true),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val goals = buildList {
            val arr = root.optJSONArray("savingsGoals") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SavingsGoalEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        targetAmountCents = o.getLong("targetAmountCents"),
                        savedAmountCents = o.optLong("savedAmountCents", 0L),
                        targetDateMs = o.optLong("targetDateMs", System.currentTimeMillis()),
                        iconKey = o.optString("iconKey", "savings"),
                        colorHex = o.optLong("colorHex", 0xFF36C995),
                        isCompleted = o.optBoolean("isCompleted", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val rules = buildList {
            val arr = root.optJSONArray("recurringRules") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RecurringRuleEntity(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        amountCents = o.getLong("amountCents"),
                        type = runCatching { TransactionType.valueOf(o.getString("type")) }.getOrDefault(TransactionType.EXPENSE),
                        categoryId = if (o.has("categoryId") && !o.isNull("categoryId")) o.getLong("categoryId") else null,
                        categoryNameSnapshot = o.optString("categoryNameSnapshot", ""),
                        subcategoryName = o.optString("subcategoryName", ""),
                        accountId = o.getLong("accountId"),
                        frequency = runCatching { RecurringFrequency.valueOf(o.getString("frequency")) }.getOrDefault(RecurringFrequency.MONTHLY),
                        intervalCount = o.optInt("intervalCount", 1),
                        startDateMs = o.getLong("startDateMs"),
                        endDateMs = if (o.has("endDateMs") && !o.isNull("endDateMs")) o.getLong("endDateMs") else null,
                        nextDueDateMs = o.getLong("nextDueDateMs"),
                        lastProcessedDateMs = if (o.has("lastProcessedDateMs") && !o.isNull("lastProcessedDateMs")) o.getLong("lastProcessedDateMs") else null,
                        isPaused = o.optBoolean("isPaused", false),
                        reminderEnabled = o.optBoolean("reminderEnabled", true),
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val loans = buildList {
            val arr = root.optJSONArray("loans") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    LoanRecordEntity(
                        id = o.getLong("id"),
                        direction = runCatching { LoanDirection.valueOf(o.getString("direction")) }.getOrDefault(LoanDirection.LENT),
                        personName = o.getString("personName"),
                        principalAmountCents = o.getLong("principalAmountCents"),
                        repaidAmountCents = o.optLong("repaidAmountCents", 0L),
                        loanDateMs = o.optLong("loanDateMs", System.currentTimeMillis()),
                        dueDateMs = if (o.has("dueDateMs") && !o.isNull("dueDateMs")) o.getLong("dueDateMs") else null,
                        accountId = if (o.has("accountId") && !o.isNull("accountId")) o.getLong("accountId") else null,
                        purpose = o.optString("purpose", ""),
                        notes = o.optString("notes", ""),
                        reminderEnabled = o.optBoolean("reminderEnabled", true),
                        lastReminderKey = o.optString("lastReminderKey", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val loanRepayments = buildList {
            val arr = root.optJSONArray("loanRepayments") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    LoanRepaymentEntity(
                        id = o.getLong("id"),
                        loanId = o.getLong("loanId"),
                        amountCents = o.getLong("amountCents"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        method = runCatching { RepaymentMethod.valueOf(o.getString("method")) }.getOrDefault(RepaymentMethod.CASH),
                        accountId = if (o.has("accountId") && !o.isNull("accountId")) o.getLong("accountId") else null,
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        database.withTransaction {
            transactionDao.deleteAllTransactions()
            budgetDao.deleteAllBudgets()
            savingsGoalDao.deleteAllGoals()
            recurringRuleDao.deleteAllRules()
            loanDao.deleteAllRepayments()
            loanDao.deleteAllLoans()
            accountDao.deleteAllAccounts()
            if (categories.isNotEmpty()) {
                categoryDao.deleteAllCategories()
                categoryDao.insertCategories(categories)
            }
            if (accounts.isNotEmpty()) accountDao.insertAccounts(accounts)
            if (txs.isNotEmpty()) transactionDao.insertTransactions(txs)
            if (budgets.isNotEmpty()) budgetDao.insertBudgets(budgets)
            if (goals.isNotEmpty()) savingsGoalDao.insertGoals(goals)
            if (rules.isNotEmpty()) recurringRuleDao.insertRules(rules)
            if (loans.isNotEmpty()) loanDao.insertLoans(loans)
            if (loanRepayments.isNotEmpty()) loanDao.insertRepayments(loanRepayments)
        }
        if (categories.isEmpty()) ensureDefaultCategories()

        accounts.size to txs.size
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) "\"$clean\"" else clean
    }
}
