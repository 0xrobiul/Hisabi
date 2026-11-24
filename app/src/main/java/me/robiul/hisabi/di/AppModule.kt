package me.robiul.hisabi.di

import android.content.Context
import me.robiul.hisabi.data.local.AccountDao
import me.robiul.hisabi.data.local.BudgetDao
import me.robiul.hisabi.data.local.CardVaultDatabase
import me.robiul.hisabi.data.local.CategoryDao
import me.robiul.hisabi.data.local.LoanDao
import me.robiul.hisabi.data.local.PreferencesManager
import me.robiul.hisabi.data.local.RecurringRuleDao
import me.robiul.hisabi.data.local.SavingsGoalDao
import me.robiul.hisabi.data.local.TransactionDao
import me.robiul.hisabi.data.repository.LedgerRepository
import me.robiul.hisabi.util.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CardVaultDatabase {
        return CardVaultDatabase.getInstance(context)
    }

    @Provides
    fun provideAccountDao(db: CardVaultDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: CardVaultDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: CardVaultDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: CardVaultDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideSavingsGoalDao(db: CardVaultDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideRecurringRuleDao(db: CardVaultDatabase): RecurringRuleDao = db.recurringRuleDao()

    @Provides
    fun provideLoanDao(db: CardVaultDatabase): LoanDao = db.loanDao()

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideLedgerRepository(
        @ApplicationContext context: Context,
        db: CardVaultDatabase,
        preferencesManager: PreferencesManager,
        notificationHelper: NotificationHelper,
    ): LedgerRepository {
        return LedgerRepository(
            context = context,
            database = db,
            preferencesManager = preferencesManager,
            notificationHelper = notificationHelper,
        )
    }
}
