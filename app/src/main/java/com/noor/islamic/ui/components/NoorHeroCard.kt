package com.noor.islamic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorHeroBottom
import com.noor.islamic.ui.theme.NoorHeroMotif
import com.noor.islamic.ui.theme.NoorHeroTop

/**
 * The Home centrepiece: a deep-blue gradient surface.
 *
 * This is the one place Noor is allowed to be fully saturated, and it is used
 * once per screen at most, so the brand reads as blue without the interface
 * becoming a wall of colour. Content defaults to white, which holds 15.4:1
 * against the top of the gradient and 10.5:1 at the bottom.
 *
 * Gold is available through [heroAccent] for a single small mark inside the
 * hero; see the accent rules in `Color.kt`.
 */
@Composable
fun NoorHeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(NoorDimens.Lg),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = Color.White,
        border = BorderStroke(NoorDimens.Hairline, Color.White.copy(alpha = 0.10f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(NoorHeroTop, NoorHeroBottom),
                    ),
                    shape = shape,
                )
                .padding(contentPadding),
        ) {
            // A whisper of the geometric motif, never enough to disturb text.
            NoorIslamicMotif(
                modifier = Modifier.matchParentSize(),
                color = NoorHeroMotif,
                alpha = 1f,
            )
            content()
        }
    }
}

/**
 * Gold for use *inside* a hero. The hero is always dark, so this is the one
 * place the light gold tone is correct and stays well clear of the contrast
 * floor (10.9:1).
 */
@Composable
fun heroAccent(): Color = com.noor.islamic.ui.theme.NoorGoldLight

/** Kept for callers that need to fill the hero behind custom content. */
@Composable
fun heroBrush(): Brush = Brush.verticalGradient(listOf(NoorHeroTop, NoorHeroBottom))
