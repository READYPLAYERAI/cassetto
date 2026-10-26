package com.readyplayerai.cassetto.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * The one theme for the whole app.
 *
 * Dynamic (Material You) colour is used on Android 12+ so the app inherits the
 * user's wallpaper palette; the Cassetto fallback palette covers API 26-30 and
 * any caller that opts out. Motion uses the Expressive scheme, which is what
 * makes transitions spring rather than ease linearly.
 */
@Composable
fun CassettoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CassettoDarkColorScheme
        else -> CassettoLightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = CassettoShapes,
        typography = CassettoTypography,
        content = content,
    )
}
