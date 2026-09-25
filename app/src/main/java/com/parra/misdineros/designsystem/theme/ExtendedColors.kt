package com.parra.misdineros.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores del rediseño que no tienen un rol equivalente en [ColorScheme]: la superficie
 * destacada (héroe), las tarjetas planas con borde, los avisos de renovación y el control
 * segmentado. Se leen con `MisDinerosTheme.colors`.
 */
@Immutable
data class ExtendedColors(
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val card: Color,
    val cardBorder: Color,
    val cardDivider: Color,
    val urgentContainer: Color,
    val onUrgentContainer: Color,
    val soonContainer: Color,
    val onSoonContainer: Color,
    val iconTile: Color,
    val onIconTile: Color,
    val segmentTrack: Color,
    val segmentSelected: Color,
    val bar: Color,
)

val LightExtendedColors = ExtendedColors(
    hero = Color(0xFF0A3D5E),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFCFE3F0),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFECE6DB),
    cardDivider = Color(0xFFF0EBE2),
    urgentContainer = Color(0xFFFBE3D6),
    onUrgentContainer = Color(0xFFA63E14),
    soonContainer = Color(0xFFE1EEF6),
    onSoonContainer = Color(0xFF0A4F7A),
    iconTile = Color(0xFFEEF5FA),
    onIconTile = Color(0xFF0A4F7A),
    segmentTrack = Color(0xFFEAE4D9),
    segmentSelected = Color(0xFFFFFFFF),
    bar = Color(0xFF0A4F7A),
)

val DarkExtendedColors = ExtendedColors(
    hero = Color(0xFF0B4A72),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFCFE3F0),
    card = Color(0xFF182129),
    cardBorder = Color(0xFF26313B),
    cardDivider = Color(0xFF232D36),
    urgentContainer = Color(0xFF4A2618),
    onUrgentContainer = Color(0xFFFFB08A),
    soonContainer = Color(0xFF173247),
    onSoonContainer = Color(0xFF8ACEFF),
    iconTile = Color(0xFF1B2F3E),
    onIconTile = Color(0xFF7CC0EE),
    segmentTrack = Color(0xFF1F2931),
    segmentSelected = Color(0xFF33404C),
    bar = Color(0xFF7CC0EE),
)

/** Con colores dinámicos (Material You) los tonos propios se derivan del esquema del sistema. */
fun extendedColorsFrom(scheme: ColorScheme, isDark: Boolean) = ExtendedColors(
    hero = if (isDark) scheme.primaryContainer else scheme.primary,
    onHero = if (isDark) scheme.onPrimaryContainer else scheme.onPrimary,
    onHeroMuted = (if (isDark) scheme.onPrimaryContainer else scheme.onPrimary).copy(alpha = 0.8f),
    card = if (isDark) scheme.surfaceContainer else scheme.surfaceContainerLowest,
    cardBorder = scheme.outlineVariant,
    cardDivider = scheme.outlineVariant.copy(alpha = 0.6f),
    urgentContainer = scheme.tertiaryContainer,
    onUrgentContainer = scheme.onTertiaryContainer,
    soonContainer = scheme.secondaryContainer,
    onSoonContainer = scheme.onSecondaryContainer,
    iconTile = scheme.secondaryContainer,
    onIconTile = scheme.onSecondaryContainer,
    segmentTrack = scheme.surfaceContainerHighest,
    segmentSelected = if (isDark) scheme.surfaceBright else scheme.surfaceContainerLowest,
    bar = scheme.primary,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
