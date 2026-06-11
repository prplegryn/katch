package io.github.prplegryn.katch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.prplegryn.katch.data.ThemeMode

@Immutable
data class AkColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val blobOne: Color,
    val blobTwo: Color,
    val blobThree: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceSoft: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
) {
    val backgroundBrush: Brush
        get() = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))

    val accentBrush: Brush
        get() = Brush.linearGradient(listOf(accent, accent2, accent3))
}

@Immutable
data class AkType(
    val hero: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val tiny: TextStyle,
    val mono: TextStyle,
)

@Immutable
data class AkSpacing(
    val page: Dp = 20.dp,
    val pageLarge: Dp = 28.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

object AkTheme {
    val colors: AkColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAkColors.current

    val type: AkType
        @Composable
        @ReadOnlyComposable
        get() = LocalAkType.current

    val spacing: AkSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAkSpacing.current
}

private val LightColors = AkColors(
    backgroundTop = Color(0xFFFFF7F4),
    backgroundBottom = Color(0xFFF4F0FF),
    blobOne = Color(0xFFFF4F72).copy(alpha = 0.34f),
    blobTwo = Color(0xFF7C4DFF).copy(alpha = 0.28f),
    blobThree = Color(0xFFFFB84D).copy(alpha = 0.18f),
    surface = Color.White.copy(alpha = 0.72f),
    surfaceStrong = Color.White.copy(alpha = 0.92f),
    surfaceSoft = Color.White.copy(alpha = 0.52f),
    border = Color(0xFFFFD5DD).copy(alpha = 0.82f),
    textPrimary = Color(0xFF21161E),
    textSecondary = Color(0xFF695B66),
    textMuted = Color(0xFF91838D),
    accent = Color(0xFFFF3D65),
    accent2 = Color(0xFF7C4DFF),
    accent3 = Color(0xFFFFB84D),
    success = Color(0xFF138A55),
    warning = Color(0xFFB56C00),
    danger = Color(0xFFD7263D),
    scrim = Color(0x990C0812),
)

private val DarkColors = AkColors(
    backgroundTop = Color(0xFF130A1F),
    backgroundBottom = Color(0xFF070A14),
    blobOne = Color(0xFFFF3D65).copy(alpha = 0.30f),
    blobTwo = Color(0xFF7C4DFF).copy(alpha = 0.32f),
    blobThree = Color(0xFFFFB84D).copy(alpha = 0.12f),
    surface = Color(0xFF201528).copy(alpha = 0.76f),
    surfaceStrong = Color(0xFF2A1D34).copy(alpha = 0.94f),
    surfaceSoft = Color(0xFF2A1D34).copy(alpha = 0.54f),
    border = Color(0xFF5D4963).copy(alpha = 0.78f),
    textPrimary = Color(0xFFFFF8FA),
    textSecondary = Color(0xFFD6C6D1),
    textMuted = Color(0xFFA997A5),
    accent = Color(0xFFFF5578),
    accent2 = Color(0xFFA182FF),
    accent3 = Color(0xFFFFC46A),
    success = Color(0xFF5BE0A4),
    warning = Color(0xFFFFC46A),
    danger = Color(0xFFFF6F84),
    scrim = Color(0xCC06030A),
)

private val LocalAkColors = compositionLocalOf { DarkColors }
private val LocalAkType = compositionLocalOf { makeType() }
private val LocalAkSpacing = compositionLocalOf { AkSpacing() }

@Composable
fun AkatchaTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val target = if (dark) DarkColors else LightColors
    val colors = target.copy(
        backgroundTop = animateColorAsState(target.backgroundTop, label = "bgTop").value,
        backgroundBottom = animateColorAsState(target.backgroundBottom, label = "bgBottom").value,
        surface = animateColorAsState(target.surface, label = "surface").value,
        surfaceStrong = animateColorAsState(target.surfaceStrong, label = "surfaceStrong").value,
        textPrimary = animateColorAsState(target.textPrimary, label = "textPrimary").value,
        textSecondary = animateColorAsState(target.textSecondary, label = "textSecondary").value,
    )
    CompositionLocalProvider(
        LocalAkColors provides colors,
        LocalAkType provides makeType(),
        LocalAkSpacing provides AkSpacing(),
        content = content,
    )
}

private fun makeType(): AkType {
    val base = FontFamily.SansSerif
    return AkType(
        hero = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 31.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
        ),
        title = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp,
        ),
        section = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
        body = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        bodyStrong = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        label = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp,
        ),
        tiny = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.2.sp,
        ),
        mono = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
    )
}
