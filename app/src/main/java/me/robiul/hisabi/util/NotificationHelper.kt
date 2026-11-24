package me.robiul.hisabi.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.robiul.hisabi.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_BUDGET = "budget_alerts"
        const val CHANNEL_RECURRING = "recurring_reminders"
        const val CHANNEL_LOAN = "loan_reminders"
    }

    fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts when spending reaches your configured budget threshold"
        }
        val recurringChannel = NotificationChannel(
            CHANNEL_RECURRING,
            "Recurring Transactions",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders and summaries for scheduled recurring expenses and income"
        }
        val loanChannel = NotificationChannel(
            CHANNEL_LOAN,
            "Loans & Repayment Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders for upcoming and overdue loan receivables and payables"
        }
        manager.createNotificationChannel(budgetChannel)
        manager.createNotificationChannel(recurringChannel)
        manager.createNotificationChannel(loanChannel)
    }

    fun showBudgetAlert(title: String, message: String, notificationId: Int) {
        postNotification(CHANNEL_BUDGET, title, message, notificationId)
    }

    fun showRecurringNotification(title: String, message: String, notificationId: Int) {
        postNotification(CHANNEL_RECURRING, title, message, notificationId)
    }

    fun showLoanReminder(title: String, message: String, notificationId: Int) {
        postNotification(CHANNEL_LOAN, title, message, notificationId)
    }

    private fun postNotification(channelId: String, title: String, message: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
