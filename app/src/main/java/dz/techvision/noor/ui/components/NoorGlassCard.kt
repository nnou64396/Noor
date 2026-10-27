package dz.techvision.noor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import dz.techvision.noor.ui.theme.LocalNoorIsDark
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.NoorGlassBorderDark
import dz.techvision.noor.ui.theme.NoorGlassBorderLight
import dz.techvision.noor.ui.theme.NoorGlassDark
import dz.techvision.noor.ui.theme.NoorGlassLight

/**
 * The Noor card.
 *
 * Despite the historical name, this is deliberately a *solid* premium surface
 * rather than a translucent one: content must stay highly readable, so depth
 * comes from an opaque tonal fill plus a hairline border and a very soft
 * elevation. The genuinely translucent treatment is reserved for
 * [NoorGlassSurface] and the Home hero, where there is no dense text to
 * preserve.
 */
@Composable
fun NoorGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    elevation: Dp = NoorDimens.ElSm,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: PaddingValues = PaddingValues(NoorDimens.Lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = elevation,
        border = BorderStroke(NoorDimens.Hairline, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            content()
        }
    }
}

/**
 * A translucent, hairline-bordered surface for chrome that sits *over* other
 * content — the bottom navigation bar and similar. Deliberately not used for
 * cards holding body copy, where a solid fill is more legible.
 */
@Composable
fun NoorGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val dark = LocalNoorIsDark.current
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (dark) NoorGlassDark else NoorGlassLight,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            NoorDimens.Hairline,
            if (dark) NoorGlassBorderDark else NoorGlassBorderLight,
        ),
        content = content,
    )
}

/** Reusable rounded shapes used by the Noor components. */
object NoorGlassShapes {
    val Small: Shape = RoundedCornerShape(NoorDimens.RadiusSm)
    val Medium: Shape = RoundedCornerShape(NoorDimens.RadiusMd)
    val Large: Shape = RoundedCornerShape(NoorDimens.RadiusLg)
}

