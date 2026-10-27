package dz.techvision.noor.presentation.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.techvision.noor.R
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.ui.components.NoorEmptyState
import dz.techvision.noor.ui.components.NoorScreenContainer
import dz.techvision.noor.ui.theme.NoorDimens

/** Locally saved ayah bookmarks, ordered newest first. */
@Composable
fun QuranBookmarksScreen(
    onBack: () -> Unit,
    onOpenAyah: (surahNumber: Int, ayahNumberInSurah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuranBookmarksViewModel = viewModel {
        QuranBookmarksViewModel(
            bookmarks = ServiceLocator.quranBookmarksRepository,
            repository = ServiceLocator.quranRepository,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NoorScreenContainer(
        modifier = modifier.testTag("quran_bookmarks_screen"),
        scrollable = false,
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
                    .testTag("bookmarks_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.quran_bookmarks_back),
                )
            }
            Text(
                text = stringResource(R.string.quran_bookmarks_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        when (val current = state) {
            QuranBookmarksUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("bookmarks_loading"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(NoorDimens.Xl),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(NoorDimens.Md))
                    Text(
                        text = stringResource(R.string.quran_bookmarks_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            QuranBookmarksUiState.Empty -> {
                NoorEmptyState(
                    icon = Icons.Rounded.Bookmark,
                    title = stringResource(R.string.quran_bookmarks_empty_title),
                    description = stringResource(R.string.quran_bookmarks_empty_body),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bookmarks_empty"),
                )
            }

            QuranBookmarksUiState.Error -> {
                NoorEmptyState(
                    icon = Icons.Rounded.Bookmark,
                    title = stringResource(R.string.quran_bookmarks_error_title),
                    description = stringResource(R.string.quran_bookmarks_error_body),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bookmarks_error"),
                )
            }

            is QuranBookmarksUiState.Ready -> {
                Text(
                    text = stringResource(R.string.quran_bookmarks_count_fmt, current.items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("bookmarks_list"),
                    verticalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
                ) {
                    items(
                        items = current.items,
                        key = { it.bookmark.ref.globalAyahNumber },
                    ) { item ->
                        BookmarkRow(
                            item = item,
                            onOpen = {
                                onOpenAyah(item.bookmark.ref.surahNumber, item.bookmark.ref.ayahNumberInSurah)
                            },
                            onRemove = { viewModel.remove(item.bookmark) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    item: BookmarkItem,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val surahLabel = item.surah?.nameTransliteration ?: item.bookmark.ref.surahNumber.toString()
    val openLabel = stringResource(
        R.string.quran_bookmarks_open_fmt,
        surahLabel,
        item.bookmark.ref.ayahNumberInSurah,
    )
    val removeLabel = stringResource(
        R.string.quran_bookmarks_remove_fmt,
        surahLabel,
        item.bookmark.ref.ayahNumberInSurah,
    )

    Surface(
        onClick = onOpen,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bookmark_${item.bookmark.ref.globalAyahNumber}")
            .semantics(mergeDescendants = true) { contentDescription = openLabel },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = NoorDimens.Md, top = NoorDimens.Sm, bottom = NoorDimens.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = NoorDimens.Sm),
            ) {
                Text(
                    text = item.surah?.nameArabic ?: surahLabel,
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.quran_ayah_number_fmt,
                        item.bookmark.ref.ayahNumberInSurah,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("bookmark_remove_${item.bookmark.ref.globalAyahNumber}")
                    .semantics { contentDescription = removeLabel },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

