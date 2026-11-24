package me.robiul.hisabi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loan_records ORDER BY updatedAt DESC, createdAt DESC")
    fun observeAllLoans(): Flow<List<LoanRecordEntity>>

    @Query("SELECT * FROM loan_repayments ORDER BY timestamp DESC, id DESC")
    fun observeAllRepayments(): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM loan_records ORDER BY updatedAt DESC, createdAt DESC")
    suspend fun getAllLoansOnce(): List<LoanRecordEntity>

    @Query("SELECT * FROM loan_repayments ORDER BY timestamp DESC, id DESC")
    suspend fun getAllRepaymentsOnce(): List<LoanRepaymentEntity>

    @Query("SELECT * FROM loan_records WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanRecordEntity?

    @Query("SELECT * FROM loan_repayments WHERE id = :id")
    suspend fun getRepaymentById(id: Long): LoanRepaymentEntity?

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY timestamp DESC, id DESC")
    suspend fun getRepaymentsForLoanOnce(loanId: Long): List<LoanRepaymentEntity>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM loan_repayments WHERE loanId = :loanId")
    suspend fun getTotalRepaidForLoan(loanId: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanRecordEntity>)

    @Update
    suspend fun updateLoan(loan: LoanRecordEntity)

    @Query("UPDATE loan_records SET lastReminderKey = :key WHERE id = :loanId")
    suspend fun markLoanReminderSent(loanId: Long, key: String)

    @Query("UPDATE loan_records SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearLoanAccountReferences(accountId: Long)

    @Query("DELETE FROM loan_records WHERE id = :id")
    suspend fun deleteLoanById(id: Long)

    @Query("DELETE FROM loan_records")
    suspend fun deleteAllLoans()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayments(repayments: List<LoanRepaymentEntity>)

    @Update
    suspend fun updateRepayment(repayment: LoanRepaymentEntity)

    @Query("UPDATE loan_repayments SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearRepaymentAccountReferences(accountId: Long)

    @Query("DELETE FROM loan_repayments WHERE id = :id")
    suspend fun deleteRepaymentById(id: Long)

    @Query("DELETE FROM loan_repayments WHERE loanId = :loanId")
    suspend fun deleteRepaymentsForLoan(loanId: Long)

    @Query("DELETE FROM loan_repayments")
    suspend fun deleteAllRepayments()
}
