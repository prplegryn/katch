package io.github.prplegryn.katch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
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
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceMuted: Color,
    val input: Color,
    val nav: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val selected: Color,
    val scrim: Color,
)

@Immutable
data class AkType(
    val appTitle: TextStyle,
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
    val page: Dp = 18.dp,
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
    background = Color(0xFFF6F7F8),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF0F2F4),
    input = Color(0xFFFAFBFC),
    nav = Color(0xFFFDFDFE),
    border = Color(0xFFD9DEE5),
    divider = Color(0xFFE7EAEE),
    textPrimary = Color(0xFF171A1F),
    textSecondary = Color(0xFF4F5966),
    textMuted = Color(0xFF7C8694),
    accent = Color(0xFF1F6FEB),
    accentSoft = Color(0xFFE8F1FF),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF16824B),
    successSoft = Color(0xFFE7F5ED),
    warning = Color(0xFF9D6500),
    warningSoft = Color(0xFFFFF5D8),
    danger = Color(0xFFC5392F),
    dangerSoft = Color(0xFFFFEBE8),
    selected = Color(0xFFEAF2FF),
    scrim = Color(0x99090C10),
)

private val DarkColors = AkColors(
    background = Color(0xFF111418),
    surface = Color(0xFF171B20),
    surfaceRaised = Color(0xFF1E242B),
    surfaceMuted = Color(0xFF222932),
    input = Color(0xFF12171D),
    nav = Color(0xFF171B20),
    border = Color(0xFF313A45),
    divider = Color(0xFF252D36),
    textPrimary = Color(0xFFF4F7FA),
    textSecondary = Color(0xFFC2CAD4),
    textMuted = Color(0xFF8994A2),
    accent = Color(0xFF72A7FF),
    accentSoft = Color(0xFF173153),
    onAccent = Color(0xFF07111F),
    success = Color(0xFF65D18C),
    successSoft = Color(0xFF143422),
    warning = Color(0xFFEBC05B),
    warningSoft = Color(0xFF3A2D11),
    danger = Color(0xFFFF8A7E),
    dangerSoft = Color(0xFF3F1D1B),
    selected = Color(0xFF162D4B),
    scrim = Color(0xCC05070A),
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
        background = animateColorAsState(target.background, label = "background").value,
        surface = animateColorAsState(target.surface, label = "surface").value,
        surfaceRaised = animateColorAsState(target.surfaceRaised, label = "surfaceRaised").value,
        nav = animateColorAsState(target.nav, label = "nav").value,
        textPrimary = animateColorAsState(target.textPrimary, label = "textPrimary").value,
        textSecondary = animateColorAsState(target.textSecondary, label = "textSecondary").value,
        accent = animateColorAsState(target.accent, label = "accent").value,
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
        appTitle = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp,
        ),
        title = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        section = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.sp,
        ),
        body = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp,
        ),
        bodyStrong = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp,
        ),
        label = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
        ),
        tiny = TextStyle(
            fontFamily = base,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.sp,
        ),
        mono = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
        ),
    )
}
