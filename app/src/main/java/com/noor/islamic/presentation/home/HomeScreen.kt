package com.noor.islamic.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.navigation.NoorDestinations
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.presentation.common.NoorFormatters
import com.noor.islamic.presentation.common.labelRes
import com.noor.islamic.presentation.prayer.NextPrayerUi
import com.noor.islamic.presentation.prayer.PrayerUiState
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorIslamicMotif
import com.noor.islamic.ui.components.NoorLogo
import com.noor.islamic.ui.components.NoorOrnamentalDivider
import com.noor.islamic.ui.components.NoorQuickCard
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorDisplay
import com.noor.islamic.ui.theme.noorTitle

/**
 * The Noor spiritual dashboard. The prayer section is real (Phase 2); the
 * remaining sections are neutral placeholders for later phases.
 */
@Composable
fun HomeScreen(
    prayerViewModel: PrayerViewModel,
    onOpenDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prayerState by prayerViewModel.uiState.collectAsStateWithLifecycle()
    val next by prayerViewModel.nextPrayer.collectAsStateWithLifecycle()

    NoorScreenContainer(modifier = modifier) {
        HomeHeader()
        HomeHeroCard()
        HomePrayerSection(
            state = prayerState,
            next = next,
            onOpen = { onOpenDestination(NoorDestinations.PRAYER) },
        )
        HomeQuickAccess(onOpenDestination = onOpenDestination)
        HomeDailySection()
    }
}

@Composable
private fun HomeHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = NoorDimens.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoorLogo(
                modifier = Modifier.size(NoorDimens.Lg),
                cutoutColor = MaterialTheme.colorScheme.background,
            )
            Spacer(Modifier.size(NoorDimens.Sm))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = stringResource(R.string.home_header_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(NoorDimens.Md))
        NoorOrnamentalDivider()
    }
}

@Composable
private fun HomeHeroCard() {
    NoorGlassCard(
        onClick = null,
    ) {
        Box {
            NoorIslamicMotif(
                modifier = Modifier.matchParentSize(),
                color = MaterialTheme.colorScheme.secondary,
                alpha = 0.07f,
            )
            Column {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.noorDisplay,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("home_greeting"),
                )
                Spacer(Modifier.size(NoorDimens.Sm))
                Text(
                    text = stringResource(R.string.home_welcome),
                    style = MaterialTheme.typography.noorTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(NoorDimens.Sm))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomePrayerSection(
    state: PrayerUiState,
    next: NextPrayerUi,
    onOpen: () -> Unit,
) {
    NoorSectionTitle(
        title = stringResource(R.string.prayer_title),
        icon = Icons.Rounded.Schedule,
    )
    Spacer(Modifier.size(NoorDimens.Md))

    when {
        state.permission != LocationPermissionUiState.GRANTED ->
            HomePrayerEnableCard(onOpen = onOpen)

        next is NextPrayerUi.Ready ->
            HomePrayerNextCard(
                state = state,
                next = next,
                onOpen = onOpen,
            )

        else ->
            HomePrayerUnavailableCard(
                isDetecting = state.isLocating,
                onOpen = onOpen,
            )
    }
}

@Composable
private fun HomePrayerEnableCard(onOpen: () -> Unit) {
    NoorGlassCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("home_prayer_card"),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_prayer_enable),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = stringResource(R.string.home_prayer_enable_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomePrayerNextCard(
    state: PrayerUiState,
    next: NextPrayerUi.Ready,
    onOpen: () -> Unit,
) {
    NoorGlassCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("home_prayer_card"),
    ) {
        Box {
            NoorIslamicMotif(
                modifier = Modifier.matchParentSize(),
                color = MaterialTheme.colorScheme.secondary,
                alpha = 0.06f,
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.prayer_next_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    state.locationLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.size(NoorDimens.Sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
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
                    modifier = Modifier.testTag("home_prayer_countdown"),
                )
            }
        }
    }
}

@Composable
private fun HomePrayerUnavailableCard(isDetecting: Boolean, onOpen: () -> Unit) {
    NoorGlassCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("home_prayer_card"),
    ) {
        Text(
            text = stringResource(R.string.home_prayer_unavailable),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = stringResource(
                if (isDetecting) R.string.prayer_loading else R.string.home_prayer_fallback,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeQuickAccess(onOpenDestination: (String) -> Unit) {
    Column {
        NoorSectionTitle(
            title = stringResource(R.string.home_quick_access),
            icon = Icons.Rounded.WbSunny,
        )
        Spacer(Modifier.size(NoorDimens.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NoorDimens.Md),
        ) {
            NoorQuickCard(
                title = stringResource(R.string.nav_quran),
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                onClick = { onOpenDestination(NoorDestinations.QURAN) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_quran"),
            )
            NoorQuickCard(
                title = stringResource(R.string.nav_athkar),
                icon = Icons.Rounded.Star,
                onClick = { onOpenDestination(NoorDestinations.ATHKAR) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_athkar"),
            )
        }
        Spacer(Modifier.size(NoorDimens.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NoorDimens.Md),
        ) {
            NoorQuickCard(
                title = stringResource(R.string.nav_qibla),
                icon = Icons.Rounded.Explore,
                onClick = { onOpenDestination(NoorDestinations.QIBLA) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_qibla"),
            )
            NoorQuickCard(
                title = stringResource(R.string.tasbih_title),
                icon = Icons.Rounded.BlurOn,
                onClick = { onOpenDestination(NoorDestinations.TASBIH) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_tasbih"),
            )
        }
    }
}

@Composable
private fun HomeDailySection() {
    Column {
        NoorSectionTitle(
            title = stringResource(R.string.home_daily),
            icon = Icons.Rounded.Star,
        )
        Spacer(Modifier.size(NoorDimens.Md))

        NoorDailyPlaceholderCard(
            title = stringResource(R.string.daily_verse_title),
            body = stringResource(R.string.daily_verse_placeholder),
        )
        Spacer(Modifier.size(NoorDimens.Md))
        NoorDailyPlaceholderCard(
            title = stringResource(R.string.daily_dhikr_title),
            body = stringResource(R.string.daily_dhikr_placeholder),
        )
    }
}

@Composable
private fun NoorDailyPlaceholderCard(title: String, body: String) {
    NoorGlassCard(
        onClick = null,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}