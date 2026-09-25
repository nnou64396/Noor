package com.noor.islamic.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noor.islamic.R
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorTitle

/**
 * Polished, non-alarming location permission card shared by Prayer and
 * Qibla. Explains why location is needed, offers a grant action and —
 * after a permanent denial — a route to app settings. Never loops dialogs.
 */
@Composable
fun NoorLocationPermissionCard(
    permission: LocationPermissionUiState,
    body: String,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "location_permission_card",
) {
    NoorGlassCard(modifier = modifier.testTag(testTag)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(NoorDimens.IconCircle),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(NoorDimens.Sm + 8.dp),
                    )
                }
            }

            Spacer(Modifier.size(NoorDimens.Lg))

            Text(
                text = stringResource(R.string.location_permission_title),
                style = MaterialTheme.typography.noorTitle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(NoorDimens.Sm))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (permission == LocationPermissionUiState.DENIED_PERMANENT) {
                Spacer(Modifier.size(NoorDimens.Md))
                Text(
                    text = stringResource(R.string.location_denied_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.size(NoorDimens.Lg))

            when (permission) {
                LocationPermissionUiState.DENIED_PERMANENT ->
                    NoorPrimaryButton(
                        text = stringResource(R.string.action_open_settings),
                        onClick = onOpenSettings,
                        testTag = "location_open_settings",
                    )

                else ->
                    NoorPrimaryButton(
                        text = stringResource(R.string.action_grant_location),
                        onClick = onGrant,
                        testTag = "location_grant",
                    )
            }
        }
    }
}