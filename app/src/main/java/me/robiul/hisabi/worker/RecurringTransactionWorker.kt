package me.robiul.hisabi.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.robiul.hisabi.data.repository.LedgerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LedgerRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "cardvault_recurring_processor"
    }

    override suspend fun doWork(): Result {
        return runCatching {
            repository.processDueRecurringRules(System.currentTimeMillis())
            repository.checkBudgetAlerts()
            repository.checkLoanDueReminders()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
