package com.noor.islamic.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noor.islamic.navigation.NoorTab
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Noor bottom navigation: glass-tinted surface, gold "glow" for the active
 * destination and the five main tabs.
 */
@Composable
fun NoorBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    NavigationBar(
        modifier = modifier,
        containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.72f),
        tonalElevation = NoorDimens.ElNone,
    ) {
        NoorTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            val selected = tab.route == currentRoute

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label,
                        tint = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onSecondaryContainer,
                    selectedTextColor = scheme.onSurface,
                    indicatorColor = scheme.secondaryContainer,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant,
                ),
                modifier = Modifier,
            )
        }
    }
}