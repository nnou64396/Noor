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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A refined ornamental divider: two thin gold hairlines meeting a small
 * diamond in the middle. Used sparingly to separate sections.
 */
@Composable
fun NoorOrnamentalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(12.dp)) {
        val y = size.height / 2f
        val diamondSize = 7.dp.toPx()
        val gap = diamondSize + 8.dp.toPx()
        val lineColor = color.copy(alpha = 0.38f)
        val strokeWidth = 1.dp.toPx()

        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width / 2f - gap, y),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width / 2f + gap, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )

        val diamond = Path().apply {
            moveTo(size.width / 2f, y - diamondSize / 2f)
            lineTo(size.width / 2f + diamondSize / 2f, y)
            lineTo(size.width / 2f, y + diamondSize / 2f)
            lineTo(size.width / 2f - diamondSize / 2f, y)
            close()
        }
        drawPath(path = diamond, color = color)
        drawCircle(color = color.copy(alpha = 0.7f), radius = 1.5.dp.toPx(), center = Offset(size.width / 2f, y), style = Stroke(width = 1.dp.toPx()))
    }
}