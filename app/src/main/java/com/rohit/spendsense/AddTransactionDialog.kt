package com.rohit.spendsense

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val paymentMethods = listOf("UPI", "Card", "Cash", "Bank transfer")

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var category by rememberSaveable { mutableStateOf(Category.Food) }
    var payment by rememberSaveable { mutableStateOf("UPI") }
    var notes by rememberSaveable { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    fun validateAndSave() {
        val cleanTitle = title.trim()
        val amount = amountText.trim().toDoubleOrNull()

        titleError = when {
            cleanTitle.isEmpty() -> "Enter a title"
            cleanTitle.length > 60 -> "Title is too long (max 60 characters)"
            else -> null
        }
        amountError = when {
            amount == null -> "Enter a valid number"
            amount <= 0.0 -> "Amount must be greater than 0"
            amount > 10_000_000.0 -> "Amount is too large"
            else -> null
        }

        if (titleError == null && amountError == null && amount != null) {
            onSave(
                Transaction(
                    title = cleanTitle,
                    amount = amount,
                    type = type,
                    category = category,
                    date = LocalDate.now(),
                    paymentMethod = payment,
                    notes = notes.trim()
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; amountError = null },
                    label = { Text("Amount (${CurrencySettings.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Text("Payment method", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        FilterChip(
                            selected = payment == method,
                            onClick = { payment = method },
                            label = { Text(method) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { validateAndSave() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}