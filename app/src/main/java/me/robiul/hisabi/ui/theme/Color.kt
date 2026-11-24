package me.robiul.hisabi.ui.theme

import androidx.compose.ui.graphics.Color

// Pure OLED Black & Champagne Gold Color System
val DarkBgPrimary = Color(0xFF000000)       // Full pure black main background
val DarkBgSecondary = Color(0xFF0A0A0A)     // Pure black-charcoal card background
val DarkSurfaceElevated = Color(0xFF121212) // Elevated pure dark surface
val DarkSurfaceHighest = Color(0xFF1B1B1B)  // Highest elevated pure dark surface
val DarkBorder = Color(0xFF242424)          // Subtle pure dark borders and dividers

val LightBgPrimary = Color(0xFFFAF9F6)      // Clean warm-white background
val LightSurface = Color(0xFFFFFFFF)        // Card surface
val LightSurfaceElevated = Color(0xFFF2EFE9)// Elevated warm surface
val LightSurfaceHighest = Color(0xFFE7E2D8) // Highest warm surface
val LightBorder = Color(0xFFE0DBD0)         // Subtle warm hairline border

// Champagne Gold & Semantic Financial Accents
val ChampagneGold = Color(0xFFD6B46A)       // Primary accent (dark)
val ChampagneGoldDeep = Color(0xFFB58E3D)   // Primary accent (light)
val ChampagneGoldSoft = Color(0xFFF0D9A0)
val ChampagneContainerDark = Color(0xFF211B0F)
val ChampagneContainerLight = Color(0xFFF5ECD7)

val AccentGreen = Color(0xFF24B47E)         // Secondary accent (Emerald)
val AccentGreenDeep = Color(0xFF1A9164)
val AccentGreenContainerDark = Color(0xFF0A241A)
val AccentGreenContainerLight = Color(0xFFD6F4E7)

val TextPrimaryDark = Color(0xFFF5F5F5)
val TextSecondaryDark = Color(0xFFA1A7B2)
val TextPrimaryLight = Color(0xFF111318)
val TextSecondaryLight = Color(0xFF5F6572)

val SemanticExpense = Color(0xFFE45D68)
val SemanticExpenseDeep = Color(0xFFCC434E)
val SemanticIncome = Color(0xFF24B47E)
val SemanticTransfer = Color(0xFF7CA7E6)
val SemanticSavings = Color(0xFFD6B46A)
val SemanticWarning = Color(0xFFE5A93C)

// Luxury Matte-Black Virtual Debit Card Palettes
data class CardPalette(
    val id: Int,
    val name: String,
    val startColor: Color,
    val midColor: Color,
    val endColor: Color,
    val accentColor: Color,
    val onCardColor: Color = Color(0xFFF5F5F5),
)

val CardPalettes = listOf(
    CardPalette(
        id = 0,
        name = "Obsidian Gold",
        startColor = Color(0xFF141414),
        midColor = Color(0xFF090909),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFFD6B46A),
    ),
    CardPalette(
        id = 1,
        name = "Carbon Emerald",
        startColor = Color(0xFF0D1714),
        midColor = Color(0xFF060B0A),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFF24B47E),
    ),
    CardPalette(
        id = 2,
        name = "Midnight Sapphire",
        startColor = Color(0xFF101620),
        midColor = Color(0xFF080B10),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFF7CA7E6),
    ),
    CardPalette(
        id = 3,
        name = "Rose Obsidian",
        startColor = Color(0xFF1C1015),
        midColor = Color(0xFF0E080B),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFFE45D68),
    ),
    CardPalette(
        id = 4,
        name = "Amber Noir",
        startColor = Color(0xFF1C150F),
        midColor = Color(0xFF0E0B08),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFFE5A93C),
    ),
    CardPalette(
        id = 5,
        name = "Amethyst Matte",
        startColor = Color(0xFF16101D),
        midColor = Color(0xFF0B0810),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFFC4A7E7),
    ),
    CardPalette(
        id = 6,
        name = "Brushed Bronze",
        startColor = Color(0xFF1A1610),
        midColor = Color(0xFF0C0A08),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFFE0C07A),
    ),
    CardPalette(
        id = 7,
        name = "Graphite Pearl",
        startColor = Color(0xFF16181D),
        midColor = Color(0xFF0B0C0F),
        endColor = Color(0xFF000000),
        accentColor = Color(0xFFD6DCE5),
    ),
)
