package com.readyplayerai.cassetto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StubColorScheme = lightColorScheme()

@Composable
fun CassettoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StubColorScheme, content = content)
}
