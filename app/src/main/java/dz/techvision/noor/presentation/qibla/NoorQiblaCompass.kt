package dz.techvision.noor.presentation.qibla

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dz.techvision.noor.R
import dz.techvision.noor.ui.theme.LocalNoorIsDark
import dz.techvision.noor.ui.theme.NoorDimens
import dz.techvision.noor.ui.theme.NoorGold
import dz.techvision.noor.ui.theme.NoorGoldLight
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular Qibla compass drawn with the Noor palette.
 *
 * The inner dial (cardinal letters + tick marks + Qibla indicator) rotates
 * against the device heading so north always points where the phone points;
 * the device is facing the Qibla when the gold indicator lines up with the
 * fixed top notch of the outer frame.
 *
 * The rotation arithmetic ([angleOnCircle], [bearingPerpendicularOffset], and
 * the `-smoothHeading` transform) is deliberately unchanged from the validated
 * build — only the rendering was restyled. The dial itself is blue and quiet;
 * the gold marker is the single focal point, because it is the one element
 * the user is actually aiming.
 */
@Composable
fun NoorQiblaCompass(
    headingDegrees: Float,
    bearingToKaaba: Float,
    modifier: Modifier = Modifier,
) {
    val smoothHeading by animateFloatAsState(
        targetValue = headingDegrees,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "qibla_heading",
    )

    val dark = LocalNoorIsDark.current
    val outline = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val dialFill = MaterialTheme.colorScheme.surfaceContainerLow
    val gold = if (dark) NoorGoldLight else NoorGold

    // East and West anchors sit on the *physical* right/left edges, where the
    // dial's cardinal letters point. Compose's CenterStart/CenterEnd follow the
    // layout direction, so in the app's RTL layout they would mirror East and
    // West onto the wrong side; these absolute-offset alignments keep the
    // letters glued to the physical frame no matter the direction.
    val edgeGap = LocalDensity.current.run { (NoorDimens.Xs + 6.dp).roundToPx() }
    val eastAlignment: Alignment = Alignment { size, space, _ ->
        IntOffset(space.width - size.width - edgeGap, (space.height - size.height) / 2)
    }
    val westAlignment: Alignment = Alignment { size, space, _ ->
        IntOffset(edgeGap, (space.height - size.height) / 2)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Fixed chrome: the dial face, its rings, and the top notch.
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Soft face so the dial reads as a physical surface, not a void.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (dark) 0.16f else 0.10f),
                        dialFill.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius * 0.97f,
                center = center,
            )

            drawCircle(
                color = outlineVariant,
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = outlineVariant.copy(alpha = 0.45f),
                radius = radius - 10.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Fixed reference notch at top (device forward direction).
            val notchY = radius * 0.06f
            drawCircle(color = gold, radius = 4.dp.toPx(), center = Offset(center.x, notchY))
        }

        // Rotating dial: -heading so north aligns with the device heading.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -smoothHeading },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Tick marks every 15 degrees, stronger on the quadrants.
                for (step in 0 until 24) {
                    val angle = step * 15f
                    val major = step % 3 == 0
                    val inner = radius * if (major) 0.80f else 0.86f
                    val outer = radius * 0.93f
                    drawLine(
                        color = if (major) outline else outlineVariant.copy(alpha = 0.55f),
                        start = angleOnCircle(center, radius, angle, inner),
                        end = angleOnCircle(center, radius, angle, outer),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Qibla indicator: gold arc + outward arrow at the bearing angle.
                val k = (bearingToKaaba + 360f) % 360f
                val arcRadius = radius * 0.74f
                drawArc(
                    color = gold,
                    startAngle = k - 10f,
                    sweepAngle = 20f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                val arrowTip = angleOnCircle(center, radius, k, radius * 0.62f)
                val arrowBase = angleOnCircle(center, radius, k, radius * 0.82f)
                val side = bearingPerpendicularOffset(k, radius * 0.045f)
                drawLine(
                    color = gold,
                    start = arrowBase + side,
                    end = arrowTip,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = gold,
                    start = arrowBase - side,
                    end = arrowTip,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color = gold, radius = 4.dp.toPx(), center = arrowTip)
            }

            // Cardinal letters (universal compass marks). North is emphasised
            // so the reference direction is unambiguous at a glance.
            //
            // East/West use explicit, layout-direction-independent biases
            // (not `Alignment.CenterStart/CenterEnd`, which follow RTL and
            // would swap the two sides in Arabic): East always sits on the
            // physical right, West on the physical left.
            Text(
                text = stringResource(R.string.qibla_north),
                color = onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = NoorDimens.Xs + 6.dp),
            )
            Text(
                text = stringResource(R.string.qibla_east),
                color = onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(eastAlignment),
            )
            Text(
                text = stringResource(R.string.qibla_south),
                color = onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = NoorDimens.Xs + 6.dp),
            )
            Text(
                text = stringResource(R.string.qibla_west),
                color = onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(westAlignment),
            )
        }

        // Center hub: a small Kaaba-inspired marker in gold.
        val hubFill = MaterialTheme.colorScheme.surface
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = hubFill,
                radius = 9.dp.toPx(),
                center = center,
            )
            drawCircle(
                color = outlineVariant,
                radius = 9.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(color = gold, radius = 3.5f.dp.toPx(), center = center)
        }
    }
}

/** Point on a circle at [angleDegrees] clockwise from the top, scaled by [fraction] of [radius]. */
private fun angleOnCircle(center: Offset, radius: Float, angleDegrees: Float, fraction: Float): Offset {
    val rad = Math.toRadians(angleDegrees.toDouble())
    val vx = sin(rad) * radius * fraction
    val vy = -cos(rad) * radius * fraction
    return Offset(center.x + vx.toFloat(), center.y + vy.toFloat())
}

/** Small perpendicular (±) offset used to draw a two-stroke arrow. */
private fun bearingPerpendicularOffset(angleDegrees: Float, magnitude: Float): Offset {
    val rad = Math.toRadians(angleDegrees.toDouble())
    val px = -cos(rad) * magnitude
    val py = -sin(rad) * magnitude
    return Offset(px.toFloat(), py.toFloat())
}

