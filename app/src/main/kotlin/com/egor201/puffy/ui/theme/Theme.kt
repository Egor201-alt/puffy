package com.egor201.puffy.ui.theme

// Puffy palette — near-black base, teal/cyan marine accent, INCY-inspired
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BgBase = Color(0xFF07090D)
val BgElevated = Color(0xFF10141C)
val CardSurface = Color(0xFF12161F)
val CardBorder = Color(0xFF1E2530)

val Cyan = Color(0xFF33D6E0)
val CyanSoft = Color(0xFF8FEFF4)
val CyanDim = Color(0xFF1C7C86)

val TextPrimary = Color(0xFFF3F8FA)
val TextSecondary = Color(0xFF8994A3)
val TextMuted = Color(0xFF56606E)

val StatusGood = Color(0xFF3DDC84)
val StatusWarn = Color(0xFFF5C542)
val StatusBad = Color(0xFFF2495C)
val StatusNone = Color(0xFF56606E)

private val PuffyColors = darkColorScheme(
    primary = Cyan,
    secondary = CyanSoft,
    background = BgBase,
    surface = CardSurface,
    error = StatusBad,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

val OceanBackground = Brush.verticalGradient(listOf(BgBase, BgElevated, BgBase))

fun powerRingGradient(active: Boolean) = Brush.radialGradient(
    if (active) listOf(CyanSoft.copy(alpha = 0.35f), Cyan.copy(alpha = 0.12f), Color.Transparent)
    else listOf(CardBorder.copy(alpha = 0.4f), Color.Transparent)
)

@Composable
fun PuffyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PuffyColors, content = content)
}

@Composable
fun OceanBackdrop(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(OceanBackground)) {
        content()
    }
}

fun pingColor(ms: Int?): Color = when {
    ms == null -> StatusNone
    ms < 150 -> StatusGood
    ms < 400 -> StatusWarn
    else -> StatusBad
}
