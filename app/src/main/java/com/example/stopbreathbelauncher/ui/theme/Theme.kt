package com.example.stopbreathbelauncher.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.dp
import com.example.stopbreathbelauncher.R

@Composable
fun StopBreathBeLauncherTheme(content: @Composable () -> Unit) {
    // 1. Define the Provider
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    // 2. Define the Google Font
    val fontName = GoogleFont("Inter")

    val CustomFontFamily = FontFamily(
        Font(googleFont = fontName, fontProvider = provider)
    )

    // 3. Define Typography (Fixed: Added missing closing parenthesis)
    val myTypography = Typography(
        bodyLarge = TextStyle(
            fontFamily = CustomFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        )
    )

    // 4. Apply to MaterialTheme
    ThemeWrapper(typography = myTypography, content = content)
}

@Composable
fun ThemeWrapper(typography: Typography, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)) {
        MaterialTheme(
            colorScheme = lightColorScheme(),
            typography = typography,
            content = content
        )
    }
}