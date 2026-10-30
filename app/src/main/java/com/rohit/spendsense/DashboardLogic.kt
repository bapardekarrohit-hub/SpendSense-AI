package com.rohit.spendsense

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class WeekBar(val label: String, val amount: Double)

// Expenses for each of the last 7 days (today is the last bar).
fun lastSevenDays(
    transactions: List<Transaction>,
    today: LocalDate = LocalDate.now()
): List<WeekBar> =
    (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        val total = transactions
            .filter { it.type == TransactionType.EXPENSE && it.date == day }
            .sumOf { it.amount }
        WeekBar(
            label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            amount = total
        )
    }

// An insight is only added when the numbers support it, and it explains its calculation.
fun buildInsights(
    transactions: List<Transaction>,
    summary: DashboardSummary,
    today: LocalDate = LocalDate.now()
): List<String> {
    val insights = mutableListOf<String>()
    val month = YearMonth.from(today)
    val byCategory = expensesByCategory(transactions, month)
    val spent = summary.totalExpenses

    if (spent > 0 && byCategory.isNotEmpty()) {
        val (category, amount) = byCategory.first()
        val share = amount / spent * 100
        insights.add(
            "${category.label} is your top category: ${formatRupees(amount)} " +
                    "(${String.format(Locale.US, "%.0f", share)}% of this month's spending)."
        )
    }

    val daysInMonth = month.lengthOfMonth()
    if (summary.daysElapsed >= 5 && summary.monthlyBudget > 0) {
        val projected = summary.dailyAverage * daysInMonth
        if (projected > summary.monthlyBudget) {
            insights.add(
                "At this pace you may spend ${formatRupees(projected)} this month, " +
                        "${formatRupees(projected - summary.monthlyBudget)} over budget " +
                        "(${formatRupees(summary.dailyAverage)}/day × $daysInMonth days)."
            )
        }
    }
    return insights
}