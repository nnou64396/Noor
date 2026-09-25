package com.noor.islamic.presentation.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.islamic.R
import com.noor.islamic.domain.model.tasbih.TasbihSession
import com.noor.islamic.ui.components.NoorGlassCard
import com.noor.islamic.ui.components.NoorSectionTitle
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.noorDisplay

/**
 * The Tasbih counter: one large tap target, the remaining count, and the three
 * controls that matter (undo, reset, target).
 *
 * The layout is deliberately static rather than animated, so repeated tapping
 * stays cheap and the count never lags behind the finger.
 */
@Composable
fun TasbihScreen(
    modifier: Modifier = Modifier,
    viewModel: TasbihViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomTarget by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = NoorDimens.Lg, vertical = NoorDimens.Lg),
        verticalArrangement = Arrangement.spacedBy(NoorDimens.Lg),
    ) {
        NoorSectionTitle(
            title = stringResource(R.string.tasbih_title),
            icon = Icons.Rounded.BlurOn,
        )

        TasbihCounter(
            count = state.count,
            target = state.target,
            fraction = state.fraction,
            isComplete = state.isComplete,
            onIncrement = viewModel::increment,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val undoLabel = stringResource(R.string.tasbih_undo_desc)
            FilledTonalIconButton(
                onClick = viewModel::decrement,
                enabled = state.canDecrement,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("tasbih_undo")
                    .semantics { contentDescription = undoLabel },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Box(modifier = Modifier.weight(1f))

            val resetLabel = stringResource(R.string.tasbih_reset_desc)
            FilledTonalIconButton(
                onClick = viewModel::reset,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("tasbih_reset")
                    .semantics { contentDescription = resetLabel },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        TargetSelector(
            target = state.target,
            presets = state.presets,
            onSelect = viewModel::setTarget,
            onCustom = { showCustomTarget = true },
        )
    }

    if (showCustomTarget) {
        CustomTargetDialog(
            current = state.target,
            onDismiss = { showCustomTarget = false },
            onConfirm = { value ->
                viewModel.setTarget(value)
                showCustomTarget = false
            },
        )
    }
}

@Composable
private fun TasbihCounter(
    count: Int,
    target: Int,
    fraction: Float,
    isComplete: Boolean,
    onIncrement: () -> Unit,
) {
    val tapLabel = stringResource(R.string.tasbih_tap_desc, count, target)
    val progressLabel = if (isComplete) {
        stringResource(R.string.tasbih_complete, target)
    } else {
        stringResource(R.string.tasbih_remaining_fmt, target - count, target)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Track and progress share the same geometry so the ring stays put while
        // the count changes. The whole ring is the tap target.
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(240.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeWidth = 12.dp,
        )
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .size(240.dp)
                .testTag("tasbih_progress"),
            color = MaterialTheme.colorScheme.secondary,
            strokeWidth = 12.dp,
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .clickable(onClick = onIncrement)
                .testTag("tasbih_tap_area")
                .semantics {
                    contentDescription = tapLabel
                    liveRegion = LiveRegionMode.Polite
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.noorDisplay,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("tasbih_count"),
            )
        }
    }

    Text(
        text = progressLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tasbih_progress_label"),
    )
}

@Composable
private fun TargetSelector(
    target: Int,
    presets: List<Int>,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    NoorGlassCard(onClick = null, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(R.string.tasbih_target_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(NoorDimens.Sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = preset == target,
                        onClick = { onSelect(preset) },
                        label = { Text(preset.toString()) },
                        modifier = Modifier.testTag("tasbih_target_$preset"),
                    )
                }
                TextButton(
                    onClick = onCustom,
                    modifier = Modifier.testTag("tasbih_target_custom"),
                ) {
                    Text(
                        text = if (TasbihSession.PRESETS.contains(target)) {
                            stringResource(R.string.tasbih_target_custom)
                        } else {
                            stringResource(R.string.tasbih_target_current_fmt, target)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomTargetDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(current.toString()) }
    val parsed = text.trim().toIntOrNull()
    val isValid = parsed != null && parsed in TasbihSession.MIN_TARGET..TasbihSession.MAX_TARGET

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasbih_target_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() }.take(5) },
                singleLine = true,
                isError = text.isNotEmpty() && !isValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.tasbih_target_dialog_label)) },
                modifier = Modifier.testTag("tasbih_target_input"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid,
                modifier = Modifier.testTag("tasbih_target_confirm"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
