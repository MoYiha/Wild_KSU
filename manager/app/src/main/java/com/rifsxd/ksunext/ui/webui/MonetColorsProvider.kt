package com.rifsxd.ksunext.ui.webui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi

import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

import com.google.android.material.color.utilities.Scheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import com.rifsxd.ksunext.ui.theme.AMOLED_BLACK


/**
 * @author rifsxd
 * @date 2025/6/2.
 */
object MonetColorsProvider {
    @RequiresApi(Build.VERSION_CODES.S)
    fun getColorsCss(context: Context): String {

        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val appThemeVal = prefs.getInt("app_theme", 0)
        val amoledMode = prefs.getBoolean("enable_amoled", false)

        if (appThemeVal == 6) { // AppTheme.CUSTOM
            val customColor = prefs.getInt("theme_custom_color", 0xFF8AADF4.toInt())
            val scheme = if (isDark) Scheme.dark(customColor) else Scheme.light(customColor)
            val hct = Hct.fromInt(customColor)
            val neutralPalette = TonalPalette.fromHueAndChroma(hct.hue, 4.0)
            val monetColors = mapOf(
                "primary" to Color(scheme.primary).toArgb().toHex(),
                "onPrimary" to Color(scheme.onPrimary).toArgb().toHex(),
                "primaryContainer" to Color(scheme.primaryContainer).toArgb().toHex(),
                "onPrimaryContainer" to Color(scheme.onPrimaryContainer).toArgb().toHex(),
                "inversePrimary" to Color(scheme.inversePrimary).toArgb().toHex(),
                "secondary" to Color(scheme.secondary).toArgb().toHex(),
                "onSecondary" to Color(scheme.onSecondary).toArgb().toHex(),
                "secondaryContainer" to Color(scheme.secondaryContainer).toArgb().toHex(),
                "onSecondaryContainer" to Color(scheme.onSecondaryContainer).toArgb().toHex(),
                "tertiary" to Color(scheme.tertiary).toArgb().toHex(),
                "onTertiary" to Color(scheme.onTertiary).toArgb().toHex(),
                "tertiaryContainer" to Color(scheme.tertiaryContainer).toArgb().toHex(),
                "onTertiaryContainer" to Color(scheme.onTertiaryContainer).toArgb().toHex(),
                "background" to Color(scheme.background).toArgb().toHex(),
                "onBackground" to Color(scheme.onBackground).toArgb().toHex(),
                "surface" to Color(scheme.surface).toArgb().toHex(),
                "tonalSurface" to Color(scheme.surface).toArgb().toHex(),
                "onSurface" to Color(scheme.onSurface).toArgb().toHex(),
                "surfaceVariant" to Color(scheme.surfaceVariant).toArgb().toHex(),
                "onSurfaceVariant" to Color(scheme.onSurfaceVariant).toArgb().toHex(),
                "surfaceTint" to Color(scheme.primary).toArgb().toHex(),
                "inverseSurface" to Color(scheme.inverseSurface).toArgb().toHex(),
                "inverseOnSurface" to Color(scheme.inverseOnSurface).toArgb().toHex(),
                "error" to Color(scheme.error).toArgb().toHex(),
                "onError" to Color(scheme.onError).toArgb().toHex(),
                "errorContainer" to Color(scheme.errorContainer).toArgb().toHex(),
                "onErrorContainer" to Color(scheme.onErrorContainer).toArgb().toHex(),
                "outline" to Color(scheme.outline).toArgb().toHex(),
                "outlineVariant" to Color(scheme.outlineVariant).toArgb().toHex(),
                "scrim" to Color(scheme.scrim).toArgb().toHex(),
                "surfaceBright" to Color(neutralPalette.tone(if (isDark) 24 else 98)).toArgb().toHex(),
                "surfaceDim" to Color(neutralPalette.tone(if (isDark) 6 else 87)).toArgb().toHex(),
                "surfaceContainer" to Color(neutralPalette.tone(if (isDark) 12 else 94)).toArgb().toHex(),
                "surfaceContainerHigh" to Color(neutralPalette.tone(if (isDark) 17 else 92)).toArgb().toHex(),
                "surfaceContainerHighest" to Color(neutralPalette.tone(if (isDark) 22 else 90)).toArgb().toHex(),
                "surfaceContainerLow" to Color(neutralPalette.tone(if (isDark) 10 else 96)).toArgb().toHex(),
                "surfaceContainerLowest" to Color(neutralPalette.tone(if (isDark) 4 else 100)).toArgb().toHex(),
                "filledTonalButtonContentColor" to Color(scheme.onSecondaryContainer).toArgb().toHex(),
                "filledTonalButtonContainerColor" to Color(scheme.secondaryContainer).toArgb().toHex(),
                "filledTonalButtonDisabledContentColor" to Color(scheme.onSurfaceVariant).toArgb().toHex(),
                "filledTonalButtonDisabledContainerColor" to Color(scheme.surfaceVariant).toArgb().toHex(),
                "filledCardContentColor" to Color(scheme.onPrimaryContainer).toArgb().toHex(),
                "filledCardContainerColor" to Color(scheme.primaryContainer).toArgb().toHex(),
                "filledCardDisabledContentColor" to Color(scheme.onSurfaceVariant).toArgb().toHex(),
                "filledCardDisabledContainerColor" to Color(scheme.surfaceVariant).toArgb().toHex()
            )
            return monetColors.toCssVars()
        }

        val colorScheme = if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }

        fun blend(c1: Color, c2: Color, ratio: Float): Color {
            val inv = 1f - ratio
            return Color(
                red = c1.red * inv + c2.red * ratio,
                green = c1.green * inv + c2.green * ratio,
                blue = c1.blue * inv + c2.blue * ratio,
                alpha = c1.alpha
            )
        }

        val monetColors = if (isDark && amoledMode) {
            mapOf(
                "primary" to colorScheme.primary.toArgb().toHex(),
                "onPrimary" to colorScheme.onPrimary.toArgb().toHex(),
                "primaryContainer" to blend(colorScheme.primaryContainer, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "onPrimaryContainer" to colorScheme.onPrimaryContainer.toArgb().toHex(),
                "inversePrimary" to colorScheme.inversePrimary.toArgb().toHex(),
                "secondary" to colorScheme.secondary.toArgb().toHex(),
                "onSecondary" to colorScheme.onSecondary.toArgb().toHex(),
                "secondaryContainer" to blend(colorScheme.secondaryContainer, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "onSecondaryContainer" to colorScheme.onSecondaryContainer.toArgb().toHex(),
                "tertiary" to colorScheme.tertiary.toArgb().toHex(),
                "onTertiary" to colorScheme.onTertiary.toArgb().toHex(),
                "tertiaryContainer" to colorScheme.tertiaryContainer.toArgb().toHex(),
                "onTertiaryContainer" to blend(colorScheme.onTertiaryContainer, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "background" to AMOLED_BLACK.toArgb().toHex(),
                "onBackground" to colorScheme.onBackground.toArgb().toHex(),
                "surface" to AMOLED_BLACK.toArgb().toHex(),
                "tonalSurface" to blend(colorScheme.surfaceColorAtElevation(1.dp), AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "onSurface" to colorScheme.onSurface.toArgb().toHex(),
                "surfaceVariant" to blend(colorScheme.surfaceVariant, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "onSurfaceVariant" to colorScheme.onSurfaceVariant.toArgb().toHex(),
                "surfaceTint" to colorScheme.surfaceTint.toArgb().toHex(),
                "inverseSurface" to colorScheme.inverseSurface.toArgb().toHex(),
                "inverseOnSurface" to colorScheme.inverseOnSurface.toArgb().toHex(),
                "error" to colorScheme.error.toArgb().toHex(),
                "onError" to colorScheme.onError.toArgb().toHex(),
                "errorContainer" to colorScheme.errorContainer.toArgb().toHex(),
                "onErrorContainer" to colorScheme.onErrorContainer.toArgb().toHex(),
                "outline" to colorScheme.outline.toArgb().toHex(),
                "outlineVariant" to colorScheme.outlineVariant.toArgb().toHex(),
                "scrim" to colorScheme.scrim.toArgb().toHex(),
                "surfaceBright" to blend(colorScheme.surfaceBright, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "surfaceDim" to blend(colorScheme.surfaceDim, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "surfaceContainer" to blend(colorScheme.surfaceContainer, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "surfaceContainerHigh" to blend(colorScheme.surfaceContainerHigh, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "surfaceContainerHighest" to blend(colorScheme.surfaceContainerHighest, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "surfaceContainerLow" to blend(colorScheme.surfaceContainerLow, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "surfaceContainerLowest" to blend(colorScheme.surfaceContainerLowest, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "filledTonalButtonContentColor" to colorScheme.onPrimaryContainer.toArgb().toHex(),
                "filledTonalButtonContainerColor" to blend(colorScheme.secondaryContainer, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "filledTonalButtonDisabledContentColor" to colorScheme.onSurfaceVariant.toArgb().toHex(),
                "filledTonalButtonDisabledContainerColor" to blend(colorScheme.surfaceVariant, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "filledCardContentColor" to colorScheme.onPrimaryContainer.toArgb().toHex(),
                "filledCardContainerColor" to blend(colorScheme.primaryContainer, AMOLED_BLACK, 0.6f).toArgb().toHex(),
                "filledCardDisabledContentColor" to colorScheme.onSurfaceVariant.toArgb().toHex(),
                "filledCardDisabledContainerColor" to blend(colorScheme.surfaceVariant, AMOLED_BLACK, 0.6f).toArgb().toHex()
            )
        } else {
            mapOf(
                "primary" to colorScheme.primary.toArgb().toHex(),
                "onPrimary" to colorScheme.onPrimary.toArgb().toHex(),
                "primaryContainer" to colorScheme.primaryContainer.toArgb().toHex(),
                "onPrimaryContainer" to colorScheme.onPrimaryContainer.toArgb().toHex(),
                "inversePrimary" to colorScheme.inversePrimary.toArgb().toHex(),
                "secondary" to colorScheme.secondary.toArgb().toHex(),
                "onSecondary" to colorScheme.onSecondary.toArgb().toHex(),
                "secondaryContainer" to colorScheme.secondaryContainer.toArgb().toHex(),
                "onSecondaryContainer" to colorScheme.onSecondaryContainer.toArgb().toHex(),
                "tertiary" to colorScheme.tertiary.toArgb().toHex(),
                "onTertiary" to colorScheme.onTertiary.toArgb().toHex(),
                "tertiaryContainer" to colorScheme.tertiaryContainer.toArgb().toHex(),
                "onTertiaryContainer" to colorScheme.onTertiaryContainer.toArgb().toHex(),
                "background" to colorScheme.background.toArgb().toHex(),
                "onBackground" to colorScheme.onBackground.toArgb().toHex(),
                "surface" to colorScheme.surface.toArgb().toHex(),
                "tonalSurface" to colorScheme.surfaceColorAtElevation(1.dp).toArgb().toHex(),
                "onSurface" to colorScheme.onSurface.toArgb().toHex(),
                "surfaceVariant" to colorScheme.surfaceVariant.toArgb().toHex(),
                "onSurfaceVariant" to colorScheme.onSurfaceVariant.toArgb().toHex(),
                "surfaceTint" to colorScheme.surfaceTint.toArgb().toHex(),
                "inverseSurface" to colorScheme.inverseSurface.toArgb().toHex(),
                "inverseOnSurface" to colorScheme.inverseOnSurface.toArgb().toHex(),
                "error" to colorScheme.error.toArgb().toHex(),
                "onError" to colorScheme.onError.toArgb().toHex(),
                "errorContainer" to colorScheme.errorContainer.toArgb().toHex(),
                "onErrorContainer" to colorScheme.onErrorContainer.toArgb().toHex(),
                "outline" to colorScheme.outline.toArgb().toHex(),
                "outlineVariant" to colorScheme.outlineVariant.toArgb().toHex(),
                "scrim" to colorScheme.scrim.toArgb().toHex(),
                "surfaceBright" to colorScheme.surfaceBright.toArgb().toHex(),
                "surfaceDim" to colorScheme.surfaceDim.toArgb().toHex(),
                "surfaceContainer" to colorScheme.surfaceContainer.toArgb().toHex(),
                "surfaceContainerHigh" to colorScheme.surfaceContainerHigh.toArgb().toHex(),
                "surfaceContainerHighest" to colorScheme.surfaceContainerHighest.toArgb().toHex(),
                "surfaceContainerLow" to colorScheme.surfaceContainerLow.toArgb().toHex(),
                "surfaceContainerLowest" to colorScheme.surfaceContainerLowest.toArgb().toHex(),
                "filledTonalButtonContentColor" to colorScheme.onPrimaryContainer.toArgb().toHex(),
                "filledTonalButtonContainerColor" to colorScheme.secondaryContainer.toArgb().toHex(),
                "filledTonalButtonDisabledContentColor" to colorScheme.onSurfaceVariant.toArgb().toHex(),
                "filledTonalButtonDisabledContainerColor" to colorScheme.surfaceVariant.toArgb().toHex(),
                "filledCardContentColor" to colorScheme.onPrimaryContainer.toArgb().toHex(),
                "filledCardContainerColor" to colorScheme.primaryContainer.toArgb().toHex(),
                "filledCardDisabledContentColor" to colorScheme.onSurfaceVariant.toArgb().toHex(),
                "filledCardDisabledContainerColor" to colorScheme.surfaceVariant.toArgb().toHex()
            )
        }
        return monetColors.toCssVars()
    }

    private fun Map<String, String>.toCssVars(): String {
        return buildString {
            append(":root {\n")
            for ((k, v) in this@toCssVars) {
                append("  --$k: $v;\n")
            }
            append("}\n")
        }
    }

    private fun Int.toHex(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
}