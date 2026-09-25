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
import androidx.compose.ui.unit.Dp
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Standard full-screen container: draws the base background, optionally
 * scrolls its content and applies the Noor spacing rhythm.
 */
@Composable
fun NoorScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    horizontalPadding: Dp = NoorDimens.Lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            verticalArrangement = Arrangement.spacedBy(NoorDimens.Lg),
        ) {
            content()
        }
    }
}