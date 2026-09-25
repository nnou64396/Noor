package com.noor.islamic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorGlassBorderDark
import com.noor.islamic.ui.theme.NoorGlassBorderLight
import com.noor.islamic.ui.theme.NoorGlassDark
import com.noor.islamic.ui.theme.NoorGlassLight

/**
 * A "liquid glass" card: translucent layered surface, a soft gold-tinted
 * hairline border and gentle elevation. No backdrop blur — kept cheap to
 * render on mid-range devices.
 *
 * Wraps a [Surface] so elevation, clipping and the ripple are handled by
 * the material layer while the glass tint is drawn underneath.
 */
@Composable
fun NoorGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    elevation: Dp = NoorDimens.ElSm,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val glass = if (dark) NoorGlassDark else NoorGlassLight
    val border = if (dark) NoorGlassBorderDark else NoorGlassBorderLight
    val container = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.45f)

    var surfaceModifier = modifier
        .shadow(elevation = elevation, shape = shape, clip = false)
        .clip(shape)
        .background(container)
        .border(width = 1.dp, color = border, shape = shape)
    if (onClick != null) {
        surfaceModifier = surfaceModifier.clickable(onClick = onClick)
    }

    Surface(modifier = surfaceModifier, color = glass, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NoorDimens.Lg),
        ) {
            content()
        }
    }
}

/** Reusable rounded shapes used by the glass components. */
object NoorGlassShapes {
    val Small: Shape = RoundedCornerShape(NoorDimens.RadiusSm)
    val Medium: Shape = RoundedCornerShape(NoorDimens.RadiusMd)
    val Large: Shape = RoundedCornerShape(NoorDimens.RadiusLg)
}