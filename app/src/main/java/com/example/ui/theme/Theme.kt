package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.model.DeckAccentTheme

val DroidDeckShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DroidDeckTheme(
    accentTheme: DeckAccentTheme = DeckAccentTheme.CYBER_CYAN,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeForAccent(accentTheme),
        typography = DroidDeckTypography,
        shapes = DroidDeckShapes,
        content = content
    )
}
