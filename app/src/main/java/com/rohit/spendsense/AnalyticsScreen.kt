package com.rohit.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    var period by rememberSaveable { mutableStateOf(Period.Month) }

    val today = LocalDate.now()
    val stats = buildStats(transactions, period)
    val expenseList = inPeriod(transactions, period)
        .filter { it.type == TransactionType.EXPENSE }
    val slices = expenseList
        .groupBy { it.category }
        .map { entry -> entry.key to entry.value.sumOf { t -> t.amount } }
        .sortedByDescending { it.second }
    val trend = spendingTrend(transactions, period)
    val weekday = weekdayTotals(transactions, period)
    val payments = paymentBreakdown(transactions, period)
    val insights = analyticsInsights(stats, slices, weekday)
    val topExpenses = expenseList.sortedByDescending { it.amount }.take(3)

    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    val trendTitle = if (period == Period.Week || period == Period.Month) {
        "Daily spending trend"
    } else {
        "Monthly spending trend"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Period.entries.forEach { option ->
                FilterChip(
                    selected = period == option,
                    onClick = { period = option },
                    label = { Text(option.label) }
                )
            }
        }
        Text(
            text = "${periodStart(period, today).format(formatter)} – ${today.format(formatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AnalyticsHero(stats = stats, period = period)
        KpiRow(stats = stats)

        if (insights.isNotEmpty()) {
            InsightsCard(insights = insights)
        }

        InfoCard {
            Text(
                text = "Spending by category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (slices.isEmpty()) {
                Text(
                    text = "No expenses in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(slices = slices)
                }
                CategoryLegend(slices = slices)
            }
        }

        InfoCard {
            Text(
                text = "Income vs Expenses",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IncomeExpenseBars(income = stats.income, expenses = stats.expenses)
            Text(
                text = "Net: " + formatRupees(stats.net),
                style = MaterialTheme.typography.titleSmall,
                color = if (stats.net >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
        }

        InfoCard {
            Text(
                text = trendTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TrendLineChart(points = trend)
        }

        if (weekday.sumOf { it.amount } > 0) {
            InfoCard {
                Text(
                    text = "Spending by weekday",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                WeeklyBarChart(bars = weekday.map { WeekBar(it.label, it.amount) })
            }
        }

        if (payments.isNotEmpty()) {
            InfoCard {
                Text(
                    text = "Payment methods",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PaymentMethodBars(items = payments)
            }
        }

        if (topExpenses.isNotEmpty()) {
            BiggestExpensesCard(items = topExpenses)
        }
    }
}