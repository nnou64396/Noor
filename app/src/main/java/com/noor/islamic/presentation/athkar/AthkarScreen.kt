package com.noor.islamic.presentation.athkar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noor.islamic.R
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorScreenContainer

@Composable
fun AthkarScreen(modifier: Modifier = Modifier) {
    NoorScreenContainer(modifier = modifier, scrollable = false) {
        NoorEmptyState(
            icon = Icons.Rounded.Star,
            title = stringResource(R.string.athkar_title),
            description = stringResource(R.string.feat_athkar_desc),
            modifier = Modifier.weight(1f),
        )
    }
}