package io.github.vlukyanets.lemonflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Fixed palette from the design canvas; dynamic color is off on purpose (research R8). */
@Composable
fun LemonFlowTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLemonFlowColors provides if (darkTheme) DarkTokens else LightTokens
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = LemonFlowTypography,
            content = content
        )
    }
}

/** Shortcut for the design tokens: `LemonFlow.colors.muted`. */
object LemonFlow {
    val colors: LemonFlowColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLemonFlowColors.current
}
