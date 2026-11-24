package me.robiul.hisabi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        RecurringRuleEntity::class,
        LoanRecordEntity::class,
        LoanRepaymentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CardVaultDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: CardVaultDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `loan_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `direction` TEXT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `principalAmountCents` INTEGER NOT NULL,
                        `repaidAmountCents` INTEGER NOT NULL,
                        `loanDateMs` INTEGER NOT NULL,
                        `dueDateMs` INTEGER,
                        `purpose` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `lastReminderKey` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `loan_repayments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `loanId` INTEGER NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `method` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`loanId`) REFERENCES `loan_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_loan_repayments_loanId` ON `loan_repayments` (`loanId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `loan_records` ADD COLUMN `accountId` INTEGER")
                db.execSQL("ALTER TABLE `loan_repayments` ADD COLUMN `accountId` INTEGER")
            }
        }

        fun getInstance(context: Context): CardVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CardVaultDatabase::class.java,
                    "cardvault_offline.db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
