package com.noor.islamic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.noor.islamic.ui.theme.NoorDimens

/**
 * Section divider: a pair of hairline rules meeting a small diamond.
 *
 * The rule is blue and low-contrast by default, so it separates content
 * without drawing attention. Pass [accent] to render the centre diamond in
 * gold — used sparingly, on the top-level boundaries only.
 */
@Composable
fun NoorOrnamentalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
    accent: Color = color,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(NoorDimens.Md + 4.dp),
    ) {
        val y = size.height / 2f
        val diamondSize = 6.dp.toPx()
        val gap = diamondSize + 8.dp.toPx()
        val lineColor = color.copy(alpha = 0.45f)

        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width / 2f - gap, y),
            strokeWidth = NoorDimens.Hairline.toPx(),
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width / 2f + gap, y),
            end = Offset(size.width, y),
            strokeWidth = NoorDimens.Hairline.toPx(),
        )

        val diamond = Path().apply {
            moveTo(size.width / 2f, y - diamondSize / 2f)
            lineTo(size.width / 2f + diamondSize / 2f, y)
            lineTo(size.width / 2f, y + diamondSize / 2f)
            lineTo(size.width / 2f - diamondSize / 2f, y)
            close()
        }
        drawPath(path = diamond, color = accent)
    }
}
