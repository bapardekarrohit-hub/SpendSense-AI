package com.rohit.spendsense

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun ProfileDialog(
    name: String,
    email: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var nameText by remember { mutableStateOf(name) }
    var emailText by remember { mutableStateOf(email) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it; nameError = null },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } }
                )
                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it; emailError = null },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } }
                )
                Text(
                    text = "Saved only on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanName = nameText.trim()
                val cleanEmail = emailText.trim()
                nameError = when {
                    cleanName.isEmpty() -> "Enter your name"
                    cleanName.length > 40 -> "Name is too long (max 40)"
                    else -> null
                }
                emailError = if (cleanEmail.isNotEmpty() &&
                    !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
                ) "Enter a valid email" else null
                if (nameError == null && emailError == null) onSave(cleanName, cleanEmail)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CurrencyDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            Column {
                currencyOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.code) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option.code, onClick = null)
                        Text(
                            text = "  ${option.symbol}  ${option.code} · ${option.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Text(
                    text = "Only the symbol changes. Amounts are not converted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val faqs = listOf(
        "How do I add a transaction?" to "Open Transactions and tap the + button.",
        "How is my budget used?" to "Set it in Budgets. Only expenses of the current month count; income is never counted as an expense.",
        "Where is my data stored?" to "Only on this phone, in a local database.",
        "How do I back up my data?" to "Settings → Export to CSV saves everything to a file."
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help Center") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                faqs.forEach { (question, answer) ->
                    Column {
                        Text(question, style = MaterialTheme.typography.titleSmall)
                        Text(
                            answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ConfirmDeleteDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete all data?") },
        text = {
            Text(
                "All $count transactions will be removed and your budget will be reset. " +
                        "This cannot be undone. Export a CSV first if you want a backup."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete everything") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}