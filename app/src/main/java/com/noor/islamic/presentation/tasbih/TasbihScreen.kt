package com.noor.islamic.presentation.tasbih

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noor.islamic.R
import com.noor.islamic.ui.components.NoorEmptyState
import com.noor.islamic.ui.components.NoorScreenContainer

@Composable
fun TasbihScreen(modifier: Modifier = Modifier) {
    NoorScreenContainer(modifier = modifier, scrollable = false) {
        NoorEmptyState(
            icon = Icons.Rounded.BlurOn,
            title = stringResource(R.string.tasbih_title),
            description = stringResource(R.string.feat_tasbih_desc),
            modifier = Modifier.weight(1f),
        )
    }
}