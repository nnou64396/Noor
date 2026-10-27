package dz.techvision.noor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dz.techvision.noor.navigation.NoorTab
import dz.techvision.noor.ui.theme.LocalNoorIsDark
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.NoorGlassBorderDark
import dz.techvision.noor.ui.theme.NoorGlassBorderLight
import dz.techvision.noor.ui.theme.NoorGlassDark
import dz.techvision.noor.ui.theme.NoorGlassLight

/**
 * The Noor bottom navigation.
 *
 * A compact, translucent bar with a hairline top edge, a slim pill behind the
 * active icon and a short cross-fade on selection. Tabs, their order, their
 * routes and the [onTabSelected] contract are unchanged — this is purely the
 * visual layer, and every item keeps a 48dp touch target plus `Role.Tab`
 * semantics so it stays reachable with a screen reader or switch access.
 */
@Composable
fun NoorBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalNoorIsDark.current
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (dark) NoorGlassDark else NoorGlassLight,
        contentColor = scheme.onSurface,
        border = BorderStroke(
            NoorDimens.Hairline,
            if (dark) NoorGlassBorderDark else NoorGlassBorderLight,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(NoorDimens.BottomBarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoorTab.entries.forEach { tab ->
                val label = stringResource(tab.labelRes)
                NoorBottomBarItem(
                    label = label,
                    selected = tab.route == currentRoute,
                    icon = tab,
                    onClick = { onTabSelected(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NoorBottomBarItem(
    label: String,
    selected: Boolean,
    icon: NoorTab,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "noor_nav_content",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            scheme.primaryContainer
        } else {
            scheme.primaryContainer.copy(alpha = 0f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "noor_nav_indicator",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) NoorDimens.BottomBarIndicatorWidth else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "noor_nav_indicator_width",
    )

    Column(
        modifier = modifier
            .height(NoorDimens.BottomBarHeight)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth.coerceAtLeast(NoorDimens.BottomBarIndicatorWidth))
                .height(NoorDimens.BottomBarIndicatorHeight)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(indicatorColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

