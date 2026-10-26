package com.noor.islamic.presentation.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.domain.model.AppUpdateInfo
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.domain.model.isActionableUpdate
import com.noor.islamic.navigation.NoorDestinations
import com.noor.islamic.presentation.common.LocationPermissionUiState
import com.noor.islamic.presentation.common.NoorFormatters
import com.noor.islamic.presentation.common.labelRes
import com.noor.islamic.presentation.prayer.NextPrayerUi
import com.noor.islamic.presentation.prayer.PrayerUiState
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.presentation.quran.labelRes
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorHeroCard
import com.noor.islamic.ui.components.NoorLockup
import com.noor.islamic.ui.components.NoorQuickCard
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.components.heroAccent
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorCaption
import com.noor.islamic.ui.theme.noorDisplay
import com.noor.islamic.ui.theme.noorLabel
import com.noor.islamic.ui.theme.noorSecondary
import com.noor.islamic.ui.theme.noorTitle

/**
 * The Noor dashboard and the app's visual centrepiece.
 *
 * The hierarchy is deliberate and short: brand header, then the single premium
 * blue hero carrying the one thing that matters *now* (the next prayer and its
 * countdown), then quick access, then the optional sections. Nothing below the
 * hero competes with it.
 *
 * Prayer doubles as the quick-access entry point to the full Prayer screen, so
 * the grid below stays a comfortable two-by-two rather than a cramped row of
 * five.
 */
@Composable
fun HomeScreen(
    prayerViewModel: PrayerViewModel,
    onOpenDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
    quran: HomeQuranUiState? = null,
    onTogglePlayback: () -> Unit = {},
    practice: HomePracticeUiState? = null,
    updateStatus: UpdateStatus,
    onDismissUpdate: (Long) -> Unit,
    onDownloadAndVerifyUpdate: (AppUpdateInfo) -> Unit,
    onInstallUpdate: (Context) -> Unit,
    onRetryUpdate: (AppUpdateInfo, Context) -> Unit,
    onOpenInstallSettings: () -> Unit,
) {
    val prayerState by prayerViewModel.uiState.collectAsStateWithLifecycle()
    val next by prayerViewModel.nextPrayer.collectAsStateWithLifecycle()

    NoorScreenContainer(modifier = modifier) {
        HomeHeader()

        if (updateStatus.isActionableUpdate) {
            Spacer(Modifier.size(NoorDimens.Sm))
            com.noor.islamic.ui.components.UpdateBannerCard(
                updateStatus = updateStatus,
                onDismiss = onDismissUpdate,
                onDownloadAndVerify = onDownloadAndVerifyUpdate,
                onInstall = onInstallUpdate,
                onRetry = onRetryUpdate,
                onOpenInstallSettings = onOpenInstallSettings,
            )
        }

        HomeHeroSection(
            state = prayerState,
            next = next,
            onOpen = { onOpenDestination(NoorDestinations.PRAYER) },
        )

        HomeQuickAccess(onOpenDestination = onOpenDestination)

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

        HomeDailySection()
    }
}

/**
 * Compact brand header: the Noor lockup with its tagline. The greeting itself
 * lives in the hero, where it introduces the prayer information rather than
 * floating on its own.
 */
@Composable
private fun HomeHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        NoorLockup()
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = stringResource(R.string.home_header_tagline),
            style = MaterialTheme.typography.noorCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The hero: one premium blue card that carries the greeting, the next prayer,
 * its time, the countdown and the resolved location. Tapping it opens Prayer.
 */
@Composable
private fun HomeHeroSection(
    state: PrayerUiState,
    next: NextPrayerUi,
    onOpen: () -> Unit,
) {
    when {
        state.permission != LocationPermissionUiState.GRANTED ->
            HomePrayerEnableCard(onOpen = onOpen)

        next is NextPrayerUi.Ready ->
            NoorHeroCard(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_prayer_card"),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_greeting),
                        style = MaterialTheme.typography.noorTitle,
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.testTag("home_greeting"),
                    )
                    Spacer(Modifier.size(NoorDimens.Md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.prayer_next_title),
                            style = MaterialTheme.typography.noorLabel,
                            color = heroAccent(),
                        )
                        state.locationLabel?.let { label ->
                            HeroLocationChip(label)
                        }
                    }

                    Spacer(Modifier.size(NoorDimens.Sm))

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

                    Spacer(Modifier.size(NoorDimens.Sm))

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
                                .padding(horizontal = NoorDimens.Md, vertical = 6.dp)
                                .testTag("home_prayer_countdown"),
                        )
                    }
                }
            }

        else ->
            HomePrayerUnavailableCard(
                isDetecting = state.isLocating,
                onOpen = onOpen,
            )
    }
}

/** A quiet translucent pill for the resolved location inside the hero. */
@Composable
private fun HeroLocationChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NoorDimens.Sm + 2.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Xs),
        ) {
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.noorCaption,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomePrayerEnableCard(onOpen: () -> Unit) {
    NoorGlassCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_prayer_card"),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.noorTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("home_greeting"),
            )
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = stringResource(R.string.home_prayer_enable_hint),
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomePrayerUnavailableCard(isDetecting: Boolean, onOpen: () -> Unit) {
    NoorGlassCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_prayer_card"),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.noorTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("home_greeting"),
            )
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = stringResource(
                    if (isDetecting) R.string.prayer_loading else R.string.home_prayer_fallback,
                ),
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home_prayer_countdown"),
            )
        }
    }
}

/**
 * One compact row summarising the practice features. It is additive: the hero,
 * quick-access and daily sections are untouched, and the whole card disappears
 * when there is nothing to report.
 */
@Composable
private fun HomePracticeSection(
    state: HomePracticeUiState,
    onOpenAthkar: () -> Unit,
    onOpenTasbih: () -> Unit,
) {
    if (!state.hasContent && state.tasbih.totalCount == 0) return

    Column {
        NoorSectionTitle(
            title = stringResource(R.string.home_practice_title),
            icon = Icons.Rounded.BlurOn,
        )
        Spacer(Modifier.size(NoorDimens.Md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
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
}

@Composable
private fun HomePracticeCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    NoorGlassCard(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(NoorDimens.Md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.noorCaption,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = body,
            style = MaterialTheme.typography.noorTitle,
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

    Column {
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
                        style = MaterialTheme.typography.noorCaption,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(NoorDimens.Xs))
                    Text(
                        text = target.surah?.nameArabic ?: surahLabel,
                        style = MaterialTheme.typography.noorTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = stringResource(
                            R.string.home_continue_reading_fmt,
                            surahLabel,
                            target.ayahNumberInSurah,
                        ),
                        style = MaterialTheme.typography.noorSecondary,
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
                    tint = MaterialTheme.colorScheme.primary,
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
                        style = MaterialTheme.typography.noorSecondary,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.quran_audio_streaming_note),
                        style = MaterialTheme.typography.noorCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier
                        .size(NoorDimens.MinTouchTarget)
                        .testTag("home_now_playing_toggle")
                        .semantics { contentDescription = playLabel },
                ) {
                    Icon(
                        imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
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
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
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
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
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

/**
 * Placeholder copy for content that is not bundled yet. Deliberately the
 * quietest card on the screen — a hairline and no elevation — so it reads as
 * "not yet" rather than as a broken or missing section.
 */
@Composable
private fun NoorDailyPlaceholderCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(NoorDimens.Hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = NoorDimens.ReadableWidthMax)
                .padding(NoorDimens.Md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.noorLabel,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = body,
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
