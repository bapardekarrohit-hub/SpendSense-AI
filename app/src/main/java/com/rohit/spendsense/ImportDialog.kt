package com.rohit.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ImportResultDialog(result: ImportResult, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val fatal = result.fatal
    if (fatal != null) {
        InfoDialog("Cannot import", fatal, onDismiss)
        return
    }
    val count = result.imported.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import preview") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${result.totalRows} rows found in the file.")
                Text("$count ready to import.", fontWeight = FontWeight.SemiBold)
                if (result.duplicates > 0) {
                    Text("${result.duplicates} already exist and will be skipped.")
                }
                if (result.errors.isNotEmpty()) {
                    Text(
                        "${result.errors.size} rows have problems and will be skipped:",
                        color = MaterialTheme.colorScheme.error
                    )
                    result.errors.take(8).forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.errors.size > 8) {
                        Text("…and ${result.errors.size - 8} more.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = count > 0) {
                Text(if (count > 0) "Import $count" else "Nothing to import")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}