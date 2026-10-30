package com.rohit.spendsense

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Period(val label: String) {
    Week("Week"),
    Month("Month"),
    Quarter("Quarter"),
    Year("Year")
}

fun periodStart(period: Period, today: LocalDate = LocalDate.now()): LocalDate =
    when (period) {
        Period.Week -> today.minusDays(6)
        Period.Month -> today.withDayOfMonth(1)
        Period.Quarter -> today.withDayOfMonth(1).minusMonths(2)
        Period.Year -> today.withDayOfMonth(1).minusMonths(11)
    }

fun inPeriod(
    transactions: List<Transaction>,
    period: Period,
    today: LocalDate = LocalDate.now()
): List<Transaction> {
    val start = periodStart(period, today)
    return transactions.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
}

data class TrendPoint(val label: String, val amount: Double)

// Rules: only expenses count. Week and Month use daily points; Quarter and Year use monthly points.
fun spendingTrend(
    transactions: List<Transaction>,
    period: Period,
    today: LocalDate = LocalDate.now()
): List<TrendPoint> {
    val expenses = inPeriod(transactions, period, today)
        .filter { it.type == TransactionType.EXPENSE }

    if (period == Period.Week || period == Period.Month) {
        val start = periodStart(period, today)
        val days = ChronoUnit.DAYS.between(start, today).toInt()
        return (0..days).map { i ->
            val day = start.plusDays(i.toLong())
            val label = if (period == Period.Week) {
                day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            } else {
                day.dayOfMonth.toString()
            }
            TrendPoint(label, expenses.filter { it.date == day }.sumOf { it.amount })
        }
    }

    val count = if (period == Period.Quarter) 3 else 12
    val thisMonth = YearMonth.from(today)
    return (count - 1 downTo 0).map { back ->
        val ym = thisMonth.minusMonths(back.toLong())
        TrendPoint(
            ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            expenses.filter { YearMonth.from(it.date) == ym }.sumOf { it.amount }
        )
    }
}

data class AnalyticsStats(
    val income: Double,
    val expenses: Double,
    val daysInRange: Int,
    val expenseCount: Int,
    val previousExpenses: Double
) {
    val net: Double get() = income - expenses
    val avgPerDay: Double get() = if (daysInRange > 0) expenses / daysInRange else 0.0
    val savingsRate: Double get() = if (income > 0) net / income * 100 else 0.0
    val changePercent: Double?
        get() = if (previousExpenses > 0) (expenses - previousExpenses) / previousExpenses * 100 else null
}

// Compares with the period of the same length that comes right before this one.
fun buildStats(
    transactions: List<Transaction>,
    period: Period,
    today: LocalDate = LocalDate.now()
): AnalyticsStats {
    val scoped = inPeriod(transactions, period, today)
    val expenseList = scoped.filter { it.type == TransactionType.EXPENSE }
    val start = periodStart(period, today)
    val days = ChronoUnit.DAYS.between(start, today).toInt() + 1
    val prevEnd = start.minusDays(1)
    val prevStart = prevEnd.minusDays((days - 1).toLong())
    val previous = transactions
        .filter {
            it.type == TransactionType.EXPENSE &&
                    !it.date.isBefore(prevStart) && !it.date.isAfter(prevEnd)
        }
        .sumOf { it.amount }
    return AnalyticsStats(
        income = scoped.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        expenses = expenseList.sumOf { it.amount },
        daysInRange = days,
        expenseCount = expenseList.size,
        previousExpenses = previous
    )
}

fun weekdayTotals(
    transactions: List<Transaction>,
    period: Period,
    today: LocalDate = LocalDate.now()
): List<TrendPoint> {
    val expenses = inPeriod(transactions, period, today)
        .filter { it.type == TransactionType.EXPENSE }
    return DayOfWeek.values().map { day ->
        TrendPoint(
            day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            expenses.filter { it.date.dayOfWeek == day }.sumOf { it.amount }
        )
    }
}

fun paymentBreakdown(
    transactions: List<Transaction>,
    period: Period,
    today: LocalDate = LocalDate.now()
): List<Pair<String, Double>> =
    inPeriod(transactions, period, today)
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.paymentMethod }
        .map { entry -> entry.key to entry.value.sumOf { t -> t.amount } }
        .sortedByDescending { it.second }

// Each insight is only added when the numbers support it, and it shows its calculation.
fun analyticsInsights(
    stats: AnalyticsStats,
    slices: List<Pair<Category, Double>>,
    weekday: List<TrendPoint>
): List<String> {
    val list = mutableListOf<String>()

    val change = stats.changePercent
    if (change != null && Math.abs(change) >= 10) {
        val direction = if (change > 0) "higher" else "lower"
        list.add(
            "Spending is ${String.format(Locale.US, "%.0f", Math.abs(change))}% $direction than the " +
                    "previous ${stats.daysInRange} days (${formatRupees(stats.expenses)} vs " +
                    "${formatRupees(stats.previousExpenses)})."
        )
    }

    if (stats.expenses > 0 && slices.isNotEmpty()) {
        val (category, amount) = slices.first()
        val share = amount / stats.expenses * 100
        list.add(
            "${category.label} is your top category at ${formatRupees(amount)} " +
                    "(${String.format(Locale.US, "%.0f", share)}% of spending)."
        )
    }

    val busiest = weekday.maxByOrNull { it.amount }
    if (busiest != null && busiest.amount > 0 && stats.expenseCount >= 5) {
        list.add(
            "Your highest-spending weekday is ${busiest.label} " +
                    "(${formatRupees(busiest.amount)} in total)."
        )
    }

    if (stats.income > 0) {
        if (stats.net >= 0) {
            list.add(
                "You kept ${String.format(Locale.US, "%.0f", stats.savingsRate)}% of your income " +
                        "(${formatRupees(stats.net)} left after expenses)."
            )
        } else {
            list.add("Expenses are ${formatRupees(-stats.net)} higher than income in this period.")
        }
    }
    return list.take(4)
}