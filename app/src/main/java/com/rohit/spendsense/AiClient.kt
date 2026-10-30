package com.rohit.spendsense

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class AiException(message: String) : Exception(message)

object AiClient {

    suspend fun analyze(
        serverUrl: String,
        transactions: List<Transaction>,
        monthlyBudget: Double,
        horizonDays: Int,
        dismissedKeys: Set<String>
    ): AiReport = withContext(Dispatchers.IO) {
        val base = serverUrl.trim().trimEnd('/')
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            throw AiException("Server address must start with http:// or https://")
        }
        val body = buildBody(transactions, monthlyBudget, horizonDays, dismissedKeys)
        var conn: HttpURLConnection? = null
        try {
            conn = URL("$base/analyze").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val detail = try {
                    JSONObject(text).opt("detail")?.toString()
                } catch (e: JSONException) {
                    null
                }
                throw AiException("The server could not analyse the data: ${detail ?: "error $code"}")
            }
            parse(text)
        } catch (e: AiException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw AiException("The server took too long to answer. (${e.message})")
        } catch (e: IOException) {
            throw AiException(
                "Cannot reach the server. Check that it is running, that your phone is on the same Wi-Fi, and that the address is correct. (${e.javaClass.simpleName}: ${e.message})"
            )
        } catch (e: JSONException) {
            throw AiException("The server sent an answer the app could not read.")
        } finally {
            conn?.disconnect()
        }
    }

    // Only what the analysis needs is sent: no notes and no payment method.
    private fun buildBody(
        transactions: List<Transaction>,
        monthlyBudget: Double,
        horizonDays: Int,
        dismissedKeys: Set<String>
    ): JSONObject {
        val arr = JSONArray()
        for (t in transactions.take(5000)) {
            val o = JSONObject()
            o.put("title", t.title.take(120).ifBlank { "Transaction" })
            o.put("amount", t.amount.toDouble())
            o.put("type", t.type.name.lowercase())
            o.put("category", t.category.label.take(40))
            o.put("date", t.date.toString())
            arr.put(o)
        }
        val body = JSONObject()
        body.put("transactions", arr)
        if (monthlyBudget > 0) body.put("monthly_budget", monthlyBudget)
        body.put("horizon_days", horizonDays)
        body.put("dismissed_keys", JSONArray(dismissedKeys.toList()))
        return body
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

    private fun parse(text: String): AiReport {
        val root = JSONObject(text)
        val f = root.getJSONObject("forecast")
        val forecast = if (f.optBoolean("available", false)) {
            val m = f.getJSONObject("metrics")
            AiForecast(
                methodUsed = f.getString("method_used"),
                metrics = m.keys().asSequence().map { name ->
                    val r = m.getJSONObject(name)
                    AiMetric(name, r.getDouble("mae"), r.getDouble("rmse"))
                }.toList(),
                days = f.getJSONArray("forecast_days").objects().map {
                    AiForecastDay(
                        it.getString("date"), it.getDouble("predicted"),
                        it.getDouble("low"), it.getDouble("high")
                    )
                },
                totalPredicted = f.getDouble("total_predicted"),
                totalLow = f.getDouble("total_low"),
                totalHigh = f.getDouble("total_high"),
                metricsNote = f.getString("metrics_note"),
                disclaimer = f.getString("disclaimer")
            )
        } else null

        val anomalies = root.getJSONObject("anomalies").getJSONArray("anomalies").objects().map {
            AiAnomaly(
                it.getString("key"), it.getString("date"), it.getString("title"),
                it.getString("category"), it.getDouble("amount"), it.getString("why_flagged")
            )
        }
        val insights = root.getJSONArray("insights").objects().map {
            AiInsight(it.getString("text"), it.getString("calculation"))
        }
        return AiReport(
            asOf = root.getString("as_of"),
            forecast = forecast,
            forecastReason = f.optString("reason", ""),
            anomalies = anomalies,
            insights = insights
        )
    }
}