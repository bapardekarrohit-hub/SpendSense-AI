package com.rohit.spendsense

import androidx.compose.runtime.Composable

@Composable
fun SettingsDialogHost(
    dialog: SettingsDialog?,
    name: String,
    email: String,
    currencyCode: String,
    transactionCount: Int,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    when (dialog) {
        SettingsDialog.Profile -> ProfileDialog(name, email, onClose) { n, e ->
            viewModel.setProfile(n, e)
            onClose()
        }

        SettingsDialog.Currency -> CurrencyDialog(currencyCode, onClose) {
            viewModel.setCurrency(it)
            onClose()
        }

        SettingsDialog.Help -> HelpDialog(onClose)

        SettingsDialog.DeleteAll -> ConfirmDeleteDialog(transactionCount, {
            viewModel.deleteAllData()
            onClose()
        }, onClose)

        SettingsDialog.Privacy -> InfoDialog(
            "Privacy & Data",
            "Your transactions, budget and settings are stored only on this phone. This version " +
                    "has no account, no analytics and does not use the internet. You can export " +
                    "everything to a CSV file or delete it all from the Data & Sync section.",
            onClose
        )

        SettingsDialog.Password -> InfoDialog(
            "Change Password",
            "A password is needed once accounts exist. Accounts arrive with cloud sync in a " +
                    "later phase, so there is no password to change yet.",
            onClose
        )

        SettingsDialog.Language -> InfoDialog(
            "Language",
            "English is the only language for now. More languages can be added later.",
            onClose
        )

        SettingsDialog.Cloud -> InfoDialog(
            "Sync with Cloud",
            "Cloud backup is not available yet. It will be added together with accounts, after " +
                    "the Python backend. Until then your data stays on this phone. Use Export to CSV " +
                    "for a backup.",
            onClose
        )

        null -> {}
    }
}