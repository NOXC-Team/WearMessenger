package top.noxc.wmessenger.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WmTheme {
    val background: Color
        @Composable get() = MaterialTheme.colors.background

    val surface: Color
        @Composable get() = MaterialTheme.colors.surface

    val onBackground: Color
        @Composable get() = MaterialTheme.colors.onBackground

    val onSurface: Color
        @Composable get() = MaterialTheme.colors.onSurface

    val primary: Color
        @Composable get() = MaterialTheme.colors.primary

    val divider: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFFE0E0E0) else Color(0xFF222222)

    val dividerStrong: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFFBDBDBD) else Color(0xFF333333)

    val textSecondary: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFF666666) else Color.LightGray

    val textHint: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFF999999) else Color.Gray

    val bubbleOutgoing: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFFDCF8C6) else Color(0xFF1A5276)

    val bubbleIncoming: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color.White else Color(0xFF2A2A2A)

    val bubbleText: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color.Black else Color.White

    val dialogBackground: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFFF5F5F5) else Color(0xFF1A2A3A)

    val pinKeyBackground: Color
        @Composable get() = if (MaterialTheme.colors.isLight) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)

    val errorColor: Color
        @Composable get() = Color(0xFFFF5252)

    val warningColor: Color
        @Composable get() = Color(0xFFFF9800)

    val successColor: Color
        @Composable get() = Color(0xFF81C784)

    val isLight: Boolean
        @Composable get() = MaterialTheme.colors.isLight
}
