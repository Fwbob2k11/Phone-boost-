package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val DroidDeckShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DroidDeckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DroidDeckDarkColorScheme,
        typography = DroidDeckTypography,
        shapes = DroidDeckShapes,
        content = content
    )
}
