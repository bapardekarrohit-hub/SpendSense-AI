package com.rohit.spendsense

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Put your own email here if you want feedback emails pre-addressed to you.
private val SUPPORT_EMAIL = ""

private fun sendFeedback(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        if (SUPPORT_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "SpendSense AI feedback")
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spendsense_prefs", Context.MODE_PRIVATE) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val name by viewModel.profileName.collectAsStateWithLifecycle()
    val email by viewModel.profileEmail.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    val notificationsOn by viewModel.notificationsOn.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val saved = try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(transactionsToCsv(transactions).toByteArray())
                } != null
            } catch (e: Exception) {
                false
            }
            if (saved) {
                prefs.edit().putLong("last_backup", System.currentTimeMillis()).apply()
            }
            val message =
                if (saved) "Saved ${transactions.size} transactions" else "Save failed"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val sb = StringBuilder()
                    val buffer = CharArray(8192)
                    while (true) {
                        val n = reader.read(buffer)
                        if (n < 0) break
                        sb.append(buffer, 0, n)
                        if (sb.length > 2_000_000) break
                    }
                    sb.toString()
                }
            } catch (e: Exception) {
                null
            }
            importResult = if (text == null) {
                ImportResult(fatal = "The file could not be read.")
            } else {
                parseTransactionCsv(text, transactions)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotifications(true)
        } else {
            Toast.makeText(context, "Allow notifications to get budget alerts", Toast.LENGTH_LONG)
                .show()
        }
    }

    val startExport = {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "Nothing to export yet", Toast.LENGTH_SHORT).show()
        } else {
            exportLauncher.launch("spendsense_backup.csv")
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
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        ProfileCard(name = name, email = email, onClick = { dialog = SettingsDialog.Profile })
        AccountSection(
            onProfile = { dialog = SettingsDialog.Profile },
            onPassword = { dialog = SettingsDialog.Password },
            onPrivacy = { dialog = SettingsDialog.Privacy }
        )
        PreferencesSection(
            themeMode = themeMode,
            onThemeChange = { viewModel.setThemeMode(it) },
            currencyCode = currencyCode,
            notificationsOn = notificationsOn,
            onNotificationsChange = { wantOn ->
                val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                if (wantOn && needsPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setNotifications(wantOn)
                }
            },
            onLanguage = { dialog = SettingsDialog.Language },
            onCurrency = { dialog = SettingsDialog.Currency }
        )

        ReminderSection()

        DataSection(
            onCloud = { dialog = SettingsDialog.Cloud },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onExport = { startExport() },
            onWelcome = {
                viewModel.showWelcomeNextLaunch()
                Toast.makeText(context, "Welcome page will show next time", Toast.LENGTH_SHORT)
                    .show()
            },
            onDelete = { dialog = SettingsDialog.DeleteAll }
        )
        HelpSection(
            onHelp = { dialog = SettingsDialog.Help },
            onContact = { sendFeedback(context) }
        )
        Text(
            text = "SpendSense AI · v1.0 · Your data stays on this phone",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val close = { dialog = null }
    when (dialog) {
        SettingsDialog.Profile -> ProfileDialog(name, email, close) { n, e ->
            viewModel.setProfile(n, e)
            dialog = null
        }

        SettingsDialog.Currency -> CurrencyDialog(currencyCode, close) {
            viewModel.setCurrency(it)
            dialog = null
        }

        SettingsDialog.Help -> HelpDialog(close)

        SettingsDialog.DeleteAll -> ConfirmDeleteDialog(transactions.size, {
            viewModel.deleteAllData()
            dialog = null
        }, close)

        SettingsDialog.Privacy -> PrivacyDialog(close)

        SettingsDialog.Password -> InfoDialog(
            "Change Password",
            "A password is needed once accounts exist. Accounts arrive with cloud sync in a " +
                    "later phase, so there is no password to change yet.",
            close
        )

        SettingsDialog.Language -> InfoDialog(
            "Language",
            "English is the only language for now. More languages can be added later.",
            close
        )

        SettingsDialog.Cloud -> BackupDialog(
            transactionCount = transactions.size,
            lastBackupMillis = prefs.getLong("last_backup", 0L),
            onBackup = {
                dialog = null
                startExport()
            },
            onRestore = {
                dialog = null
                importLauncher.launch(arrayOf("*/*"))
            },
            onClose = close
        )

        null -> {}
    }

    importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onConfirm = {
                viewModel.addTransactions(result.imported)
                Toast.makeText(
                    context,
                    "Imported ${result.imported.size} transactions",
                    Toast.LENGTH_SHORT
                ).show()
                importResult = null
            },
            onDismiss = { importResult = null }
        )
    }
}