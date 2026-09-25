package com.noor.islamic.presentation.prayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.presentation.common.NoorFormatters
import com.noor.islamic.presentation.common.labelRes
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorIslamicMotif
import com.noor.islamic.ui.components.NoorLocationPermissionCard
import com.noor.islamic.ui.components.NoorPrimaryButton
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorDisplay
import com.noor.islamic.ui.theme.noorLargeTitle
import java.time.LocalDate

/**
 * Prayer Times destination. [PrayerScreen] wires the shared activity-scoped
 * ViewModel + Android permission machinery; [PrayerScreenContent] is the
 * stateless, testable body.
 */
@Composable
fun PrayerScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val next by viewModel.nextPrayer.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    LaunchedEffect(Unit) { viewModel.onScreenEntered() }

    PrayerScreenContent(
        state = state,
        next = next,
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
private fun PrayerScreenContent(
    state: PrayerUiState,
    next: NextPrayerUi,
    onGrant: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoorScreenContainer(modifier = modifier.testTag("prayer_screen")) {
        when (state.permission) {
            LocationPermissionUiState.GRANTED -> PrayerContent(state, next, onRetry)
            else -> {
                PrayerHeader(state)
                NoorLocationPermissionCard(
                    permission = state.permission,
                    body = stringResource(R.string.location_permission_body_prayer),
                    onGrant = onGrant,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PrayerContent(state: PrayerUiState, next: NextPrayerUi, onRetry: () -> Unit) {
    PrayerHeader(state)

    PrayerLocationRow(state)

    when (val schedule = state.schedule) {
        PrayerScheduleUi.Idle, PrayerScheduleUi.Failed -> {
            if (state.schedule is PrayerScheduleUi.Failed) {
                NoorEmptyState(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.prayer_error_title),
                    description = stringResource(R.string.prayer_error_body),
                )
                NoorPrimaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry,
                )
            }
        }

        is PrayerScheduleUi.Loading -> {
            if (schedule.cached == null) {
                PrayerLoadingCard()
            } else {
                PrayerScheduleBlock(schedule.cached, null, next, fromCache = true)
            }
        }

        is PrayerScheduleUi.Ready ->
            PrayerScheduleBlock(schedule.today, schedule.tomorrow, next, fromCache = schedule.fromCache)
    }
}

@Composable
private fun PrayerHeader(state: PrayerUiState) {
    val schedule = when (val s = state.schedule) {
        is PrayerScheduleUi.Ready -> s.today
        is PrayerScheduleUi.Loading -> s.cached
        else -> null
    }
    val hijri = schedule?.hijriReadable
    val displayDate = schedule?.date ?: LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.prayer_title),
            style = MaterialTheme.typography.noorLargeTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(NoorDimens.Sm))
        Text(
            text = NoorFormatters.date(displayDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hijri != null) {
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = hijri,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun PrayerLocationRow(state: PrayerUiState) {
    val label = when {
        state.isLocating -> stringResource(R.string.prayer_detecting_location)
        !state.locationLabel.isNullOrBlank() -> state.locationLabel!!
        else -> stringResource(R.string.location_city_unknown)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(NoorDimens.Md),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrayerScheduleBlock(
    today: DailyPrayerSchedule,
    tomorrow: DailyPrayerSchedule?,
    next: NextPrayerUi,
    fromCache: Boolean,
) {
    val nextReady = next as? NextPrayerUi.Ready

    if (nextReady != null) {
        NextPrayerHero(next = nextReady)
    }

    Spacer(Modifier.size(NoorDimens.Sm))

    if (fromCache) {
        Text(
            text = stringResource(R.string.prayer_offline_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("prayer_offline_badge"),
        )
        Spacer(Modifier.size(NoorDimens.Sm))
    }

    PrayerListCard(schedule = today, next = nextReady)

    Spacer(Modifier.size(NoorDimens.Sm))
    Text(
        text = stringResource(R.string.prayer_source),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun PrayerLoadingCard() {
    NoorGlassCard(modifier = Modifier.fillMaxWidth().testTag("prayer_loading")) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().testTag("prayer_loading"),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(NoorDimens.Xl),
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(NoorDimens.Md))
            Text(
                text = stringResource(R.string.prayer_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextPrayerHero(next: NextPrayerUi.Ready) {
    NoorGlassCard(
        onClick = null,
        modifier = Modifier.fillMaxWidth().testTag("prayer_next_card"),
    ) {
        Box {
            NoorIslamicMotif(
                modifier = Modifier.matchParentSize(),
                color = MaterialTheme.colorScheme.secondary,
                alpha = 0.08f,
            )
            Column {
                Text(
                    text = stringResource(R.string.prayer_next_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.size(NoorDimens.Xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(next.prayer.labelRes),
                        style = MaterialTheme.typography.noorDisplay,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = NoorFormatters.prayerTime(next.time),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.size(NoorDimens.Sm))
                Text(
                    text = stringResource(
                        R.string.prayer_remaining_fmt,
                        NoorFormatters.countdown(next.remaining),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("prayer_countdown"),
                )
            }
        }
    }
}

@Composable
private fun PrayerListCard(schedule: DailyPrayerSchedule, next: NextPrayerUi.Ready?) {
    Column {
        NoorSectionTitle(
            title = stringResource(R.string.prayer_list_title),
            icon = Icons.Rounded.Schedule,
        )
        Spacer(Modifier.size(NoorDimens.Sm))
        NoorGlassCard(
            onClick = null,
            modifier = Modifier.fillMaxWidth().testTag("prayer_list"),
        ) {
            Column {
                PrayerName.entries.forEachIndexed { index, name ->
                    val time = schedule.prayers.firstOrNull { it.prayer == name }?.time
                    val isNext = next != null && next.prayer == name && next.date == schedule.date
                    PrayerRow(
                        name = name,
                        time = time?.let { NoorFormatters.prayerTime(it) } ?: "--:--",
                        isNext = isNext,
                    )
                    if (index < PrayerName.entries.lastIndex) {
                        Spacer(Modifier.size(NoorDimens.Md))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerRow(name: PrayerName, time: String, isNext: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isNext) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                },
                shape = RoundedCornerShape(NoorDimens.RadiusSm),
            )
            .testTag("prayer_row_${name.apiKey}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
    ) {
        Icon(
            imageVector = Icons.Rounded.Diamond,
            contentDescription = null,
            tint = if (isNext) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(NoorDimens.Sm),
        )
        Text(
            text = stringResource(name.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = if (isNext) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isNext) {
            Text(
                text = stringResource(R.string.prayer_next_pill),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(NoorDimens.Xs))
        }
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            color = if (isNext) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}