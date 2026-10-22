package com.noor.islamic.presentation.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noor.islamic.R
import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.services.ServiceLocator
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.components.NoorSecondaryButton
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorFonts

/**
 * Offline search over the bundled FTS index. Nothing is uploaded and no network
 * call is made; the only input is the user's own query.
 */
@Composable
fun QuranSearchScreen(
    onBack: () -> Unit,
    onOpenAyah: (surahNumber: Int, ayahNumberInSurah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuranSearchViewModel = viewModel {
        QuranSearchViewModel(repository = ServiceLocator.quranRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val inputLabel = stringResource(R.string.quran_search_input_accessibility)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    NoorScreenContainer(
        modifier = modifier.testTag("quran_search_screen"),
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
                    .testTag("search_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.quran_search_back),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.quran_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::clear,
                            modifier = Modifier.testTag("search_clear_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.quran_search_clear),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("search_input")
                    .semantics { contentDescription = "$inputLabel: $query" },
            )
        }

        when (val current = state) {
            QuranSearchUiState.Prompt -> {
                NoorEmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.quran_search_title),
                    description = stringResource(R.string.quran_search_prompt_body),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_prompt"),
                )
            }

            QuranSearchUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("search_loading"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(NoorDimens.Xl),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(NoorDimens.Md))
                    Text(
                        text = stringResource(R.string.quran_search_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is QuranSearchUiState.Failed -> {
                Column(modifier = Modifier.weight(1f)) {
                    NoorEmptyState(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.quran_search_error_title),
                        description = stringResource(R.string.quran_search_error_body),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_error"),
                    )
                    NoorSecondaryButton(
                        text = stringResource(R.string.action_retry),
                        onClick = viewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NoorDimens.Xl),
                    )
                }
            }

            is QuranSearchUiState.Results -> {
                if (current.results.isEmpty()) {
                    NoorEmptyState(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.quran_search_empty_title),
                        description = stringResource(R.string.quran_search_empty_body, current.query),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_empty"),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.quran_search_results_count_fmt, current.results.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("search_result_count"),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("search_results"),
                        verticalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
                    ) {
                        items(
                            items = current.results,
                            key = { it.globalAyahNumber },
                        ) { ayah ->
                            SearchResultRow(
                                ayah = ayah,
                                surah = surahs[ayah.surahNumber],
                                onClick = { onOpenAyah(ayah.surahNumber, ayah.ayahNumberInSurah) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    ayah: Ayah,
    surah: Surah?,
    onClick: () -> Unit,
) {
    val surahLabel = surah?.nameTransliteration ?: ayah.surahNumber.toString()
    val accessibility = stringResource(
        R.string.quran_search_result_accessibility_fmt,
        surahLabel,
        ayah.ayahNumberInSurah,
    )
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_result_${ayah.globalAyahNumber}")
            .semantics(mergeDescendants = true) { contentDescription = accessibility },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NoorDimens.Md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = surah?.nameArabic ?: surahLabel,
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.quran_ayah_number_fmt, ayah.ayahNumberInSurah),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = ayah.textUthmani,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = NoorFonts.Display,
                    lineHeight = 32.sp,
                    textDirection = TextDirection.Rtl,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
