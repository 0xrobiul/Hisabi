package me.robiul.hisabi.util

import me.robiul.hisabi.data.local.AccountType
import me.robiul.hisabi.data.local.LoanDirection
import me.robiul.hisabi.data.local.LoanRecordEntity
import me.robiul.hisabi.data.local.LoanStatus
import me.robiul.hisabi.data.local.RepaymentMethod
import java.time.LocalDate

data class LoanSummaryTotals(
    val totalLentCents: Long = 0L,
    val totalBorrowedCents: Long = 0L,
    val totalReceivedBackCents: Long = 0L,
    val totalPaidBackCents: Long = 0L,
    val outstandingReceivablesCents: Long = 0L,
    val outstandingPayablesCents: Long = 0L,
)

object LoanCalculations {
    fun computeRemainingCents(principalCents: Long, repaidCents: Long): Long {
        if (principalCents <= 0L) return 0L
        return (principalCents - repaidCents.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    fun computeStatus(
        principalCents: Long,
        repaidCents: Long,
        dueDateMs: Long?,
        today: LocalDate = LocalDate.now(),
    ): LoanStatus {
        val safePrincipal = principalCents.coerceAtLeast(0L)
        val safeRepaid = repaidCents.coerceAtLeast(0L)
        if (safePrincipal > 0L && safeRepaid >= safePrincipal) {
            return LoanStatus.FULLY_REPAID
        }
        if (dueDateMs != null && Formatters.toLocalDate(dueDateMs).isBefore(today)) {
            return LoanStatus.OVERDUE
        }
        if (safeRepaid > 0L) {
            return LoanStatus.PARTIALLY_REPAID
        }
        return LoanStatus.ACTIVE
    }

    fun computeProgressPercent(principalCents: Long, repaidCents: Long): Int {
        if (principalCents <= 0L) return 0
        return ((repaidCents.coerceAtLeast(0L) * 100L) / principalCents).toInt().coerceIn(0, 100)
    }

    fun validateRepaymentAmount(
        principalCents: Long,
        currentRepaidCents: Long,
        newRepaymentCents: Long,
        existingRepaymentCents: Long = 0L,
    ): Boolean {
        if (principalCents <= 0L || newRepaymentCents <= 0L) return false
        val baseRepaid = (currentRepaidCents - existingRepaymentCents.coerceAtLeast(0L)).coerceAtLeast(0L)
        return baseRepaid + newRepaymentCents <= principalCents
    }

    /**
     * Computes account balance delta for recording (or reverting) a loan principal:
     * - LENT: outflow from source account (reduces asset balance, increases credit card used debt).
     * - BORROWED: inflow into destination account (increases asset balance, reduces credit card used debt).
     */
    fun computeLoanPrincipalBalanceDelta(
        direction: LoanDirection,
        accountType: AccountType,
        amountCents: Long,
        revert: Boolean = false,
    ): Long {
        if (amountCents <= 0L) return 0L
        val sign = if (revert) -1L else 1L
        val baseDelta = when (direction) {
            LoanDirection.LENT -> if (accountType == AccountType.CREDIT_CARD) amountCents else -amountCents
            LoanDirection.BORROWED -> if (accountType == AccountType.CREDIT_CARD) -amountCents else amountCents
        }
        return baseDelta * sign
    }

    /**
     * Computes account balance delta for recording (or reverting) a loan repayment:
     * - LENT: receiving money back into destination account (inflow).
     * - BORROWED: paying money back from source account (outflow).
     */
    fun computeRepaymentBalanceDelta(
        direction: LoanDirection,
        accountType: AccountType,
        amountCents: Long,
        revert: Boolean = false,
    ): Long {
        if (amountCents <= 0L) return 0L
        val sign = if (revert) -1L else 1L
        val baseDelta = when (direction) {
            LoanDirection.LENT -> if (accountType == AccountType.CREDIT_CARD) -amountCents else amountCents
            LoanDirection.BORROWED -> if (accountType == AccountType.CREDIT_CARD) amountCents else -amountCents
        }
        return baseDelta * sign
    }

    fun defaultRepaymentMethodForAccount(accountType: AccountType): RepaymentMethod = when (accountType) {
        AccountType.CASH -> RepaymentMethod.CASH
        AccountType.BANK -> RepaymentMethod.BANK
        AccountType.BKASH -> RepaymentMethod.BKASH
        AccountType.NAGAD -> RepaymentMethod.NAGAD
        AccountType.ROCKET, AccountType.UPAY, AccountType.CREDIT_CARD, AccountType.CUSTOM -> RepaymentMethod.OTHER
    }

    fun computePortfolioSummary(loans: List<LoanRecordEntity>): LoanSummaryTotals {
        var totalLent = 0L
        var totalBorrowed = 0L
        var totalReceivedBack = 0L
        var totalPaidBack = 0L
        var outstandingReceivables = 0L
        var outstandingPayables = 0L

        for (loan in loans) {
            val principal = loan.principalAmountCents.coerceAtLeast(0L)
            val repaid = loan.repaidAmountCents.coerceIn(0L, principal)
            val remaining = computeRemainingCents(principal, repaid)
            when (loan.direction) {
                LoanDirection.LENT -> {
                    totalLent += principal
                    totalReceivedBack += repaid
                    outstandingReceivables += remaining
                }
                LoanDirection.BORROWED -> {
                    totalBorrowed += principal
                    totalPaidBack += repaid
                    outstandingPayables += remaining
                }
            }
        }

        return LoanSummaryTotals(
            totalLentCents = totalLent,
            totalBorrowedCents = totalBorrowed,
            totalReceivedBackCents = totalReceivedBack,
            totalPaidBackCents = totalPaidBack,
            outstandingReceivablesCents = outstandingReceivables,
            outstandingPayablesCents = outstandingPayables,
        )
    }
}
