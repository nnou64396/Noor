package dz.techvision.noor.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dz.techvision.noor.R
import dz.techvision.noor.ui.theme.LocalNoorIsDark
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.noorCaption
import dz.techvision.noor.ui.theme.noorLargeTitle

/**
 * The Noor mark: an eight-point khatam star with a gold centre, drawn as a
 * vector so it stays sharp from a 16dp in-list icon up to a full-bleed splash.
 *
 * Two colour variants exist because the mark sits on both the light page and
 * the midnight page; the variant follows the *resolved* theme, so pinning Dark
 * while the system is Light still picks the light-on-dark artwork.
 *
 * [onDarkSurface] forces the light artwork for the places that sit on a deep
 * blue backdrop regardless of the user's theme (onboarding).
 */
@Composable
fun NoorLogo(
    modifier: Modifier = Modifier,
    size: Dp = NoorDimens.Logo,
    onDarkSurface: Boolean = false,
) {
    val dark = LocalNoorIsDark.current
    Image(
        painter = painterResource(
            if (dark || onDarkSurface) R.drawable.noor_mark_dark else R.drawable.noor_mark,
        ),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

/**
 * The Noor wordmark set in the display serif, so the name keeps the same voice
 * as the rest of the display type instead of arriving as an image.
 */
@Composable
fun NoorWordmark(
    modifier: Modifier = Modifier,
    onDarkSurface: Boolean = false,
) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.noorLargeTitle,
        color = if (onDarkSurface) Color.White else MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/**
 * Mark and wordmark side by side — the compact brand lockup used in headers.
 *
 * Laid out with a plain [Row], so it mirrors automatically in RTL locales
 * without a separate asset.
 */
@Composable
fun NoorLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = NoorDimens.Logo,
    onDarkSurface: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
    ) {
        NoorLogo(size = markSize, onDarkSurface = onDarkSurface)
        NoorWordmark(onDarkSurface = onDarkSurface)
    }
}

/**
 * The full stacked lockup: mark above the wordmark and tagline. Reserved for
 * places with room for the whole identity, such as onboarding.
 */
@Composable
fun NoorFullLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 96.dp,
    tagline: String? = stringResource(R.string.home_header_tagline),
    onDarkSurface: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NoorDimens.Md),
    ) {
        NoorLogo(size = markSize, onDarkSurface = onDarkSurface)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NoorWordmark(onDarkSurface = onDarkSurface)
            if (tagline != null) {
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.noorCaption,
                    color = if (onDarkSurface) {
                        Color.White.copy(alpha = 0.74f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

