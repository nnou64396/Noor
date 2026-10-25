package com.noor.islamic.ui.components

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
import com.noor.islamic.R
import com.noor.islamic.domain.model.AppUpdateInfo
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Restrained, glassmorphic update banner card supporting download, verification, and installation flow.
 */
@Composable
fun UpdateBannerCard(
    updateStatus: UpdateStatus,
    onDismiss: (Long) -> Unit,
    onDownloadAndVerify: (AppUpdateInfo) -> Unit = {},
    onInstall: (Context) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val info: AppUpdateInfo? = when (updateStatus) {
        is UpdateStatus.UpdateAvailable -> updateStatus.info
        is UpdateStatus.UpdateRequired -> updateStatus.info
        is UpdateStatus.Downloading -> null
        is UpdateStatus.VerificationFailed -> null
        is UpdateStatus.ReadyToInstall -> updateStatus.info
        else -> return
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
                        text = stringResource(R.string.update_verification_failed_title),
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
                    is UpdateStatus.ReadyToInstall -> {
                        Button(
                            onClick = { onInstall(context) },
                            modifier = Modifier.testTag("update_install_button"),
                        ) {
                            Text(text = stringResource(R.string.update_action_install))
                        }
                    }
                    is UpdateStatus.Downloading -> {
                        // In-progress, action disabled
                    }
                    is UpdateStatus.VerificationFailed -> {
                        Button(
                            onClick = {
                                // Retry check or download
                            },
                            modifier = Modifier.testTag("update_retry_button"),
                        ) {
                            Text(text = stringResource(R.string.action_retry))
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
