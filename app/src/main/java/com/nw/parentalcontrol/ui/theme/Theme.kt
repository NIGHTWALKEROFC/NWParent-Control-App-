// PATH: nw-parent-app/app/src/main/java/com/nw/parentalcontrol/ui/theme/Theme.kt
package com.nw.parentalcontrol.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ParentPrimary    = Color(0xFF1A237E)
val ParentAccent     = Color(0xFF42A5F5)
val ParentSuccess    = Color(0xFF4CAF50)
val ParentWarning    = Color(0xFFFF9800)
val ParentError      = Color(0xFFF44336)
val ParentBackground = Color(0xFF0D1B2A)
val ParentSurface    = Color(0xFF1A2C3D)
val ParentCard       = Color(0xFF1E3448)
val ParentOnBackground = Color(0xFFE8EAF6)
val ParentOnSurface  = Color(0xFFB0BEC5)

private val DarkColorScheme = darkColorScheme(
    primary        = ParentAccent,
    secondary      = ParentPrimary,
    background     = ParentBackground,
    surface        = ParentSurface,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = ParentOnBackground,
    onSurface      = ParentOnSurface,
    error          = ParentError
)

@Composable
fun NWParentalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}