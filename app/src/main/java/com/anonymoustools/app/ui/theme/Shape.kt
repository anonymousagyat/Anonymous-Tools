package com.anonymoustools.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardShapeXl = RoundedCornerShape(32.dp)
val CardShapeLg = RoundedCornerShape(24.dp)
val CardShapeMd = RoundedCornerShape(16.dp)
val PillShape = CircleShape

val Shapes = Shapes(
    small = CardShapeMd,
    medium = CardShapeLg,
    large = CardShapeXl
)
