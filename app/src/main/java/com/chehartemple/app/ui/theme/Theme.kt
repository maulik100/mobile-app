package com.chehartemple.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Maroon = Color(0xFF800000)
private val MaroonDark = Color(0xFF5C0000)
private val Gold = Color(0xFFCDA434)
private val GoldLight = Color(0xFFFFF8E8)
private val Saffron = Color(0xFFE8860C)
private val Cream = Color(0xFFFFF9F0)
private val White = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF2D1B00)
private val TextSecondary = Color(0xFF6B5B4B)
private val TextHint = Color(0xFF9B8B7B)
private val Border = Color(0xFFF0E6D6)

private val TempleColorScheme = lightColorScheme(
    primary = Maroon,
    onPrimary = Color(0xFFFFD700),
    primaryContainer = GoldLight,
    onPrimaryContainer = MaroonDark,
    secondary = Gold,
    onSecondary = White,
    background = Cream,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFFFF5E6),
    onSurfaceVariant = TextSecondary,
    outline = Border,
    error = Color(0xFFB22222),
)

private val TempleShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

private val TempleTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = TextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = TextSecondary),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = TextHint),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Maroon),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextHint),
)

@Composable
fun CheharTempleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TempleColorScheme,
        shapes = TempleShapes,
        typography = TempleTypography,
        content = content
    )
}
