package com.rohit.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.util.Locale

// Rules: only expenses count, and only inside the given month.
fun monthTotal(transactions: List<Transaction>, month: YearMonth): Double =
    transactions
        .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
        .sumOf { it.amount }

fun expensesByCategory(transactions: List<Transaction>, month: YearMonth): List<Pair<Category, Double>> =
    transactions
        .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
        .groupBy { it.category }
        .map { entry -> entry.key to entry.value.sumOf { it.amount } }
        .sortedByDescending { it.second }

@Composable
fun BudgetsScreen(
    transactions: List<Transaction>,
    monthlyBudget: Double,
    onSetBudget: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val thisMonth = YearMonth.now()
    val lastMonth = thisMonth.minusMonths(1)
    val spent = monthTotal(transactions, thisMonth)
    val lastSpent = monthTotal(transactions, lastMonth)
    val usage = if (monthlyBudget > 0) spent / monthlyBudget * 100 else 0.0
    val byCategory = expensesByCategory(transactions, thisMonth)
    var showDialog by remember { mutableStateOf(false) }

    val (statusText, statusColor) = when {
        usage >= 100 ->
            "Budget exceeded by ${formatRupees(spent - monthlyBudget)}" to MaterialTheme.colorScheme.error
        usage >= 80 ->
            "Careful: ${String.format(Locale.US, "%.0f", usage)}% used, ${formatRupees(monthlyBudget - spent)} left" to Color(0xFFD97706)
        else ->
            "On track · ${formatRupees(monthlyBudget - spent)} left" to Color(0xFF16A34A)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Budgets",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        SectionCard {
            Text(
                text = "Monthly budget",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatRupees(monthlyBudget),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Spent ${formatRupees(spent)} (${String.format(Locale.US, "%.0f", usage)}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { (usage / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                color = statusColor
            )
            Button(onClick = { showDialog = true }) {
                Text("Change budget")
            }
        }

        SectionCard {
            Text(
                text = "Compared with last month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastSpent > 0) {
                val change = (spent - lastSpent) / lastSpent * 100
                val direction = if (change >= 0) "higher" else "lower"
                Text(
                    text = "Last month: ${formatRupees(lastSpent)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This month so far is ${String.format(Locale.US, "%.0f", Math.abs(change))}% $direction",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No expenses recorded last month, so there is nothing to compare yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard {
            Text(
                text = "Spending by category (this month)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (byCategory.isEmpty()) {
                Text(
                    text = "No expenses this month yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            byCategory.forEach { (category, amount) ->
                val share = if (spent > 0) amount / spent else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatRupees(amount)} · ${String.format(Locale.US, "%.0f", share * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { share.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            current = monthlyBudget,
            onDismiss = { showDialog = false },
            onSave = {
                onSetBudget(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun BudgetDialog(
    current: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(String.format(Locale.US, "%.0f", current)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; error = null },
                label = { Text("Amount (${CurrencySettings.symbol})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null,
                supportingText = { error?.let { Text(it) } }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val value = text.trim().toDoubleOrNull()
                when {
                    value == null -> error = "Enter a valid number"
                    value <= 0.0 -> error = "Budget must be greater than 0"
                    value > 10_000_000.0 -> error = "Budget is too large"
                    else -> onSave(value)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}