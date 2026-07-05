@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
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
    snapToGrid: Boolean,
    gridSize: Int,
    isPreviewMode: Boolean,
    useSingleDragMode: Boolean = false,
    onToggleDragMode: () -> Unit = {}
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
                snapToGrid = snapToGrid,
                gridSize = gridSize,
                scaleFactor = scaleFactor,
                useSingleDragMode = useSingleDragMode,
                onToggleDragMode = onToggleDragMode
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
    snapToGrid: Boolean,
    gridSize: Int,
    scaleFactor: Float,
    useSingleDragMode: Boolean = false,
    onToggleDragMode: () -> Unit = {}
) {
    // Check basic structural values
    val isVisible = obj.properties["Visible"] as? Boolean ?: true
    if (!isVisible) return

    val density = LocalDensity.current.density

    // Position and size extraction (UDim2)
    val pos = obj.properties["Position"] as? UDim2 ?: UDim2(0f, 0, 0f, 0)
    val size = obj.properties["Size"] as? UDim2 ?: UDim2(0.2f, 0, 0.2f, 0)
    
    // Remember updated states to avoid capturing stale values in pointerInput
    val currentPos by rememberUpdatedState(pos)
    val currentSize by rememberUpdatedState(size)
    val currentSnapToGrid by rememberUpdatedState(snapToGrid)
    val currentGridSize by rememberUpdatedState(gridSize)
    val currentScaleFactor by rememberUpdatedState(scaleFactor)
    val currentParentW by rememberUpdatedState(parentW)
    val currentParentH by rememberUpdatedState(parentH)
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
    val shape = if (cornerChild != null) {
        val r = cornerChild.properties["CornerRadius"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
        val topLeft = cornerChild.properties["TopLeft"] as? UDim2 ?: r
        val topRight = cornerChild.properties["TopRight"] as? UDim2 ?: r
        val bottomLeft = cornerChild.properties["BottomLeft"] as? UDim2 ?: r
        val bottomRight = cornerChild.properties["BottomRight"] as? UDim2 ?: r
        
        RoundedCornerShape(
            topStart = topLeft.offsetX.dp,
            topEnd = topRight.offsetX.dp,
            bottomStart = bottomLeft.offsetX.dp,
            bottomEnd = bottomRight.offsetX.dp
        )
    } else {
        RoundedCornerShape(0.dp)
    }

    // Handle UIGradient
    val gradientChild = obj.children.firstOrNull { it.className == RobloxClass.UIGradient }
    val backgroundModifier = if (gradientChild != null) {
        val colorStr = gradientChild.properties["Color"] as? String ?: "255,255,255 to 150,150,150"
        val rotation = (gradientChild.properties["Rotation"] as? Float ?: 0f) % 360f
        
        val colorStops = try {
            if (colorStr.contains(";")) {
                colorStr.split(";").map { part ->
                    val segments = part.split(":")
                    val pos = segments[0].toFloat()
                    val rgb = segments[1].split(",").map { it.trim().toInt() }
                    pos to Color(rgb[0], rgb[1], rgb[2])
                }.toTypedArray()
            } else {
                val colors = colorStr.split(" to ").map { part ->
                    val rgb = part.split(",").map { it.trim().toInt() }
                    Color(rgb[0], rgb[1], rgb[2])
                }
                arrayOf(0f to colors[0], 1f to colors[1])
            }
        } catch (e: Exception) {
            arrayOf(0f to Color.White, 1f to Color.Gray)
        }

        // Calculate gradient start and end based on rotation
        val angleRad = (rotation - 90f) * (PI.toFloat() / 180f)
        val startX = 0.5f - 0.5f * cos(angleRad)
        val startY = 0.5f - 0.5f * sin(angleRad)
        val endX = 0.5f + 0.5f * cos(angleRad)
        val endY = 0.5f + 0.5f * sin(angleRad)

        Modifier.background(
            Brush.linearGradient(
                colorStops = colorStops,
                start = Offset(startX * wPx, startY * hPx),
                end = Offset(endX * wPx, endY * hPx),
                tileMode = TileMode.Clamp
            ),
            shape = shape
        )
    } else {
        Modifier.background(bgColor, shape)
    }

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

    // Handle UIShadow rendering behind the Box
    val shadowChild = obj.children.firstOrNull { it.className == RobloxClass.UIShadow }
    if (shadowChild != null) {
        val shadowEnabled = shadowChild.properties["Enabled"] as? Boolean ?: true
        if (shadowEnabled) {
            val shadowColorRaw = shadowChild.properties["Color"] as? Color3 ?: Color3(0, 0, 0)
            val shadowTrans = shadowChild.properties["Transparency"] as? Float ?: 0.5f
            val shadowOffset = shadowChild.properties["Offset"] as? Vector2 ?: Vector2(0f, 4f)
            val shadowColor = Color(shadowColorRaw.r, shadowColorRaw.g, shadowColorRaw.b).copy(alpha = (1.0f - shadowTrans).coerceIn(0f, 1f))
            
            Box(
                modifier = Modifier
                    .offset((xPx + shadowOffset.x).dp, (yPx + shadowOffset.y).dp)
                    .size(wPx.dp, hPx.dp)
                    .background(shadowColor, shape)
            )
        }
    }

    Box(
        modifier = Modifier
            .offset(xPx.dp, yPx.dp)
            .size(wPx.dp, hPx.dp)
            .clip(shape)
            .then(backgroundModifier)
            .then(borderModifier)
            .combinedClickable(
                enabled = !isPreviewMode,
                onClick = { onSelect(obj.id) },
                onDoubleClick = {
                    onSelect(obj.id)
                    onToggleDragMode()
                }
            )
            .pointerInput(obj.id, isPreviewMode, useSingleDragMode) {
                if (!isPreviewMode) {
                    if (useSingleDragMode) {
                        var startPos = currentPos
                        var dragAccumulatedX = 0f
                        var dragAccumulatedY = 0f
                        detectDragGestures(
                            onDragStart = {
                                startPos = currentPos
                                dragAccumulatedX = 0f
                                dragAccumulatedY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dxDp = dragAmount.x / (density * currentScaleFactor)
                                val dyDp = dragAmount.y / (density * currentScaleFactor)
                                
                                dragAccumulatedX += dxDp
                                dragAccumulatedY += dyDp
                                
                                val newOffsetX = startPos.offsetX + dragAccumulatedX
                                val newOffsetY = startPos.offsetY + dragAccumulatedY
                                
                                val finalX = if (currentSnapToGrid && currentGridSize > 1) {
                                    (newOffsetX.roundToInt() / currentGridSize) * currentGridSize
                                } else newOffsetX.roundToInt()
                                
                                val finalY = if (currentSnapToGrid && currentGridSize > 1) {
                                    (newOffsetY.roundToInt() / currentGridSize) * currentGridSize
                                } else newOffsetY.roundToInt()
                                
                                if (finalX != currentPos.offsetX || finalY != currentPos.offsetY) {
                                    onMoveOrResize(
                                        obj.id,
                                        currentPos.copy(offsetX = finalX, offsetY = finalY),
                                        currentSize
                                    )
                                }
                            }
                        )
                    } else {
                        var dragStartX = currentPos.offsetX.toFloat()
                        var dragStartY = currentPos.offsetY.toFloat()
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1.0f) {
                                // 2-finger pinch resize
                                val newOffsetX = (currentSize.offsetX * zoom).roundToInt()
                                val newOffsetY = (currentSize.offsetY * zoom).roundToInt()
                                val newScaleX = (currentSize.scaleX * zoom).coerceIn(0f, 10f)
                                val newScaleY = (currentSize.scaleY * zoom).coerceIn(0f, 10f)
                                
                                val updatedSize = currentSize.copy(
                                    scaleX = if (currentSize.scaleX > 0f) newScaleX else 0f,
                                    offsetX = if (currentSize.scaleX == 0f) newOffsetX.coerceAtLeast(10) else currentSize.offsetX,
                                    scaleY = if (currentSize.scaleY > 0f) newScaleY else 0f,
                                    offsetY = if (currentSize.scaleY == 0f) newOffsetY.coerceAtLeast(10) else currentSize.offsetY
                                )
                                onMoveOrResize(obj.id, currentPos, updatedSize)
                            } else if (pan != Offset.Zero) {
                                // 1-finger hold and drag to move
                                val dxDp = pan.x / (density * currentScaleFactor)
                                val dyDp = pan.y / (density * currentScaleFactor)
                                
                                val currentX = currentPos.offsetX.toFloat()
                                val currentY = currentPos.offsetY.toFloat()
                                if (kotlin.math.abs(dragStartX - currentX) > 30f || kotlin.math.abs(dragStartY - currentY) > 30f) {
                                    dragStartX = currentX
                                    dragStartY = currentY
                                }
                                
                                dragStartX += dxDp
                                dragStartY += dyDp
                                
                                val finalX = if (currentSnapToGrid && currentGridSize > 1) {
                                    (dragStartX.roundToInt() / currentGridSize) * currentGridSize
                                } else dragStartX.roundToInt()
                                
                                val finalY = if (currentSnapToGrid && currentGridSize > 1) {
                                    (dragStartY.roundToInt() / currentGridSize) * currentGridSize
                                } else dragStartY.roundToInt()
                                
                                if (finalX != currentPos.offsetX || finalY != currentPos.offsetY) {
                                    onMoveOrResize(
                                        obj.id,
                                        currentPos.copy(offsetX = finalX, offsetY = finalY),
                                        currentSize
                                    )
                                }
                            }
                        }
                    }
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
        val nonRenderableClasses = setOf(
            RobloxClass.UICorner,
            RobloxClass.UIStroke,
            RobloxClass.UIGradient,
            RobloxClass.UIAspectRatioConstraint,
            RobloxClass.UIScale,
            RobloxClass.UIShadow,
            RobloxClass.UIPadding,
            RobloxClass.UIListLayout,
            RobloxClass.UIGridLayout
        )
        val childrenToRender = obj.children.filter { it.className !in nonRenderableClasses }

        // If it is a list or grid layout, we can stack them!
        val listLayout = obj.children.firstOrNull { it.className == RobloxClass.UIListLayout }
        val gridLayout = obj.children.firstOrNull { it.className == RobloxClass.UIGridLayout }
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
        } else if (gridLayout != null && childrenToRender.isNotEmpty()) {
            val cellSize = gridLayout.properties["CellSize"] as? UDim2 ?: UDim2(0f, 96, 0f, 96)
            val cellPadding = gridLayout.properties["CellPadding"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
            val cellW = ((cellSize.scaleX * wPx) + cellSize.offsetX).coerceAtLeast(32f)
            val cellH = ((cellSize.scaleY * hPx) + cellSize.offsetY).coerceAtLeast(32f)
            val gapX = ((cellPadding.scaleX * wPx) + cellPadding.offsetX).coerceAtLeast(0f)
            val gapY = ((cellPadding.scaleY * hPx) + cellPadding.offsetY).coerceAtLeast(0f)

            FlowRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(gapX.dp),
                verticalArrangement = Arrangement.spacedBy(gapY.dp)
            ) {
                childrenToRender.forEach { child ->
                    RenderLayoutChild(
                        obj = child,
                        parentW = cellW,
                        parentH = cellH,
                        fillParent = true
                    )
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
                    snapToGrid = snapToGrid,
                    gridSize = gridSize,
                    scaleFactor = scaleFactor,
                    useSingleDragMode = useSingleDragMode,
                    onToggleDragMode = onToggleDragMode
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
                if (useSingleDragMode) {
                    // TL Handle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopStart)
                            .pointerInput(obj.id) {
                                var startPos = currentPos
                                var startSize = currentSize
                                var dragAccumulatedX = 0f
                                var dragAccumulatedY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        startPos = currentPos
                                        startSize = currentSize
                                        dragAccumulatedX = 0f
                                        dragAccumulatedY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = dragAmount.x / (density * currentScaleFactor)
                                        val dyDp = dragAmount.y / (density * currentScaleFactor)
                                        
                                        dragAccumulatedX += dxDp
                                        dragAccumulatedY += dyDp
                                        
                                        val newW = startSize.offsetX - dragAccumulatedX
                                        val newH = startSize.offsetY - dragAccumulatedY
                                        
                                        val finalW = if (currentSnapToGrid && currentGridSize > 1) (newW.roundToInt() / currentGridSize) * currentGridSize else newW.roundToInt()
                                        val finalH = if (currentSnapToGrid && currentGridSize > 1) (newH.roundToInt() / currentGridSize) * currentGridSize else newH.roundToInt()
                                        
                                        val minOffsetX = (10 - (startSize.scaleX * currentParentW)).toInt()
                                        val minOffsetY = (10 - (startSize.scaleY * currentParentH)).toInt()
                                        val safeW = finalW.coerceAtLeast(minOffsetX)
                                        val safeH = finalH.coerceAtLeast(minOffsetY)
                                        
                                        val diffX = startSize.offsetX - safeW
                                        val adjustedX = startPos.offsetX + diffX
                                        val diffY = startSize.offsetY - safeH
                                        val adjustedY = startPos.offsetY + diffY
                                        
                                        onMoveOrResize(
                                            obj.id,
                                            currentPos.copy(offsetX = adjustedX, offsetY = adjustedY),
                                            currentSize.copy(offsetX = safeW, offsetY = safeH)
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        )
                    }

                    // TR Handle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                            .pointerInput(obj.id) {
                                var startPos = currentPos
                                var startSize = currentSize
                                var dragAccumulatedX = 0f
                                var dragAccumulatedY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        startPos = currentPos
                                        startSize = currentSize
                                        dragAccumulatedX = 0f
                                        dragAccumulatedY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = dragAmount.x / (density * currentScaleFactor)
                                        val dyDp = dragAmount.y / (density * currentScaleFactor)
                                        
                                        dragAccumulatedX += dxDp
                                        dragAccumulatedY += dyDp
                                        
                                        val newW = startSize.offsetX + dragAccumulatedX
                                        val newH = startSize.offsetY - dragAccumulatedY
                                        
                                        val finalW = if (currentSnapToGrid && currentGridSize > 1) (newW.roundToInt() / currentGridSize) * currentGridSize else newW.roundToInt()
                                        val finalH = if (currentSnapToGrid && currentGridSize > 1) (newH.roundToInt() / currentGridSize) * currentGridSize else newH.roundToInt()
                                        
                                        val minOffsetX = (10 - (startSize.scaleX * currentParentW)).toInt()
                                        val minOffsetY = (10 - (startSize.scaleY * currentParentH)).toInt()
                                        val safeW = finalW.coerceAtLeast(minOffsetX)
                                        val safeH = finalH.coerceAtLeast(minOffsetY)
                                        
                                        val diffY = startSize.offsetY - safeH
                                        val adjustedY = startPos.offsetY + diffY
                                        
                                        onMoveOrResize(
                                            obj.id,
                                            currentPos.copy(offsetY = adjustedY),
                                            currentSize.copy(offsetX = safeW, offsetY = safeH)
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        )
                    }

                    // BL Handle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomStart)
                            .pointerInput(obj.id) {
                                var startPos = currentPos
                                var startSize = currentSize
                                var dragAccumulatedX = 0f
                                var dragAccumulatedY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        startPos = currentPos
                                        startSize = currentSize
                                        dragAccumulatedX = 0f
                                        dragAccumulatedY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = dragAmount.x / (density * currentScaleFactor)
                                        val dyDp = dragAmount.y / (density * currentScaleFactor)
                                        
                                        dragAccumulatedX += dxDp
                                        dragAccumulatedY += dyDp
                                        
                                        val newW = startSize.offsetX - dragAccumulatedX
                                        val newH = startSize.offsetY + dragAccumulatedY
                                        
                                        val finalW = if (currentSnapToGrid && currentGridSize > 1) (newW.roundToInt() / currentGridSize) * currentGridSize else newW.roundToInt()
                                        val finalH = if (currentSnapToGrid && currentGridSize > 1) (newH.roundToInt() / currentGridSize) * currentGridSize else newH.roundToInt()
                                        
                                        val minOffsetX = (10 - (startSize.scaleX * currentParentW)).toInt()
                                        val minOffsetY = (10 - (startSize.scaleY * currentParentH)).toInt()
                                        val safeW = finalW.coerceAtLeast(minOffsetX)
                                        val safeH = finalH.coerceAtLeast(minOffsetY)
                                        
                                        val diffX = startSize.offsetX - safeW
                                        val adjustedX = startPos.offsetX + diffX
                                        
                                        onMoveOrResize(
                                            obj.id,
                                            currentPos.copy(offsetX = adjustedX),
                                            currentSize.copy(offsetX = safeW, offsetY = safeH)
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        )
                    }

                    // BR Handle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .pointerInput(obj.id) {
                                var startSize = currentSize
                                var dragAccumulatedX = 0f
                                var dragAccumulatedY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        startSize = currentSize
                                        dragAccumulatedX = 0f
                                        dragAccumulatedY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = dragAmount.x / (density * currentScaleFactor)
                                        val dyDp = dragAmount.y / (density * currentScaleFactor)
                                        
                                        dragAccumulatedX += dxDp
                                        dragAccumulatedY += dyDp
                                        
                                        val newW = startSize.offsetX + dragAccumulatedX
                                        val newH = startSize.offsetY + dragAccumulatedY
                                        
                                        val finalW = if (currentSnapToGrid && currentGridSize > 1) {
                                            (newW.roundToInt() / currentGridSize) * currentGridSize
                                        } else newW.roundToInt()
                                        
                                        val finalH = if (currentSnapToGrid && currentGridSize > 1) {
                                            (newH.roundToInt() / currentGridSize) * currentGridSize
                                        } else newH.roundToInt()
                                        
                                        val minOffsetX = (10 - (startSize.scaleX * currentParentW)).toInt()
                                        val minOffsetY = (10 - (startSize.scaleY * currentParentH)).toInt()
                                        val safeW = finalW.coerceAtLeast(minOffsetX)
                                        val safeH = finalH.coerceAtLeast(minOffsetY)
                                        
                                        if (safeW != currentSize.offsetX || safeH != currentSize.offsetY) {
                                            onMoveOrResize(
                                                obj.id,
                                                currentPos,
                                                currentSize.copy(offsetX = safeW, offsetY = safeH)
                                            )
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                } else {
                    // Resize Handle dots at bottom-right corner
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                            .align(Alignment.BottomEnd)
                            .pointerInput(obj.id) {
                                var startSize = currentSize
                                var dragAccumulatedX = 0f
                                var dragAccumulatedY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        startSize = currentSize
                                        dragAccumulatedX = 0f
                                        dragAccumulatedY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dxDp = dragAmount.x / (density * currentScaleFactor)
                                        val dyDp = dragAmount.y / (density * currentScaleFactor)
                                        
                                        dragAccumulatedX += dxDp
                                        dragAccumulatedY += dyDp
                                        
                                        val newW = startSize.offsetX + dragAccumulatedX
                                        val newH = startSize.offsetY + dragAccumulatedY
                                        
                                        val finalW = if (currentSnapToGrid && currentGridSize > 1) {
                                            (newW.roundToInt() / currentGridSize) * currentGridSize
                                        } else newW.roundToInt()
                                        
                                        val finalH = if (currentSnapToGrid && currentGridSize > 1) {
                                            (newH.roundToInt() / currentGridSize) * currentGridSize
                                        } else newH.roundToInt()
                                        
                                        val minOffsetX = (10 - (startSize.scaleX * currentParentW)).toInt()
                                        val minOffsetY = (10 - (startSize.scaleY * currentParentH)).toInt()
                                        val safeW = finalW.coerceAtLeast(minOffsetX)
                                        val safeH = finalH.coerceAtLeast(minOffsetY)
                                        
                                        if (safeW != currentSize.offsetX || safeH != currentSize.offsetY) {
                                            onMoveOrResize(
                                                obj.id,
                                                currentPos,
                                                currentSize.copy(offsetX = safeW, offsetY = safeH)
                                            )
                                        }
                                    }
                                )
                            }
                    )
                }
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
fun RenderLayoutChild(
    obj: RobloxObject,
    parentW: Float,
    parentH: Float,
    fillParent: Boolean = false
) {
    val bgTrans = obj.properties["BackgroundTransparency"] as? Float ?: 0f
    val bgRaw = obj.properties["BackgroundColor3"] as? Color3 ?: Color3(163, 162, 165)
    val bgColor = Color(bgRaw.r, bgRaw.g, bgRaw.b).copy(alpha = (1.0f - bgTrans).coerceIn(0f, 1f))
    
    val size = obj.properties["Size"] as? UDim2 ?: UDim2(0.8f, 0, 0.2f, 0)
    val w = if (fillParent) parentW.dp else ((size.scaleX * parentW) + size.offsetX).dp.coerceAtLeast(30.dp)
    val h = if (fillParent) parentH.dp else ((size.scaleY * parentH) + size.offsetY).dp.coerceAtLeast(15.dp)

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
