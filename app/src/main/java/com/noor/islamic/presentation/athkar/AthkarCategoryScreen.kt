package com.noor.islamic.presentation.athkar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorOrnamentalDivider
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorDisplay

/**
 * The adhkar of one category, in source order.
 *
 * Each card shows the required repetition count, how many are left, and whether
 * the dhikr is finished. Repetition only advances on an explicit tap and is
 * clamped to the source's count, and a per-card reset keeps a mis-tap easy to
 * undo.
 */
@Composable
fun AthkarCategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: AthkarCategoryViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backLabel = stringResource(R.string.action_back)

    Column(modifier = modifier.fillMaxSize().testTag("athkar_category_screen")) {
        CategoryHeader(
            state = state,
            onBack = onBack,
            backLabel = backLabel,
            onReset = viewModel::resetCategory,
        )

        when (val current = state) {
            AthkarCategoryUiState.Loading -> Unit

            AthkarCategoryUiState.NotFound -> NoorEmptyState(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(R.string.athkar_title),
                description = stringResource(R.string.athkar_category_unavailable),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("athkar_category_unavailable"),
            )

            AthkarCategoryUiState.Error -> NoorEmptyState(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(R.string.athkar_title),
                description = stringResource(R.string.athkar_error),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("athkar_error"),
            )

            is AthkarCategoryUiState.Ready -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("athkar_dhikr_list"),
                contentPadding = PaddingValues(
                    start = NoorDimens.Lg,
                    end = NoorDimens.Lg,
                    top = NoorDimens.Sm,
                    bottom = NoorDimens.Xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(NoorDimens.Md),
            ) {
                items(current.items, key = { it.dhikr.id }) { item ->
                    DhikrCard(
                        item = item,
                        onRepeat = { viewModel.repeat(item.dhikr) },
                        onReset = { viewModel.resetDhikr(item.dhikr) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    state: AthkarCategoryUiState,
    onBack: () -> Unit,
    backLabel: String,
    onReset: () -> Unit,
) {
    val ready = state as? AthkarCategoryUiState.Ready
    val title = ready?.let { stringResource(athkarCategoryLabelRes(it.category.id)) }
        ?: stringResource(R.string.athkar_title)

    Column(modifier = Modifier.padding(horizontal = NoorDimens.Lg, vertical = NoorDimens.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("athkar_back")
                    .semantics { contentDescription = backLabel },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(NoorDimens.Sm))
            Text(
                text = title,
                style = MaterialTheme.typography.noorDisplay,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (ready != null && ready.totalCount > 0) {
            val summary = stringResource(
                R.string.athkar_category_progress_fmt,
                ready.completedCount,
                ready.totalCount,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val resetLabel = stringResource(R.string.athkar_reset_category)
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("athkar_reset_category")
                        .semantics { contentDescription = resetLabel },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            LinearProgressIndicator(
                progress = {
                    if (ready.totalCount == 0) 0f
                    else ready.completedCount.toFloat() / ready.totalCount
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("athkar_category_progress"),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Spacer(Modifier.size(NoorDimens.Sm))
            NoorOrnamentalDivider()
        }
    }
}

@Composable
private fun DhikrCard(
    item: AthkarItemUi,
    onRepeat: () -> Unit,
    onReset: () -> Unit,
) {
    val dhikr = item.dhikr
    val counterLabel = if (item.isComplete) {
        stringResource(R.string.athkar_completed_fmt, dhikr.repeatCount, dhikr.repeatCount)
    } else {
        stringResource(
            R.string.athkar_remaining_fmt,
            item.completedRepetitions,
            dhikr.repeatCount,
        )
    }
    val tapLabel = stringResource(
        R.string.athkar_repeat_desc,
        dhikr.repeatCount,
        item.remaining,
    )
    val resetLabel = stringResource(R.string.athkar_reset_dhikr)

    NoorGlassCard(
        onClick = onRepeat,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("athkar_dhikr_${dhikr.id}")
            .semantics {
                contentDescription = tapLabel
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.athkar_repeat_of_fmt, dhikr.repeatCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.weight(1f))
                if (item.isComplete) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(NoorDimens.Icon),
                    )
                } else {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("athkar_reset_${dhikr.id}")
                            .semantics { contentDescription = resetLabel },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.size(NoorDimens.Sm))

            // Arabic is always laid out right-to-left, whichever UI language is active.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = dhikr.arabicText,
                    style = MaterialTheme.typography.noorDisplay.copy(
                        textDirection = TextDirection.Rtl,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
            }

            if (dhikr.translation != null) {
                Spacer(Modifier.size(NoorDimens.Sm))
                Text(
                    text = dhikr.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (dhikr.reference != null) {
                Spacer(Modifier.size(NoorDimens.Sm))
                Text(
                    text = dhikr.reference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.size(NoorDimens.Md))

            LinearProgressIndicator(
                progress = { item.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("athkar_dhikr_progress_${dhikr.id}"),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Spacer(Modifier.size(NoorDimens.Xs))
            Text(
                text = counterLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
