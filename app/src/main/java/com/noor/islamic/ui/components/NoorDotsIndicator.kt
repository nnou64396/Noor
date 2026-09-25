package com.noor.islamic.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Onboarding page indicator: the active page is a wider gold pill while the
 * rest are quiet dots. Subtle width animation only — no continuous work.
 */
@Composable
fun NoorDotsIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(NoorDimens.Md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == currentIndex
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
            )
            Row(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .padding(end = if (index != count - 1) NoorDimens.Sm else 0.dp)
                    .background(
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        },
                        shape = CircleShape,
                    ),
            ) {}
        }
    }
}