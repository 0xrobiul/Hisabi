package me.robiul.hisabi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class AccountType(
    val label: String,
    val shortLabel: String,
    val defaultPaletteIndex: Int,
    val defaultIconKey: String,
) {
    CASH("Cash", "CASH", 6, "wallet"),
    BANK("Bank Account", "BANK", 2, "bank"),
    BKASH("bKash", "bKash", 3, "mfs"),
    NAGAD("Nagad", "Nagad", 4, "mfs"),
    ROCKET("Rocket", "Rocket", 5, "mfs"),
    UPAY("Upay", "Upay", 7, "mfs"),
    CREDIT_CARD("Credit Card", "CREDIT", 0, "card"),
    CUSTOM("Custom Account", "CUSTOM", 1, "wallet"),
}

enum class CardNetwork(val displayName: String) {
    VISA("VISA"),
    MASTERCARD("MASTERCARD"),
    AMEX("AMEX"),
    MFS("MFS"),
    BANK("BANK"),
    CASH("CASH"),
}

enum class CategoryType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
}

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    SAVINGS_CONTRIBUTION("Goal Savings"),
    ADJUSTMENT("Balance Adjust"),
}

enum class BudgetPeriod(val label: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    CUSTOM("Custom Range"),
}

enum class RecurringFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM_DAYS("Custom Days"),
}

enum class LoanDirection(val label: String) {
    LENT("Money I Lent"),
    BORROWED("Money I Borrowed"),
}

enum class LoanStatus(val label: String) {
    ACTIVE("Active"),
    PARTIALLY_REPAID("Partially Repaid"),
    FULLY_REPAID("Fully Repaid"),
    OVERDUE("Overdue"),
}

enum class RepaymentMethod(val label: String) {
    CASH("Cash"),
    BANK("Bank"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    OTHER("Other"),
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountType: AccountType,
    val customTypeName: String = "",
    val network: CardNetwork = CardNetwork.BANK,
    val lastFourDigits: String = "0000",
    val openingBalanceCents: Long = 0L,
    /**
     * For asset accounts (CASH, BANK, BKASH, NAGAD, ROCKET, UPAY, CUSTOM): available balance in paisa/cents.
     * For CREDIT_CARD: amount currently owed/used in paisa/cents.
     */
    val currentBalanceCents: Long = 0L,
    val creditLimitCents: Long = 0L,
    val cardStyleIndex: Int = 0,
    val iconKey: String = "wallet",
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconKey: String,
    val colorHex: Long,
    val subcategoriesCsv: String = "",
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
) {
    fun subcategories(): List<String> = subcategoriesCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val feeCents: Long = 0L,
    val type: TransactionType,
    val categoryId: Long? = null,
    val categoryNameSnapshot: String = "",
    val subcategoryName: String = "",
    val accountId: Long,
    val toAccountId: Long? = null,
    val savingsGoalId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val notes: String = "",
    val receiptPath: String? = null,
    val tagsCsv: String = "",
    val recurringRuleId: Long? = null,
) {
    fun tags(): List<String> = tagsCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categoryId: Long? = null,
    val limitCents: Long,
    val startDateMs: Long = 0L,
    val endDateMs: Long = 0L,
    val alertThresholdPercent: Int = 80,
    val alertsEnabled: Boolean = true,
    val lastAlertPeriodKey: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmountCents: Long,
    val savedAmountCents: Long = 0L,
    val targetDateMs: Long,
    val iconKey: String = "savings",
    val colorHex: Long = 0xFF36C995,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long? = null,
    val categoryNameSnapshot: String = "",
    val subcategoryName: String = "",
    val accountId: Long,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val intervalCount: Int = 1,
    val startDateMs: Long,
    val endDateMs: Long? = null,
    val nextDueDateMs: Long,
    val lastProcessedDateMs: Long? = null,
    val isPaused: Boolean = false,
    val reminderEnabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "loan_records")
data class LoanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: LoanDirection,
    val personName: String,
    val principalAmountCents: Long,
    val repaidAmountCents: Long = 0L,
    val loanDateMs: Long,
    val dueDateMs: Long? = null,
    val accountId: Long? = null,
    val purpose: String = "",
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val lastReminderKey: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "loan_repayments",
    foreignKeys = [
        ForeignKey(
            entity = LoanRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("loanId")],
)
data class LoanRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amountCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val method: RepaymentMethod = RepaymentMethod.CASH,
    val accountId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

class Converters {
    @TypeConverter
    fun fromAccountType(v: AccountType): String = v.name

    @TypeConverter
    fun toAccountType(v: String): AccountType =
        runCatching { AccountType.valueOf(v) }.getOrDefault(AccountType.CUSTOM)

    @TypeConverter
    fun fromCardNetwork(v: CardNetwork): String = v.name

    @TypeConverter
    fun toCardNetwork(v: String): CardNetwork =
        runCatching { CardNetwork.valueOf(v) }.getOrDefault(CardNetwork.BANK)

    @TypeConverter
    fun fromCategoryType(v: CategoryType): String = v.name

    @TypeConverter
    fun toCategoryType(v: String): CategoryType =
        runCatching { CategoryType.valueOf(v) }.getOrDefault(CategoryType.EXPENSE)

    @TypeConverter
    fun fromTransactionType(v: TransactionType): String = v.name

    @TypeConverter
    fun toTransactionType(v: String): TransactionType =
        runCatching { TransactionType.valueOf(v) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromBudgetPeriod(v: BudgetPeriod): String = v.name

    @TypeConverter
    fun toBudgetPeriod(v: String): BudgetPeriod =
        runCatching { BudgetPeriod.valueOf(v) }.getOrDefault(BudgetPeriod.MONTHLY)

    @TypeConverter
    fun fromRecurringFrequency(v: RecurringFrequency): String = v.name

    @TypeConverter
    fun toRecurringFrequency(v: String): RecurringFrequency =
        runCatching { RecurringFrequency.valueOf(v) }.getOrDefault(RecurringFrequency.MONTHLY)

    @TypeConverter
    fun fromLoanDirection(v: LoanDirection): String = v.name

    @TypeConverter
    fun toLoanDirection(v: String): LoanDirection =
        runCatching { LoanDirection.valueOf(v) }.getOrDefault(LoanDirection.LENT)

    @TypeConverter
    fun fromRepaymentMethod(v: RepaymentMethod): String = v.name

    @TypeConverter
    fun toRepaymentMethod(v: String): RepaymentMethod =
        runCatching { RepaymentMethod.valueOf(v) }.getOrDefault(RepaymentMethod.CASH)
}
