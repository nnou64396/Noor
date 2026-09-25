package com.noor.islamic.presentation.quran

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noor.islamic.R
import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.RevelationType
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.services.ServiceLocator
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorOrnamentalDivider
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSecondaryButton
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorFonts
import com.noor.islamic.ui.theme.noorDisplay
import com.noor.islamic.ui.theme.noorTitle

@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuranReaderViewModel = viewModel {
        QuranReaderViewModel(
            repository = ServiceLocator.quranRepository,
            surahNumber = surahNumber,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    QuranReaderScreenContent(
        surahNumber = surahNumber,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun QuranReaderScreenContent(
    surahNumber: Int,
    state: QuranReaderUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoorScreenContainer(
        modifier = modifier.testTag("quran_reader_screen"),
        scrollable = false,
    ) {
        ReaderTopBar(
            surahNumber = surahNumber,
            state = state,
            onBack = onBack,
        )

        when (state) {
            QuranReaderUiState.Loading -> ReaderLoadingState()
            QuranReaderUiState.Empty -> ReaderEmptyState()
            is QuranReaderUiState.Error -> ReaderErrorState(
                error = state.kind,
                onRetry = onRetry,
            )
            is QuranReaderUiState.Ready -> ReaderContent(state)
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
            color = MaterialTheme.colorScheme.secondary,
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
private fun ColumnScope.ReaderContent(state: QuranReaderUiState.Ready) {
    LazyColumn(
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
                    showDivider = index < state.ayahs.lastIndex,
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
            color = MaterialTheme.colorScheme.secondary,
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
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NoorDimens.Md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
        ) {
            AyahNumberMarker(number = ayah.ayahNumberInSurah)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ayah.textUthmani,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NoorFonts.Display,
                        fontWeight = FontWeight.Normal,
                        fontSize = 26.sp,
                        lineHeight = 52.sp,
                        letterSpacing = 0.sp,
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
                        color = MaterialTheme.colorScheme.secondary,
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
private fun AyahNumberMarker(number: Int) {
    val markerDescription = stringResource(R.string.quran_ayah_number_fmt, number)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
        ),
        modifier = Modifier
            .size(40.dp)
            .clearAndSetSemantics {
                contentDescription = markerDescription
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
