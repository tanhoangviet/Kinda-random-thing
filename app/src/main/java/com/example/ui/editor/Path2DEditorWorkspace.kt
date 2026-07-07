package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Color3
import com.example.data.model.RobloxObject
import com.example.data.model.Vector2
import kotlin.math.roundToInt

@Composable
fun Path2DEditorWorkspace(
    pathObj: RobloxObject,
    accent: Color,
    onUpdateProperty: (String, Any) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pointsString = pathObj.properties["ControlPoints"] as? String
        ?: "0.000,0.800;0.260,0.240;0.580,0.660;1.000,0.180"
    val points = remember(pointsString) { parsePath2DEditorPoints(pointsString) }
    val closed = pathObj.properties["Closed"] as? Boolean ?: false
    val thickness = (pathObj.properties["Thickness"] as? Int ?: 4).coerceIn(1, 32)
    val colorRaw = pathObj.properties["Color3"] as? Color3 ?: Color3(0, 162, 255)
    val pathColor = Color(colorRaw.r, colorRaw.g, colorRaw.b)
    var selectedIndex by remember(pathObj.id) { mutableIntStateOf(0) }
    var draggingIndex by remember(pathObj.id) { mutableStateOf<Int?>(null) }
    val safeIndex = selectedIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val selectedPoint = points.getOrNull(safeIndex)

    fun commitPoints(next: List<Vector2>) {
        onUpdateProperty("ControlPoints", serializePath2DEditorPoints(next.take(80)))
    }

    Row(
        modifier = modifier
            .background(Color(0xE611151B), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF303743), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFF1A2029), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF303743), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Path2D / ${pathObj.name}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${points.size} keys  |  ${if (closed) "Closed" else "Open"}  |  ${thickness}px",
                        color = Color(0xFF8E96A3),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Path2D Tab", tint = Color(0xFFC6CED8))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E1218))
                    .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(8.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pointsString) {
                            detectTapGestures { tap ->
                                val nearest = nearestPath2DEditorPoint(points, tap, size.width.toFloat(), size.height.toFloat())
                                if (nearest != null) {
                                    selectedIndex = nearest
                                    return@detectTapGestures
                                }
                                val inserted = points + Vector2(
                                    x = (tap.x / size.width.toFloat()).coerceIn(0f, 1f),
                                    y = (tap.y / size.height.toFloat()).coerceIn(0f, 1f)
                                )
                                selectedIndex = inserted.lastIndex
                                commitPoints(inserted)
                            }
                        }
                        .pointerInput(pointsString) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    draggingIndex = nearestPath2DEditorPoint(points, start, size.width.toFloat(), size.height.toFloat())
                                    draggingIndex?.let { selectedIndex = it }
                                },
                                onDragEnd = { draggingIndex = null },
                                onDragCancel = { draggingIndex = null },
                                onDrag = { change, _ ->
                                    val index = draggingIndex ?: return@detectDragGestures
                                    change.consume()
                                    val updated = points.toMutableList()
                                    updated[index] = Vector2(
                                        x = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f),
                                        y = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    )
                                    commitPoints(updated)
                                }
                            )
                        }
                ) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF111827), Color(0xFF0B1017)),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )

                    val minor = Color(77, 86, 104, 64)
                    val major = accent.copy(alpha = 0.32f)
                    val axis = Color(255, 181, 77, 135)
                    val step = 24.dp.toPx()
                    var x = 0f
                    var column = 0
                    while (x <= size.width) {
                        drawLine(
                            color = if (column % 4 == 0) major else minor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = if (column % 4 == 0) 1.dp.toPx() else 0.5.dp.toPx()
                        )
                        x += step
                        column += 1
                    }
                    var y = 0f
                    var row = 0
                    while (y <= size.height) {
                        drawLine(
                            color = if (row % 4 == 0) major else minor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = if (row % 4 == 0) 1.dp.toPx() else 0.5.dp.toPx()
                        )
                        y += step
                        row += 1
                    }
                    drawLine(axis, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.dp.toPx())
                    drawLine(axis, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx())

                    if (points.size >= 2) {
                        val path = Path().apply {
                            moveTo(points.first().x * size.width, points.first().y * size.height)
                            points.drop(1).forEach { point ->
                                lineTo(point.x * size.width, point.y * size.height)
                            }
                            if (closed) close()
                        }
                        drawPath(
                            path = path,
                            color = pathColor.copy(alpha = 0.22f),
                            style = Stroke(width = (thickness + 7).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = path,
                            color = pathColor,
                            style = Stroke(width = thickness.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val center = Offset(point.x * size.width, point.y * size.height)
                        val selected = index == safeIndex
                        drawCircle(
                            color = Color(0xFF05080D).copy(alpha = 0.85f),
                            radius = if (selected) 13.dp.toPx() else 10.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = if (selected) Color.White else Color(0xFFE7EAEE),
                            radius = if (selected) 9.dp.toPx() else 7.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = if (selected) accent else pathColor,
                            radius = if (selected) 5.dp.toPx() else 4.dp.toPx(),
                            center = center
                        )
                    }
                }

                Surface(
                    color = Color(0xCC151A22),
                    contentColor = Color(0xFFC6CED8),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF303743)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Tap add  |  Drag move  |  Keys use 0.0-1.0 scale",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 276.dp)
                .fillMaxHeight()
                .background(Color(0xFF151A22), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF303743), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Keys", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(pathObj.className.name, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val insertAt = (safeIndex + 1).coerceIn(1, points.size)
                        val current = points.getOrNull(safeIndex) ?: Vector2(0.5f, 0.5f)
                        val next = points.getOrNull(insertAt) ?: Vector2((current.x + 0.12f).coerceIn(0f, 1f), current.y)
                        val midpoint = Vector2((current.x + next.x) / 2f, (current.y + next.y) / 2f)
                        selectedIndex = insertAt
                        commitPoints(points.toMutableList().apply { add(insertAt, midpoint) })
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        if (points.size > 2) {
                            selectedIndex = (safeIndex - 1).coerceAtLeast(0)
                            commitPoints(points.toMutableList().apply { removeAt(safeIndex) })
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, Color(0xFF3D4148))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        selectedIndex = (points.lastIndex - safeIndex).coerceAtLeast(0)
                        commitPoints(points.reversed())
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, Color(0xFF3D4148))
                ) {
                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reverse", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        selectedIndex = 0
                        commitPoints(listOf(Vector2(0f, 0.8f), Vector2(0.26f, 0.24f), Vector2(0.58f, 0.66f), Vector2(1f, 0.18f)))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, Color(0xFF3D4148))
                ) {
                    Text("Reset", fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0xFF1D232D), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Closed", color = Color(0xFFE7EAEE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(
                    checked = closed,
                    onCheckedChange = { onUpdateProperty("Closed", it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0xFF1D232D), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Thickness", color = Color(0xFFE7EAEE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StepperInput(
                    value = thickness,
                    onValueChange = { onUpdateProperty("Thickness", it) },
                    min = 1,
                    max = 32
                )
            }

            selectedPoint?.let { point ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1D232D), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Point ${safeIndex + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    PathPointSlider(
                        label = "X",
                        value = point.x,
                        accent = accent,
                        onValueChange = { nextX ->
                            val updated = points.toMutableList()
                            updated[safeIndex] = point.copy(x = nextX)
                            commitPoints(updated)
                        }
                    )
                    PathPointSlider(
                        label = "Y",
                        value = point.y,
                        accent = accent,
                        onValueChange = { nextY ->
                            val updated = points.toMutableList()
                            updated[safeIndex] = point.copy(y = nextY)
                            commitPoints(updated)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                points.forEachIndexed { index, point ->
                    PathPointRow(
                        index = index,
                        point = point,
                        selected = index == safeIndex,
                        accent = accent,
                        onClick = { selectedIndex = index }
                    )
                }
            }

            RobloxTextField(
                value = pointsString,
                onValueChange = { onUpdateProperty("ControlPoints", it) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                placeholder = "0.0,0.0;1.0,1.0"
            )
        }
    }
}

@Composable
private fun PathPointSlider(
    label: String,
    value: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFFA6ACB3), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onValueChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun PathPointRow(
    index: Int,
    point: Vector2,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else Color(0xFF1D232D))
            .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else Color(0xFF2E3540), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (selected) accent else Color(0xFF2B3442), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "x ${String.format("%.3f", point.x)}",
            color = Color(0xFFE7EAEE),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "y ${String.format("%.3f", point.y)}",
            color = Color(0xFFE7EAEE),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun parsePath2DEditorPoints(raw: String): List<Vector2> {
    return raw.split(';')
        .mapNotNull { point ->
            val parts = point.trim().split(',')
            if (parts.size < 2) return@mapNotNull null
            val x = parts[0].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            val y = parts[1].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            Vector2(x, y)
        }
        .takeIf { it.size >= 2 }
        ?: listOf(Vector2(0f, 0.8f), Vector2(0.26f, 0.24f), Vector2(0.58f, 0.66f), Vector2(1f, 0.18f))
}

private fun serializePath2DEditorPoints(points: List<Vector2>): String {
    return points.joinToString(";") { point ->
        "${String.format("%.3f", point.x.coerceIn(0f, 1f))},${String.format("%.3f", point.y.coerceIn(0f, 1f))}"
    }
}

private fun nearestPath2DEditorPoint(points: List<Vector2>, offset: Offset, width: Float, height: Float): Int? {
    val maxDistance = 34f
    var bestIndex = -1
    var bestDistanceSq = maxDistance * maxDistance
    points.forEachIndexed { index, point ->
        val dx = offset.x - point.x * width
        val dy = offset.y - point.y * height
        val distanceSq = dx * dx + dy * dy
        if (distanceSq < bestDistanceSq) {
            bestDistanceSq = distanceSq
            bestIndex = index
        }
    }
    return bestIndex.takeIf { it >= 0 }
}
