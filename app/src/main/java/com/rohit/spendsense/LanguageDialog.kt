package com.rohit.spendsense

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun LanguageDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val current = LocaleHelper.saved(context)
    val systemLabel = stringResource(R.string.language_system)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column {
                LocaleHelper.options.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == code,
                                onClick = {
                                    LocaleHelper.apply(context, code)
                                    onClose()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == code, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(if (code.isEmpty()) systemLabel else label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } }
    )
}