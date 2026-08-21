package dev.shorthouse.coinwatch.ui.preview

import androidx.compose.runtime.Composable
import dev.shorthouse.coinwatch.ui.theme.AppTheme

class AppPreviewWrapper {
    @Composable
    fun Wrap(content: @Composable () -> Unit) {
        AppTheme(content = content)
    }
}
