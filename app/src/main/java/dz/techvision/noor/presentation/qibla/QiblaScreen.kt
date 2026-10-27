package dz.techvision.noor.presentation.qibla

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.techvision.noor.R
import dz.techvision.noor.data.local.location.AndroidLocationPermissionChecker
import dz.techvision.noor.data.local.sensor.HeadingSensor
import dz.techvision.noor.presentation.common.LocationPermissionUiState
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.ui.components.NoorEmptyState
import dz.techvision.noor.ui.components.NoorGlassCard
import dz.techvision.noor.ui.components.NoorLocationPermissionCard
import dz.techvision.noor.ui.components.NoorPrimaryButton
import dz.techvision.noor.ui.components.NoorScreenContainer
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.noorCaption
import dz.techvision.noor.ui.theme.noorLargeTitle
import dz.techvision.noor.ui.theme.noorSecondary
import dz.techvision.noor.ui.theme.noorTitle

/**
 * Qibla destination.
 *
 * The ViewModel is scoped to this nav entry, and the bottom bar saves nav
 * state when switching tabs, so the ViewModel — and its sensor — outlive the
 * moment the user leaves this screen. The rotation-vector sensor is therefore
 * driven by the *visible* lifecycle rather than by the ViewModel's existence:
 * it runs only between ON_START and ON_STOP, which covers both tapping another
 * tab and the Activity going to the background.
 *
 * None of that lifecycle wiring is touched by the redesign; the dial simply
 * gets a quieter surface and the two readings become legible stat chips.
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
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = stringResource(R.string.qibla_subtitle),
            style = MaterialTheme.typography.noorSecondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(NoorDimens.Md))

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
                            style = MaterialTheme.typography.noorTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.size(NoorDimens.Sm))
                        Text(
                            text = stringResource(R.string.location_unavailable_body),
                            style = MaterialTheme.typography.noorSecondary,
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
                NoorGlassCard(
                    onClick = null,
                    elevation = NoorDimens.ElNone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_compass"),
                ) {
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
                            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
                        ) {
                            QiblaStat(
                                text = stringResource(
                                    R.string.qibla_heading_fmt,
                                    state.headingDegrees.toInt(),
                                ),
                                emphasised = false,
                                modifier = Modifier.weight(1f),
                            )
                            QiblaStat(
                                text = stringResource(
                                    R.string.qibla_kaaba_bearing_fmt,
                                    state.bearingToKaaba?.toInt() ?: 0,
                                ),
                                emphasised = true,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (state.accuracyLow) {
                            Spacer(Modifier.size(NoorDimens.Md))
                            Text(
                                text = stringResource(R.string.qibla_calibrate_hint),
                                style = MaterialTheme.typography.noorCaption,
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

/**
 * One reading. The Kaaba bearing is the number the user is aiming at, so it
 * gets the emphasised (blue) treatment; the heading is supporting context.
 */
@Composable
private fun QiblaStat(
    text: String,
    emphasised: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (emphasised) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (emphasised) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.noorCaption,
            color = if (emphasised) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = NoorDimens.Sm, vertical = 10.dp),
        )
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
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.size(NoorDimens.Md))
            Text(
                text = stringResource(R.string.location_detecting),
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

