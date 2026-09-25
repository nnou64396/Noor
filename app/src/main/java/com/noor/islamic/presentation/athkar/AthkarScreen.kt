package com.noor.islamic.presentation.athkar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.components.NoorScreenContainer
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorTitle

/**
 * Athkar index: one card per category with the user's progress through it.
 *
 * The screen reads entirely from bundled content plus local counters, so it
 * works with no connectivity. When no corpus has been cleared for bundling it
 * explains that instead of showing invented categories.
 */
@Composable
fun AthkarScreen(
    modifier: Modifier = Modifier,
    viewModel: AthkarViewModel,
    onOpenCategory: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NoorScreenContainer(
        modifier = modifier.testTag("athkar_screen"),
        scrollable = true,
    ) {
        NoorSectionTitle(
            title = stringResource(R.string.athkar_title),
            icon = Icons.Rounded.Star,
        )

        when (val current = state) {
            AthkarUiState.Loading -> AthkarLoadingPlaceholder()

            AthkarUiState.Error -> NoorEmptyState(
                icon = Icons.Rounded.Star,
                title = stringResource(R.string.athkar_title),
                description = stringResource(R.string.athkar_error),
                modifier = Modifier
                    .weight(1f)
                    .testTag("athkar_error"),
            )

            is AthkarUiState.NoContentBundled -> NoorEmptyState(
                icon = Icons.Rounded.Star,
                title = stringResource(R.string.athkar_corpus_pending_title),
                description = stringResource(R.string.athkar_corpus_pending_body),
                modifier = Modifier
                    .weight(1f)
                    .testTag("athkar_no_content"),
            )

            is AthkarUiState.Ready -> Column(
                modifier = Modifier.testTag("athkar_category_list"),
                verticalArrangement = Arrangement.spacedBy(NoorDimens.Md),
            ) {
                current.items.forEach { item ->
                    AthkarCategoryCard(
                        item = item,
                        onClick = { onOpenCategory(item.category.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AthkarLoadingPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(NoorDimens.Md)) {
        repeat(3) {
            NoorGlassCard(onClick = null) {
                Text(
                    text = stringResource(R.string.athkar_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AthkarCategoryCard(
    item: AthkarCategoryItem,
    onClick: () -> Unit,
) {
    val title = stringResource(athkarCategoryLabelRes(item.category.id))
    val summary = stringResource(
        R.string.athkar_progress_fmt,
        item.progress.completedDhikr,
        item.progress.totalDhikr,
    )
    val accessibility = stringResource(R.string.athkar_category_open_desc, title, summary)

    NoorGlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("athkar_category_${item.category.id}")
            .semantics { contentDescription = accessibility },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.noorTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(NoorDimens.Xs))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(NoorDimens.Sm))
                LinearProgressIndicator(
                    progress = { item.progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { }
                        .testTag("athkar_category_progress_${item.category.id}"),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
            Spacer(Modifier.size(NoorDimens.Md))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
