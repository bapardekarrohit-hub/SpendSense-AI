package com.rohit.spendsense

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.YearMonth
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "budget_alerts"

    private fun send(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    // Rules: only expenses count, and an alert fires only when a limit is newly crossed.
    fun checkBudgetAlert(
        context: Context,
        before: List<Transaction>,
        after: List<Transaction>,
        budget: Double
    ) {
        if (budget <= 0) return
        val month = YearMonth.now()
        val spentBefore = monthTotal(before, month)
        val spentAfter = monthTotal(after, month)
        val usageBefore = spentBefore / budget * 100
        val usageAfter = spentAfter / budget * 100

        when {
            usageBefore < 100 && usageAfter >= 100 -> send(
                context, 2, "Budget exceeded",
                "You have spent ${formatRupees(spentAfter)} of your ${formatRupees(budget)} monthly budget."
            )
            usageBefore < 80 && usageAfter >= 80 -> send(
                context, 1, "Approaching your budget",
                "You have used ${String.format(Locale.US, "%.0f", usageAfter)}% of your " +
                        "${formatRupees(budget)} monthly budget."
            )
        }
    }
}