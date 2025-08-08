package com.rifsxd.ksunext.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.rifsxd.ksunext.ui.theme.LocalUIBlur

/**
 * UI blur effect that works on the same layer as UI transparency.
 * This applies a very subtle blur to card backgrounds and UI elements,
 * creating a light frosted glass effect that complements transparency.
 */
@Composable
fun Modifier.applyUIBlur(): Modifier {
    val uiBlur = LocalUIBlur.current
    return if (uiBlur > 0f) {
        // Apply very subtle blur - much lower intensity to avoid over-blurring
        // This creates a light frosted glass effect on card backgrounds only
        this.blur(radius = (uiBlur * 0.05f).dp)
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