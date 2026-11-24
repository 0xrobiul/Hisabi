package me.robiul.hisabi

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.CardNetwork
import me.robiul.hisabi.data.local.CardVaultDatabase
import me.robiul.hisabi.data.local.PreferencesManager
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import me.robiul.hisabi.data.repository.LedgerRepository
import me.robiul.hisabi.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: CardVaultDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CardVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        val notificationHelper = NotificationHelper(context)
        repository = LedgerRepository(context, db, prefs, notificationHelper)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches Hisabi`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hisabi", appName)
    }

    @Test
    fun `expense income and mfs transfer with fee update balances atomically`() = runBlocking {
        val bankId = repository.saveAccount(
            AccountEntity(
                name = "City Bank",
                accountType = AccountType.BANK,
                network = CardNetwork.BANK,
                lastFourDigits = "1234",
                openingBalanceCents = 100_000L,
            )
        )
        val bkashId = repository.saveAccount(
            AccountEntity(
                name = "bKash Personal",
                accountType = AccountType.BKASH,
                network = CardNetwork.MFS,
                lastFourDigits = "5678",
                openingBalanceCents = 10_000L,
            )
        )

        repository.recordTransaction(
            TransactionEntity(
                accountId = bankId,
                toAccountId = bkashId,
                type = TransactionType.TRANSFER,
                amountCents = 20_000L,
                feeCents = 350L,
                categoryNameSnapshot = "Transfer",
                title = "Top-up bKash",
            )
        )

        val accountsAfterTransfer = repository.allAccounts.first().associateBy { it.id }
        assertEquals(79_650L, accountsAfterTransfer[bankId]?.currentBalanceCents)
        assertEquals(30_000L, accountsAfterTransfer[bkashId]?.currentBalanceCents)

        val txs = repository.allTransactions.first()
        assertEquals(1, txs.size)
        repository.deleteTransaction(txs.first().id)

        val accountsAfterRollback = repository.allAccounts.first().associateBy { it.id }
        assertEquals(100_000L, accountsAfterRollback[bankId]?.currentBalanceCents)
        assertEquals(10_000L, accountsAfterRollback[bkashId]?.currentBalanceCents)
    }

    @Test
    fun `credit card expense increases owed balance and bank payment pays it down`() = runBlocking {
        val bankId = repository.saveAccount(
            AccountEntity(
                name = "Dutch-Bangla",
                accountType = AccountType.BANK,
                network = CardNetwork.VISA,
                lastFourDigits = "4242",
                openingBalanceCents = 50_000L,
            )
        )
        val creditId = repository.saveAccount(
            AccountEntity(
                name = "Platinum Credit",
                accountType = AccountType.CREDIT_CARD,
                network = CardNetwork.MASTERCARD,
                lastFourDigits = "8888",
                openingBalanceCents = 0L,
                creditLimitCents = 200_000L,
            )
        )

        repository.recordTransaction(
            TransactionEntity(
                accountId = creditId,
                type = TransactionType.EXPENSE,
                amountCents = 4_500L,
                categoryNameSnapshot = "Food",
                title = "Dinner",
            )
        )
        repository.recordTransaction(
            TransactionEntity(
                accountId = bankId,
                toAccountId = creditId,
                type = TransactionType.TRANSFER,
                amountCents = 2_500L,
                categoryNameSnapshot = "Transfer",
                title = "Card Bill Pay",
            )
        )

        val accounts = repository.allAccounts.first().associateBy { it.id }
        assertEquals(47_500L, accounts[bankId]?.currentBalanceCents)
        assertEquals(2_000L, accounts[creditId]?.currentBalanceCents)

        val json = repository.exportBackupJson()
        assertTrue(json.contains("Platinum Credit"))
        val restoreResult = repository.importBackupJson(json)
        assertTrue(restoreResult.isSuccess)
        assertEquals(2 to 2, restoreResult.getOrNull())
    }
}
