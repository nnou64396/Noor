package dz.techvision.noor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.noorTitle

/**
 * Section heading: a small blue icon, the title, and a hairline rule.
 *
 * The rule is what separates sections; the title is what orients. Keeping the
 * icon in `primary` rather than an accent colour is what makes the icon read
 * as part of the blue identity instead of as decoration.
 */
@Composable
fun NoorSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoorDimens.Sm),
            modifier = Modifier.padding(horizontal = NoorDimens.Xs),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(NoorDimens.Md + 2.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.noorTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (trailing == null) {
                    Modifier.weight(1f, fill = false)
                } else {
                    Modifier.weight(1f)
                },
            )
            if (trailing != null) {
                trailing()
            }
        }
        Spacer(Modifier.height(NoorDimens.Sm))
        NoorOrnamentalDivider()
    }
}

