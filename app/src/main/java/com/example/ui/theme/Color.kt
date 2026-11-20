package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val ObsidianVoid = Color(0xFF0B0D11)
val DeckGlass = Color(0xFF141820)
val DeckGlassElevated = Color(0xFF1B202B)
val DeckStroke = Color(0xFF283040)
val DeckStrokeSubtle = Color(0xFF364154)

val ElectricCyan = Color(0xFF22D3EE)
val TitaniumSlate = Color(0xFF8E9BAE)
val CyanContainer = Color(0xFF132C38)
val SlateContainer = Color(0xFF222938)
val OnNeon = Color(0xFF06161D)

val EmeraldPulse = Color(0xFF22C55E)
val AmberWarm = Color(0xFFF59E0B)
val CoralAlert = Color(0xFFEF4444)

val DeckTextPrimary = Color(0xFFF1F5F9)
val DeckTextSecondary = Color(0xFF94A3B8)
val DeckTextMuted = Color(0xFF64748B)

val DroidDeckDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnNeon,
    primaryContainer = CyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = TitaniumSlate,
    onSecondary = Color(0xFF111620),
    secondaryContainer = SlateContainer,
    onSecondaryContainer = DeckTextPrimary,
    tertiary = EmeraldPulse,
    onTertiary = Color(0xFF052010),
    tertiaryContainer = Color(0xFF102A1B),
    onTertiaryContainer = EmeraldPulse,
    background = ObsidianVoid,
    onBackground = DeckTextPrimary,
    surface = DeckGlass,
    onSurface = DeckTextPrimary,
    surfaceVariant = DeckGlassElevated,
    onSurfaceVariant = DeckTextSecondary,
    outline = DeckStroke,
    outlineVariant = DeckStrokeSubtle,
    error = CoralAlert,
    onError = Color(0xFF2B000B)
)
