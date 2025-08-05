package com.rifsxd.ksunext.ui.component

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.rifsxd.ksunext.ui.util.ImageCropUtils
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedImageCropDialog(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    
    // Transformation states
    var scaleX by remember { mutableFloatStateOf(prefs.getFloat("background_scale_x", 1.0f)) }
    var scaleY by remember { mutableFloatStateOf(prefs.getFloat("background_scale_y", 1.0f)) }
    var offsetX by remember { mutableFloatStateOf(prefs.getFloat("background_offset_x", 0.0f)) }
    var offsetY by remember { mutableFloatStateOf(prefs.getFloat("background_offset_y", 0.0f)) }
    var rotation by remember { mutableFloatStateOf(prefs.getFloat("background_rotation", 0.0f)) }
    
    // UI states
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var selectedTool by remember { mutableStateOf("transform") } // transform, position, zoom, rotate
    
    val painter = rememberAsyncImagePainter(imageUri)
    val (minScale, maxScale) = ImageCropUtils.getScaleLimits()
    val (minTranslation, maxTranslation) = ImageCropUtils.getTranslationLimits()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image preview with transformations
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTool) {
                        when (selectedTool) {
                            "transform" -> {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    scaleX = (scaleX * zoom).coerceIn(minScale, maxScale)
                                    scaleY = (scaleY * zoom).coerceIn(minScale, maxScale)
                                    offsetX = (offsetX + pan.x).coerceIn(minTranslation, maxTranslation)
                                    offsetY = (offsetY + pan.y).coerceIn(minTranslation, maxTranslation)
                                    rotation = (rotation + rotationChange) % 360f
                                }
                            }
                            "position" -> {
                                detectDragGestures { _, dragAmount ->
                                    offsetX = (offsetX + dragAmount.x).coerceIn(minTranslation, maxTranslation)
                                    offsetY = (offsetY + dragAmount.y).coerceIn(minTranslation, maxTranslation)
                                }
                            }
                            "zoom" -> {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scaleX = (scaleX * zoom).coerceIn(minScale, maxScale)
                                    scaleY = (scaleY * zoom).coerceIn(minScale, maxScale)
                                }
                            }
                            "rotate" -> {
                                detectTransformGestures { _, _, _, rotationChange ->
                                    rotation = (rotation + rotationChange) % 360f
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Background Image Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scaleX,
                            scaleY = scaleY,
                            translationX = offsetX,
                            translationY = offsetY,
                            rotationZ = rotation
                        ),
                    contentScale = ContentScale.Fit
                )
                
                // Grid overlay for better positioning
                if (showControls && !isFullscreen) {
                    GridOverlay()
                }
            }
            
            // Top toolbar
            if (showControls) {
                TopAppBar(
                    title = { 
                        Text(
                            "Advanced Image Editor",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showControls = !showControls }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Toggle Controls", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                )
            }
            
            // Bottom controls
            if (showControls && !isFullscreen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .padding(16.dp)
                ) {
                    // Tool selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ToolButton("transform", "Transform", Icons.Default.Transform, selectedTool) { selectedTool = it }
                        ToolButton("position", "Position", Icons.Default.PanTool, selectedTool) { selectedTool = it }
                        ToolButton("zoom", "Zoom", Icons.Default.ZoomIn, selectedTool) { selectedTool = it }
                        ToolButton("rotate", "Rotate", Icons.Default.RotateRight, selectedTool) { selectedTool = it }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Transformation controls
                    when (selectedTool) {
                        "transform", "zoom" -> {
                            TransformationSliders(
                                scaleX = scaleX,
                                scaleY = scaleY,
                                onScaleXChange = { scaleX = it.coerceIn(minScale, maxScale) },
                                onScaleYChange = { scaleY = it.coerceIn(minScale, maxScale) },
                                minScale = minScale,
                                maxScale = maxScale
                            )
                        }
                        "position" -> {
                            PositionSliders(
                                offsetX = offsetX,
                                offsetY = offsetY,
                                onOffsetXChange = { offsetX = it.coerceIn(minTranslation, maxTranslation) },
                                onOffsetYChange = { offsetY = it.coerceIn(minTranslation, maxTranslation) },
                                minTranslation = minTranslation,
                                maxTranslation = maxTranslation
                            )
                        }
                        "rotate" -> {
                            RotationSlider(
                                rotation = rotation,
                                onRotationChange = { rotation = it % 360f }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Reset to defaults
                                scaleX = 1.0f
                                scaleY = 1.0f
                                offsetX = 0.0f
                                offsetY = 0.0f
                                rotation = 0.0f
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }
                        
                        Button(
                            onClick = {
                                // Save transformations
                                ImageCropUtils.saveConstrainedScale(prefs, "background_scale_x", scaleX)
                                ImageCropUtils.saveConstrainedScale(prefs, "background_scale_y", scaleY)
                                ImageCropUtils.saveConstrainedTranslation(prefs, "background_offset_x", offsetX)
                                ImageCropUtils.saveConstrainedTranslation(prefs, "background_offset_y", offsetY)
                                ImageCropUtils.saveConstrainedRotation(prefs, "background_rotation", rotation)
                                onSave()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                }
            }
            
            // Fullscreen toggle hint
            if (isFullscreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        "Tap to show controls",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    tool: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedTool: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selectedTool == tool
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { onSelect(tool) },
            modifier = Modifier
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color.Gray
            )
        }
        Text(
            label,
            color = if (isSelected) Color.White else Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TransformationSliders(
    scaleX: Float,
    scaleY: Float,
    onScaleXChange: (Float) -> Unit,
    onScaleYChange: (Float) -> Unit,
    minScale: Float,
    maxScale: Float
) {
    Column {
        Text("Scale X: ${String.format("%.2f", scaleX)}", color = Color.White)
        Slider(
            value = scaleX,
            onValueChange = onScaleXChange,
            valueRange = minScale..maxScale,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Scale Y: ${String.format("%.2f", scaleY)}", color = Color.White)
        Slider(
            value = scaleY,
            onValueChange = onScaleYChange,
            valueRange = minScale..maxScale,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun PositionSliders(
    offsetX: Float,
    offsetY: Float,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    minTranslation: Float,
    maxTranslation: Float
) {
    Column {
        Text("Position X: ${String.format("%.0f", offsetX)}", color = Color.White)
        Slider(
            value = offsetX,
            onValueChange = onOffsetXChange,
            valueRange = minTranslation..maxTranslation,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Position Y: ${String.format("%.0f", offsetY)}", color = Color.White)
        Slider(
            value = offsetY,
            onValueChange = onOffsetYChange,
            valueRange = minTranslation..maxTranslation,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun RotationSlider(
    rotation: Float,
    onRotationChange: (Float) -> Unit
) {
    Column {
        Text("Rotation: ${String.format("%.0f", rotation)}°", color = Color.White)
        Slider(
            value = rotation,
            onValueChange = onRotationChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun GridOverlay() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.3f)
        
        // Draw rule of thirds grid
        val width = size.width
        val height = size.height
        
        // Vertical lines
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 3, 0f),
            end = androidx.compose.ui.geometry.Offset(width / 3, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(2 * width / 3, 0f),
            end = androidx.compose.ui.geometry.Offset(2 * width / 3, height),
            strokeWidth = strokeWidth
        )
        
        // Horizontal lines
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, height / 3),
            end = androidx.compose.ui.geometry.Offset(width, height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 2 * height / 3),
            end = androidx.compose.ui.geometry.Offset(width, 2 * height / 3),
            strokeWidth = strokeWidth
        )
    }
}