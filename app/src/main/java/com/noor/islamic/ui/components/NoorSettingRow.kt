package com.noor.islamic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorGlassBorderDark
import com.noor.islamic.ui.theme.NoorGlassBorderLight

/**
 * A tappable row used in Settings — icon chip, title (and optional
 * subtitle), trailing slot, and a disclosure chevron when clickable.
 */
@Composable
fun NoorSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val dark = isSystemInDarkTheme()
    val shape = MaterialTheme.shapes.medium
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
    val borderColor = if (dark) NoorGlassBorderDark else NoorGlassBorderLight

    var rowModifier = modifier
        .clip(shape)
        .background(containerColor)
        .padding(horizontal = NoorDimens.Md, vertical = NoorDimens.Md)
    if (onClick != null) {
        rowModifier = rowModifier.clickable(onClick = onClick)
    }

    Surface(color = Color.Transparent) {
        Row(
            modifier = rowModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.size(NoorDimens.Md + 8.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(NoorDimens.Md),
                        )
                    }
                }
                Spacer(Modifier.width(NoorDimens.Md))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(NoorDimens.Sm))
                trailing()
            } else if (onClick != null) {
                Spacer(Modifier.width(NoorDimens.Sm))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}