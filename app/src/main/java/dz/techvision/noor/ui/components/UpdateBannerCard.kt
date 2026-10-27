package dz.techvision.noor.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.domain.model.isActionableUpdate
import dz.techvision.noor.ui.theme.NoorDimens

/**
 * Restrained, glassmorphic update banner card supporting download, verification, and installation flow.
 */
@Composable
fun UpdateBannerCard(
    updateStatus: UpdateStatus,
    onDismiss: (Long) -> Unit,
    onDownloadAndVerify: (AppUpdateInfo) -> Unit,
    onInstall: (Context) -> Unit,
    onRetry: (AppUpdateInfo, Context) -> Unit,
    onOpenInstallSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!updateStatus.isActionableUpdate) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val info: AppUpdateInfo? = when (updateStatus) {
        is UpdateStatus.UpdateAvailable -> updateStatus.info
        is UpdateStatus.UpdateRequired -> updateStatus.info
        is UpdateStatus.VerificationFailed -> updateStatus.info
        is UpdateStatus.ReadyToInstall -> updateStatus.info
        is UpdateStatus.InstallPermissionRequired -> updateStatus.info
        is UpdateStatus.InstallFailed -> updateStatus.info
        is UpdateStatus.Downloading -> null
        else -> null
    }

    val isRequired = updateStatus is UpdateStatus.UpdateRequired
    val currentLang = configuration.locales[0].language
    val releaseNote = info?.getReleaseNoteForLanguage(currentLang)

    NoorGlassCard(
        onClick = null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("update_banner_card"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
            modifier = Modifier.padding(NoorDimens.Xs),
        ) {
            when (updateStatus) {
                is UpdateStatus.Downloading -> {
                    val percent = (updateStatus.progress * 100).toInt()
                    Text(
                        text = stringResource(R.string.update_downloading_fmt, percent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { updateStatus.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is UpdateStatus.VerificationFailed -> {
                    Text(
                        text = stringResource(R.string.update_failed_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = updateStatus.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is UpdateStatus.InstallFailed -> {
                    Text(
                        text = stringResource(R.string.update_install_failed_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = updateStatus.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is UpdateStatus.InstallPermissionRequired -> {
                    Text(
                        text = stringResource(R.string.update_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.update_permission_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is UpdateStatus.ReadyToInstall -> {
                    Text(
                        text = stringResource(R.string.update_ready_title, updateStatus.info.latestVersion),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.update_ready_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    if (info != null) {
                        Text(
                            text = if (isRequired) {
                                stringResource(R.string.update_required_title, info.latestVersion)
                            } else {
                                stringResource(R.string.update_available_title, info.latestVersion)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = releaseNote ?: if (isRequired) {
                                stringResource(R.string.update_required_body, info.latestVersion)
                            } else {
                                stringResource(R.string.update_available_body)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (updateStatus) {
                    is UpdateStatus.InstallPermissionRequired -> {
                        Button(
                            onClick = onOpenInstallSettings,
                            modifier = Modifier.testTag("update_open_settings_button"),
                        ) {
                            Text(text = stringResource(R.string.update_action_open_settings))
                        }
                    }
                    is UpdateStatus.ReadyToInstall -> {
                        Button(
                            onClick = { onInstall(context) },
                            modifier = Modifier.testTag("update_install_button"),
                        ) {
                            Text(text = stringResource(R.string.update_action_install))
                        }
                    }
                    is UpdateStatus.Downloading -> {
                        // In progress: no action that could start a second download.
                    }
                    is UpdateStatus.VerificationFailed -> {
                        if (info != null) {
                            Button(
                                onClick = { onRetry(info, context) },
                                modifier = Modifier.testTag("update_retry_button"),
                            ) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }
                    is UpdateStatus.InstallFailed -> {
                        if (info != null) {
                            Button(
                                onClick = { onRetry(info, context) },
                                modifier = Modifier.testTag("update_retry_button"),
                            ) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }
                    else -> {
                        if (info != null) {
                            if (!isRequired) {
                                OutlinedButton(
                                    onClick = { onDismiss(info.latestVersionCode) },
                                    modifier = Modifier
                                        .padding(end = NoorDimens.Sm)
                                        .testTag("update_dismiss_button"),
                                ) {
                                    Text(text = stringResource(R.string.update_action_dismiss))
                                }
                            }

                            Button(
                                onClick = { onDownloadAndVerify(info) },
                                modifier = Modifier.testTag("update_get_button"),
                            ) {
                                Text(text = stringResource(R.string.update_action_get))
                            }
                        }
                    }
                }
            }
        }
    }
}

