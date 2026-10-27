package dz.techvision.noor.presentation.quran

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.quran.RevelationType
import dz.techvision.noor.domain.model.quran.Surah
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.ui.components.NoorEmptyState
import dz.techvision.noor.ui.components.NoorGlassShapes
import dz.techvision.noor.ui.components.NoorOrnamentalDivider
import dz.techvision.noor.ui.components.NoorScreenContainer
import dz.techvision.noor.ui.components.NoorSecondaryButton
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.noorLargeTitle
import dz.techvision.noor.ui.theme.noorTitle

@Composable
fun QuranScreen(
    onSurahClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
) {
    val viewModel: QuranViewModel = viewModel {
        QuranViewModel(ServiceLocator.quranRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    QuranScreenContent(
        state = state,
        onSurahClick = onSurahClick,
        onRetry = viewModel::retry,
        onSearchClick = onSearchClick,
        onBookmarksClick = onBookmarksClick,
        modifier = modifier,
    )
}

@Composable
private fun QuranScreenContent(
    state: QuranUiState,
    onSurahClick: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
) {
    NoorScreenContainer(
        modifier = modifier.testTag("quran_screen"),
        scrollable = false,
    ) {
        QuranHeader(
            onSearchClick = onSearchClick,
            onBookmarksClick = onBookmarksClick,
        )

        when (state) {
            QuranUiState.Loading -> QuranLoadingState()
            QuranUiState.Empty -> QuranEmptyState()
            is QuranUiState.Error -> QuranErrorState(onRetry = onRetry)
            is QuranUiState.Success -> SurahList(
                surahs = state.surahs,
                onSurahClick = onSurahClick,
            )
        }
    }
}

@Composable
private fun QuranHeader(
    onSearchClick: () -> Unit,
    onBookmarksClick: () -> Unit,
) {
    val searchLabel = stringResource(R.string.quran_action_search)
    val bookmarksLabel = stringResource(R.string.quran_action_bookmarks)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quran_title),
                    style = MaterialTheme.typography.noorLargeTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(NoorDimens.Sm))
                Text(
                    text = stringResource(R.string.quran_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("quran_search_button")
                    .semantics { contentDescription = searchLabel },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = onBookmarksClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("quran_bookmarks_button")
                    .semantics { contentDescription = bookmarksLabel },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.size(NoorDimens.IconCircle + 8.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(NoorDimens.Xl),
                    )
                }
            }
        }
        Spacer(Modifier.size(NoorDimens.Md))
        NoorOrnamentalDivider()
    }
}

@Composable
private fun QuranLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(NoorDimens.Xl),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(NoorDimens.Md))
        Text(
            text = stringResource(R.string.quran_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.QuranEmptyState() {
    NoorEmptyState(
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        title = stringResource(R.string.quran_empty_title),
        description = stringResource(R.string.quran_empty_body),
        modifier = Modifier
            .weight(1f)
            .testTag("quran_empty"),
    )
}

@Composable
private fun QuranErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NoorEmptyState(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            title = stringResource(R.string.quran_error_title),
            description = stringResource(R.string.quran_error_body),
            modifier = Modifier
                .weight(1f)
                .testTag("quran_error"),
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

@Composable
private fun SurahList(
    surahs: List<Surah>,
    onSurahClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("surah_list"),
        verticalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
        contentPadding = PaddingValues(bottom = NoorDimens.Md),
    ) {
        items(
            items = surahs,
            key = { it.surahNumber },
        ) { surah ->
            SurahListItem(
                surah = surah,
                onClick = { onSurahClick(surah.surahNumber) },
            )
        }
    }
}

@Composable
private fun SurahListItem(
    surah: Surah,
    onClick: () -> Unit,
) {
    val accessibilityLabel = stringResource(
        R.string.quran_surah_accessibility_fmt,
        surah.surahNumber,
        surah.nameArabic,
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .testTag("surah_row_${surah.surahNumber}")
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            },
        shape = NoorGlassShapes.Medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = NoorDimens.ElSm,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NoorDimens.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
        ) {
            SurahNumberMedallion(surah.surahNumber)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.noorTitle.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = surah.nameTransliteration,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(NoorDimens.Xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.quran_ayah_count_fmt, surah.ayahCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(surah.revelationType.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(NoorDimens.Lg),
            )
        }
    }
}

@Composable
private fun SurahNumberMedallion(surahNumber: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        ),
        modifier = Modifier.size(NoorDimens.IconCircle),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = surahNumber.toString(),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
            )
        }
    }
}

private fun RevelationType.labelRes(): Int = when (this) {
    RevelationType.MECCAN -> R.string.quran_revelation_meccan
    RevelationType.MEDINAN -> R.string.quran_revelation_medinan
}

