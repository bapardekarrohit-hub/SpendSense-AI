package com.rohit.spendsense

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

private const val DEFAULT_SERVER = "http://192.168.0.102:8000"

private fun money(v: Double) = CurrencySettings.symbol + String.format(Locale.US, "%,.0f", v)

private fun prettyMethod(name: String) = when (name) {
    "ridge_regression" -> "Ridge regression model"
    "baseline_last_7_day_average" -> "Baseline: last 7-day average"
    "baseline_same_weekday_average" -> "Baseline: same-weekday average"
    else -> name
}

@Composable
fun AiInsightsScreen(
    transactions: List<Transaction>,
    monthlyBudget: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var serverUrl by remember {
        mutableStateOf(prefs.getString("ai_server_url", DEFAULT_SERVER) ?: DEFAULT_SERVER)
    }
    var horizon by remember { mutableStateOf(7) }
    var dismissed by remember {
        mutableStateOf(prefs.getStringSet("dismissed_anomalies", emptySet())?.toSet() ?: emptySet())
    }
    var state by remember { mutableStateOf<AiState>(AiState.Idle) }
    val hasExpenses = transactions.any { it.type == TransactionType.EXPENSE }
    val lanPermission = "android.permission.ACCESS_LOCAL_NETWORK"

    val runAnalysis: () -> Unit = {
        prefs.edit().putString("ai_server_url", serverUrl.trim()).apply()
        state = AiState.Loading
        scope.launch {
            state = try {
                val report = AiClient.analyze(serverUrl, transactions, monthlyBudget, horizon, dismissed)
                report.forecast?.let { ForecastLog.save(context, it.days) }
                AiState.Success(report)
            } catch (e: AiException) {
                AiState.Error(e.message ?: "Something went wrong.")
            } catch (e: Exception) {
                AiState.Error("Unexpected problem: ${e.message}")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runAnalysis()
        } else {
            state = AiState.Error(
                "Android needs the \"Nearby devices\" permission to reach your PC on your Wi-Fi. Allow it and try again."
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "AI Insights",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Forecast, unusual spending and insights calculated by your Python server. " +
                    "Your transactions (without notes) are sent to the address below, so use only a server you run yourself.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = horizon == 7, onClick = { horizon = 7 }, label = { Text("Next 7 days") })
            FilterChip(selected = horizon == 30, onClick = { horizon = 30 }, label = { Text("Next 30 days") })
        }

        Button(
            onClick = {
                val needsPermission = Build.VERSION.SDK_INT >= 37 &&
                        ContextCompat.checkSelfPermission(context, lanPermission) !=
                        PackageManager.PERMISSION_GRANTED
                if (needsPermission) permissionLauncher.launch(lanPermission) else runAnalysis()
            },
            enabled = state !is AiState.Loading && hasExpenses,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasExpenses) "Analyze my spending" else "Add some expenses first")
        }
        ForecastReportCardView(transactions = transactions, refreshKey = state)
        when (val s = state) {
            AiState.Idle -> Text(
                "Tap the button to run the analysis. Make sure the server is running on your PC.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AiState.Loading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text("Analysing…", color = MaterialTheme.colorScheme.onBackground)
            }

            is AiState.Error -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    s.message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            is AiState.Success -> ReportView(
                report = s.report,
                dismissed = dismissed,
                onDismiss = { key ->
                    dismissed = dismissed + key
                    prefs.edit().putStringSet("dismissed_anomalies", HashSet(dismissed)).apply()
                }
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ReportView(report: AiReport, dismissed: Set<String>, onDismiss: (String) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val f = report.forecast

    if (f == null) {
        SectionCard("Spending forecast") { Text(report.forecastReason) }
    } else {
        SectionCard("Spending forecast (next ${f.days.size} days)") {
            Text("Estimated total: ${money(f.totalPredicted)}", style = MaterialTheme.typography.titleLarge)
            Text("Likely range: ${money(f.totalLow)} – ${money(f.totalHigh)}")
            ForecastBars(f.days)
            Text("Bars = estimate per day, thin line = likely range.", style = MaterialTheme.typography.bodySmall, color = muted)
            HorizontalDivider()
            Text("Model check (lower is better)", style = MaterialTheme.typography.titleSmall)
            f.metrics.forEach { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        prettyMethod(m.name) + if (m.name == f.methodUsed) "  ✓ used" else "",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("MAE ${m.mae.toInt()} · RMSE ${m.rmse.toInt()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(f.metricsNote, style = MaterialTheme.typography.bodySmall, color = muted)
            Text(f.disclaimer, style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }

    val shown = report.anomalies.filter { it.key !in dismissed }
    SectionCard("Unusual spending") {
        if (shown.isEmpty()) {
            Text("Nothing unusual found.", color = muted)
        }
        shown.forEach { a ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${a.title} · ${money(a.amount)}", fontWeight = FontWeight.SemiBold)
                Text("${a.category} · ${a.date}", style = MaterialTheme.typography.bodySmall, color = muted)
                Text(a.why, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onDismiss(a.key) }) { Text("Dismiss – this is normal for me") }
            }
        }
    }

    SectionCard("Insights (as of ${report.asOf})") {
        if (report.insights.isEmpty()) {
            Text("Not enough data for insights yet.", color = muted)
        }
        report.insights.forEach { i ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(i.text, fontWeight = FontWeight.SemiBold)
                Text("How: ${i.calculation}", style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
    }
}

@Composable
private fun ForecastBars(days: List<AiForecastDay>) {
    val barColor = MaterialTheme.colorScheme.primary
    val rangeColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val maxV = (days.maxOfOrNull { it.high } ?: 1.0).coerceAtLeast(1.0)
        val slot = size.width / days.size
        val barW = slot * 0.6f
        days.forEachIndexed { i, d ->
            val x = i * slot + (slot - barW) / 2f
            val h = (d.predicted / maxV).toFloat() * size.height
            drawRect(color = barColor, topLeft = Offset(x, size.height - h), size = Size(barW, h))
            val cx = x + barW / 2f
            drawLine(
                color = rangeColor,
                start = Offset(cx, size.height - (d.high / maxV).toFloat() * size.height),
                end = Offset(cx, size.height - (d.low / maxV).toFloat() * size.height),
                strokeWidth = 3f
            )
        }
    }
}