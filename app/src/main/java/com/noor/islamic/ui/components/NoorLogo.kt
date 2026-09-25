package com.noor.islamic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.noor.islamic.ui.theme.NoorGold
import com.noor.islamic.ui.theme.NoorIvory

/**
 * The Noor mark: a gold crescent with a star, drawn on canvas so it can
 * be reused (onboarding, placeholders, launcher-adjacent branding).
 */
@Composable
fun NoorLogo(
    modifier: Modifier = Modifier,
    cutoutColor: Color,
    starColor: Color = NoorIvory,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f

        // Crescent
        drawCircle(
            color = NoorGold,
            radius = r * 0.44f,
            center = Offset(cx - r * 0.10f, cy),
        )
        drawCircle(
            color = cutoutColor,
            radius = r * 0.34f,
            center = Offset(cx + r * 0.12f, cy),
        )

        // Star sparkle
        val starCenter = Offset(cx + r * 0.24f, cy - r * 0.28f)
        val arm = r * 0.055f
        val depth = r * 0.16f
        val star = Path().apply {
            moveTo(starCenter.x, starCenter.y - arm)
            lineTo(starCenter.x + depth, starCenter.y)
            lineTo(starCenter.x, starCenter.y + arm)
            lineTo(starCenter.x - depth, starCenter.y)
            close()
        }
        drawPath(path = star, color = starColor)
    }
}