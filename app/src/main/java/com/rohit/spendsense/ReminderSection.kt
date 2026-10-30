package com.rohit.spendsense

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

private val REMINDER_TIMES = listOf(
    8 to "8 AM", 12 to "12 PM", 18 to "6 PM", 20 to "8 PM", 21 to "9 PM", 22 to "10 PM"
)

@Composable
fun ReminderSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(prefs.getBoolean("reminder_on", false)) }
    var hour by remember { mutableStateOf(prefs.getInt("reminder_hour", 20)) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    fun enable() {
        on = true
        prefs.edit().putBoolean("reminder_on", true).apply()
        ReminderScheduler.schedule(context, hour)
    }

    fun disable() {
        on = false
        prefs.edit().putBoolean("reminder_on", false).apply()
        ReminderScheduler.cancel(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enable()
        } else {
            Toast.makeText(context, "Allow notifications to get the reminder", Toast.LENGTH_LONG).show()
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Daily reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "A nudge to log today's expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
                Switch(
                    checked = on,
                    onCheckedChange = { wantOn ->
                        if (!wantOn) {
                            disable()
                        } else if (!ReminderNotifier.canNotify(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            enable()
                        }
                    }
                )
            }

            if (on) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REMINDER_TIMES.forEach { (h, label) ->
                        FilterChip(
                            selected = hour == h,
                            onClick = {
                                hour = h
                                prefs.edit().putInt("reminder_hour", h).apply()
                                ReminderScheduler.schedule(context, h)
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    "Android runs background jobs when it can, so the reminder may arrive a few minutes " +
                            "after the chosen time, and later if battery saver is on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }

            OutlinedButton(
                onClick = {
                    if (ReminderNotifier.canNotify(context)) {
                        ReminderNotifier.show(context)
                    } else {
                        Toast.makeText(context, "Allow notifications first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send a test reminder now")
            }
        }
    }
}