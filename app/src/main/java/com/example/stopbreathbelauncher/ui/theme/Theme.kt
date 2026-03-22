package com.example.stopbreathbelauncher.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.stopbreathbelauncher.R

// ── Palette ───────────────────────────────────────────────────────────────────

object SbbColors {
    val Background      = Color(0xFF0A0A0A)
    val Surface         = Color(0xFF0F0F0F)
    val SurfaceVariant  = Color(0xFF111111)
    val Border          = Color(0xFF1A1A1A)
    val BorderStrong    = Color(0xFF2A2A2A)

    val TextPrimary     = Color(0xFFCCCCCC)
    val TextSecondary   = Color(0xFF555555)
    val TextMuted       = Color(0xFF555555)
    val TextDim         = Color(0xFF555555)

    val PlantGreen      = Color(0xFF4A7C3F)
    val PlantGreenLight = Color(0xFF5AAD4E)
    val PlantGreenDark  = Color(0xFF1E3A1A)
    val PlantBrown      = Color(0xFF7C5C3F)

    val WatchRed        = Color(0xFFBB4040)
    val WatchRedBg      = Color(0xFF1A0A0A)
    val WatchRedBorder  = Color(0xFF4A1A1A)

    val GoalGreen       = Color(0xFF1E5A1E)
    val GoalGreenLight  = Color(0xFF3A6B3A)
    val GoalGreenBg     = Color(0xFF0A150A)
    val GoalGreenBorder = Color(0xFF1A3A1A)

    val StressedYellow  = Color(0xFF6B6B2A)
    val WiltingBrown    = Color(0xFF7C4A1A)
    val DyingDark       = Color(0xFF4A2A0A)
    val DeadDarkest     = Color(0xFF2A1A0A)


    val WatchOrange   = Color(0xFFFF8800)
    val WatchYellow   = Color(0xFFFFCC00)
    val GoalGreenMid  = Color(0xFF4CAF50)
    val NeutralBlue   = Color(0xFF90CAF9)
}

// ── Color scheme ──────────────────────────────────────────────────────────────

val SbbColorScheme = darkColorScheme(
    background       = SbbColors.Background,
    surface          = SbbColors.Surface,
    surfaceVariant   = SbbColors.SurfaceVariant,
    onBackground     = SbbColors.TextPrimary,
    onSurface        = SbbColors.TextPrimary,
    onSurfaceVariant = SbbColors.TextSecondary,
    primary          = SbbColors.PlantGreen,
    onPrimary        = SbbColors.Background,
    error            = SbbColors.WatchRed,
    onError          = SbbColors.Background,
    outline          = SbbColors.Border,
    outlineVariant   = SbbColors.BorderStrong,
)

// ── Font ──────────────────────────────────────────────────────────────────────

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

val Vt323 = FontFamily(
    Font(googleFont = GoogleFont("VT323"), fontProvider = fontProvider)
)

// ── Typography ────────────────────────────────────────────────────────────────

val SbbTypography = Typography(
    displayLarge = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 64.sp,
        lineHeight    = 64.sp,
        letterSpacing = 3.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 40.sp,
        lineHeight    = 40.sp,
        letterSpacing = 3.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 32.sp,
        lineHeight    = 32.sp,
        letterSpacing = 3.sp,
    ),
    titleLarge = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 28.sp,
        lineHeight    = 28.sp,
        letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 24.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 20.sp,
        lineHeight    = 20.sp,
        letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = Vt323,
        fontWeight    = FontWeight.Normal,
        fontSize      = 18.sp,
        lineHeight    = 18.sp,
        letterSpacing = 2.sp,
    ),
)

// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
fun StopBreathBeLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SbbColorScheme,
        typography  = SbbTypography,
        content     = content,
    )
}

// ── Root wrapper — safe drawing insets, no extra padding ─────────────────────

@Composable
fun SbbScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        content()
    }
}
