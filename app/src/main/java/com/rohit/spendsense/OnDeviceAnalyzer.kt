package com.rohit.spendsense

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Forecast, unusual-spending check and insights, calculated on the phone. Nothing is sent anywhere. */
object OnDeviceAnalyzer {
    private const val MIN_DAYS = 35
    private const val BACKTEST_HORIZON = 7
    private const val Z80 = 1.28
    private const val MIN_PER_CATEGORY = 8
    private const val NAME_MEAN7 = "baseline_last_7_day_average"
    private const val NAME_WEEKDAY = "baseline_same_weekday_average"
    private const val NAME_RIDGE = "ridge_regression"

    private fun n0(v: Double) = String.format(Locale.US, "%,.0f", v)
    private fun n2(v: Double) = String.format(Locale.US, "%,.2f", v)

    fun analyze(
        transactions: List<Transaction>,
        monthlyBudget: Double,
        horizon: Int,
        dismissed: Set<String>
    ): AiReport {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) throw AiException("There are no expense transactions to analyse.")
        val today = transactions.maxOf { it.date }
        val (forecast, reason) = forecast(expenses, today, horizon)
        return AiReport(
            asOf = today.toString(),
            forecast = forecast,
            forecastReason = reason,
            anomalies = anomalies(expenses, dismissed),
            insights = insights(expenses, today, monthlyBudget)
        )
    }

    // ---------------------------------------------------------------- forecast

    private fun forecast(
        expenses: List<Transaction>,
        end: LocalDate,
        horizon: Int
    ): Pair<AiForecast?, String> {
        val start = expenses.minOf { it.date }
        val n = ChronoUnit.DAYS.between(start, end).toInt() + 1
        if (n < MIN_DAYS) return Pair(null, "Need at least $MIN_DAYS days of history, found $n.")

        val values = DoubleArray(n)
        for (t in expenses) {
            val idx = ChronoUnit.DAYS.between(start, t.date).toInt()
            if (idx in 0 until n) values[idx] += t.amount.toDouble()
        }

        val windows = min(4, (n - 28) / BACKTEST_HORIZON)
        val errors = linkedMapOf(
            NAME_MEAN7 to ArrayList<Double>(),
            NAME_WEEKDAY to ArrayList<Double>(),
            NAME_RIDGE to ArrayList<Double>()
        )
        for (k in windows downTo 1) {
            val o = n - k * BACKTEST_HORIZON
            val model = fitRidge(values, start, o)
            val day0 = start.plusDays(o.toLong())
            for ((name, list) in errors) {
                val preds = predict(name, values, o, day0, BACKTEST_HORIZON, model)
                for (i in 0 until BACKTEST_HORIZON) list.add(values[o + i] - preds[i])
            }
        }

        val metrics = errors.map { (name, list) ->
            AiMetric(
                name = name,
                mae = list.sumOf { abs(it) } / list.size,
                rmse = sqrt(list.sumOf { it * it } / list.size)
            )
        }
        val chosen = metrics.minByOrNull { it.mae }!!.name

        val finalModel = fitRidge(values, start, n)
        val first = end.plusDays(1)
        val preds = predict(chosen, values, n, first, horizon, finalModel)

        val chosenErrors = errors.getValue(chosen)
        val mean = chosenErrors.average()
        val std = sqrt(chosenErrors.sumOf { (it - mean) * (it - mean) } / chosenErrors.size)
        val spread = Z80 * std

        val days = preds.mapIndexed { i, p ->
            AiForecastDay(
                date = first.plusDays(i.toLong()).toString(),
                predicted = p,
                low = max(0.0, p - spread),
                high = p + spread
            )
        }
        val total = preds.sum()
        val totalSpread = spread * sqrt(horizon.toDouble())

        return Pair(
            AiForecast(
                methodUsed = chosen,
                metrics = metrics,
                days = days,
                totalPredicted = total,
                totalLow = max(0.0, total - totalSpread),
                totalHigh = total + totalSpread,
                metricsNote = "Calculated on this phone. Scored on $windows held-out week(s) (${windows * 7} days) " +
                        "the model had not seen. Lower is better. Scores are for 7-day windows; " +
                        "a 30-day forecast is less reliable.",
                disclaimer = "Forecasts are statistical estimates, not guaranteed outcomes. " +
                        "The range is an approximate 80% band based on past forecast errors."
            ),
            ""
        )
    }

    private fun features(h: DoubleArray, t: Int, day: LocalDate): DoubleArray {
        var s7 = 0.0
        for (i in t - 7 until t) s7 += h[i]
        var s14 = 0.0
        for (i in t - 14 until t) s14 += h[i]
        val f = DoubleArray(12)
        f[0] = h[t - 1]
        f[1] = h[t - 7]
        f[2] = s7 / 7.0
        f[3] = s14 / 14.0
        f[4] = if (day.dayOfMonth == 1) 1.0 else 0.0   // rent and salary often land on the 1st
        f[5 + day.dayOfWeek.value - 1] = 1.0
        return f
    }

    private fun predict(
        name: String,
        values: DoubleArray,
        len: Int,
        day0: LocalDate,
        horizon: Int,
        model: Ridge
    ): DoubleArray {
        val work = DoubleArray(len + horizon)
        System.arraycopy(values, 0, work, 0, len)
        val out = DoubleArray(horizon)
        for (i in 0 until horizon) {
            val t = len + i
            val p = when (name) {
                NAME_MEAN7 -> {
                    var s = 0.0
                    for (j in len - 7 until len) s += values[j]
                    s / 7.0
                }

                NAME_WEEKDAY -> {
                    var s = 0.0
                    var c = 0
                    for (k in intArrayOf(7, 14, 21, 28)) {
                        if (t - k >= 0) {
                            s += work[t - k]
                            c++
                        }
                    }
                    s / c
                }

                else -> max(0.0, model.predict(features(work, t, day0.plusDays(i.toLong()))))
            }
            out[i] = p
            work[t] = p
        }
        return out
    }

    private class Ridge(
        val mean: DoubleArray,
        val std: DoubleArray,
        val w: DoubleArray,
        val b: Double
    ) {
        fun predict(x: DoubleArray): Double {
            var s = b
            for (j in x.indices) s += w[j] * (x[j] - mean[j]) / std[j]
            return s
        }
    }

    private fun fitRidge(values: DoubleArray, start: LocalDate, len: Int, alpha: Double = 10.0): Ridge {
        val rows = ArrayList<DoubleArray>()
        val y = ArrayList<Double>()
        for (t in 14 until len) {
            rows.add(features(values, t, start.plusDays(t.toLong())))
            y.add(values[t])
        }
        val m = rows.size
        val p = rows[0].size
        val mean = DoubleArray(p)
        val std = DoubleArray(p)
        for (j in 0 until p) {
            var s = 0.0
            for (r in rows) s += r[j]
            mean[j] = s / m
            var v = 0.0
            for (r in rows) v += (r[j] - mean[j]) * (r[j] - mean[j])
            val sd = sqrt(v / m)
            std[j] = if (sd < 1e-9) 1.0 else sd
        }
        val yMean = y.average()
        val a = Array(p) { DoubleArray(p) }
        val rhs = DoubleArray(p)
        for (i in 0 until m) {
            val z = DoubleArray(p) { j -> (rows[i][j] - mean[j]) / std[j] }
            val yc = y[i] - yMean
            for (j in 0 until p) {
                rhs[j] += z[j] * yc
                for (k in 0 until p) a[j][k] += z[j] * z[k]
            }
        }
        for (j in 0 until p) a[j][j] += alpha
        return Ridge(mean, std, solve(a, rhs), yMean)
    }

    /** Solves a x = rhs with Gaussian elimination and partial pivoting. */
    private fun solve(a: Array<DoubleArray>, rhs: DoubleArray): DoubleArray {
        val n = rhs.size
        for (col in 0 until n) {
            var piv = col
            for (r in col + 1 until n) if (abs(a[r][col]) > abs(a[piv][col])) piv = r
            val rowTmp = a[col]; a[col] = a[piv]; a[piv] = rowTmp
            val rhsTmp = rhs[col]; rhs[col] = rhs[piv]; rhs[piv] = rhsTmp
            val d = a[col][col]
            if (abs(d) < 1e-12) continue
            for (r in col + 1 until n) {
                val f = a[r][col] / d
                if (f == 0.0) continue
                for (c in col until n) a[r][c] -= f * a[col][c]
                rhs[r] -= f * rhs[col]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var s = rhs[i]
            for (j in i + 1 until n) s -= a[i][j] * x[j]
            x[i] = if (abs(a[i][i]) < 1e-12) 0.0 else s / a[i][i]
        }
        return x
    }

    // --------------------------------------------------------------- anomalies

    private fun quantile(sorted: List<Double>, q: Double): Double {
        val pos = (sorted.size - 1) * q
        val lo = pos.toInt()
        val hi = min(lo + 1, sorted.size - 1)
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo)
    }

    private fun anomalies(expenses: List<Transaction>, dismissed: Set<String>): List<AiAnomaly> {
        val out = ArrayList<AiAnomaly>()
        for ((cat, group) in expenses.groupBy { it.category.label }) {
            if (group.size < MIN_PER_CATEGORY) continue
            val sorted = group.map { it.amount.toDouble() }.sorted()
            val q1 = quantile(sorted, 0.25)
            val q3 = quantile(sorted, 0.75)
            val median = quantile(sorted, 0.5)
            val upper = q3 + 1.5 * (q3 - q1)
            for (t in group) {
                val amount = t.amount.toDouble()
                if (amount > upper && amount >= 2 * median) {
                    val key = "${t.date}|${t.title.trim()}|${String.format(Locale.US, "%.2f", amount)}"
                    if (key in dismissed) continue
                    out.add(
                        AiAnomaly(
                            key = key,
                            date = t.date.toString(),
                            title = t.title,
                            category = cat,
                            amount = amount,
                            why = "${n2(amount)} is above the upper limit of ${n2(upper)} for $cat " +
                                    "(median ${n2(median)}, based on ${group.size} transactions)."
                        )
                    )
                }
            }
        }
        return out.sortedByDescending { it.amount }
    }

    // ---------------------------------------------------------------- insights

    private fun insights(expenses: List<Transaction>, today: LocalDate, budget: Double): List<AiInsight> {
        val out = ArrayList<AiInsight>()
        val monthStart = today.withDayOfMonth(1)
        val fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
        val cur = expenses.filter { !it.date.isBefore(monthStart) && !it.date.isAfter(today) }

        if (cur.size >= 5) {
            val by = cur.groupBy { it.category.label }.mapValues { e -> e.value.sumOf { it.amount.toDouble() } }
            val total = by.values.sum()
            val top = by.maxByOrNull { it.value }!!
            out.add(
                AiInsight(
                    "${top.key} is your highest-spending category this month " +
                            "(${Math.round(top.value / total * 100)}% of spending).",
                    "${n2(top.value)} / ${n2(total)} total expenses since ${monthStart.format(fmt)}."
                )
            )
        }

        val prevLast = monthStart.minusDays(1)
        val prevStart = prevLast.withDayOfMonth(1)
        val day = min(today.dayOfMonth, prevLast.dayOfMonth)
        val prevEnd = prevStart.withDayOfMonth(day)
        val prev = expenses.filter { !it.date.isBefore(prevStart) && !it.date.isAfter(prevEnd) }
        val curBy = cur.groupBy { it.category.label }.mapValues { e -> e.value.sumOf { it.amount.toDouble() } }
        val prevBy = prev.groupBy { it.category.label }.mapValues { e -> e.value.sumOf { it.amount.toDouble() } }
        for (cat in (curBy.keys + prevBy.keys).toSortedSet()) {
            val c = curBy[cat] ?: 0.0
            val p = prevBy[cat] ?: 0.0
            if (p >= 500.0) {
                val pct = (c - p) / p * 100
                if (abs(pct) >= 15) {
                    val word = if (pct > 0) "increased" else "decreased"
                    out.add(
                        AiInsight(
                            "Your $cat spending $word by ${Math.round(abs(pct))}% compared with the same days last month.",
                            "Days 1-$day: ${n2(c)} this month vs ${n2(p)} last month = " +
                                    "${String.format(Locale.US, "%+.1f", pct)}%."
                        )
                    )
                }
            }
        }

        if (budget > 0 && today.dayOfMonth >= 7) {
            val spent = cur.sumOf { it.amount.toDouble() }
            val dim = monthStart.lengthOfMonth()
            val projected = spent / today.dayOfMonth * dim
            if (projected > budget) {
                out.add(
                    AiInsight(
                        "At this pace you may spend about ${n0(projected)} this month, above your budget of ${n0(budget)}.",
                        "${n2(spent)} spent in ${today.dayOfMonth} days / ${today.dayOfMonth} x $dim days = " +
                                "${n2(projected)}. This is an estimate."
                    )
                )
            }
        }
        return out
    }
}