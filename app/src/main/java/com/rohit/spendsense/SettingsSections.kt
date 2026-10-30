package com.rohit.spendsense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rohit.spendsense.ui.theme.BrandBlue
import com.rohit.spendsense.ui.theme.BrandPurple
import androidx.compose.material.icons.filled.Add

@Composable
fun ProfileCard(name: String, email: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandPurple, BrandBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.trim().take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Your name" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email.ifBlank { "Tap to add your details" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ArrowIcon()
        }
    }
}

@Composable
fun AccountSection(onProfile: () -> Unit, onPassword: () -> Unit, onPrivacy: () -> Unit) {
    SettingsSection("Account") {
        SettingsRow("Profile Information", "Update your personal details",
            Color(0xFF4F46E5), { RowIcon(Icons.Filled.Person) }, onProfile)
        SettingsDivider()
        SettingsRow("Change Password", "Needs an account (coming later)",
            Color(0xFF16A34A), { RowIcon(Icons.Filled.Lock) }, onPassword,
            trailing = { TrailingText("Soon", MaterialTheme.colorScheme.secondary) })
        SettingsDivider()
        SettingsRow("Privacy & Data", "How your data is handled",
            Color(0xFF7C3AED), { RowIcon(Icons.Filled.Info) }, onPrivacy)
    }
}

@Composable
fun PreferencesSection(
    themeMode: String,
    onThemeChange: (String) -> Unit,
    currencyCode: String,
    notificationsOn: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onLanguage: () -> Unit,
    onCurrency: () -> Unit
) {
    SettingsSection("App Preferences") {
        SettingsRow("Theme", "Choose your preferred appearance",
            Color(0xFFB45309), { RowIcon(Icons.Filled.Settings) }, trailing = {})
        Row(
            modifier = Modifier.padding(start = 72.dp, end = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { option ->
                FilterChip(
                    selected = themeMode == option.first,
                    onClick = { onThemeChange(option.first) },
                    label = { Text(option.second) }
                )
            }
        }
        SettingsDivider()
        SettingsRow("Language", "Select your app language",
            Color(0xFF2563EB), { RowGlyph("A") }, onLanguage,
            trailing = { TrailingText("English") })
        SettingsDivider()
        SettingsRow("Currency", "Set your preferred currency",
            Color(0xFF7C3AED), { RowGlyph(CurrencySettings.symbol) }, onCurrency,
            trailing = { TrailingText(currencyCode) })
        SettingsDivider()
        SettingsRow("Notifications", "Budget alerts at 80% and 100%",
            Color(0xFFDB2777), { RowIcon(Icons.Filled.Notifications) },
            trailing = { Switch(checked = notificationsOn, onCheckedChange = onNotificationsChange) })
    }
}

@Composable
fun DataSection(
    onCloud: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onWelcome: () -> Unit,
    onDelete: () -> Unit
) {
    SettingsSection("Data & Sync") {
        SettingsRow("Sync with Cloud", "Backup is not available yet",
            Color(0xFF0891B2), { RowIcon(Icons.Filled.Refresh) }, onCloud,
            trailing = { TrailingText("Not connected", Color(0xFFEF4444)) })
        SettingsDivider()
        SettingsRow("Import from CSV", "Add transactions from a file",
            Color(0xFF059669), { RowIcon(Icons.Filled.Add) }, onImport)
        SettingsDivider()
        SettingsRow("Export to CSV", "Save your transactions to a file",
            Color(0xFF4F46E5), { RowIcon(Icons.Filled.Share) }, onExport)
        SettingsDivider()
        SettingsRow("Show welcome page", "Show the start page next time",
            Color(0xFF0EA5E9), { RowIcon(Icons.Filled.PlayArrow) }, onWelcome)
        SettingsDivider()
        SettingsRow("Delete all data", "Remove everything from this phone",
            Color(0xFFDC2626), { RowIcon(Icons.Filled.Delete) }, onDelete)
    }
}

@Composable
fun HelpSection(onHelp: () -> Unit, onContact: () -> Unit) {
    SettingsSection("Help & Support") {
        SettingsRow("Help Center", "FAQs and guides",
            Color(0xFF2563EB), { RowGlyph("?") }, onHelp)
        SettingsDivider()
        SettingsRow("Send feedback", "Open your email app",
            Color(0xFF16A34A), { RowIcon(Icons.Filled.Email) }, onContact)
    }
}