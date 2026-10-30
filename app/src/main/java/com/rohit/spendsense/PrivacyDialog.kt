package com.rohit.spendsense

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    var blocked by remember { mutableStateOf(PrivacyControls.isBlocked(context)) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Privacy & Data") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block screenshots", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Hides the app in screenshots, screen recordings and the recent-apps preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    }
                    Switch(
                        checked = blocked,
                        onCheckedChange = {
                            blocked = it
                            PrivacyControls.setBlocked(context, it)
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        PrivacyControls.clearAiData(context)
                        Toast.makeText(context, "AI server address and dismissed alerts cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear AI server address and dismissed alerts")
                }

                Text("What stays on this phone", fontWeight = FontWeight.SemiBold)
                Text(
                    "Your transactions, budget, profile and settings are stored only on this phone. " +
                            "There is no account and no analytics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )

                Text("What can leave this phone", fontWeight = FontWeight.SemiBold)
                Text(
                    "1. When you tap Analyze on the AI tab: title, amount, type, category and date of " +
                            "your transactions (no notes, no payment method) go to the server address " +
                            "you typed. Use only a server you run yourself.\n" +
                            "2. When you save a backup or export: a CSV file in the place you choose.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )

                Text(
                    "You can delete everything from Settings > Delete all data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}