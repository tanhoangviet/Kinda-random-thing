package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import com.example.data.model.*

@Composable
fun RobloxCanvasPreview(
    root: RobloxObject,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMoveOrResize: (String, UDim2, UDim2) -> Unit, // (id, newPos, newSize)
    screenWidth: Int,
    screenHeight: Int,
    scaleFactor: Float,
    showGrid: Boolean,
    gridSize: Int,
    isPreviewMode: Boolean
) {
    Box(
        modifier = Modifier
            .size(screenWidth.dp, screenHeight.dp)
            .background(Color(25, 25, 25))
            .border(1.dp, Color(60, 60, 65))
    ) {
        // Draw Grid if enabled
        if (showGrid && !isPreviewMode) {
            CanvasGrid(gridSize = gridSize, width = screenWidth, height = screenHeight)
        }

        // Recursively render children of the ScreenGui
        root.children.forEach { child ->
            RenderRobloxObject(
                obj = child,
                parentW = screenWidth.toFloat(),
                parentH = screenHeight.toFloat(),
                selectedId = selectedId,
                onSelect = onSelect,
                onMoveOrResize = onMoveOrResize,
                isPreviewMode = isPreviewMode,
                gridSize = gridSize,
                scaleFactor = scaleFactor
            )
        }

        // Draw Safe Area Guides (dashed lines on edges if in edit mode)
        if (!isPreviewMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(230, 126, 34, 128)) // Orange Safe Area guide
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun CanvasGrid(gridSize: Int, width: Int, height: Int) {
    // Elegant dot/line grid using compose canvas or standard overlay lines
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeColor = Color(50, 50, 50)
        val step = gridSize.toFloat()
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = strokeColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )
            y += step
        }
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = strokeColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 0.5.dp.toPx()
            )
            x += step
        }
    }
}

@Composable
fun RenderRobloxObject(
    obj: RobloxObject,
    parentW: Float,
    parentH: Float,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMoveOrResize: (String, UDim2, UDim2) -> Unit,
    isPreviewMode: Boolean,
    gridSize: Int,
    scaleFactor: Float
) {
    // Check basic structural values
    val isVisible = obj.properties["Visible"] as? Boolean ?: true
    if (!isVisible) return

    val density = LocalDensity.current.density

    // Position and size extraction (UDim2)
    val pos = obj.properties["Position"] as? UDim2 ?: UDim2(0f, 0, 0f, 0)
    val size = obj.properties["Size"] as? UDim2 ?: UDim2(0.2f, 0, 0.2f, 0)
    val anchor = obj.properties["AnchorPoint"] as? Vector2 ?: Vector2(0f, 0f)

    // Compute absolute pixel bounds
    val wPx = (size.scaleX * parentW) + size.offsetX
    val hPx = (size.scaleY * parentH) + size.offsetY

    val xPx = (pos.scaleX * parentW) + pos.offsetX - (anchor.x * wPx)
    val yPx = (pos.scaleY * parentH) + pos.offsetY - (anchor.y * hPx)

    // Layout configuration
    val isSelected = obj.id == selectedId

    // Map properties to Compose graphics styling
    val bgTrans = obj.properties["BackgroundTransparency"] as? Float ?: 0f
    val bgRaw = obj.properties["BackgroundColor3"] as? Color3 ?: Color3(163, 162, 165)
    val bgColor = Color(bgRaw.r, bgRaw.g, bgRaw.b).copy(alpha = (1.0f - bgTrans).coerceIn(0f, 1f))

    // Handle UICorner inside children
    val cornerChild = obj.children.firstOrNull { it.className == RobloxClass.UICorner }
    val cornerRadius = if (cornerChild != null) {
        val r = cornerChild.properties["CornerRadius"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
        r.offsetX.dp
    } else {
        0.dp
    }
    val shape = RoundedCornerShape(cornerRadius)

    // Border/Stroke
    val strokeChild = obj.children.firstOrNull { it.className == RobloxClass.UIStroke }
    val borderModifier = if (strokeChild != null) {
        val col = strokeChild.properties["Color"] as? Color3 ?: Color3(0, 0, 0)
        val thick = strokeChild.properties["Thickness"] as? Int ?: 1
        Modifier.border(thick.dp, Color(col.r, col.g, col.b), shape)
    } else {
        val bSize = obj.properties["BorderSizePixel"] as? Int ?: 1
        if (bSize > 0) {
            Modifier.border(bSize.dp, Color(0, 0, 0, 100), shape)
        } else {
            Modifier
        }
    }

    Box(
        modifier = Modifier
            .offset(xPx.dp, yPx.dp)
            .size(wPx.dp, hPx.dp)
            .clip(shape)
            .background(bgColor, shape)
            .then(borderModifier)
            .clickable(enabled = !isPreviewMode) {
                onSelect(obj.id)
            }
            .pointerInput(obj.id, isPreviewMode) {
                if (!isPreviewMode) {
                    var dragStartX = pos.offsetX.toFloat()
                    var dragStartY = pos.offsetY.toFloat()
                    detectDragGestures(
                        onDragStart = {
                            onSelect(obj.id)
                            dragStartX = pos.offsetX.toFloat()
                            dragStartY = pos.offsetY.toFloat()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dxDp = dragAmount.x / (density * scaleFactor)
                            val dyDp = dragAmount.y / (density * scaleFactor)
                            
                            dragStartX += dxDp
                            dragStartY += dyDp
                            
                            val finalXOffset = if (gridSize > 1) {
                                (dragStartX.roundToInt() / gridSize) * gridSize
                            } else dragStartX.roundToInt()
                            
                            val finalYOffset = if (gridSize > 1) {
                                (dragStartY.roundToInt() / gridSize) * gridSize
                            } else dragStartY.roundToInt()
                            
                            if (finalXOffset != pos.offsetX || finalYOffset != pos.offsetY) {
                                onMoveOrResize(
                                    obj.id,
                                    pos.copy(offsetX = finalXOffset, offsetY = finalYOffset),
                                    size
                                )
                            }
                        }
                    )
                }
            }
    ) {
        // Render element-specific content
        when (obj.className) {
            RobloxClass.TextLabel, RobloxClass.TextButton -> {
                val text = obj.properties["Text"] as? String ?: "Text"
                val textColorRaw = obj.properties["TextColor3"] as? Color3 ?: Color3(255, 255, 255)
                val textColor = Color(textColorRaw.r, textColorRaw.g, textColorRaw.b)
                val textSize = obj.properties["TextSize"] as? Int ?: 14
                val textScaled = obj.properties["TextScaled"] as? Boolean ?: false
                val wrapped = obj.properties["TextWrapped"] as? Boolean ?: true
                val font = obj.properties["Font"] as? String ?: "SourceSans"
                val textXAlign = obj.properties["TextXAlignment"] as? String ?: "Center"
                
                val align = when (textXAlign) {
                    "Left" -> TextAlign.Left
                    "Right" -> TextAlign.Right
                    else -> TextAlign.Center
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = if (textScaled) (hPx * 0.6f).sp else textSize.sp,
                        textAlign = align,
                        fontWeight = if (font.contains("Bold")) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (font.contains("Italic")) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = if (wrapped) 10 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            RobloxClass.ImageLabel, RobloxClass.ImageButton -> {
                val image = obj.properties["Image"] as? String ?: "rbxassetid://0"
                
                // Draw a simple Roblox placeholder visual inside the image box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0, 0, 0, 50)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏞️", fontSize = 16.sp)
                        Text(
                            text = image.replace("rbxassetid://", "Asset "),
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            RobloxClass.ScrollingFrame -> {
                // Show a fake scroll handle on the right edge
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(Color(0, 0, 0, 80))
                        .align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(Color(120, 120, 125, 180))
                    )
                }
            }
            RobloxClass.ViewportFrame -> {
                var yaw by remember { mutableStateOf(0.5f) }
                var pitch by remember { mutableStateOf(0.3f) }
                var autoRotateActive by remember { mutableStateOf(true) }
                var lastInteractionTime by remember { mutableLongStateOf(0L) }

                LaunchedEffect(autoRotateActive) {
                    if (autoRotateActive) {
                        var lastTime = System.currentTimeMillis()
                        while (autoRotateActive) {
                            val now = System.currentTimeMillis()
                            val delta = (now - lastTime) * 0.001f
                            lastTime = now
                            yaw += delta * 0.6f
                            pitch += delta * 0.3f
                            kotlinx.coroutines.delay(16)
                        }
                    }
                }

                LaunchedEffect(lastInteractionTime) {
                    if (lastInteractionTime > 0L) {
                        kotlinx.coroutines.delay(3000)
                        autoRotateActive = true
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(12, 12, 15))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    autoRotateActive = false
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    yaw += dragAmount.x * 0.01f
                                    pitch = (pitch - dragAmount.y * 0.01f).coerceIn(-1.4f, 1.4f)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val vertices = listOf(
                            Point3D(-1f, -1f, -1f),
                            Point3D(1f, -1f, -1f),
                            Point3D(1f, 1f, -1f),
                            Point3D(-1f, 1f, -1f),
                            Point3D(-1f, -1f, 1f),
                            Point3D(1f, -1f, 1f),
                            Point3D(1f, 1f, 1f),
                            Point3D(-1f, 1f, 1f)
                        )

                        val rotatedVertices = vertices.map { vertex ->
                            val cosY = kotlin.math.cos(yaw.toDouble()).toFloat()
                            val sinY = kotlin.math.sin(yaw.toDouble()).toFloat()
                            val x1 = vertex.x * cosY - vertex.z * sinY
                            val z1 = vertex.x * sinY + vertex.z * cosY

                            val cosX = kotlin.math.cos(pitch.toDouble()).toFloat()
                            val sinX = kotlin.math.sin(pitch.toDouble()).toFloat()
                            val y2 = vertex.y * cosX - z1 * sinX
                            val z2 = vertex.y * sinX + z1 * cosX

                            val distance = 3.5f
                            val scale = (distance / (distance + z2)) * (this.size.width * 0.3f)
                            val screenX = this.size.width / 2 + x1 * scale
                            val screenY = this.size.height / 2 - y2 * scale
                            
                            Pair(Offset(screenX, screenY), z2)
                        }

                        val faces = listOf(
                            listOf(0, 1, 2, 3) to Color(0, 110, 200),     // back
                            listOf(4, 5, 6, 7) to Color(0, 162, 255),     // front
                            listOf(0, 1, 5, 4) to Color(0, 70, 140),      // bottom
                            listOf(2, 3, 7, 6) to Color(100, 210, 255),   // top
                            listOf(0, 3, 7, 4) to Color(0, 90, 170),      // left
                            listOf(1, 2, 6, 5) to Color(0, 130, 230)      // right
                        )

                        val sortedFaces = faces.map { (indices, baseColor) ->
                            val avgZ = indices.map { rotatedVertices[it].second }.average().toFloat()
                            Triple(indices, baseColor, avgZ)
                        }.sortedByDescending { it.third }

                        sortedFaces.forEach { (indices, baseColor, _) ->
                            val path = Path().apply {
                                val p0 = rotatedVertices[indices[0]].first
                                moveTo(p0.x, p0.y)
                                for (i in 1 until indices.size) {
                                    val p = rotatedVertices[indices[i]].first
                                    lineTo(p.x, p.y)
                                }
                                close()
                            }
                            drawPath(path, baseColor)
                            drawPath(path, Color(220, 245, 255, 150), style = Stroke(width = 1.dp.toPx()))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color(0, 162, 255, 40), RoundedCornerShape(3.dp))
                            .border(0.5.dp, Color(0, 162, 255, 120), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "3D Viewport",
                            fontSize = 8.sp,
                            color = Color(150, 220, 255),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            else -> {}
        }

        // Apply Layout engines visually on preview if possible
        // For our mobile preview, we render the children recursively!
        val childrenToRender = obj.children.filter { 
            it.className != RobloxClass.UICorner && 
            it.className != RobloxClass.UIStroke &&
            it.className != RobloxClass.UIAspectRatioConstraint
        }

        // If it is a list or grid layout, we can stack them!
        val listLayout = obj.children.firstOrNull { it.className == RobloxClass.UIListLayout }
        if (listLayout != null && childrenToRender.isNotEmpty()) {
            val isVertical = (listLayout.properties["FillDirection"] as? String ?: "Vertical") == "Vertical"
            val spacing = (listLayout.properties["Padding"] as? UDim2)?.offsetY ?: 8
            
            if (isVertical) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    childrenToRender.forEach { child ->
                        Box(modifier = Modifier.wrapContentSize()) {
                            RenderLayoutChild(child, wPx - 8f, hPx - 8f)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    childrenToRender.forEach { child ->
                        Box(modifier = Modifier.wrapContentSize()) {
                            RenderLayoutChild(child, wPx - 8f, hPx - 8f)
                        }
                    }
                }
            }
        } else {
            // Otherwise, render free-form absolute positioned children
            childrenToRender.forEach { child ->
                RenderRobloxObject(
                    obj = child,
                    parentW = wPx,
                    parentH = hPx,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMoveOrResize = onMoveOrResize,
                    isPreviewMode = isPreviewMode,
                    gridSize = gridSize,
                    scaleFactor = scaleFactor
                )
            }
        }

        // Draw Selected Indicator Outline and simple handles
        if (isSelected && !isPreviewMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.5.dp, Color(0, 162, 255)) // Highlight Blue selection
            ) {
                // Resize Handle dots at bottom-right corner
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                        .align(Alignment.BottomEnd)
                        .pointerInput(obj.id) {
                            var resizeStartW = size.offsetX.toFloat()
                            var resizeStartH = size.offsetY.toFloat()
                            detectDragGestures(
                                onDragStart = {
                                    resizeStartW = size.offsetX.toFloat()
                                    resizeStartH = size.offsetY.toFloat()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dxDp = dragAmount.x / (density * scaleFactor)
                                    val dyDp = dragAmount.y / (density * scaleFactor)
                                    
                                    resizeStartW += dxDp
                                    resizeStartH += dyDp
                                    
                                    val finalW = if (gridSize > 1) {
                                        (resizeStartW.roundToInt() / gridSize) * gridSize
                                    } else resizeStartW.roundToInt()
                                    
                                    val finalH = if (gridSize > 1) {
                                        (resizeStartH.roundToInt() / gridSize) * gridSize
                                    } else resizeStartH.roundToInt()
                                    
                                    val minOffsetX = (10 - (size.scaleX * parentW)).toInt()
                                    val minOffsetY = (10 - (size.scaleY * parentH)).toInt()
                                    val safeW = finalW.coerceAtLeast(minOffsetX)
                                    val safeH = finalH.coerceAtLeast(minOffsetY)
                                    
                                    if (safeW != size.offsetX || safeH != size.offsetY) {
                                        onMoveOrResize(
                                            obj.id,
                                            pos,
                                            size.copy(offsetX = safeW, offsetY = safeH)
                                        )
                                    }
                                }
                            )
                        }
                )
                // Top-Left Name label overlay
                Box(
                    modifier = Modifier
                        .offset(0.dp, (-14).dp)
                        .background(Color(0, 162, 255))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${obj.name} (${wPx.toInt()}x${hPx.toInt()})",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RenderLayoutChild(obj: RobloxObject, parentW: Float, parentH: Float) {
    val bgTrans = obj.properties["BackgroundTransparency"] as? Float ?: 0f
    val bgRaw = obj.properties["BackgroundColor3"] as? Color3 ?: Color3(163, 162, 165)
    val bgColor = Color(bgRaw.r, bgRaw.g, bgRaw.b).copy(alpha = (1.0f - bgTrans).coerceIn(0f, 1f))
    
    val size = obj.properties["Size"] as? UDim2 ?: UDim2(0.8f, 0, 0.2f, 0)
    val w = ((size.scaleX * parentW) + size.offsetX).dp.coerceAtLeast(30.dp)
    val h = ((size.scaleY * parentH) + size.offsetY).dp.coerceAtLeast(15.dp)

    Box(
        modifier = Modifier
            .size(w, h)
            .background(bgColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (obj.className) {
            RobloxClass.TextLabel, RobloxClass.TextButton -> {
                Text(
                    text = obj.properties["Text"] as? String ?: obj.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> {
                Text(
                    text = obj.name,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.sp
                )
            }
        }
    }
}

data class Point3D(val x: Float, val y: Float, val z: Float)
