package com.rifsxd.ksunext.ui.theme

import android.os.Build
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_DARK,
    tertiary = SECONDARY_DARK
)

private val LightColorScheme = lightColorScheme(
    primary = PRIMARY,
    secondary = PRIMARY_LIGHT,
    tertiary = SECONDARY_LIGHT
)

fun Color.blend(other: Color, ratio: Float): Color {
    val inverse = 1f - ratio
    return Color(
        red = red * inverse + other.red * ratio,
        green = green * inverse + other.green * ratio,
        blue = blue * inverse + other.blue * ratio,
        alpha = alpha
    )
}

@Composable
fun KernelSUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    isCustomBackgroundEnabled: Boolean = false,
    backgroundTransparency: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        amoledMode && darkTheme && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = dynamicDarkColorScheme(context)
            val baseSurfaceVariant = dynamicScheme.surfaceVariant.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainer = dynamicScheme.surfaceContainer.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerLow = dynamicScheme.surfaceContainerLow.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerLowest = dynamicScheme.surfaceContainerLowest.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerHigh = dynamicScheme.surfaceContainerHigh.blend(AMOLED_BLACK, 0.6f)
            val baseSurfaceContainerHighest = dynamicScheme.surfaceContainerHighest.blend(AMOLED_BLACK, 0.6f)
            val basePrimaryContainer = dynamicScheme.primaryContainer.blend(AMOLED_BLACK, 0.6f)
            val baseSecondaryContainer = dynamicScheme.secondaryContainer.blend(AMOLED_BLACK, 0.6f)
            val baseTertiaryContainer = dynamicScheme.tertiaryContainer.blend(AMOLED_BLACK, 0.6f)
            
            dynamicScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surfaceVariant = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
                surfaceContainer = if (isCustomBackgroundEnabled) baseSurfaceContainer.copy(alpha = backgroundTransparency) else baseSurfaceContainer,
                surfaceContainerLow = if (isCustomBackgroundEnabled) baseSurfaceContainerLow.copy(alpha = backgroundTransparency) else baseSurfaceContainerLow,
                surfaceContainerLowest = if (isCustomBackgroundEnabled) baseSurfaceContainerLowest.copy(alpha = backgroundTransparency) else baseSurfaceContainerLowest,
                surfaceContainerHigh = if (isCustomBackgroundEnabled) baseSurfaceContainerHigh.copy(alpha = backgroundTransparency) else baseSurfaceContainerHigh,
                surfaceContainerHighest = if (isCustomBackgroundEnabled) baseSurfaceContainerHighest.copy(alpha = backgroundTransparency) else baseSurfaceContainerHighest,
                primaryContainer = if (isCustomBackgroundEnabled) basePrimaryContainer.copy(alpha = backgroundTransparency) else basePrimaryContainer,
                secondaryContainer = if (isCustomBackgroundEnabled) baseSecondaryContainer.copy(alpha = backgroundTransparency) else baseSecondaryContainer,
                tertiaryContainer = if (isCustomBackgroundEnabled) baseTertiaryContainer.copy(alpha = backgroundTransparency) else baseTertiaryContainer
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isCustomBackgroundEnabled) {
                baseScheme.copy(
                    background = Color.Transparent,
                    surface = Color.Transparent,
                    surfaceVariant = baseScheme.surfaceVariant.copy(alpha = backgroundTransparency),
                    surfaceContainer = baseScheme.surfaceContainer.copy(alpha = backgroundTransparency),
                    surfaceContainerLow = baseScheme.surfaceContainerLow.copy(alpha = backgroundTransparency),
                    surfaceContainerLowest = baseScheme.surfaceContainerLowest.copy(alpha = backgroundTransparency),
                    surfaceContainerHigh = baseScheme.surfaceContainerHigh.copy(alpha = backgroundTransparency),
                    surfaceContainerHighest = baseScheme.surfaceContainerHighest.copy(alpha = backgroundTransparency),
                    primaryContainer = baseScheme.primaryContainer.copy(alpha = backgroundTransparency),
                    secondaryContainer = baseScheme.secondaryContainer.copy(alpha = backgroundTransparency),
                    tertiaryContainer = baseScheme.tertiaryContainer.copy(alpha = backgroundTransparency)
                )
            } else {
                baseScheme
            }
        }
        amoledMode && darkTheme -> {
            val baseSurfaceVariant = DARK_GREY.blend(AMOLED_BLACK, 0.8f)
            DarkColorScheme.copy(
                background = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surface = if (isCustomBackgroundEnabled) Color.Transparent else AMOLED_BLACK,
                surfaceVariant = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
                surfaceContainer = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
                surfaceContainerLow = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
                surfaceContainerLowest = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
                surfaceContainerHigh = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
                surfaceContainerHighest = if (isCustomBackgroundEnabled) baseSurfaceVariant.copy(alpha = backgroundTransparency) else baseSurfaceVariant,
            )
        }
        darkTheme -> {
            if (isCustomBackgroundEnabled) {
                DarkColorScheme.copy(
                    background = Color.Transparent,
                    surface = Color.Transparent,
                    surfaceVariant = DarkColorScheme.surfaceVariant.copy(alpha = backgroundTransparency),
                    surfaceContainer = DarkColorScheme.surfaceContainer.copy(alpha = backgroundTransparency),
                    surfaceContainerLow = DarkColorScheme.surfaceContainerLow.copy(alpha = backgroundTransparency),
                    surfaceContainerLowest = DarkColorScheme.surfaceContainerLowest.copy(alpha = backgroundTransparency),
                    surfaceContainerHigh = DarkColorScheme.surfaceContainerHigh.copy(alpha = backgroundTransparency),
                    surfaceContainerHighest = DarkColorScheme.surfaceContainerHighest.copy(alpha = backgroundTransparency),
                    primaryContainer = DarkColorScheme.primaryContainer.copy(alpha = backgroundTransparency),
                    secondaryContainer = DarkColorScheme.secondaryContainer.copy(alpha = backgroundTransparency),
                    tertiaryContainer = DarkColorScheme.tertiaryContainer.copy(alpha = backgroundTransparency)
                )
            } else {
                DarkColorScheme
            }
        }
        else -> {
            if (isCustomBackgroundEnabled) {
                LightColorScheme.copy(
                    background = Color.Transparent,
                    surface = Color.Transparent,
                    surfaceVariant = LightColorScheme.surfaceVariant.copy(alpha = backgroundTransparency),
                    surfaceContainer = LightColorScheme.surfaceContainer.copy(alpha = backgroundTransparency),
                    surfaceContainerLow = LightColorScheme.surfaceContainerLow.copy(alpha = backgroundTransparency),
                    surfaceContainerLowest = LightColorScheme.surfaceContainerLowest.copy(alpha = backgroundTransparency),
                    surfaceContainerHigh = LightColorScheme.surfaceContainerHigh.copy(alpha = backgroundTransparency),
                    surfaceContainerHighest = LightColorScheme.surfaceContainerHighest.copy(alpha = backgroundTransparency),
                    primaryContainer = LightColorScheme.primaryContainer.copy(alpha = backgroundTransparency),
                    secondaryContainer = LightColorScheme.secondaryContainer.copy(alpha = backgroundTransparency),
                    tertiaryContainer = LightColorScheme.tertiaryContainer.copy(alpha = backgroundTransparency)
                )
            } else {
                LightColorScheme
            }
        }
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}