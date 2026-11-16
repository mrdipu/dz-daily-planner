package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// DZ Ai-STUDIO Official Brand Palette
val DzRoyalBlue = Color(0xFF1151A0)
val DzRoyalBlueDark = Color(0xFF0A3975)
val DzRoyalBlueSoft = Color(0xFFEAF1FB)
val DzRoyalBlueBright = Color(0xFF5A9DF2)

val DzCrimsonRed = Color(0xFFDC1018)
val DzCrimsonRedDark = Color(0xFFA80A10)
val DzCrimsonSoft = Color(0xFFFDECEC)
val DzCrimsonBright = Color(0xFFFF5A60)

val DzWhite = Color(0xFFFFFFFF)
val DzNearBlack = Color(0xFF171717)

// Light Theme Surfaces
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF8FAFD)
val LightSurfaceVariant = Color(0xFFEEF2F8)
val LightOutline = Color(0xFFD5DEE9)
val LightOnSurfaceVariant = Color(0xFF525E70)

// Dark Theme Surfaces
val DarkBackground = Color(0xFF0E1218)
val DarkSurface = Color(0xFF151B24)
val DarkSurfaceVariant = Color(0xFF1E2633)
val DarkOutline = Color(0xFF2D3748)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)

// Semantic Status Colors
val EmeraldSuccess = Color(0xFF15803D)
val EmeraldSuccessSoft = Color(0xFFE8F7EE)
val AmberWarning = Color(0xFFB45309)
val AmberWarningSoft = Color(0xFFFEF3C7)

data class NoteColorOption(
    val key: String,
    val labelEn: String,
    val labelBn: String,
    val lightBg: Color,
    val darkBg: Color,
    val accentDot: Color
)

val NoteColorPalette = listOf(
    NoteColorOption("default", "Default", "ডিফল্ট", Color(0xFFF8FAFD), Color(0xFF151B24), DzRoyalBlue),
    NoteColorOption("royal_blue", "Royal Blue", "রয়্যাল ব্লু", Color(0xFFEBF2FC), Color(0xFF15253D), DzRoyalBlue),
    NoteColorOption("crimson_tint", "Crimson", "ক্রিমসন", Color(0xFFFCEEEE), Color(0xFF33181C), DzCrimsonRed),
    NoteColorOption("amber", "Warm Amber", "অ্যাম্বার", Color(0xFFFEF6E4), Color(0xFF2E2514), Color(0xFFD97706)),
    NoteColorOption("emerald", "Sage Mint", "সেজ মিন্ট", Color(0xFFECF8F0), Color(0xFF142B1E), Color(0xFF15803D)),
    NoteColorOption("lavender", "Soft Iris", "ল্যাভেন্ডার", Color(0xFFF3EEFD), Color(0xFF241D36), Color(0xFF6D28D9)),
    NoteColorOption("slate", "Cool Mist", "স্লেট", Color(0xFFF1F4F8), Color(0xFF1D232B), Color(0xFF475569))
)

fun noteCardColor(key: String, isDark: Boolean): Color {
    val option = NoteColorPalette.firstOrNull { it.key == key } ?: NoteColorPalette.first()
    return if (isDark) option.darkBg else option.lightBg
}

fun noteAccentDot(key: String): Color {
    return (NoteColorPalette.firstOrNull { it.key == key } ?: NoteColorPalette.first()).accentDot
}

fun eventAccentColor(key: String): Color = when (key) {
    "crimson_tint" -> DzCrimsonRed
    "amber" -> Color(0xFFD97706)
    "emerald" -> EmeraldSuccess
    "lavender" -> Color(0xFF6D28D9)
    "slate" -> Color(0xFF475569)
    else -> DzRoyalBlue
}
