package pl.cukrzycowy.ollee.glycemia.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object OlleeShapes {
    val Card = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(CornerSize(50))
    val Chip = RoundedCornerShape(20.dp)
}

val OlleeMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = OlleeShapes.Card,
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
