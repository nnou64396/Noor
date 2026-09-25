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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.navigation.NoorDestinations
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.presentation.common.NoorFormatters
import com.noor.islamic.presentation.common.labelRes
import com.noor.islamic.presentation.prayer.NextPrayerUi
import com.noor.islamic.presentation.prayer.PrayerUiState
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.presentation.quran.labelRes
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
 * The Noor spiritual dashboard. The prayer section is real (Phase 2) and the Quran
 * section surfaces the local reading position and shared recitation (Phase 4).
 */
@Composable
fun HomeScreen(
    prayerViewModel: PrayerViewModel,
    onOpenDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
    quran: HomeQuranUiState? = null,
    onTogglePlayback: () -> Unit = {},
    practice: HomePracticeUiState? = null,
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
        if (quran != null) {
            HomeQuranSection(
                state = quran,
                onContinueReading = { target ->
                    onOpenDestination(
                        NoorDestinations.quranAyahRoute(
                            surahNumber = target.surahNumber,
                            ayahNumberInSurah = target.ayahNumberInSurah,
                        ),
                    )
                },
                onTogglePlayback = onTogglePlayback,
            )
        }
        if (practice != null) {
            HomePracticeSection(
                state = practice,
                onOpenAthkar = { onOpenDestination(NoorDestinations.ATHKAR) },
                onOpenTasbih = { onOpenDestination(NoorDestinations.TASBIH) },
            )
        }
        HomeQuickAccess(onOpenDestination = onOpenDestination)
        HomeDailySection()
    }
}

/**
 * One compact row summarising the two Phase 5 features. It is additive: the
 * existing prayer, Quran, quick-access and daily sections are untouched, and the
 * whole card disappears when there is nothing to report.
 */
@Composable
private fun HomePracticeSection(
    state: HomePracticeUiState,
    onOpenAthkar: () -> Unit,
    onOpenTasbih: () -> Unit,
) {
    if (!state.hasContent && state.tasbih.totalCount == 0) return

    NoorSectionTitle(
        title = stringResource(R.string.home_practice_title),
        icon = Icons.Rounded.BlurOn,
    )
    Spacer(Modifier.size(NoorDimens.Md))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NoorDimens.Md),
    ) {
        if (state.hasContent) {
            val label = stringResource(
                R.string.home_practice_athkar_fmt,
                state.completedDhikr,
                state.totalDhikr,
            )
            HomePracticeCard(
                title = stringResource(R.string.nav_athkar),
                body = label,
                onClick = onOpenAthkar,
                testTag = "home_practice_athkar",
                modifier = Modifier.weight(1f),
            )
        }
        if (state.tasbih.totalCount > 0) {
            val label = stringResource(
                R.string.home_practice_tasbih_fmt,
                state.tasbih.count,
                state.tasbih.target,
            )
            HomePracticeCard(
                title = stringResource(R.string.tasbih_title),
                body = label,
                onClick = onOpenTasbih,
                testTag = "home_practice_tasbih",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomePracticeCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    NoorGlassCard(onClick = onClick, modifier = modifier.testTag(testTag)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = body,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HomeQuranSection(
    state: HomeQuranUiState,
    onContinueReading: (ContinueReadingTarget) -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val target = state.continueReading
    val nowPlaying = state.nowPlaying
    if (target == null && nowPlaying == null) return

    NoorSectionTitle(
        title = stringResource(R.string.home_continue_reading),
        icon = Icons.AutoMirrored.Rounded.MenuBook,
    )
    Spacer(Modifier.size(NoorDimens.Md))

    if (target != null) {
        val surahLabel = target.surah?.nameTransliteration ?: target.surahNumber.toString()
        NoorGlassCard(
            onClick = { onContinueReading(target) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_continue_reading"),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_continue_reading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.size(NoorDimens.Xs))
                Text(
                    text = target.surah?.nameArabic ?: surahLabel,
                    style = MaterialTheme.typography.noorTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.home_continue_reading_fmt,
                        surahLabel,
                        target.ayahNumberInSurah,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(NoorDimens.Sm))
    }

    if (nowPlaying != null) {
        val reciterName = stringResource(nowPlaying.reciterNameKey.labelRes)
        val playLabel = stringResource(
            if (nowPlaying.isPlaying) R.string.quran_audio_pause else R.string.quran_audio_play,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_now_playing"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(NoorDimens.Icon),
            )
            Spacer(Modifier.size(NoorDimens.Sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.home_now_playing_fmt,
                        reciterName,
                        nowPlaying.ayahNumberInSurah,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.quran_audio_streaming_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onTogglePlayback,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_now_playing_toggle")
                    .semantics { contentDescription = playLabel },
            ) {
                Icon(
                    imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
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