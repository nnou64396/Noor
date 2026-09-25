package com.noor.islamic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noor.islamic.R

@Composable
fun NoorLogo(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Image(
        painter = painterResource(if (isDark) R.drawable.noor_emblem_dark else R.drawable.noor_emblem),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/**
 * The full Noor lockup: mark, wordmark and rule stacked the way the brand
 * artwork sets them. Used where there is room for the whole identity rather
 * than just the mark.
 */
@Composable
fun NoorFullLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 160.dp,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Image(
        painter = painterResource(if (isDark) R.drawable.noor_logo_dark else R.drawable.noor_logo),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier
            .width(markSize)
            .aspectRatio(418f / 568f),
        contentScale = ContentScale.Fit,
    )
}
