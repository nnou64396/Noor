package com.noor.islamic.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.noor.islamic.R
import com.noor.islamic.domain.model.AsrSchool
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.CalculationMethodConfig
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.LocationMode
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
    calculationMethod: CalculationMethod = CalculationMethodConfig.defaultMethod,
    asrSchool: AsrSchool = AsrSchool.DEFAULT,
    locationMode: LocationMode = LocationMode.DEFAULT,
    manualLatitude: Double? = null,
    manualLongitude: Double? = null,
    onLanguageSelected: (Language) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onCalculationMethodSelected: (CalculationMethod) -> Unit = {},
    onAsrSchoolSelected: (AsrSchool) -> Unit = {},
    onLocationModeSelected: (LocationMode) -> Unit = {},
    onManualLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    updateStatus: com.noor.islamic.domain.model.UpdateStatus = com.noor.islamic.domain.model.UpdateStatus.UpToDate,
    onCheckForUpdates: () -> Unit = {},
    onDismissUpdate: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCalculationMethodDialog by remember { mutableStateOf(false) }
    var showAsrSchoolDialog by remember { mutableStateOf(false) }
    var showLocationModeDialog by remember { mutableStateOf(false) }
    var showManualLocationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // The version is read from the package at runtime rather than stored as a
    // string resource, so it can never drift from the built versionName.
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    NoorScreenContainer(modifier = modifier) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.noorLargeTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.size(NoorDimens.Sm))

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
                title = stringResource(R.string.settings_section_prayer),
                icon = Icons.Rounded.LocationOn,
            )
            NoorSettingRow(
                title = stringResource(R.string.pref_prayer_calculation_method),
                subtitle = stringResource(calculationMethod.displayNameRes),
                icon = Icons.Rounded.LocationOn,
                onClick = { showCalculationMethodDialog = true },
            )
            NoorSettingRow(
                title = stringResource(R.string.pref_prayer_asr_school),
                subtitle = stringResource(asrSchool.displayNameRes),
                icon = Icons.Rounded.LocationOn,
                onClick = { showAsrSchoolDialog = true },
            )
            NoorSettingRow(
                title = stringResource(R.string.pref_prayer_location),
                subtitle = if (
                    locationMode == LocationMode.MANUAL &&
                    manualLatitude != null && manualLongitude != null
                ) {
                    stringResource(
                        R.string.manual_location_coordinates,
                        manualLatitude,
                        manualLongitude,
                    )
                } else {
                    stringResource(locationMode.displayNameRes)
                },
                icon = Icons.Rounded.LocationOn,
                onClick = { showLocationModeDialog = true },
            )

            NoorSectionTitle(
                title = stringResource(R.string.update_section_title),
                icon = Icons.Rounded.Info,
            )
            if (updateStatus is com.noor.islamic.domain.model.UpdateStatus.UpdateAvailable || updateStatus is com.noor.islamic.domain.model.UpdateStatus.UpdateRequired) {
                com.noor.islamic.ui.components.UpdateBannerCard(
                    updateStatus = updateStatus,
                    onDismiss = onDismissUpdate,
                )
            }
            NoorSettingRow(
                title = stringResource(R.string.update_check_action),
                subtitle = when (updateStatus) {
                    is com.noor.islamic.domain.model.UpdateStatus.UpToDate -> stringResource(R.string.update_status_up_to_date)
                    is com.noor.islamic.domain.model.UpdateStatus.Disabled -> stringResource(R.string.update_status_disabled)
                    is com.noor.islamic.domain.model.UpdateStatus.CheckUnavailable -> stringResource(R.string.update_status_unavailable)
                    is com.noor.islamic.domain.model.UpdateStatus.UpdateAvailable -> stringResource(R.string.update_available_title, updateStatus.info.latestVersion)
                    is com.noor.islamic.domain.model.UpdateStatus.UpdateRequired -> stringResource(R.string.update_required_title, updateStatus.info.latestVersion)
                },
                icon = Icons.Rounded.Info,
                onClick = onCheckForUpdates,
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
                subtitle = versionName,
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

    if (showCalculationMethodDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_prayer_calculation_method),
            options = CalculationMethod.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = CalculationMethod.entries.indexOf(calculationMethod),
            onOptionSelected = { index ->
                onCalculationMethodSelected(CalculationMethod.entries[index])
                showCalculationMethodDialog = false
            },
            onDismiss = { showCalculationMethodDialog = false },
        )
    }

    if (showAsrSchoolDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_prayer_asr_school),
            options = AsrSchool.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = AsrSchool.entries.indexOf(asrSchool),
            onOptionSelected = { index ->
                onAsrSchoolSelected(AsrSchool.entries[index])
                showAsrSchoolDialog = false
            },
            onDismiss = { showAsrSchoolDialog = false },
        )
    }

    if (showLocationModeDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_prayer_location),
            options = LocationMode.entries.map { stringResource(it.displayNameRes) },
            selectedIndex = LocationMode.entries.indexOf(locationMode),
            onOptionSelected = { index ->
                val selected = LocationMode.entries[index]
                onLocationModeSelected(selected)
                showLocationModeDialog = false
                if (selected == LocationMode.MANUAL) showManualLocationDialog = true
            },
            onDismiss = { showLocationModeDialog = false },
        )
    }

    if (showManualLocationDialog) {
        ManualLocationDialog(
            latitude = manualLatitude?.toString().orEmpty(),
            longitude = manualLongitude?.toString().orEmpty(),
            onSave = { latitude, longitude ->
                onManualLocationSelected(latitude, longitude)
                showManualLocationDialog = false
            },
            onDismiss = { showManualLocationDialog = false },
        )
    }

    if (showAboutDialog) {
        AboutDialog(version = versionName, onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ManualLocationDialog(
    latitude: String,
    longitude: String,
    onSave: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var latitudeText by remember(latitude) { mutableStateOf(latitude) }
    var longitudeText by remember(longitude) { mutableStateOf(longitude) }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitudeText.replace(',', '.').toDoubleOrNull()
                    val lng = longitudeText.replace(',', '.').toDoubleOrNull()
                    if (lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
                        lat in -90.0..90.0 && lng in -180.0..180.0
                    ) {
                        onSave(lat, lng)
                    } else {
                        invalid = true
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = { Text(stringResource(R.string.pref_prayer_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NoorDimens.Sm)) {
                Text(
                    text = stringResource(R.string.manual_location_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it; invalid = false },
                    label = { Text(stringResource(R.string.latitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it; invalid = false },
                    label = { Text(stringResource(R.string.longitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                if (invalid) {
                    Text(
                        text = stringResource(R.string.manual_location_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
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
                    val selected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .clickable { onOptionSelected(index) }
                            .padding(horizontal = NoorDimens.Sm, vertical = NoorDimens.Sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
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
private fun AboutDialog(version: String?, onDismiss: () -> Unit) {
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
                    text = stringResource(R.string.quran_source_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.quran_audio_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (version != null) {
                    Text(
                        text = stringResource(R.string.settings_version_title) + " " + version,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = NoorDimens.Sm),
                    )
                }
            }
        },
    )
}