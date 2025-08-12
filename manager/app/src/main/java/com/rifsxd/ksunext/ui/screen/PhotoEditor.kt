package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import coil.compose.rememberAsyncImagePainter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.rifsxd.ksunext.ui.component.ImageTransformSettings
import com.rifsxd.ksunext.ui.util.ImageEditorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PhotoEditorScreen(
    imageUri: String,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val uri = Uri.parse(imageUri)
    
    PhotoEditor(
        imageUri = uri,
        onDismiss = {
            navigator.popBackStack()
        },
        onSave = { scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue ->
            // Clear any previous photo settings first
            ImageEditorUtils.clearImageTransformSettings(prefs)
            
            // Save image URI and reset transparency (like original AdvancedImageTransformDialog)
            prefs.edit()
                .putString("background_image_uri", imageUri)
                .putFloat("background_transparency", 0.0f) // Reset darkness so image is visible
                .apply()
            
            // Save transform settings for graphicsLayer transformations
            val transformSettings = ImageTransformSettings(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                rotation = rotation
            )
            ImageEditorUtils.saveImageTransformSettings(prefs, imageUri, transformSettings)
            
            // Save adjustment settings
            prefs.edit()
                .putFloat("image_brightness", brightness)
                .putFloat("image_contrast", contrast)
                .putFloat("image_saturation", saturation)
                .putFloat("image_hue", hue)
                .apply()
            
            navigator.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditor(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float, Float, Float, Float, Float) -> Unit // scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue
) {
    // Transform states - simple like original AdvancedImageTransformDialog
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // Image adjustment states with expanded ranges for better effects
    var brightness by remember { mutableFloatStateOf(0f) } // -200 to 200 (expanded from -100 to 100)
    var contrast by remember { mutableFloatStateOf(1f) } // 0 to 4 (expanded from 0 to 2)
    var saturation by remember { mutableFloatStateOf(1f) } // 0 to 3 (expanded from 0 to 2)
    var hue by remember { mutableFloatStateOf(0f) } // -360 to 360 (expanded from -180 to 180)
    
    // UI state
    var freeFormMode by remember { mutableStateOf(true) }
    var showAdjustments by remember { mutableStateOf(false) }
    
    // Simple image painter without custom decoders
    val painter = rememberAsyncImagePainter(
        model = imageUri,
        onError = { error ->
            android.util.Log.e("PhotoEditor", "Failed to load image: ${error.result.throwable?.message}")
        }
    )
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Create color matrix for adjustments
        val colorMatrix = remember(brightness, contrast, saturation, hue) {
            ColorMatrix().apply {
                // Apply brightness (-200 to 200 range)
                val brightnessValue = brightness / 255f
                this[4] = brightnessValue // Red offset
                this[9] = brightnessValue // Green offset
                this[14] = brightnessValue // Blue offset
                
                // Apply contrast (0 to 4 range)
                val contrastValue = contrast
                this[0] = contrastValue // Red scale
                this[6] = contrastValue // Green scale
                this[12] = contrastValue // Blue scale
                
                // Apply saturation (0 to 3 range)
                val saturationMatrix = ColorMatrix()
                saturationMatrix.setToSaturation(saturation)
                this.timesAssign(saturationMatrix)
                
                // Apply hue rotation (-360 to 360 range)
                if (hue != 0f) {
                    val hueMatrix = ColorMatrix()
                    hueMatrix.setToRotateRed(hue)
                    this.timesAssign(hueMatrix)
                }
            }
        }
        
        // Main image display with graphicsLayer transformations and color adjustments
        Image(
            painter = painter,
            contentDescription = "Photo to edit",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = rotation,
                    transformOrigin = TransformOrigin.Center
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotationChange ->
                        if (freeFormMode) {
                            // Apply transformations directly to state variables
                            val normalizedPanX = pan.x / scale
                            val normalizedPanY = pan.y / scale
                            offsetX += normalizedPanX
                            offsetY += normalizedPanY
                            rotation += rotationChange
                        }
                        // Always allow zoom
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                    }
                },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(colorMatrix)
        )
        
        // Top bar with close and save buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Adjustments toggle button
            IconButton(
                onClick = { showAdjustments = !showAdjustments },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showAdjustments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Adjustments",
                    tint = if (showAdjustments) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Save button
            IconButton(
                onClick = { onSave(scale, offsetX, offsetY, rotation, brightness, contrast, saturation, hue) },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        // Bottom controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxHeight(if (showAdjustments) 0.6f else 0.3f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Free-form mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Free Transform",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = freeFormMode,
                        onCheckedChange = { freeFormMode = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transform controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rotate Left
                    IconButton(
                        onClick = { rotation -= 90f },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateLeft,
                            contentDescription = "Rotate Left",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Reset
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            rotation = 0f
                            brightness = 0f
                            contrast = 1f
                            saturation = 1f
                            hue = 0f
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Rotate Right
                    IconButton(
                        onClick = { rotation += 90f },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate Right",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scale slider
                Column {
                    Text(
                        text = "Scale: ${String.format("%.1f", scale)}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.1f..5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Image Adjustment Controls (only show when adjustments are enabled)
                if (showAdjustments) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Image Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Brightness slider (-200 to 200, expanded range)
                    Column {
                        Text(
                            text = "Brightness: ${brightness.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = -200f..200f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Contrast slider (0 to 4, expanded range)
                    Column {
                        Text(
                            text = "Contrast: ${String.format("%.1f", contrast)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = contrast,
                            onValueChange = { contrast = it },
                            valueRange = 0f..4f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Saturation slider (0 to 3, expanded range)
                    Column {
                        Text(
                            text = "Saturation: ${String.format("%.1f", saturation)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = 0f..3f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Hue slider (-360 to 360, expanded range)
                    Column {
                        Text(
                            text = "Hue: ${hue.toInt()}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = hue,
                            onValueChange = { hue = it },
                            valueRange = -360f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}