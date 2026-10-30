package com.rohit.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class SafeToSpend(
    val monthlyBudget: Double,
    val spentBeforeToday: Double,
    val spentToday: Double,
    val daysLeft: Int
) {
    val budgetLeftBeforeToday: Double get() = monthlyBudget - spentBeforeToday
    val allowanceToday: Double
        get() = if (daysLeft > 0) budgetLeftBeforeToday / daysLeft else 0.0
    val remainingToday: Double get() = allowanceToday - spentToday
}

fun calculateSafeToSpend(
    transactions: List<Transaction>,
    monthlyBudget: Double,
    today: LocalDate = LocalDate.now()
): SafeToSpend {
    val month = YearMonth.from(today)
    val expenses = transactions.filter {
        it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month
    }
    val before = expenses.filter { it.date.isBefore(today) }.sumOf { it.amount.toDouble() }
    val todaySum = expenses.filter { it.date.isEqual(today) }.sumOf { it.amount.toDouble() }
    val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1
    return SafeToSpend(monthlyBudget, before, todaySum, daysLeft)
}

private fun safeMoney(v: Double) = CurrencySettings.symbol + String.format(Locale.US, "%,.0f", v)

@Composable
fun SafeToSpendCard(
    transactions: List<Transaction>,
    monthlyBudget: Double,
    modifier: Modifier = Modifier
) {
    val s = calculateSafeToSpend(transactions, monthlyBudget)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Safe to spend today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (monthlyBudget <= 0) {
                Text("Set a monthly budget in the Budgets tab to see this.", color = muted)
            } else if (s.budgetLeftBeforeToday <= 0) {
                Text(
                    "Monthly budget used up. Over by ${safeMoney(-s.budgetLeftBeforeToday)}.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "How: ${safeMoney(monthlyBudget)} budget − ${safeMoney(s.spentBeforeToday)} spent before today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            } else {
                val over = s.remainingToday < 0
                val tint = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(
                    if (over) "Over by ${safeMoney(-s.remainingToday)}"
                    else "${safeMoney(s.remainingToday)} left",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tint
                )
                LinearProgressIndicator(
                    progress = { (s.spentToday / s.allowanceToday).toFloat().coerceIn(0f, 1f) },
                    color = tint,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Today's allowance ${safeMoney(s.allowanceToday)} · spent today ${safeMoney(s.spentToday)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "How: (${safeMoney(monthlyBudget)} − ${safeMoney(s.spentBeforeToday)}) ÷ " +
                            "${s.daysLeft} days left (including today) = ${safeMoney(s.allowanceToday)}. " +
                            "Bills due later this month are not reserved yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }
    }
}