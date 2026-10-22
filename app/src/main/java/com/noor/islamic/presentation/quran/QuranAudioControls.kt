package com.noor.islamic.presentation.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.islamic.R
import com.noor.islamic.domain.model.quran.QuranAudioPlaybackState
import com.noor.islamic.domain.model.quran.Reciter
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Compact player shown under the reader. Play/pause acts on the ayah the reader is
 * focused on; prev/next step through the surah; repeat restarts the surah when it ends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAudioBar(
    state: QuranAudioPlaybackState,
    reciters: List<Reciter>,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeatChange: (Boolean) -> Unit,
    onReciterSelected: (Reciter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showReciterSheet by remember { mutableStateOf(false) }
    val isActive = state.currentAyah != null
    val playLabel = if (state.isPlaying) {
        stringResource(R.string.quran_audio_pause)
    } else {
        stringResource(R.string.quran_audio_play)
    }

    NoorGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quran_audio_bar"),
    ) {
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("quran_audio_error"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
        ) {
            AudioControlButton(
                icon = Icons.Rounded.SkipPrevious,
                enabled = isActive,
                label = stringResource(R.string.quran_audio_previous),
                testTag = "quran_audio_previous",
                onClick = onPrevious,
            )
            AudioControlButton(
                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                enabled = true,
                label = playLabel,
                testTag = "quran_audio_toggle",
                loading = state.isBuffering,
                onClick = onTogglePlayback,
            )
            AudioControlButton(
                icon = Icons.Rounded.SkipNext,
                enabled = isActive,
                label = stringResource(R.string.quran_audio_next),
                testTag = "quran_audio_next",
                onClick = onNext,
            )
            AudioControlButton(
                icon = Icons.Rounded.Repeat,
                enabled = true,
                label = stringResource(R.string.quran_audio_repeat),
                testTag = "quran_audio_repeat",
                active = state.repeatCurrentSurah,
                onClick = { onRepeatChange(!state.repeatCurrentSurah) },
            )
            Spacer(Modifier.weight(1f))
            ReciterButton(
                reciter = state.reciter,
                onClick = { showReciterSheet = true },
            )
        }

        AudioSeekRow(
            state = state,
            onSeek = onSeek,
        )

        Text(
            text = stringResource(R.string.quran_audio_streaming_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showReciterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showReciterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ReciterPicker(
                reciters = reciters,
                selected = state.reciter,
                onSelected = { reciter ->
                    onReciterSelected(reciter)
                    showReciterSheet = false
                },
            )
        }
    }
}

@Composable
private fun AudioControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    active: Boolean = false,
    loading: Boolean = false,
) {
    val contentColor = when {
        active -> MaterialTheme.colorScheme.primary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .testTag(testTag)
            .semantics { contentDescription = label },
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(NoorDimens.Icon),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(NoorDimens.Icon),
            )
        }
    }
}

@Composable
private fun AudioSeekRow(
    state: QuranAudioPlaybackState,
    onSeek: (Long) -> Unit,
) {
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, duration.coerceAtLeast(0L))
    val hasQueue = state.currentAyah != null
    val positionLabel = stringResource(
        R.string.quran_audio_position_fmt,
        formatAudioTimestamp(position),
        formatAudioTimestamp(duration),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = position.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            enabled = hasQueue && duration > 0L,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quran_audio_seek")
                .clearAndSetSemantics { contentDescription = positionLabel },
        )
        Text(
            text = positionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReciterButton(reciter: Reciter, onClick: () -> Unit) {
    val reciterName = stringResource(reciter.nameKey.labelRes)
    val label = stringResource(R.string.quran_audio_reciter_fmt, reciterName)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("quran_audio_reciter_button")
            .semantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NoorDimens.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.quran_audio_reciter_label),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(reciter.nameKey.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(NoorDimens.Xs))
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(NoorDimens.Md),
            )
        }
    }
}

@Composable
private fun ReciterPicker(
    reciters: List<Reciter>,
    selected: Reciter,
    onSelected: (Reciter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = NoorDimens.Xl),
    ) {
        Text(
            text = stringResource(R.string.quran_audio_reciter_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = NoorDimens.Lg),
        )
        Spacer(Modifier.size(NoorDimens.Xs))
        Text(
            text = stringResource(R.string.quran_audio_reciter_sheet_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = NoorDimens.Lg),
        )
        Spacer(Modifier.size(NoorDimens.Sm))
        reciters.forEach { reciter ->
            val selectedLabel = stringResource(R.string.quran_audio_reciter_selected)
            Surface(
                onClick = { onSelected(reciter) },
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("quran_reciter_${reciter.id}")
                    .semantics { stateDescription = selectedLabel },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NoorDimens.Lg, vertical = NoorDimens.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = reciter.id == selected.id,
                        onClick = { onSelected(reciter) },
                    )
                    Spacer(Modifier.width(NoorDimens.Md))
                    Column {
                        Text(
                            text = stringResource(reciter.nameKey.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(reciter.style.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
