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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rohit.spendsense.ui.theme.BrandBlue
import com.rohit.spendsense.ui.theme.BrandPurple
import androidx.compose.material.icons.filled.Add
import android.content.Context

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
                    text = name.ifBlank { stringResource(R.string.profile_your_name) },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email.ifBlank { stringResource(R.string.profile_tap_to_add) },
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
    SettingsSection(stringResource(R.string.settings_account)) {
        SettingsRow(stringResource(R.string.row_profile_title), stringResource(R.string.row_profile_sub),
            Color(0xFF4F46E5), { RowIcon(Icons.Filled.Person) }, onProfile)
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_password_title), stringResource(R.string.row_password_sub),
            Color(0xFF16A34A), { RowIcon(Icons.Filled.Lock) }, onPassword,
            trailing = { TrailingText(stringResource(R.string.soon), MaterialTheme.colorScheme.secondary) })
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_privacy_title), stringResource(R.string.row_privacy_sub),
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
    SettingsSection(stringResource(R.string.settings_preferences)) {
        SettingsRow(stringResource(R.string.theme_title), stringResource(R.string.theme_sub),
            Color(0xFFB45309), { RowIcon(Icons.Filled.Settings) }, trailing = {})
        Row(
            modifier = Modifier.padding(start = 72.dp, end = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "system" to R.string.theme_system,
                "light" to R.string.theme_light,
                "dark" to R.string.theme_dark
            ).forEach { option ->
                FilterChip(
                    selected = themeMode == option.first,
                    onClick = { onThemeChange(option.first) },
                    label = { Text(stringResource(option.second)) }
                )
            }
        }
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_language_title), stringResource(R.string.row_language_sub),
            Color(0xFF2563EB), { RowGlyph("A") }, onLanguage,
            trailing = { TrailingText(currentLanguageLabel()) })
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_currency_title), stringResource(R.string.row_currency_sub),
            Color(0xFF7C3AED), { RowGlyph(CurrencySettings.symbol) }, onCurrency,
            trailing = { TrailingText(currencyCode) })
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_notif_title), stringResource(R.string.row_notif_sub),
            Color(0xFFDB2777), { RowIcon(Icons.Filled.Notifications) },
            trailing = { Switch(checked = notificationsOn, onCheckedChange = onNotificationsChange) })
    }
}

@Composable
fun DataSection(
    onCloud: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    SettingsSection(stringResource(R.string.settings_data)) {
        SettingsRow(stringResource(R.string.row_backup_title), stringResource(R.string.row_backup_sub),
            Color(0xFF0891B2), { RowIcon(Icons.Filled.Refresh) }, onCloud)
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_import_title), stringResource(R.string.row_import_sub),
            Color(0xFF059669), { RowIcon(Icons.Filled.Add) }, onImport)
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_export_title), stringResource(R.string.row_export_sub),
            Color(0xFF4F46E5), { RowIcon(Icons.Filled.Share) }, onExport)
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_delete_title), stringResource(R.string.row_delete_sub),
            Color(0xFFDC2626), { RowIcon(Icons.Filled.Delete) }, onDelete)
    }
}

@Composable
fun HelpSection(onHelp: () -> Unit, onContact: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_help)) {
        SettingsRow(stringResource(R.string.row_help_title), stringResource(R.string.row_help_sub),
            Color(0xFF2563EB), { RowGlyph("?") }, onHelp)
        SettingsDivider()
        SettingsRow(stringResource(R.string.row_feedback_title), stringResource(R.string.row_feedback_sub),
            Color(0xFF16A34A), { RowIcon(Icons.Filled.Email) }, onContact)
    }
}

@Composable
private fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val code = context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE)
        .getString("app_language", "") ?: ""
    return when (code) {
        "en" -> "English"
        "hi" -> "हिन्दी"
        "mr" -> "मराठी"
        else -> stringResource(R.string.language_system)
    }
}