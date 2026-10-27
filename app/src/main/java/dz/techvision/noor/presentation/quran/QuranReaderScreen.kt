package dz.techvision.noor.presentation.quran

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.RevelationType
import dz.techvision.noor.domain.model.quran.Surah
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.ui.components.NoorEmptyState
import dz.techvision.noor.ui.components.NoorOrnamentalDivider
import dz.techvision.noor.ui.components.NoorScreenContainer
import dz.techvision.noor.ui.components.NoorSecondaryButton
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.NoorFonts
import dz.techvision.noor.ui.theme.noorDisplay
import dz.techvision.noor.ui.theme.noorQuranArabic
import dz.techvision.noor.ui.theme.noorTitle

/** Header + trailing attribution occupy the first and last items of the list. */
private const val LEADING_HEADER_ITEMS = 1

@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialAyahNumber: Int? = null,
) {
    val viewModel: QuranReaderViewModel = viewModel {
        QuranReaderViewModel(
            repository = ServiceLocator.quranRepository,
            surahNumber = surahNumber,
            initialAyahNumber = initialAyahNumber,
            bookmarks = ServiceLocator.quranBookmarksRepository,
            readingProgress = ServiceLocator.readingProgressRepository,
            audio = ServiceLocator.quranAudioController,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    /**
     * Asks for POST_NOTIFICATIONS the first time the user starts recitation, so
     * the Media3 media notification can surface. Asking is fire-and-forget:
     * playback always proceeds, and a denial never blocks audio.
     */
    val requestNotificationPermissionOnce: () -> Unit = {
        if (!NotificationPermissionAsked.value) {
            NotificationPermissionAsked.value = true
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    QuranReaderScreenContent(
        surahNumber = surahNumber,
        state = state,
        reciters = ServiceLocator.quranAudioController.reciters,
        onBack = {
            viewModel.flushProgress()
            onBack()
        },
        onRetry = viewModel::retry,
        onAyahClick = viewModel::selectAyah,
        onBookmarkClick = viewModel::toggleBookmarkOf,
        onPlayAyah = { ayah ->
            requestNotificationPermissionOnce()
            viewModel.playAyah(ayah)
        },
        onTogglePlayback = {
            requestNotificationPermissionOnce()
            viewModel.togglePlayback()
        },
        onNextAyah = viewModel::skipToNextAyah,
        onPreviousAyah = viewModel::skipToPreviousAyah,
        onSeek = viewModel::seekAudio,
        onRepeatChange = viewModel::setRepeatCurrentSurah,
        onReciterSelected = viewModel::selectReciter,
        modifier = modifier,
    )
}

@Composable
private fun QuranReaderScreenContent(
    surahNumber: Int,
    state: QuranReaderUiState,
    reciters: List<dz.techvision.noor.domain.model.quran.Reciter>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAyahClick: (Int) -> Unit,
    onBookmarkClick: (Ayah) -> Unit,
    onPlayAyah: (Ayah) -> Unit,
    onTogglePlayback: () -> Unit,
    onNextAyah: () -> Unit,
    onPreviousAyah: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeatChange: (Boolean) -> Unit,
    onReciterSelected: (dz.techvision.noor.domain.model.quran.Reciter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, (state as? QuranReaderUiState.Ready)?.ayahs?.size) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { visibleIndex ->
            onAyahClick(visibleIndex - LEADING_HEADER_ITEMS)
        }
    }

    LaunchedEffect((state as? QuranReaderUiState.Ready)?.currentIndex) {
        val ready = state as? QuranReaderUiState.Ready ?: return@LaunchedEffect
        val target = ready.currentIndex + LEADING_HEADER_ITEMS
        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == target }
        if (!isVisible) {
            listState.animateScrollToItem(target.coerceAtLeast(0))
        }
    }

    NoorScreenContainer(
        modifier = modifier.testTag("quran_reader_screen"),
        scrollable = false,
    ) {
        ReaderTopBar(
            surahNumber = surahNumber,
            state = state,
            onBack = onBack,
        )

        when (val current = state) {
            QuranReaderUiState.Loading -> ReaderLoadingState()
            QuranReaderUiState.Empty -> ReaderEmptyState()
            is QuranReaderUiState.Error -> ReaderErrorState(
                error = current.kind,
                onRetry = onRetry,
            )
            is QuranReaderUiState.Ready -> {
                ReaderContent(
                    state = current,
                    listState = listState,
                    onAyahClick = onAyahClick,
                    onBookmarkClick = onBookmarkClick,
                    onPlayAyah = onPlayAyah,
                )
                current.audio?.let { playback ->
                    QuranAudioBar(
                        state = playback,
                        reciters = reciters,
                        onTogglePlayback = onTogglePlayback,
                        onNext = onNextAyah,
                        onPrevious = onPreviousAyah,
                        onSeek = onSeek,
                        onRepeatChange = onRepeatChange,
                        onReciterSelected = onReciterSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    surahNumber: Int,
    state: QuranReaderUiState,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("reader_back_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.quran_reader_back),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (state is QuranReaderUiState.Ready) {
                Text(
                    text = state.surah.nameArabic,
                    style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = stringResource(R.string.quran_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            if (surahNumber > 0) {
                Text(
                    text = stringResource(R.string.quran_surah_number_fmt, surahNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReaderLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reader_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(NoorDimens.Xl),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(NoorDimens.Md))
        Text(
            text = stringResource(R.string.quran_reader_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.ReaderEmptyState() {
    NoorEmptyState(
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        title = stringResource(R.string.quran_reader_empty_title),
        description = stringResource(R.string.quran_reader_empty_body),
        modifier = Modifier
            .weight(1f)
            .testTag("reader_empty"),
    )
}

@Composable
private fun ReaderErrorState(
    error: QuranReaderError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NoorEmptyState(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            title = stringResource(R.string.quran_reader_error_title),
            description = stringResource(readerErrorDescription(error)),
            modifier = Modifier
                .weight(1f)
                .testTag("reader_error"),
        )
        NoorSecondaryButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NoorDimens.Xl),
        )
    }
}

private fun readerErrorDescription(error: QuranReaderError): Int = when (error) {
    QuranReaderError.InvalidSurahNumber -> R.string.quran_reader_invalid_body
    QuranReaderError.SurahNotFound -> R.string.quran_reader_not_found_body
    QuranReaderError.NoAyahs -> R.string.quran_reader_empty_body
    QuranReaderError.LoadFailed -> R.string.quran_reader_error_load_body
}

@Composable
private fun ColumnScope.ReaderContent(
    state: QuranReaderUiState.Ready,
    listState: LazyListState,
    onAyahClick: (Int) -> Unit,
    onBookmarkClick: (Ayah) -> Unit,
    onPlayAyah: (Ayah) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("ayah_list"),
        verticalArrangement = Arrangement.spacedBy(NoorDimens.Xs),
        contentPadding = PaddingValues(
            top = NoorDimens.Sm,
            bottom = NoorDimens.Xl,
        ),
    ) {
        item(key = "surah_header") {
            ReaderHeader(surah = state.surah)
        }
        itemsIndexed(
            items = state.ayahs,
            key = { _, ayah -> ayah.globalAyahNumber },
        ) { index, ayah ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AyahItem(
                    ayah = ayah,
                    isCurrent = index == state.currentIndex,
                    isBookmarked = ayah.ayahNumberInSurah in state.bookmarkedAyahNumbers,
                    isPlaying = state.audio?.let { playback ->
                        playback.isPlaying &&
                            playback.currentAyah?.globalAyahNumber == ayah.globalAyahNumber
                    } ?: false,
                    showDivider = index < state.ayahs.lastIndex,
                    onClick = { onAyahClick(index) },
                    onBookmarkClick = { onBookmarkClick(ayah) },
                    onPlayClick = { onPlayAyah(ayah) },
                )
            }
        }
        item(key = "source_attribution") {
            SourceAttribution()
        }
    }
}

@Composable
private fun ReaderHeader(surah: Surah) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = surah.nameArabic,
            style = MaterialTheme.typography.noorDisplay.copy(textDirection = TextDirection.Rtl),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = surah.nameTransliteration,
            style = MaterialTheme.typography.noorTitle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = surah.nameEnglish,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(NoorDimens.Md))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NoorDimens.Xs),
        ) {
            Text(
                text = stringResource(R.string.quran_ayah_count_fmt, surah.ayahCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(surah.revelationType.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (surah.startPage > 0) {
                Text(
                    text = stringResource(R.string.quran_start_page_fmt, surah.startPage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(NoorDimens.Md))
        NoorOrnamentalDivider()
    }
}

@Composable
private fun AyahItem(
    ayah: Ayah,
    isCurrent: Boolean,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    val currentLabel = stringResource(R.string.quran_reader_current_ayah)
    val bookmarkLabel = stringResource(
        if (isBookmarked) R.string.quran_reader_bookmark_remove else R.string.quran_reader_bookmark_add,
    )
    val playLabel = stringResource(
        if (isPlaying) R.string.quran_audio_pause else R.string.quran_audio_play,
    )
    val accessibilityLabel = stringResource(
        R.string.quran_reader_ayah_accessibility_fmt,
        ayah.ayahNumberInSurah,
        ayah.textUthmani,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_row_${ayah.ayahNumberInSurah}"),
    ) {
        Surface(
            onClick = onClick,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            shape = MaterialTheme.shapes.medium,
            border = if (isCurrent) {
                null
            } else {
                BorderStroke(
                    NoorDimens.Hairline,
                    MaterialTheme.colorScheme.outlineVariant,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NoorDimens.Md),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
            ) {
                AyahNumberMarker(
                    number = ayah.ayahNumberInSurah,
                    isCurrent = isCurrent,
                    currentLabel = currentLabel,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = ayah.textUthmani,
                        style = MaterialTheme.typography.noorQuranArabic.copy(
                            textDirection = TextDirection.Rtl,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                    )
                    if (ayah.hasSajdah) {
                        Spacer(Modifier.size(NoorDimens.Xs))
                        Text(
                            text = stringResource(R.string.quran_sajda),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AyahActionButton(
                        icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        label = bookmarkLabel,
                        testTag = "ayah_bookmark_${ayah.ayahNumberInSurah}",
                        onClick = onBookmarkClick,
                    )
                    AyahActionButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        label = playLabel,
                        testTag = "ayah_play_${ayah.ayahNumberInSurah}",
                        onClick = onPlayClick,
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                thickness = 1.dp,
            )
        }
    }
}

@Composable
private fun AyahActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .testTag(testTag)
            .semantics { contentDescription = label },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(NoorDimens.Icon),
        )
    }
}

@Composable
private fun AyahNumberMarker(number: Int, isCurrent: Boolean, currentLabel: String) {
    val markerDescription = stringResource(R.string.quran_ayah_number_fmt, number)
    Surface(
        shape = CircleShape,
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (isCurrent) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        ),
        modifier = Modifier
            .size(40.dp)
            .clearAndSetSemantics {
                contentDescription = markerDescription
                this.stateDescription = if (isCurrent) currentLabel else ""
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr),
            )
        }
    }
}

@Composable
private fun SourceAttribution() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = NoorDimens.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NoorDimens.Xs),
    ) {
        Text(
            text = stringResource(R.string.quran_source_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.quran_source_copyright),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun RevelationType.labelRes(): Int = when (this) {
    RevelationType.MECCAN -> R.string.quran_revelation_meccan
    RevelationType.MEDINAN -> R.string.quran_revelation_medinan
}

/**
 * Remembers that POST_NOTIFICATIONS has already been asked for in this process.
 * Keeps the permission prompt from reappearing after the user has answered it.
 */
private object NotificationPermissionAsked {
    var value: Boolean = false
}

