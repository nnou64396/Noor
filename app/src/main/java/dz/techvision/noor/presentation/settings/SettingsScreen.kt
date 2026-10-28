package dz.techvision.noor.presentation.settings

import android.content.Context
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.ui.text.style.TextAlign
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.CalculationMethodConfig
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.domain.model.isActionableUpdate
import dz.techvision.noor.ui.components.NoorScreenContainer
import dz.techvision.noor.ui.components.NoorSectionTitle
import dz.techvision.noor.ui.components.NoorSettingRow
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.noorLargeTitle

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
    updateStatus: UpdateStatus,
    onCheckForUpdates: () -> Unit,
    onDismissUpdate: (Long) -> Unit,
    onDownloadAndVerifyUpdate: (AppUpdateInfo) -> Unit,
    onInstallUpdate: (Context) -> Unit,
    onRetryUpdate: (AppUpdateInfo, Context) -> Unit,
    onOpenInstallSettings: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCalculationMethodDialog by remember { mutableStateOf(false) }
    var showAsrSchoolDialog by remember { mutableStateOf(false) }
    var showLocationModeDialog by remember { mutableStateOf(false) }
    var showManualLocationDialog by remember { mutableStateOf(false) }

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

            // Privacy, Permissions, Notifications sections
            NoorSectionTitle(
                title = stringResource(R.string.settings_privacy_menu_title),
                icon = Icons.Rounded.Lock,
            )
            NoorSettingRow(
                title = stringResource(R.string.settings_privacy_menu_title),
                subtitle = stringResource(R.string.privacy_policy_header),
                icon = Icons.Rounded.Lock,
                onClick = onOpenPrivacy,
            )
            NoorSettingRow(
                title = stringResource(R.string.settings_permissions_menu_title),
                subtitle = stringResource(R.string.permissions_screen_title),
                icon = Icons.Rounded.Security,
                onClick = onOpenPermissions,
            )
            NoorSettingRow(
                title = stringResource(R.string.settings_notifications_menu_title),
                subtitle = stringResource(R.string.notification_master_switch),
                icon = Icons.Rounded.Notifications,
                onClick = onOpenNotifications,
            )

            NoorSectionTitle(
                title = stringResource(R.string.update_section_title),
                icon = Icons.Rounded.Info,
            )
            if (updateStatus.isActionableUpdate) {
                dz.techvision.noor.ui.components.UpdateBannerCard(
                    updateStatus = updateStatus,
                    onDismiss = onDismissUpdate,
                    onDownloadAndVerify = onDownloadAndVerifyUpdate,
                    onInstall = onInstallUpdate,
                    onRetry = onRetryUpdate,
                    onOpenInstallSettings = onOpenInstallSettings,
                )
            }
            NoorSettingRow(
                title = stringResource(R.string.update_check_action),
                subtitle = when (updateStatus) {
                    is UpdateStatus.UpToDate -> stringResource(R.string.update_status_up_to_date)
                    is UpdateStatus.Disabled -> stringResource(R.string.update_status_disabled)
                    is UpdateStatus.CheckUnavailable -> stringResource(R.string.update_status_unavailable)
                    is UpdateStatus.UpdateAvailable -> stringResource(R.string.update_available_title, updateStatus.info.latestVersion)
                    is UpdateStatus.UpdateRequired -> stringResource(R.string.update_required_title, updateStatus.info.latestVersion)
                    is UpdateStatus.Downloading -> stringResource(R.string.update_downloading_fmt, (updateStatus.progress * 100).toInt())
                    is UpdateStatus.VerificationFailed -> stringResource(R.string.update_failed_title)
                    is UpdateStatus.ReadyToInstall -> stringResource(R.string.update_ready_title, updateStatus.info.latestVersion)
                    is UpdateStatus.InstallPermissionRequired -> stringResource(R.string.update_permission_title)
                    is UpdateStatus.InstallFailed -> stringResource(R.string.update_install_failed_title)
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
                subtitle = stringResource(R.string.settings_about_body),
                icon = Icons.Rounded.Info,
                onClick = onOpenAbout,
            )
            NoorSettingRow(
                title = stringResource(R.string.settings_version_title),
                subtitle = versionName,
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
            )

            Spacer(Modifier.height(NoorDimens.Md))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NoorDimens.Md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NoorDimens.Xs),
            ) {
                Text(
                    text = stringResource(R.string.developer_credit),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${stringResource(R.string.app_name)} • v${versionName ?: "1.1.5"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
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
