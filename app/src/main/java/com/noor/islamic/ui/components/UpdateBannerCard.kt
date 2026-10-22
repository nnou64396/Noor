package com.noor.islamic.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Restrained, glassmorphic update banner card matching the premium-blue redesign.
 */
@Composable
fun UpdateBannerCard(
    updateStatus: UpdateStatus,
    onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val (info, isRequired) = when (updateStatus) {
        is UpdateStatus.UpdateAvailable -> updateStatus.info to false
        is UpdateStatus.UpdateRequired -> updateStatus.info to true
        else -> return
    }

    val currentLang = configuration.locales[0].language
    val releaseNote = info.getReleaseNoteForLanguage(currentLang)

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    onClick = {
                        val url = info.downloadUrl
                        if (!url.isNullOrBlank()) {
                            runCatching {
                                val parsed = Uri.parse(url)
                                val scheme = parsed.scheme?.lowercase()
                                if (scheme == "https") {
                                    val intent = Intent(Intent.ACTION_VIEW, parsed).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("update_get_button"),
                ) {
                    Text(text = stringResource(R.string.update_action_get))
                }
            }
        }
    }
}
