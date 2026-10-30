package com.rohit.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale

data class DashboardSummary(
    val totalExpenses: Double,
    val totalIncome: Double,
    val monthlyBudget: Double,
    val daysElapsed: Int
) {
    // Rules: income is never counted as an expense.
    val remainingBudget: Double get() = monthlyBudget - totalExpenses
    val savingsRate: Double
        get() = if (totalIncome > 0) (totalIncome - totalExpenses) / totalIncome * 100 else 0.0
    val dailyAverage: Double
        get() = if (daysElapsed > 0) totalExpenses / daysElapsed else 0.0
    val budgetUsage: Double
        get() = if (monthlyBudget > 0) totalExpenses / monthlyBudget * 100 else 0.0
}

fun formatRupees(amount: Double): String {
    val sign = if (amount < 0) "-" else ""
    return sign + CurrencySettings.symbol + String.format(Locale.US, "%,.0f", Math.abs(amount))
}

fun greetingForNow(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val insights = buildInsights(transactions, summary)
    val weekBars = lastSevenDays(transactions)
    val month = YearMonth.now()
    val topExpenses = transactions
        .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
        .sortedByDescending { it.amount }
        .take(3)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppLogo(logoSize = 44.dp)
            Column {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = greetingForNow(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (insights.isNotEmpty()) {
            InfoCard {
                Text(
                    text = "Insight",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                insights.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HeroCard(summary)

        SafeToSpendCard(transactions = transactions, monthlyBudget = summary.monthlyBudget)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStatCard(
                title = "Savings Rate",
                value = String.format(Locale.US, "%.1f%%", summary.savingsRate),
                modifier = Modifier.weight(1f)
            )
            SmallStatCard(
                title = "Daily Average",
                value = formatRupees(summary.dailyAverage),
                modifier = Modifier.weight(1f)
            )
        }

        InfoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weekly Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatRupees(weekBars.sumOf { it.amount }),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            WeeklyBarChart(bars = weekBars)
        }

        InfoCard {
            Text(
                text = "Top Expenses",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (topExpenses.isEmpty()) {
                Text(
                    text = "No expenses this month yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            topExpenses.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.category.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "-" + formatRupees(item.amount),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}