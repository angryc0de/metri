package com.leanlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 皮肤只分两套：黑夜 / 白天，默认黑夜。
 * 不做"跟随系统"——这是一个每天开十几次的记录工具，配色应该由用户自己定。
 */
@Composable
fun LeanLogTheme(content: @Composable () -> Unit) {
    val dark = LL.isDark
    val scheme = remember(dark) {
        if (dark) {
            darkColorScheme(
                primary = LL.Green,
                onPrimary = LL.OnAccent,
                secondary = LL.Blue,
                onSecondary = LL.OnAccent,
                tertiary = LL.Amber,
                background = LL.Bg,
                onBackground = LL.Text,
                surface = LL.Surface,
                onSurface = LL.Text,
                surfaceVariant = LL.Surface2,
                onSurfaceVariant = LL.Muted,
                outline = LL.Line,
                outlineVariant = LL.Line2,
                error = LL.Red,
                onError = LL.OnAccent,
            )
        } else {
            lightColorScheme(
                primary = LL.Green,
                onPrimary = LL.OnAccent,
                secondary = LL.Blue,
                onSecondary = LL.OnAccent,
                tertiary = LL.Amber,
                background = LL.Bg,
                onBackground = LL.Text,
                surface = LL.Surface,
                onSurface = LL.Text,
                surfaceVariant = LL.Surface2,
                onSurfaceVariant = LL.Muted,
                outline = LL.Line,
                outlineVariant = LL.Line2,
                error = LL.Red,
                onError = LL.OnAccent,
            )
        }
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
