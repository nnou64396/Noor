package com.noor.islamic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noor.islamic.R
import com.noor.islamic.ui.theme.NoorDimens
import com.noor.islamic.ui.theme.NoorGold
import com.noor.islamic.ui.theme.noorDisplay

/**
 * The Noor mark is defined once, in a 108-unit design space that matches the
 * launcher vector exactly. Drawing scales that space uniformly into whatever
 * box it is given, so the mark can never be distorted or overflow.
 */
private const val DesignSpace = 108f

private const val CrescentOuterCx = 50f
private const val CrescentOuterCy = 56f
private const val CrescentOuterR = 27f
private const val CrescentInnerCx = 63.5f
private const val CrescentInnerCy = 56f
private const val CrescentInnerR = 20.9f
private const val CrescentHornX = 67.572f
private const val CrescentHornTopY = 35.5f
private const val CrescentHornBottomY = 76.5f

private const val StarCx = 72f
private const val StarCy = 39f
private const val StarSpan = 10f
private const val StarWaist = 2f

private val LockupRuleWidth = 112.dp

/**
 * A true crescent: the lune left by subtracting one circle from another. The
 * opening is real geometry rather than a hole painted in the background colour,
 * so the mark reads correctly on any surface and in any theme.
 */
private val crescentPath = Path().apply {
    moveTo(CrescentHornX, CrescentHornTopY)
    arcTo(
        rect = Rect(
            left = CrescentOuterCx - CrescentOuterR,
            top = CrescentOuterCy - CrescentOuterR,
            right = CrescentOuterCx + CrescentOuterR,
            bottom = CrescentOuterCy + CrescentOuterR,
        ),
        startAngleDegrees = -49.4f,
        sweepAngleDegrees = -261.2f,
        forceMoveTo = false,
    )
    arcTo(
        rect = Rect(
            left = CrescentInnerCx - CrescentInnerR,
            top = CrescentInnerCy - CrescentInnerR,
            right = CrescentInnerCx + CrescentInnerR,
            bottom = CrescentInnerCy + CrescentInnerR,
        ),
        startAngleDegrees = 78.77f,
        sweepAngleDegrees = 202.47f,
        forceMoveTo = false,
    )
    close()
}

private val starPath = Path().apply {
    moveTo(StarCx, StarCy - StarSpan)
    lineTo(StarCx + StarWaist, StarCy - StarWaist)
    lineTo(StarCx + StarSpan, StarCy)
    lineTo(StarCx + StarWaist, StarCy + StarWaist)
    lineTo(StarCx, StarCy + StarSpan)
    lineTo(StarCx + StarWaist, StarCy + StarWaist)
    lineTo(StarCx - StarSpan, StarCy)
    lineTo(StarCx - StarWaist, StarCy - StarWaist)
    close()
}

/**
 * The Noor mark: a gold crescent with a star, drawn on canvas so it can be
 * reused (onboarding, placeholders, launcher-adjacent branding).
 */
@Composable
fun NoorLogo(
    modifier: Modifier = Modifier,
    crescentColor: Color = NoorGold,
    starColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / DesignSpace
        val offsetX = (size.width - DesignSpace * unit) / 2f
        val offsetY = (size.height - DesignSpace * unit) / 2f
        withTransform({
            translate(offsetX, offsetY)
            scale(unit, unit)
        }) {
            drawPath(path = crescentPath, color = crescentColor)
            drawPath(path = starPath, color = starColor)
        }
    }
}

/**
 * The full Noor lockup: mark, wordmark and rule stacked the way the brand
 * artwork sets them. Used where there is room for the whole identity rather
 * than just the mark.
 */
@Composable
fun NoorFullLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = NoorDimens.Logo,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NoorLogo(modifier = Modifier.size(markSize))
        Spacer(Modifier.height(NoorDimens.Md))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.noorDisplay,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(NoorDimens.Sm))
        NoorOrnamentalDivider(modifier = Modifier.width(LockupRuleWidth))
    }
}
