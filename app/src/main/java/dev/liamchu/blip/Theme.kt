package dev.liamchu.blip

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DESIGN.md "How it looks": black, white, grey, and nothing else. Monospaced,
 * square-cornered, to sit next to the glasses rather than next to other apps.
 */
val BlipBlack = Color(0xFF000000)
val BlipWhite = Color(0xFFFFFFFF)
val BlipGrey = Color(0xFF8A8A8A)
val BlipDimGrey = Color(0xFF3C3C3C)
val BlipNearBlack = Color(0xFF0C0C0C)

private val Mono = FontFamily.Monospace

private val BlipColours = darkColorScheme(
    primary = BlipWhite,
    onPrimary = BlipBlack,
    secondary = BlipGrey,
    onSecondary = BlipBlack,
    background = BlipBlack,
    onBackground = BlipWhite,
    surface = BlipBlack,
    onSurface = BlipWhite,
    surfaceVariant = BlipNearBlack,
    onSurfaceVariant = BlipGrey,
    outline = BlipDimGrey,
    outlineVariant = BlipDimGrey,
    // Monochrome throughout: an error is told apart by inversion, never by hue.
    error = BlipWhite,
    onError = BlipBlack,
)

private val BlipTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = Mono,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 10.sp,
    ),
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(
        fontFamily = Mono,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        letterSpacing = 1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Mono,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Mono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    ),
)

/**
 * Every corner is square. Material3 Shapes insists on a corner-based shape, so
 * this is a zero radius rather than RectangleShape — identical on screen.
 */
private val Square = RoundedCornerShape(0.dp)

private val BlipShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)

@Composable
fun BlipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlipColours,
        typography = BlipTypography,
        shapes = BlipShapes,
        content = content,
    )
}
