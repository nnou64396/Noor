package com.noor.islamic.presentation.qibla

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noor.islamic.R
import com.noor.islamic.data.local.location.AndroidLocationPermissionChecker
import com.noor.islamic.data.local.sensor.HeadingSensor
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.services.ServiceLocator
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorLocationPermissionCard
import com.noor.islamic.ui.components.NoorPrimaryButton
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorLargeTitle

/**
 * Qibla destination.
 *
 * The ViewModel is scoped to this nav entry, and the bottom bar saves nav
 * state when switching tabs, so the ViewModel — and its sensor — outlive the
 * moment the user leaves this screen. The rotation-vector sensor is therefore
 * driven by the *visible* lifecycle rather than by the ViewModel's existence:
 * it runs only between ON_START and ON_STOP, which covers both tapping another
 * tab and the Activity going to the background.
 */
@Composable
fun QiblaScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel = viewModel {
        QiblaViewModel(
            permissionChecker = AndroidLocationPermissionChecker(context),
            locationRepository = ServiceLocator.locationRepository,
            headingSensor = ServiceLocator.headingSensor(context),
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    val openAppSettings: () -> Unit = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        runCatching { context.startActivity(intent) }
    }

    // The compass only runs while this screen is actually on display.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onScreenActive() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onScreenInactive() }

    // Re-syncs permission and location on every resume, which is what makes
    // returning from the system Settings screen (after granting location
    // there) pick the new grant up. A composition-scoped effect would not
    // fire, because composition survives the Activity being backgrounded.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenEntered() }

    QiblaScreenContent(
        state = state,
        onGrant = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        },
        onRetry = viewModel::retry,
        onOpenSettings = openAppSettings,
        modifier = modifier,
    )
}

@Composable
private fun QiblaScreenContent(
    state: QiblaUiState,
    onGrant: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoorScreenContainer(modifier = modifier.testTag("qibla_screen")) {
        Text(
            text = stringResource(R.string.qibla_title),
            style = MaterialTheme.typography.noorLargeTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(NoorDimens.Sm))
        Text(
            text = stringResource(R.string.qibla_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            !state.sensorAvailable -> {
                NoorEmptyState(
                    icon = Icons.Rounded.Explore,
                    title = stringResource(R.string.qibla_sensor_unavailable),
                    description = stringResource(R.string.qibla_sensor_unavailable_body),
                )
            }

            state.permission != LocationPermissionUiState.GRANTED -> {
                NoorLocationPermissionCard(
                    permission = state.permission,
                    body = stringResource(R.string.location_permission_body_qibla),
                    onGrant = onGrant,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            !state.locationReady -> {
                if (state.isLocating) {
                    QiblaLoadingCard()
                } else {
                    NoorGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.location_unavailable_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.size(NoorDimens.Md))
                        Text(
                            text = stringResource(R.string.location_unavailable_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.size(NoorDimens.Lg))
                        NoorPrimaryButton(
                            text = stringResource(R.string.action_retry),
                            onClick = onRetry,
                        )
                    }
                }
            }

            else -> {
                NoorGlassCard(modifier = Modifier.fillMaxWidth().testTag("qibla_compass")) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        NoorQiblaCompass(
                            headingDegrees = state.headingDegrees,
                            bearingToKaaba = state.bearingToKaaba ?: 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(NoorDimens.QiblaDial),
                        )

                        Spacer(Modifier.size(NoorDimens.Md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.qibla_heading_fmt,
                                    state.headingDegrees.toInt(),
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(
                                    R.string.qibla_kaaba_bearing_fmt,
                                    state.bearingToKaaba?.toInt() ?: 0,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        if (state.accuracyLow) {
                            Spacer(Modifier.size(NoorDimens.Md))
                            Text(
                                text = stringResource(R.string.qibla_calibrate_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaLoadingCard() {
    NoorGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(NoorDimens.Xl),
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(NoorDimens.Md))
            Text(
                text = stringResource(R.string.location_detecting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}