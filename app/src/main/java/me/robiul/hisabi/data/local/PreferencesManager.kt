package me.robiul.hisabi.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cardvault_prefs")

data class UserPreferences(
    val currencySymbol: String = "৳",
    val darkThemePreference: Int = 1, // 0 = System, 1 = Dark (default), 2 = Light
    val dateFormatPattern: String = "dd MMM yyyy",
    val firstDayOfMonth: Int = 1,
    val hideBalances: Boolean = false,
    val defaultAccountId: Long? = null,
    val defaultExpenseCategoryId: Long? = null,
    val defaultIncomeCategoryId: Long? = null,
    val notificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val recurringRemindersEnabled: Boolean = true,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DARK_THEME_PREF = intPreferencesKey("dark_theme_pref")
        val DATE_FORMAT = stringPreferencesKey("date_format_pattern")
        val FIRST_DAY_OF_MONTH = intPreferencesKey("first_day_of_month")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val DEFAULT_EXPENSE_CAT_ID = longPreferencesKey("default_expense_cat_id")
        val DEFAULT_INCOME_CAT_ID = longPreferencesKey("default_income_cat_id")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val RECURRING_REMINDERS_ENABLED = booleanPreferencesKey("recurring_reminders_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "৳",
            darkThemePreference = prefs[Keys.DARK_THEME_PREF] ?: 1,
            dateFormatPattern = prefs[Keys.DATE_FORMAT] ?: "dd MMM yyyy",
            firstDayOfMonth = (prefs[Keys.FIRST_DAY_OF_MONTH] ?: 1).coerceIn(1, 28),
            hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
            defaultAccountId = prefs[Keys.DEFAULT_ACCOUNT_ID]?.takeIf { it > 0L },
            defaultExpenseCategoryId = prefs[Keys.DEFAULT_EXPENSE_CAT_ID]?.takeIf { it > 0L },
            defaultIncomeCategoryId = prefs[Keys.DEFAULT_INCOME_CAT_ID]?.takeIf { it > 0L },
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            recurringRemindersEnabled = prefs[Keys.RECURRING_REMINDERS_ENABLED] ?: true,
            pinHash = prefs[Keys.PIN_HASH]?.takeIf { it.isNotBlank() },
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
        )
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setDarkThemePreference(mode: Int) {
        context.dataStore.edit { it[Keys.DARK_THEME_PREF] = mode }
    }

    suspend fun setDateFormatPattern(pattern: String) {
        context.dataStore.edit { it[Keys.DATE_FORMAT] = pattern }
    }

    suspend fun setFirstDayOfMonth(day: Int) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_MONTH] = day.coerceIn(1, 28) }
    }

    suspend fun setHideBalances(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_BALANCES] = hide }
    }

    suspend fun setDefaultAccountId(accountId: Long?) {
        context.dataStore.edit {
            if (accountId == null) it.remove(Keys.DEFAULT_ACCOUNT_ID)
            else it[Keys.DEFAULT_ACCOUNT_ID] = accountId
        }
    }

    suspend fun setDefaultExpenseCategoryId(categoryId: Long?) {
        context.dataStore.edit {
            if (categoryId == null) it.remove(Keys.DEFAULT_EXPENSE_CAT_ID)
            else it[Keys.DEFAULT_EXPENSE_CAT_ID] = categoryId
        }
    }

    suspend fun setDefaultIncomeCategoryId(categoryId: Long?) {
        context.dataStore.edit {
            if (categoryId == null) it.remove(Keys.DEFAULT_INCOME_CAT_ID)
            else it[Keys.DEFAULT_INCOME_CAT_ID] = categoryId
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
    }

    suspend fun setRecurringRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RECURRING_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setPinHash(hash: String?) {
        context.dataStore.edit {
            if (hash.isNullOrBlank()) it.remove(Keys.PIN_HASH)
            else it[Keys.PIN_HASH] = hash
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }
}
