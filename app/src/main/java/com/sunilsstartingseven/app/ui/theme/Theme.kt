package com.sunilsstartingseven.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunilsstartingseven.app.R

object Brand {
    val Navy = Color(0xFF032962)
    val SecondaryNavy = Color(0xFF0C3F86)
    val Green = Color(0xFF0CAA4D)
    val Grass = Color(0xFF2ED35D)
    val Primary = Color(0xFF087B39)
    val AccentBlue = Color(0xFF075EFC)
    val Sky = Color(0xFFE7F0FF)
    val Mint = Color(0xFFCCEDDC)
    val MutedMint = Color(0xFF83C4AE)
    val Highlight = Color(0xFFFAC410)
    val Background = Color(0xFFF7FAF8)
    val TextSecondary = Color(0xFF4C6073)
    val Outline = Color(0xFF70857C)
    val Success = Primary
    val Warning = Color(0xFF765900)
    val Error = Color(0xFFBA1A1A)
}

object Space {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Normal = 16.dp
    val Large = 24.dp
    val Extra = 32.dp
}

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)
private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Poppins, fontWeight = weight, fontSize = size.sp, lineHeight = height.sp)

private val AppTypography = Typography(
    displayLarge = type(48, 56, FontWeight.Bold), displayMedium = type(40, 48, FontWeight.Bold), displaySmall = type(36, 44, FontWeight.Bold),
    headlineLarge = type(30, 38, FontWeight.SemiBold), headlineMedium = type(26, 34, FontWeight.SemiBold), headlineSmall = type(22, 30, FontWeight.SemiBold),
    titleLarge = type(20, 28, FontWeight.SemiBold), titleMedium = type(16, 24, FontWeight.SemiBold), titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24), bodyMedium = type(14, 22), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold), labelMedium = type(12, 18, FontWeight.Medium), labelSmall = type(11, 16, FontWeight.Medium)
)

@Composable
fun StartingSevenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brand.Primary, onPrimary = Color.White, primaryContainer = Brand.Mint, onPrimaryContainer = Brand.Navy,
            secondary = Brand.AccentBlue, onSecondary = Color.White, secondaryContainer = Brand.Sky, onSecondaryContainer = Brand.Navy,
            tertiary = Brand.Warning, tertiaryContainer = Brand.Highlight, onTertiaryContainer = Brand.Navy,
            background = Brand.Background, onBackground = Brand.Navy, surface = Color.White, onSurface = Brand.Navy,
            surfaceVariant = Brand.Mint, onSurfaceVariant = Brand.TextSecondary, outline = Brand.Outline,
            error = Brand.Error, surfaceContainer = Brand.Background, surfaceContainerHigh = Brand.Sky,
            surfaceContainerHighest = Brand.Mint, surfaceContainerLow = Color.White, surfaceContainerLowest = Color.White,
            surfaceTint = Brand.Primary
        ),
        typography = AppTypography,
        shapes = Shapes(RoundedCornerShape(8.dp), RoundedCornerShape(12.dp), RoundedCornerShape(16.dp), RoundedCornerShape(24.dp), RoundedCornerShape(28.dp)),
        content = content
    )
}
