package com.rohit.spendsense

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

data class ReportCard(
    val daysChecked: Int,
    val daysPending: Int,
    val meanAbsError: Double,
    val totalPredicted: Double,
    val totalActual: Double
)

object ForecastLog {
    private const val KEY = "forecast_log"
    private const val MAX_DAYS = 120
    const val MIN_DAYS_TO_SHOW = 3

    private fun prefs(c: Context) = c.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE)

    private fun load(c: Context): JSONObject =
        try {
            JSONObject(prefs(c).getString(KEY, "{}") ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }

    /** Keeps the FIRST forecast made for each date, so later, easier forecasts can't flatter the score. */
    fun save(context: Context, days: List<AiForecastDay>) {
        if (days.isEmpty()) return
        val log = load(context)
        days.forEach { if (!log.has(it.date)) log.put(it.date, it.predicted) }
        val keys = log.keys().asSequence().toList().sorted()
        keys.take((keys.size - MAX_DAYS).coerceAtLeast(0)).forEach { log.remove(it) }
        prefs(context).edit().putString(KEY, log.toString()).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    fun evaluate(
        context: Context,
        transactions: List<Transaction>,
        today: LocalDate = LocalDate.now()
    ): ReportCard? {
        val log = load(context)
        if (log.length() == 0) return null

        val actualByDay = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.date }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() } }

        var checked = 0
        var pending = 0
        var errorSum = 0.0
        var predictedSum = 0.0
        var actualSum = 0.0
        log.keys().forEach { key ->
            val date = try {
                LocalDate.parse(key)
            } catch (e: Exception) {
                return@forEach
            }
            if (date.isBefore(today)) {
                val predicted = log.getDouble(key)
                val actual = actualByDay[date] ?: 0.0
                errorSum += Math.abs(predicted - actual)
                predictedSum += predicted
                actualSum += actual
                checked++
            } else {
                pending++
            }
        }
        if (checked == 0) return ReportCard(0, pending, 0.0, 0.0, 0.0)
        return ReportCard(checked, pending, errorSum / checked, predictedSum, actualSum)
    }
}

@Composable
fun ForecastReportCardView(transactions: List<Transaction>, refreshKey: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val card = remember(transactions, refreshKey) { ForecastLog.evaluate(context, transactions) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    fun money(v: Double) = CurrencySettings.symbol + String.format(Locale.US, "%,.0f", v)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Forecast report card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (card == null) {
                Text(
                    "Run an analysis. The app saves its forecast and checks it against your real spending once those days have passed.",
                    color = muted
                )
            } else if (card.daysChecked < ForecastLog.MIN_DAYS_TO_SHOW) {
                Text(
                    "${card.daysChecked} forecast day(s) checked so far, ${card.daysPending} still to come. " +
                            "The score appears after ${ForecastLog.MIN_DAYS_TO_SHOW} days, because fewer is not a fair test.",
                    color = muted
                )
            } else {
                val diff = card.totalActual - card.totalPredicted
                val relation = when {
                    Math.abs(diff) < 1.0 -> "matched"
                    diff > 0 -> "was ${money(diff)} higher than predicted"
                    else -> "was ${money(-diff)} lower than predicted"
                }
                Text(
                    "Average daily miss: ${money(card.meanAbsError)}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text("Over ${card.daysChecked} checked days you spent ${money(card.totalActual)}, which $relation (${money(card.totalPredicted)}).")
                Text(
                    "How: for each past day, |forecast − actual spending|, then averaged. " +
                            "Only the first forecast made for a day counts. Days where you logged nothing count as ₹0 spent, " +
                            "so log your expenses regularly for a fair score.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }
    }
}