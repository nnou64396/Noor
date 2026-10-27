package dz.techvision.noor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dz.techvision.noor.ui.theme.NoorAccent
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.noorSecondary
import dz.techvision.noor.ui.theme.noorTitle

/**
 * Polished empty / "coming soon" state.
 *
 * This is a designed state, not a failure state: a soft blue disc holds the
 * icon, the headline explains what will appear, and a single gold hairline
 * closes the composition. Copy is capped with `widthIn(max = ...)` rather than
 * a fixed width, so it stays comfortable on a narrow phone and does not stretch
 * on a large one.
 */
@Composable
fun NoorEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(NoorDimens.IconCircle + 20.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(NoorDimens.Xl),
                )
            }
        }

        Spacer(Modifier.height(NoorDimens.Lg))

        Text(
            text = title,
            style = MaterialTheme.typography.noorTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (description != null) {
            Spacer(Modifier.height(NoorDimens.Sm))
            Text(
                text = description,
                style = MaterialTheme.typography.noorSecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = NoorDimens.ReadableWidthMax),
            )
        }

        if (action != null) {
            Spacer(Modifier.height(NoorDimens.Lg))
            action()
        }

        Spacer(Modifier.height(NoorDimens.Lg))
        NoorOrnamentalDivider(
            modifier = Modifier.widthIn(max = 140.dp),
            color = MaterialTheme.colorScheme.outline,
            accent = NoorAccent.Decorative,
        )
    }
}

