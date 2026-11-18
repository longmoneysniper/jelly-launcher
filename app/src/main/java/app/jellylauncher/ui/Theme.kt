package app.jellylauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF00A4DC)
val Bg = Color(0xFF0B0F17)
val CardBg = Color(0xFF1B2332)

@Composable
fun JellyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            background = Bg,
            surface = CardBg,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}
