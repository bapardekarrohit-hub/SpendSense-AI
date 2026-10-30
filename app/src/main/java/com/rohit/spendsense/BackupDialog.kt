package com.rohit.spendsense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupDialog(
    transactionCount: Int,
    lastBackupMillis: Long,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    val lastText = if (lastBackupMillis > 0L) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastBackupMillis))
    } else {
        "never"
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Backup & Restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$transactionCount transactions on this phone.")
                Text("Last backup: $lastText", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("Back up now")
                }
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore from a backup file")
                }
                Text(
                    "A backup is a CSV file you choose where to save (for example Downloads or Google Drive). " +
                            "Restoring adds the transactions from a file, skips ones you already have, and " +
                            "shows any invalid rows before importing. It never deletes your current data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}