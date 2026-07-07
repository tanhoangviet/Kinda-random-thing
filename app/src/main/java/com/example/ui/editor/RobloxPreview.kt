@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.example.ui.editor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt
import com.example.data.model.*
import coil.compose.AsyncImage

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
    onToggleDragMode: () -> Unit = {},
    onOpenScrollMode: (String) -> Unit = {},
    onOpenPathMode: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(screenWidth.dp, screenHeight.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF181B22), Color(0xFF10131A)),
                    start = Offset.Zero,
                    end = Offset(screenWidth.toFloat(), screenHeight.toFloat())
                )
            )
            .border(1.dp, Color(72, 80, 92))
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
                onToggleDragMode = onToggleDragMode,
                onOpenScrollMode = onOpenScrollMode,
                onOpenPathMode = onOpenPathMode
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
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val minorColor = Color(78, 86, 98, 70)
        val majorColor = Color(100, 170, 230, 95)
        val axisColor = Color(255, 181, 77, 140)
        val step = gridSize.toFloat().coerceAtLeast(2f)
        val majorEvery = 4
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        var y = 0f
        var row = 0
        while (y < size.height) {
            val isMajor = row % majorEvery == 0
            drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = if (isMajor) 0.9.dp.toPx() else 0.45.dp.toPx()
            )
            y += step
            row += 1
        }

        var x = 0f
        var column = 0
        while (x < size.width) {
            val isMajor = column % majorEvery == 0
            drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = if (isMajor) 0.9.dp.toPx() else 0.45.dp.toPx()
            )
            x += step
            column += 1
        }

        drawLine(
            color = axisColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = axisColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private enum class ResizeEdge { Left, Right, Top, Bottom }

@Composable
private fun BoxScope.EdgeResizeHandle(
    edge: ResizeEdge,
    objId: String,
    currentPos: UDim2,
    currentSize: UDim2,
    density: Float,
    currentScaleFactor: Float,
    currentSnapToGrid: Boolean,
    currentGridSize: Int,
    currentParentW: Float,
    currentParentH: Float,
    onMoveOrResize: (String, UDim2, UDim2) -> Unit
) {
    val alignment = when (edge) {
        ResizeEdge.Left -> Alignment.CenterStart
        ResizeEdge.Right -> Alignment.CenterEnd
        ResizeEdge.Top -> Alignment.TopCenter
        ResizeEdge.Bottom -> Alignment.BottomCenter
    }
    val handleModifier = when (edge) {
        ResizeEdge.Left, ResizeEdge.Right -> Modifier.width(28.dp).fillMaxHeight()
        ResizeEdge.Top, ResizeEdge.Bottom -> Modifier.fillMaxWidth().height(28.dp)
    }

    Box(
        modifier = handleModifier
            .align(alignment)
            .pointerInput(objId, edge) {
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
                        dragAccumulatedX += dragAmount.x / (density * currentScaleFactor)
                        dragAccumulatedY += dragAmount.y / (density * currentScaleFactor)

                        val minOffsetX = (10 - (startSize.scaleX * currentParentW)).toInt()
                        val minOffsetY = (10 - (startSize.scaleY * currentParentH)).toInt()

                        fun snap(raw: Float): Int {
                            return if (currentSnapToGrid && currentGridSize > 1) {
                                (raw.roundToInt() / currentGridSize) * currentGridSize
                            } else {
                                raw.roundToInt()
                            }
                        }

                        when (edge) {
                            ResizeEdge.Left -> {
                                val safeW = snap(startSize.offsetX - dragAccumulatedX).coerceAtLeast(minOffsetX)
                                val adjustedX = startPos.offsetX + (startSize.offsetX - safeW)
                                onMoveOrResize(objId, currentPos.copy(offsetX = adjustedX), currentSize.copy(offsetX = safeW))
                            }
                            ResizeEdge.Right -> {
                                val safeW = snap(startSize.offsetX + dragAccumulatedX).coerceAtLeast(minOffsetX)
                                onMoveOrResize(objId, currentPos, currentSize.copy(offsetX = safeW))
                            }
                            ResizeEdge.Top -> {
                                val safeH = snap(startSize.offsetY - dragAccumulatedY).coerceAtLeast(minOffsetY)
                                val adjustedY = startPos.offsetY + (startSize.offsetY - safeH)
                                onMoveOrResize(objId, currentPos.copy(offsetY = adjustedY), currentSize.copy(offsetY = safeH))
                            }
                            ResizeEdge.Bottom -> {
                                val safeH = snap(startSize.offsetY + dragAccumulatedY).coerceAtLeast(minOffsetY)
                                onMoveOrResize(objId, currentPos, currentSize.copy(offsetY = safeH))
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val visualModifier = when (edge) {
            ResizeEdge.Left, ResizeEdge.Right -> Modifier.size(width = 4.dp, height = 36.dp)
            ResizeEdge.Top, ResizeEdge.Bottom -> Modifier.size(width = 36.dp, height = 4.dp)
        }
        Box(
            modifier = visualModifier
                .background(Color(0, 162, 255), RoundedCornerShape(2.dp))
                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
        )
    }
}

private data class LocalTweenPreviewSpec(
    val enabled: Boolean,
    val movesPosition: Boolean,
    val resizes: Boolean,
    val fades: Boolean
)

private fun resolveLocalScriptTweenPreview(obj: RobloxObject): LocalTweenPreviewSpec {
    val source = obj.children
        .filter { it.className == RobloxClass.LocalScript }
        .joinToString("\n") { it.properties["Source"] as? String ?: "" }
        .lowercase()
    val hasTween = source.contains("tweenservice") || source.contains(":create(") || source.contains("tween")
    if (!hasTween) {
        return LocalTweenPreviewSpec(enabled = false, movesPosition = false, resizes = false, fades = false)
    }
    val moves = source.contains("position")
    val resizes = source.contains("size")
    val fades = source.contains("transparency")
    return LocalTweenPreviewSpec(
        enabled = true,
        movesPosition = moves || (!resizes && !fades),
        resizes = resizes,
        fades = fades
    )
}

private fun RobloxClass.isRenderDecoratorClass(): Boolean {
    return this in setOf(
        RobloxClass.UICorner,
        RobloxClass.UIStroke,
        RobloxClass.UIGradient,
        RobloxClass.UIAspectRatioConstraint,
        RobloxClass.UIScale,
        RobloxClass.UIShadow,
        RobloxClass.UIPadding,
        RobloxClass.UIListLayout,
        RobloxClass.UIGridLayout,
        RobloxClass.LocalScript,
        RobloxClass.ModuleScript
    )
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
    onToggleDragMode: () -> Unit = {},
    onOpenScrollMode: (String) -> Unit = {},
    onOpenPathMode: (String) -> Unit = {}
) {
    // Check basic structural values
    val isVisible = obj.properties["Visible"] as? Boolean ?: true
    if (!isVisible) return

    if (obj.className == RobloxClass.Folder || obj.className == RobloxClass.ScreenGui) {
        obj.children
            .filter { !it.className.isRenderDecoratorClass() }
            .forEach { child ->
                RenderRobloxObject(
                    obj = child,
                    parentW = parentW,
                    parentH = parentH,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMoveOrResize = onMoveOrResize,
                    isPreviewMode = isPreviewMode,
                    snapToGrid = snapToGrid,
                    gridSize = gridSize,
                    scaleFactor = scaleFactor,
                    useSingleDragMode = useSingleDragMode,
                    onToggleDragMode = onToggleDragMode,
                    onOpenScrollMode = onOpenScrollMode,
                    onOpenPathMode = onOpenPathMode
                )
            }
        return
    }

    if (obj.className.isRenderDecoratorClass()) return

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

    // Compute absolute pixel bounds, then apply supported constraints.
    val rawWPx = (size.scaleX * parentW) + size.offsetX
    val rawHPx = (size.scaleY * parentH) + size.offsetY
    val aspectChild = obj.children.firstOrNull { it.className == RobloxClass.UIAspectRatioConstraint }
    val constrainedSize = if (aspectChild != null) {
        val ratio = (aspectChild.properties["AspectRatio"] as? Float ?: 1f).coerceAtLeast(0.01f)
        val dominantAxis = aspectChild.properties["DominantAxis"] as? String ?: "Width"
        when (dominantAxis) {
            "Height" -> (rawHPx * ratio) to rawHPx
            "Width" -> rawWPx to (rawWPx / ratio)
            else -> {
                val currentRatio = rawWPx / rawHPx.coerceAtLeast(1f)
                if (currentRatio > ratio) (rawHPx * ratio) to rawHPx else rawWPx to (rawWPx / ratio)
            }
        }
    } else {
        rawWPx to rawHPx
    }
    val wPx = constrainedSize.first.coerceAtLeast(1f)
    val hPx = constrainedSize.second.coerceAtLeast(1f)
    val uiScale = ((obj.children.firstOrNull { it.className == RobloxClass.UIScale }?.properties?.get("Scale") as? Float) ?: 1f)
        .coerceIn(0.05f, 10f)

    val xPx = (pos.scaleX * parentW) + pos.offsetX - (anchor.x * wPx)
    val yPx = (pos.scaleY * parentH) + pos.offsetY - (anchor.y * hPx)

    // Layout configuration
    val isSelected = obj.id == selectedId
    val tweenPreviewSpec = remember(obj.id, obj.children) { resolveLocalScriptTweenPreview(obj) }
    val tweenTransition = rememberInfiniteTransition(label = "LocalScriptTweenPreview")
    val tweenProgress by tweenTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TweenPreviewProgress"
    )
    val tweenActive = isPreviewMode && tweenPreviewSpec.enabled
    val tweenOffsetX = if (tweenActive && tweenPreviewSpec.movesPosition) 18f * tweenProgress else 0f
    val tweenOffsetY = if (tweenActive && tweenPreviewSpec.movesPosition) 8f * tweenProgress else 0f
    val tweenSizeScale = if (tweenActive && tweenPreviewSpec.resizes) 1f + (0.06f * tweenProgress) else 1f
    val tweenAlpha = if (tweenActive && tweenPreviewSpec.fades) (1f - 0.35f * tweenProgress).coerceIn(0.2f, 1f) else 1f

    // Map properties to Compose graphics styling
    val bgTrans = obj.properties["BackgroundTransparency"] as? Float ?: if (obj.className == RobloxClass.Path2D) 1f else 0f
    val bgRaw = obj.properties["BackgroundColor3"] as? Color3 ?: Color3(163, 162, 165)
    val bgColor = Color(bgRaw.r, bgRaw.g, bgRaw.b).copy(alpha = (1.0f - bgTrans).coerceIn(0f, 1f))

    // Handle UICorner inside children
    val cornerChild = obj.children.firstOrNull { it.className == RobloxClass.UICorner }
    val shape = if (cornerChild != null) {
        val r = cornerChild.properties["CornerRadius"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
        val topLeft = cornerChild.properties["TopLeftRadius"] as? UDim2
            ?: cornerChild.properties["TopLeft"] as? UDim2
            ?: r
        val topRight = cornerChild.properties["TopRightRadius"] as? UDim2
            ?: cornerChild.properties["TopRight"] as? UDim2
            ?: r
        val bottomLeft = cornerChild.properties["BottomLeftRadius"] as? UDim2
            ?: cornerChild.properties["BottomLeft"] as? UDim2
            ?: r
        val bottomRight = cornerChild.properties["BottomRightRadius"] as? UDim2
            ?: cornerChild.properties["BottomRight"] as? UDim2
            ?: r

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

        val parsedColors = parseGradientColorStops(colorStr)
        val parsedTransparency = parseNumberSequenceStops(gradientChild.properties["Transparency"])
        val combinedPositions = (parsedColors.map { it.first } + parsedTransparency.map { it.first } + listOf(0f, 1f))
            .distinct()
            .sorted()
        val colorStops = combinedPositions.map { position ->
            val color = interpolateGradientColor(parsedColors, position)
            val alpha = (1f - interpolateNumberSequence(parsedTransparency, position)).coerceIn(0f, 1f)
            position to color.copy(alpha = alpha)
        }.toTypedArray()

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
        val alpha = (1f - (strokeChild.properties["Transparency"] as? Float ?: 0f)).coerceIn(0f, 1f)
        Modifier.border(thick.dp, Color(col.r, col.g, col.b).copy(alpha = alpha), shape)
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
            val shadowBlur = (shadowChild.properties["Blur"] as? Int ?: 8).coerceAtLeast(0)
            val shadowSpread = (shadowChild.properties["Spread"] as? Int ?: 0)
            val shadowOffset = shadowChild.properties["Offset"] as? Vector2 ?: Vector2(0f, 4f)
            val shadowColor = Color(shadowColorRaw.r, shadowColorRaw.g, shadowColorRaw.b).copy(alpha = (1.0f - shadowTrans).coerceIn(0f, 1f))
            val shadowW = (wPx + shadowSpread * 2).coerceAtLeast(1f)
            val shadowH = (hPx + shadowSpread * 2).coerceAtLeast(1f)

            Box(
                modifier = Modifier
                    .offset((xPx + shadowOffset.x - shadowSpread).dp, (yPx + shadowOffset.y - shadowSpread).dp)
                    .size(shadowW.dp, shadowH.dp)
                    .background(shadowColor, shape)
                    .blur(shadowBlur.dp)
            )
        }
    }

    Box(
        modifier = Modifier
            .offset((xPx + tweenOffsetX).dp, (yPx + tweenOffsetY).dp)
            .size((wPx * tweenSizeScale).dp, (hPx * tweenSizeScale).dp)
            .graphicsLayer(scaleX = uiScale, scaleY = uiScale, alpha = tweenAlpha)
            .clip(shape)
            .then(backgroundModifier)
            .then(borderModifier)
            .combinedClickable(
                enabled = !isPreviewMode,
                onClick = {
                    onSelect(obj.id)
                    if (obj.className == RobloxClass.ScrollingFrame) {
                        onOpenScrollMode(obj.id)
                    }
                    if (obj.className == RobloxClass.Path2D) {
                        onOpenPathMode(obj.id)
                    }
                },
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
                val textTrans = obj.properties["TextTransparency"] as? Float ?: 0f
                val textColor = Color(textColorRaw.r, textColorRaw.g, textColorRaw.b)
                    .copy(alpha = (1f - textTrans).coerceIn(0f, 1f))
                val textSize = obj.properties["TextSize"] as? Int ?: 14
                val textScaled = obj.properties["TextScaled"] as? Boolean ?: false
                val wrapped = obj.properties["TextWrapped"] as? Boolean ?: true
                val font = obj.properties["Font"] as? String ?: "SourceSans"
                val textXAlign = obj.properties["TextXAlignment"] as? String ?: "Center"
                val textYAlign = obj.properties["TextYAlignment"] as? String ?: "Center"

                val align = when (textXAlign) {
                    "Left" -> TextAlign.Left
                    "Right" -> TextAlign.Right
                    else -> TextAlign.Center
                }
                val contentAlignment = when (textYAlign) {
                    "Top" -> when (textXAlign) {
                        "Left" -> Alignment.TopStart
                        "Right" -> Alignment.TopEnd
                        else -> Alignment.TopCenter
                    }
                    "Bottom" -> when (textXAlign) {
                        "Left" -> Alignment.BottomStart
                        "Right" -> Alignment.BottomEnd
                        else -> Alignment.BottomCenter
                    }
                    else -> when (textXAlign) {
                        "Left" -> Alignment.CenterStart
                        "Right" -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = contentAlignment
                ) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = if (textScaled) (hPx * 0.45f).coerceIn(8f, 72f).sp else textSize.sp,
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
                val imageAlpha = (1f - (obj.properties["ImageTransparency"] as? Float ?: 0f)).coerceIn(0f, 1f)
                val scaleType = obj.properties["ScaleType"] as? String ?: "Stretch"
                val previewModel = remember(image) { resolveRobloxImagePreviewUrl(image) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = imageAlpha)
                        .background(Color(0, 0, 0, 35)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = null,
                            contentScale = when (scaleType) {
                                "Fit" -> ContentScale.Fit
                                "Crop" -> ContentScale.Crop
                                else -> ContentScale.FillBounds
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }
            RobloxClass.ScrollingFrame -> {
                val scrollBarThickness = (obj.properties["ScrollBarThickness"] as? Int ?: 6).coerceIn(0, 24)
                // Show a fake scroll handle on the right edge
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(scrollBarThickness.dp)
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
            RobloxClass.Path2D -> {
                Path2DPreview(obj = obj)
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
                        val canvasSize = this.size
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1F4D68), Color(0xFF090B10)),
                                center = Offset(canvasSize.width * 0.45f, canvasSize.height * 0.35f),
                                radius = minOf(canvasSize.width, canvasSize.height) * 0.75f
                            )
                        )
                        val horizon = canvasSize.height * 0.68f
                        for (i in 0..8) {
                            val t = i / 8f
                            val y = horizon + (canvasSize.height - horizon) * t * t
                            drawLine(
                                color = Color(90, 170, 230, (70 * (1f - t)).roundToInt()),
                                start = Offset(0f, y),
                                end = Offset(canvasSize.width, y),
                                strokeWidth = 0.7.dp.toPx()
                            )
                        }
                        for (i in -8..8) {
                            val offset = i * canvasSize.width * 0.09f
                            drawLine(
                                color = Color(90, 170, 230, 55),
                                start = Offset(canvasSize.width / 2f + offset * 0.2f, horizon),
                                end = Offset(canvasSize.width / 2f + offset, canvasSize.height),
                                strokeWidth = 0.7.dp.toPx()
                            )
                        }
                        drawCircle(
                            color = Color(0, 0, 0, 120),
                            radius = minOf(canvasSize.width, canvasSize.height) * 0.22f,
                            center = Offset(canvasSize.width / 2f, canvasSize.height * 0.72f)
                        )

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
                            drawPath(path, Color.White.copy(alpha = 0.08f))
                            drawPath(path, Color(220, 245, 255, 150), style = Stroke(width = 1.dp.toPx()))
                        }
                    }

                }
            }
            else -> {}
        }

        // Apply Layout engines visually on preview if possible
        // For our mobile preview, we render the children recursively!
        val childrenToRender = obj.children
            .filter { !it.className.isRenderDecoratorClass() }
            .sortedWith(compareBy<RobloxObject> { it.properties["LayoutOrder"] as? Int ?: 0 }.thenBy { it.name })
        val contentPadding = resolveRenderPadding(
            paddingChild = obj.children.firstOrNull { it.className == RobloxClass.UIPadding },
            parentW = wPx,
            parentH = hPx
        )
        val paddedW = (wPx - contentPadding.left - contentPadding.right).coerceAtLeast(1f)
        val paddedH = (hPx - contentPadding.top - contentPadding.bottom).coerceAtLeast(1f)

        // If it is a list or grid layout, we can stack them!
        val listLayout = obj.children.firstOrNull { it.className == RobloxClass.UIListLayout }
        val gridLayout = obj.children.firstOrNull { it.className == RobloxClass.UIGridLayout }
        if (listLayout != null && childrenToRender.isNotEmpty()) {
            val isVertical = (listLayout.properties["FillDirection"] as? String ?: "Vertical") == "Vertical"
            val spacing = (listLayout.properties["Padding"] as? UDim2)?.offsetY ?: 8
            val horizontalAlignment = toHorizontalAlignment(listLayout.properties["HorizontalAlignment"] as? String ?: "Center")
            val verticalAlignment = toVerticalAlignment(listLayout.properties["VerticalAlignment"] as? String ?: "Center")

            if (isVertical) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = contentPadding.left.dp,
                            top = contentPadding.top.dp,
                            end = contentPadding.right.dp,
                            bottom = contentPadding.bottom.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                    horizontalAlignment = horizontalAlignment
                ) {
                    childrenToRender.forEach { child ->
                        Box(modifier = Modifier.wrapContentSize()) {
                            RenderLayoutChild(child, paddedW, paddedH)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = contentPadding.left.dp,
                            top = contentPadding.top.dp,
                            end = contentPadding.right.dp,
                            bottom = contentPadding.bottom.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                    verticalAlignment = verticalAlignment
                ) {
                    childrenToRender.forEach { child ->
                        Box(modifier = Modifier.wrapContentSize()) {
                            RenderLayoutChild(child, paddedW, paddedH)
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
                    .padding(
                        start = contentPadding.left.dp,
                        top = contentPadding.top.dp,
                        end = contentPadding.right.dp,
                        bottom = contentPadding.bottom.dp
                    ),
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
                    onToggleDragMode = onToggleDragMode,
                    onOpenScrollMode = onOpenScrollMode,
                    onOpenPathMode = onOpenPathMode
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

	                    listOf(ResizeEdge.Left, ResizeEdge.Right, ResizeEdge.Top, ResizeEdge.Bottom).forEach { edge ->
	                        EdgeResizeHandle(
	                            edge = edge,
	                            objId = obj.id,
	                            currentPos = currentPos,
	                            currentSize = currentSize,
	                            density = density,
	                            currentScaleFactor = currentScaleFactor,
	                            currentSnapToGrid = currentSnapToGrid,
	                            currentGridSize = currentGridSize,
	                            currentParentW = currentParentW,
	                            currentParentH = currentParentH,
	                            onMoveOrResize = onMoveOrResize
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
            }
        }
    }
}

private fun resolveRobloxImagePreviewUrl(image: String): String? {
    val trimmed = image.trim()
    if (trimmed.isBlank() || trimmed == "rbxassetid://0") return null
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }

    val assetId = listOf(
        Regex("""rbxassetid://(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""assetId=(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""/library/(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""id=(\d+)""", RegexOption.IGNORE_CASE)
    ).firstNotNullOfOrNull { regex ->
        regex.find(trimmed)?.groupValues?.getOrNull(1)
    }

    return assetId
        ?.takeIf { it != "0" }
        ?.let { "https://www.roblox.com/asset-thumbnail/image?assetId=$it&width=420&height=420&format=png" }
}

private fun parseGradientColorStops(raw: String): List<Pair<Float, Color>> {
    return try {
        if (raw.contains(";")) {
            raw.split(";").mapNotNull { part ->
                val segments = part.split(":")
                if (segments.size < 2) return@mapNotNull null
                val position = segments[0].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
                val rgb = segments[1].split(",").map { it.trim().toInt() }
                if (rgb.size < 3) return@mapNotNull null
                position to Color(rgb[0].coerceIn(0, 255), rgb[1].coerceIn(0, 255), rgb[2].coerceIn(0, 255))
            }.takeIf { it.isNotEmpty() }?.sortedBy { it.first }
                ?: listOf(0f to Color.White, 1f to Color.Gray)
        } else {
            val colors = raw.split(" to ").map { part ->
                val rgb = part.split(",").map { it.trim().toInt() }
                Color(rgb[0].coerceIn(0, 255), rgb[1].coerceIn(0, 255), rgb[2].coerceIn(0, 255))
            }
            listOf(0f to colors[0], 1f to colors[1])
        }
    } catch (e: Exception) {
        listOf(0f to Color.White, 1f to Color.Gray)
    }
}

private fun parseNumberSequenceStops(raw: Any?): List<Pair<Float, Float>> {
    val stops = when (raw) {
        is Number -> listOf(0f to raw.toFloat().coerceIn(0f, 1f), 1f to raw.toFloat().coerceIn(0f, 1f))
        is String -> raw.split(";").mapNotNull { part ->
            val segments = part.split(":")
            if (segments.size < 2) return@mapNotNull null
            val position = segments[0].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            val value = segments[1].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            position to value
        }
        else -> emptyList()
    }.sortedBy { it.first }

    if (stops.isEmpty()) return listOf(0f to 0f, 1f to 0f)
    val normalized = stops.toMutableList()
    if (normalized.first().first != 0f) {
        normalized.add(0, 0f to normalized.first().second)
    }
    if (normalized.last().first != 1f) {
        normalized.add(1f to normalized.last().second)
    }
    return normalized
}

private fun interpolateGradientColor(stops: List<Pair<Float, Color>>, position: Float): Color {
    if (stops.isEmpty()) return Color.White
    if (position <= stops.first().first) return stops.first().second
    if (position >= stops.last().first) return stops.last().second

    for (i in 0 until stops.size - 1) {
        val start = stops[i]
        val end = stops[i + 1]
        if (position >= start.first && position <= end.first) {
            val span = (end.first - start.first).coerceAtLeast(0.0001f)
            val t = (position - start.first) / span
            return Color(
                red = start.second.red + (end.second.red - start.second.red) * t,
                green = start.second.green + (end.second.green - start.second.green) * t,
                blue = start.second.blue + (end.second.blue - start.second.blue) * t,
                alpha = start.second.alpha + (end.second.alpha - start.second.alpha) * t
            )
        }
    }
    return stops.last().second
}

private fun interpolateNumberSequence(stops: List<Pair<Float, Float>>, position: Float): Float {
    if (stops.isEmpty()) return 0f
    if (position <= stops.first().first) return stops.first().second
    if (position >= stops.last().first) return stops.last().second

    for (i in 0 until stops.size - 1) {
        val start = stops[i]
        val end = stops[i + 1]
        if (position >= start.first && position <= end.first) {
            val span = (end.first - start.first).coerceAtLeast(0.0001f)
            val t = (position - start.first) / span
            return start.second + (end.second - start.second) * t
        }
    }
    return 0f
}

@Composable
private fun Path2DPreview(obj: RobloxObject) {
    val colorRaw = obj.properties["Color3"] as? Color3 ?: Color3(0, 162, 255)
    val alpha = (1f - (obj.properties["Transparency"] as? Float ?: 0f)).coerceIn(0f, 1f)
    val thickness = (obj.properties["Thickness"] as? Int ?: 4).coerceIn(1, 32)
    val closed = obj.properties["Closed"] as? Boolean ?: false
    val points = remember(obj.properties["ControlPoints"]) {
        parsePath2DPreviewPoints(obj.properties["ControlPoints"] as? String)
    }
    val pathColor = Color(colorRaw.r, colorRaw.g, colorRaw.b).copy(alpha = alpha)

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas
        val path = Path().apply {
            val first = points.first()
            moveTo(first.x * size.width, first.y * size.height)
            points.drop(1).forEach { point ->
                lineTo(point.x * size.width, point.y * size.height)
            }
            if (closed) close()
        }
        drawPath(
            path = path,
            color = pathColor,
            style = Stroke(
                width = thickness.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        points.forEach { point ->
            val center = Offset(point.x * size.width, point.y * size.height)
            drawCircle(Color(0xFF0B1220).copy(alpha = 0.75f), radius = (thickness + 4).dp.toPx(), center = center)
            drawCircle(pathColor, radius = (thickness + 1).dp.toPx(), center = center)
            drawCircle(Color.White.copy(alpha = 0.75f), radius = 2.dp.toPx(), center = center)
        }
    }
}

private fun parsePath2DPreviewPoints(raw: String?): List<Vector2> {
    return raw
        ?.split(';')
        ?.mapNotNull { point ->
            val parts = point.trim().split(',')
            if (parts.size < 2) return@mapNotNull null
            val x = parts[0].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            val y = parts[1].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            Vector2(x, y)
        }
        ?.takeIf { it.size >= 2 }
        ?: listOf(
            Vector2(0f, 0.8f),
            Vector2(0.26f, 0.24f),
            Vector2(0.58f, 0.66f),
            Vector2(1f, 0.18f)
        )
}

private data class RenderPadding(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

private fun resolveRenderPadding(
    paddingChild: RobloxObject?,
    parentW: Float,
    parentH: Float
): RenderPadding {
    val properties = paddingChild?.properties ?: return RenderPadding(4f, 4f, 4f, 4f)
    val top = (properties["PaddingTop"] as? UDim2)?.let { (it.scaleY * parentH) + it.offsetY } ?: 0f
    val bottom = (properties["PaddingBottom"] as? UDim2)?.let { (it.scaleY * parentH) + it.offsetY } ?: 0f
    val left = (properties["PaddingLeft"] as? UDim2)?.let { (it.scaleX * parentW) + it.offsetX } ?: 0f
    val right = (properties["PaddingRight"] as? UDim2)?.let { (it.scaleX * parentW) + it.offsetX } ?: 0f
    return RenderPadding(
        left = left.coerceAtLeast(0f),
        top = top.coerceAtLeast(0f),
        right = right.coerceAtLeast(0f),
        bottom = bottom.coerceAtLeast(0f)
    )
}

private fun toHorizontalAlignment(value: String): Alignment.Horizontal {
    return when (value) {
        "Right" -> Alignment.End
        "Center" -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
}

private fun toVerticalAlignment(value: String): Alignment.Vertical {
    return when (value) {
        "Bottom" -> Alignment.Bottom
        "Center" -> Alignment.CenterVertically
        else -> Alignment.Top
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
    val cornerChild = obj.children.firstOrNull { it.className == RobloxClass.UICorner }
    val shape = if (cornerChild != null) {
        val radius = cornerChild.properties["CornerRadius"] as? UDim2 ?: UDim2(0f, 4, 0f, 4)
        RoundedCornerShape(radius.offsetX.dp)
    } else {
        RoundedCornerShape(4.dp)
    }
    val strokeChild = obj.children.firstOrNull { it.className == RobloxClass.UIStroke }
    val borderModifier = if (strokeChild != null) {
        val col = strokeChild.properties["Color"] as? Color3 ?: Color3(255, 255, 255)
        val thick = strokeChild.properties["Thickness"] as? Int ?: 1
        val alpha = (1f - (strokeChild.properties["Transparency"] as? Float ?: 0f)).coerceIn(0f, 1f)
        Modifier.border(thick.dp, Color(col.r, col.g, col.b).copy(alpha = alpha), shape)
    } else {
        Modifier
    }

    val size = obj.properties["Size"] as? UDim2 ?: UDim2(0.8f, 0, 0.2f, 0)
    val w = if (fillParent) parentW.dp else ((size.scaleX * parentW) + size.offsetX).dp.coerceAtLeast(30.dp)
    val h = if (fillParent) parentH.dp else ((size.scaleY * parentH) + size.offsetY).dp.coerceAtLeast(15.dp)

    Box(
        modifier = Modifier
            .size(w, h)
            .clip(shape)
            .background(bgColor, shape)
            .then(borderModifier),
        contentAlignment = Alignment.Center
    ) {
        when (obj.className) {
            RobloxClass.TextLabel, RobloxClass.TextButton -> {
                val textColorRaw = obj.properties["TextColor3"] as? Color3 ?: Color3(255, 255, 255)
                val textTrans = obj.properties["TextTransparency"] as? Float ?: 0f
                Text(
                    text = obj.properties["Text"] as? String ?: obj.name,
                    color = Color(textColorRaw.r, textColorRaw.g, textColorRaw.b)
                        .copy(alpha = (1f - textTrans).coerceIn(0f, 1f)),
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
