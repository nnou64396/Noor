package com.noor.islamic.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.noor.islamic.R
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.components.NoorSettingRow
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorLargeTitle

@Composable
fun SettingsScreen(
    language: Language,
    themeMode: ThemeMode,
    onLanguageSelected: (Language) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    NoorScreenContainer(modifier = modifier) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.noorLargeTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Column(verticalArrangement = Arrangement.spacedBy(NoorDimens.Md)) {
            NoorSectionTitle(
                title = stringResource(R.string.settings_section_appearance),
                icon = Icons.Rounded.Palette,
            )
            NoorSettingRow(
                title = stringResource(R.string.pref_theme),
                subtitle = stringResource(themeMode.displayNameRes),
                icon = Icons.Rounded.Palette,
                onClick = { showThemeDialog = true },
            )

            NoorSectionTitle(
                title = stringResource(R.string.settings_section_language),
                icon = Icons.Rounded.Translate,
            )
            NoorSettingRow(
                title = stringResource(R.string.pref_language),
                subtitle = stringResource(language.displayNameRes),
                icon = Icons.Rounded.Translate,
                onClick = { showLanguageDialog = true },
            )

            NoorSectionTitle(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Rounded.Info,
            )
            NoorSettingRow(
                title = stringResource(R.string.settings_about_title),
                icon = Icons.Rounded.Info,
                onClick = { showAboutDialog = true },
            )
            NoorSettingRow(
                title = stringResource(R.string.settings_version_title),
                subtitle = stringResource(R.string.settings_version_value),
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
            )
        }
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_theme),
            options = ThemeMode.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = ThemeMode.entries.indexOf(themeMode),
            onOptionSelected = { index ->
                onThemeSelected(ThemeMode.entries[index])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_language),
            options = Language.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = Language.entries.indexOf(language),
            onOptionSelected = { index ->
                onLanguageSelected(Language.entries[index])
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NoorDimens.Xs),
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(index) }
                            .padding(vertical = NoorDimens.Sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onOptionSelected(index) },
                        )
                        Spacer(Modifier.width(NoorDimens.Sm))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(R.string.settings_about_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NoorDimens.Sm)) {
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_version_title) +
                        " " +
                        stringResource(R.string.settings_version_value),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = NoorDimens.Sm),
                )
            }
        },
    )
}