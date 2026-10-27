package dz.techvision.noor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * A restrained geometric lattice — an eight-point-star rhythm drawn with
 * Canvas at very low opacity.
 *
 * Decorative only: it sits behind hero and onboarding content, never over
 * text, and the default alpha is low enough to read as texture rather than
 * pattern.
 */
@Composable
fun NoorIslamicMotif(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    alpha: Float = 0.05f,
) {
    Canvas(modifier = modifier) {
        val spacing = 46.dp.toPx()
        val starSize = 7.dp.toPx()
        val motifColor = color.copy(alpha = alpha)
        var y = 0f
        while (y < size.height + spacing) {
            var x = 0f
            while (x < size.width + spacing) {
                drawStarMotif(center = Offset(x, y), size = starSize, color = motifColor)
                x += spacing
            }
            y += spacing
        }
    }
}

private fun DrawScope.drawStarMotif(center: Offset, size: Float, color: Color) {
    val diamond = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size, center.y)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size, center.y)
        close()
    }
    drawPath(path = diamond, color = color)
    drawCircle(color = color, radius = size * 0.3f, center = center)
}

