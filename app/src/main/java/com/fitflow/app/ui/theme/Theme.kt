package com.fitflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/* 与网页版 FitFlow 一致的深色运动配色 */
val Cbg = Color(0xFF0B0D12)
val Csurface = Color(0xFF14171F)
val Csurface2 = Color(0xFF1B1F2A)
val Csurface3 = Color(0xFF232834)
val Cline = Color(0xFF262B36)
val Ctext = Color(0xFFE9ECF5)
val Cmuted = Color(0xFF8B93A7)
val Caccent = Color(0xFFC6FF3D)
val Caccent2 = Color(0xFF35E0D0)
val Cwarn = Color(0xFFFF9F45)
val Cdanger = Color(0xFFFF5470)

private val Colors = darkColorScheme(
    primary = Caccent,
    onPrimary = Color(0xFF10130A),
    secondary = Caccent2,
    background = Cbg,
    surface = Csurface,
    surfaceVariant = Csurface2,
    onSurface = Ctext,
    onSurfaceVariant = Cmuted,
    outline = Cline,
    error = Cdanger,
    onBackground = Ctext
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun FitFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, shapes = AppShapes, content = content)
}
