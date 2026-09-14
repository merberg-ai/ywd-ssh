package net.kj6ywd.ywdssh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val YwdBackground = Color(0xFF05070A)
val YwdSurface = Color(0xFF0B1117)
val YwdSurfaceAlt = Color(0xFF101A22)
val YwdCyan = Color(0xFF00E5FF)
val YwdMagenta = Color(0xFFFF3DF2)
val YwdGreen = Color(0xFF53FF9A)
val YwdRed = Color(0xFFFF5263)
val YwdText = Color(0xFFD7F7FF)
val YwdMuted = Color(0xFF6C8792)

private val YwdColors = darkColorScheme(
    primary = YwdCyan,
    secondary = YwdMagenta,
    tertiary = YwdGreen,
    background = YwdBackground,
    surface = YwdSurface,
    surfaceVariant = YwdSurfaceAlt,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = YwdText,
    onSurface = YwdText,
    error = YwdRed,
)

private val YwdTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
)

@Composable
fun YwdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YwdColors,
        typography = YwdTypography,
        content = content,
    )
}
