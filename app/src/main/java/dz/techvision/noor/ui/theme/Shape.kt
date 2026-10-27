package dz.techvision.noor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape foundation.
 *
 * A single, evenly-stepped radius scale. Small elements stay tight so they read
 * as controls; large surfaces open up so cards feel generous. The steps are
 * monotonic and share the same visual language as [NoorDimens.Radius*], which
 * is what component code uses when it needs a concrete shape.
 */
val NoorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

