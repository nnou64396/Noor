package com.noor.islamic.presentation.prayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.LocationMode
import com.noor.islamic.domain.model.PrayerErrorKind
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.presentation.common.NoorFormatters
import com.noor.islamic.presentation.common.labelRes
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorHeroCard
import com.noor.islamic.ui.components.NoorIslamicMotif
import com.noor.islamic.ui.components.NoorLocationPermissionCard
import com.noor.islamic.ui.components.NoorPrimaryButton
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.components.heroAccent
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorCaption
import com.noor.islamic.ui.theme.noorDisplay
import com.noor.islamic.ui.theme.noorLabel
import com.noor.islamic.ui.theme.noorLargeTitle
import com.noor.islamic.ui.theme.noorSecondary
import com.noor.islamic.ui.theme.noorTitle
import java.time.LocalDate

/**
 * Prayer Times destination. [PrayerScreen] wires the shared activity-scoped
 * ViewModel + Android permission machinery; [PrayerScreenContent] is the
 * stateless, testable body.
 *
 * Visually the screen is anchored on one premium blue hero — the next prayer
 * and its countdown — because that is the only time-sensitive number here.
 * Everything else (date, location, the full day list) is quiet by comparison.
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

    // Re-syncs permission and data on every resume, which is what makes
    // returning from the system Settings screen (after granting location
    // there) pick the new grant up. A composition-scoped effect would not
    // fire, because composition survives the Activity being backgrounded.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenEntered() }

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
        if (state.permission == LocationPermissionUiState.GRANTED ||
            state.locationMode == LocationMode.MANUAL
        ) {
            PrayerContent(state, next, onRetry)
        } else {
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

@Composable
private fun PrayerContent(state: PrayerUiState, next: NextPrayerUi, onRetry: () -> Unit) {
    PrayerHeader(state)

    PrayerLocationRow(state)

    when (val schedule = state.schedule) {
        PrayerScheduleUi.Idle -> Unit

        is PrayerScheduleUi.Failed -> {
            NoorEmptyState(
                icon = Icons.Rounded.Schedule,
                title = stringResource(R.string.prayer_error_title),
                description = stringResource(schedule.kind.descriptionRes()),
            )
            NoorPrimaryButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
            )
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

/**
 * Screen title, Gregorian date, and the Hijri date as a small gold-accented
 * line. The gold is decorative only — it is never the sole carrier of meaning.
 */
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
        Spacer(Modifier.size(NoorDimens.Xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = NoorFormatters.date(displayDate),
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hijri != null) {
                Spacer(Modifier.width(NoorDimens.Sm))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.noorSecondary,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(NoorDimens.Sm))
                Text(
                    text = hijri,
                    style = MaterialTheme.typography.noorCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The resolved location, presented as a quiet chip rather than a bare row. */
@Composable
private fun PrayerLocationRow(state: PrayerUiState) {
    val label = when {
        state.isLocating -> stringResource(R.string.prayer_detecting_location)
        !state.locationLabel.isNullOrBlank() -> state.locationLabel!!
        else -> stringResource(R.string.location_city_unknown)
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NoorDimens.Md, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(NoorDimens.Icon),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

    Spacer(Modifier.size(NoorDimens.Md))

    if (fromCache) {
        OfflineBadge()
        Spacer(Modifier.size(NoorDimens.Md))
    }

    PrayerListCard(schedule = today, next = nextReady)

    Spacer(Modifier.size(NoorDimens.Sm))
    Text(
        text = stringResource(R.string.prayer_source),
        style = MaterialTheme.typography.noorCaption,
        color = MaterialTheme.colorScheme.outline,
    )
}

/** Shown when the schedule is being served from the on-device cache. */
@Composable
private fun OfflineBadge() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("prayer_offline_badge"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NoorDimens.Md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(NoorDimens.Md),
            )
            Text(
                text = stringResource(R.string.prayer_offline_badge),
                style = MaterialTheme.typography.noorCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrayerLoadingCard() {
    NoorGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_loading"),
    ) {
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
                text = stringResource(R.string.prayer_loading),
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The premium centrepiece: a deep blue gradient carrying the next prayer, its
 * time, and the live countdown. White type on this gradient clears WCAG AA
 * comfortably; the countdown sits in a translucent pill so it reads as a
 * distinct, glanceable number.
 */
@Composable
private fun NextPrayerHero(next: NextPrayerUi.Ready) {
    NoorHeroCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_next_card"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(NoorDimens.Lg),
    ) {
        Box {
            NoorIslamicMotif(
                modifier = Modifier.matchParentSize(),
                color = Color.White,
                alpha = 0.07f,
            )
            Column {
                Text(
                    text = stringResource(R.string.prayer_next_title),
                    style = MaterialTheme.typography.noorLabel,
                    color = heroAccent(),
                )
                Spacer(Modifier.size(NoorDimens.Md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(next.prayer.labelRes),
                        style = MaterialTheme.typography.noorDisplay,
                        color = Color.White,
                    )
                    Text(
                        text = NoorFormatters.prayerTime(next.time),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.size(NoorDimens.Md))

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = stringResource(
                            R.string.prayer_remaining_fmt,
                            NoorFormatters.countdown(next.remaining),
                        ),
                        style = MaterialTheme.typography.noorLabel,
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = NoorDimens.Md, vertical = 7.dp)
                            .testTag("prayer_countdown"),
                    )
                }
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
        Spacer(Modifier.size(NoorDimens.Md))
        NoorGlassCard(
            onClick = null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prayer_list"),
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
                        Spacer(Modifier.size(NoorDimens.Sm))
                    }
                }
            }
        }
    }
}

/**
 * One prayer in the day list. The upcoming prayer is marked by a solid blue
 * dot, blue time and a small "next" pill rather than a heavy background fill,
 * which keeps the list calm.
 */
@Composable
private fun PrayerRow(name: PrayerName, time: String, isNext: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${name.apiKey}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm + 2.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Diamond,
            contentDescription = null,
            tint = if (isNext) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
            modifier = Modifier.size(NoorDimens.Sm),
        )
        Text(
            text = stringResource(name.labelRes),
            style = MaterialTheme.typography.noorTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isNext) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.prayer_next_pill),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = NoorDimens.Sm, vertical = 3.dp),
                )
            }
            Spacer(Modifier.size(NoorDimens.Xs))
        }
        Text(
            text = time,
            style = MaterialTheme.typography.noorTitle,
            color = if (isNext) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Copy for each failure kind: location problems have their own message. */
@StringRes
private fun PrayerErrorKind.descriptionRes(): Int = when (this) {
    PrayerErrorKind.LOCATION -> R.string.location_unavailable_body
    PrayerErrorKind.NETWORK, PrayerErrorKind.TIMEOUT -> R.string.prayer_error_network
    PrayerErrorKind.HTTP, PrayerErrorKind.MALFORMED, PrayerErrorKind.UNKNOWN -> R.string.prayer_error_body
}
