package me.robiul.hisabi

import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.LoanDirection
import me.robiul.hisabi.data.local.LoanRecordEntity
import me.robiul.hisabi.data.local.LoanStatus
import me.robiul.hisabi.data.local.RecurringFrequency
import me.robiul.hisabi.data.local.RepaymentMethod
import me.robiul.hisabi.util.Formatters
import me.robiul.hisabi.util.InputValidation
import me.robiul.hisabi.util.LoanCalculations
import me.robiul.hisabi.util.PinSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExampleUnitTest {
    @Test
    fun formatMoney_handlesPositiveNegativePlusAndMask() {
        assertEquals("৳1,250.50", Formatters.formatMoney(125_050L, "৳"))
        assertEquals("-৳42.00", Formatters.formatMoney(-4_200L, "৳"))
        assertEquals("+৳500.00", Formatters.formatMoney(50_000L, "৳", showPlus = true))
        assertEquals("৳••••••", Formatters.formatMoney(99_900L, "৳", masked = true))
    }

    @Test
    fun parseAmountToCents_exactIntegerMinorUnitsWithoutFloatDrift() {
        assertEquals(12345L, Formatters.parseAmountToCents("123.45"))
        assertEquals(100000L, Formatters.parseAmountToCents("1,000"))
        //Classic IEEE-754 float trap: 19.99 + 0.01
        val a = Formatters.parseAmountToCents("19.99")!!
        val b = Formatters.parseAmountToCents("0.01")!!
        assertEquals(2000L, a + b)
        assertNull(Formatters.parseAmountToCents(""))
        assertNull(Formatters.parseAmountToCents("abc"))
    }

    @Test
    fun billingMonthBounds_handlesCustomStartDayAndShortMonths() {
        val (startMs, endMs) = Formatters.billingMonthBounds(year = 2026, monthOneBased = 2, firstDayOfMonth = 30)
        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMs).atZone(zone).toLocalDate()
        // Feb 2026 has 28 days, clamped from 30 -> 28; March clamps to 30
        assertEquals(2026, startDate.year)
        assertEquals(2, startDate.monthValue)
        assertEquals(28, startDate.dayOfMonth)
        assertEquals(3, endDate.monthValue)
        assertEquals(30, endDate.dayOfMonth)
    }

    @Test
    fun computeNextDueDate_advancesCorrectly() {
        val zone = ZoneId.systemDefault()
        val jan15 = LocalDate.of(2026, 1, 15).atStartOfDay(zone).toInstant().toEpochMilli()

        val nextMonth = Instant.ofEpochMilli(
            Formatters.computeNextDueDate(jan15, RecurringFrequency.MONTHLY, 1)
        ).atZone(zone).toLocalDate()
        assertEquals(2, nextMonth.monthValue)
        assertEquals(15, nextMonth.dayOfMonth)

        val nextCustom = Instant.ofEpochMilli(
            Formatters.computeNextDueDate(jan15, RecurringFrequency.CUSTOM_DAYS, 10)
        ).atZone(zone).toLocalDate()
        assertEquals(1, nextCustom.monthValue)
        assertEquals(25, nextCustom.dayOfMonth)
    }

    @Test
    fun pinSecurity_hashesAndVerifiesDeterministically() {
        val hash1 = PinSecurity.hashPin("4829")
        val hash2 = PinSecurity.hashPin("4829")
        assertEquals(hash1, hash2)
        assertNotEquals("4829", hash1)
        assertTrue(PinSecurity.verifyPin("4829", hash1))
        assertFalse(PinSecurity.verifyPin("0000", hash1))
        assertFalse(PinSecurity.verifyPin("4829", null))
    }

    @Test
    fun loanStatus_transitionsActivePartialOverdueAndFullyRepaid() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 5, 10)
        val futureDue = today.plusDays(5).atStartOfDay(zone).toInstant().toEpochMilli()
        val pastDue = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(LoanStatus.ACTIVE, LoanCalculations.computeStatus(100_000L, 0L, futureDue, today))
        assertEquals(LoanStatus.PARTIALLY_REPAID, LoanCalculations.computeStatus(100_000L, 40_000L, futureDue, today))
        assertEquals(LoanStatus.OVERDUE, LoanCalculations.computeStatus(100_000L, 40_000L, pastDue, today))
        assertEquals(LoanStatus.FULLY_REPAID, LoanCalculations.computeStatus(100_000L, 100_000L, pastDue, today))
        assertEquals(60_000L, LoanCalculations.computeRemainingCents(100_000L, 40_000L))
        assertEquals(0L, LoanCalculations.computeRemainingCents(100_000L, 120_000L))
    }

    @Test
    fun validateRepaymentAmount_preventsOverpaymentOnCreateAndEdit() {
        // Principal 1000.00, already repaid 400.00 -> max new repayment is 600.00
        assertTrue(LoanCalculations.validateRepaymentAmount(100_000L, 40_000L, 60_000L))
        assertFalse(LoanCalculations.validateRepaymentAmount(100_000L, 40_000L, 60_001L))
        assertFalse(LoanCalculations.validateRepaymentAmount(100_000L, 40_000L, 0L))

        // Editing an existing 250.00 repayment when total repaid is 400.00 -> max edited value is 850.00
        assertTrue(LoanCalculations.validateRepaymentAmount(100_000L, 40_000L, 85_000L, existingRepaymentCents = 25_000L))
        assertFalse(LoanCalculations.validateRepaymentAmount(100_000L, 40_000L, 85_001L, existingRepaymentCents = 25_000L))
    }

    @Test
    fun computePortfolioSummary_totalsReceivablesAndPayablesIndependently() {
        val now = 1_700_000_000_000L
        val loans = listOf(
            LoanRecordEntity(
                id = 1L,
                direction = LoanDirection.LENT,
                personName = "Rahim",
                principalAmountCents = 500_000L,
                repaidAmountCents = 200_000L,
                loanDateMs = now,
            ),
            LoanRecordEntity(
                id = 2L,
                direction = LoanDirection.LENT,
                personName = "Karim",
                principalAmountCents = 150_000L,
                repaidAmountCents = 150_000L,
                loanDateMs = now,
            ),
            LoanRecordEntity(
                id = 3L,
                direction = LoanDirection.BORROWED,
                personName = "Nadia",
                principalAmountCents = 300_000L,
                repaidAmountCents = 50_000L,
                loanDateMs = now,
            ),
        )
        val summary = LoanCalculations.computePortfolioSummary(loans)
        assertEquals(650_000L, summary.totalLentCents)
        assertEquals(300_000L, summary.totalBorrowedCents)
        assertEquals(350_000L, summary.totalReceivedBackCents)
        assertEquals(50_000L, summary.totalPaidBackCents)
        assertEquals(300_000L, summary.outstandingReceivablesCents)
        assertEquals(250_000L, summary.outstandingPayablesCents)
    }

    @Test
    fun inputValidation_sanitizesDecimalsAndValidatesRanges() {
        assertEquals("1234.56", InputValidation.sanitizeDecimalInput("1,23a4.5678"))
        assertEquals("0.5", InputValidation.sanitizeDecimalInput(".5"))
        assertEquals("-45.2", InputValidation.sanitizeDecimalInput("-45..2abc", allowNegative = true))
        assertEquals("45.2", InputValidation.sanitizeDecimalInput("-45.2", allowNegative = false))

        assertEquals("Amount is required", InputValidation.validatePositiveAmountError(""))
        assertEquals("Amount must be greater than 0", InputValidation.validatePositiveAmountError("0"))
        assertNull(InputValidation.validatePositiveAmountError("150.00"))
        assertEquals(
            "Overpayment blocked",
            InputValidation.validatePositiveAmountError(
                input = "200.00",
                maxCents = 15_000L,
                maxErrorLabel = "Overpayment blocked",
            ),
        )

        assertNull(InputValidation.validateLastFourDigitsError(""))
        assertNull(InputValidation.validateLastFourDigitsError("4821"))
        assertEquals("Enter all 4 digits or leave empty", InputValidation.validateLastFourDigitsError("48"))

        val (pinErr, confirmErr) = InputValidation.validatePinPair("12", "1234")
        assertEquals("PIN must be 4 digits", pinErr)
        assertEquals("PINs do not match", confirmErr)
        assertEquals(null to null, InputValidation.validatePinPair("4829", "4829"))
    }

    @Test
    fun loanAccountBalanceDeltas_adjustAndRevertCorrectly() {
        // Lending 500 from Cash deducts 500; reverting restores +500; receiving 500 back adds +500.
        assertEquals(
            -50_000L,
            LoanCalculations.computeLoanPrincipalBalanceDelta(LoanDirection.LENT, AccountType.CASH, 50_000L),
        )
        assertEquals(
            50_000L,
            LoanCalculations.computeLoanPrincipalBalanceDelta(LoanDirection.LENT, AccountType.CASH, 50_000L, revert = true),
        )
        assertEquals(
            50_000L,
            LoanCalculations.computeRepaymentBalanceDelta(LoanDirection.LENT, AccountType.BANK, 50_000L),
        )

        // Borrowing 1000 into Bank adds +1000; paying back 400 from bKash deducts -400; reverting repayment restores +400.
        assertEquals(
            100_000L,
            LoanCalculations.computeLoanPrincipalBalanceDelta(LoanDirection.BORROWED, AccountType.BANK, 100_000L),
        )
        assertEquals(
            -40_000L,
            LoanCalculations.computeRepaymentBalanceDelta(LoanDirection.BORROWED, AccountType.BKASH, 40_000L),
        )
        assertEquals(
            40_000L,
            LoanCalculations.computeRepaymentBalanceDelta(LoanDirection.BORROWED, AccountType.BKASH, 40_000L, revert = true),
        )

        // Credit card debt semantics: lending from credit card increases debt (+), receiving onto credit card lowers debt (-).
        assertEquals(
            25_000L,
            LoanCalculations.computeLoanPrincipalBalanceDelta(LoanDirection.LENT, AccountType.CREDIT_CARD, 25_000L),
        )
        assertEquals(
            -25_000L,
            LoanCalculations.computeRepaymentBalanceDelta(LoanDirection.LENT, AccountType.CREDIT_CARD, 25_000L),
        )

        // Default repayment method maps from account type.
        assertEquals(RepaymentMethod.CASH, LoanCalculations.defaultRepaymentMethodForAccount(AccountType.CASH))
        assertEquals(RepaymentMethod.BANK, LoanCalculations.defaultRepaymentMethodForAccount(AccountType.BANK))
        assertEquals(RepaymentMethod.BKASH, LoanCalculations.defaultRepaymentMethodForAccount(AccountType.BKASH))
        assertEquals(RepaymentMethod.NAGAD, LoanCalculations.defaultRepaymentMethodForAccount(AccountType.NAGAD))
        assertEquals(RepaymentMethod.OTHER, LoanCalculations.defaultRepaymentMethodForAccount(AccountType.ROCKET))
    }

    @Test
    fun computePercentageCents_calculatesExactTransferCharges() {
        // 1.85% of 1000.00 -> 18.50 (1850 cents)
        assertEquals(1_850L, Formatters.computePercentageCents(100_000L, 185))
        // 1.20% of 1000.00 -> 12.00 (1200 cents)
        assertEquals(1_200L, Formatters.computePercentageCents(100_000L, 120))
        // 1.50% of 2000.00 -> 30.00 (3000 cents)
        assertEquals(3_000L, Formatters.computePercentageCents(200_000L, 150))
        // 1.00% of 555.00 -> 5.55 (555 cents)
        assertEquals(555L, Formatters.computePercentageCents(55_500L, 100))
        // Zero / negative guard
        assertEquals(0L, Formatters.computePercentageCents(0L, 185))
        assertEquals(0L, Formatters.computePercentageCents(100_000L, 0))
    }
}
