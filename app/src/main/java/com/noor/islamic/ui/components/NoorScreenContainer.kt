package com.noor.islamic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Standard full-screen container.
 *
 * Draws a very soft vertical wash instead of a flat fill — enough to stop the
 * page reading as a dead rectangle, shallow enough that no text contrast is
 * affected. Applies the Noor spacing rhythm and, when [scrollable], the
 * vertical scroll.
 */
@Composable
fun NoorScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    horizontalPadding: Dp = NoorDimens.Lg,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(NoorDimens.Lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.background, scheme.surfaceContainerLow),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (scrollable) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = horizontalPadding, vertical = NoorDimens.Lg),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}
