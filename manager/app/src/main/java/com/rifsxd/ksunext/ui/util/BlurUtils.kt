package com.rifsxd.ksunext.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.rifsxd.ksunext.ui.theme.LocalUIBlur

/**
 * UI blur effect that works on the same layer as UI transparency.
 * This applies blur to UI components when the UI blur setting is enabled,
 * creating a frosted glass effect that complements the transparency system.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    val uiBlur = LocalUIBlur.current
    return if (uiBlur > 0f) {
        // Apply blur with intensity matching the UI blur setting
        // This creates a frosted glass effect on UI components
        this.blur(radius = (uiBlur * 0.5f).dp)
    } else {
        this
    }
}

/**
 * Applies custom blur effect with specified radius
 */
fun Modifier.applyBlur(blurRadius: Float): Modifier {
    return if (blurRadius > 0f) {
        this.blur(blurRadius.dp)
    } else {
        this
    }
}