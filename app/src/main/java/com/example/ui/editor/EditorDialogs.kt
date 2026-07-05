package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import android.content.Intent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.RobloxObject
import com.example.data.model.RobloxClass
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import com.example.data.local.ProjectEntity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.Color3
import kotlin.math.roundToInt

// Category-based Insert Object Dialog
@Composable
fun InsertObjectDialog(
    onDismiss: () -> Unit,
    onInsert: (RobloxClass) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Basic UI") }
    
    val categories = listOf("Basic UI", "Text", "Image", "Layout", "Constraint", "Script", "Effects")
    
    val objectsForCategory = when (selectedCategory) {
        "Basic UI" -> listOf(RobloxClass.Frame, RobloxClass.ScrollingFrame, RobloxClass.ViewportFrame)
        "Text" -> listOf(RobloxClass.TextLabel, RobloxClass.TextButton)
        "Image" -> listOf(RobloxClass.ImageLabel, RobloxClass.ImageButton)
        "Layout" -> listOf(RobloxClass.UIListLayout, RobloxClass.UIGridLayout, RobloxClass.UIPadding)
        "Constraint" -> listOf(RobloxClass.UICorner, RobloxClass.UIStroke, RobloxClass.UIAspectRatioConstraint)
        "Script" -> listOf(RobloxClass.LocalScript, RobloxClass.ModuleScript)
        "Effects" -> listOf(RobloxClass.UIGradient, RobloxClass.UIScale, RobloxClass.UIShadow)
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Object", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Category list on the left
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color(45, 45, 50))
                ) {
                    items(categories) { cat ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedCategory == cat) Color(0, 162, 255, 40) else Color.Transparent)
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                color = if (selectedCategory == cat) Color.White else Color.Gray,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Object grid on the right
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(2.2f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(objectsForCategory) { obj ->
                        Card(
                            onClick = { onInsert(obj); onDismiss() },
                            colors = CardDefaults.cardColors(containerColor = Color(35, 35, 40)),
                            border = BorderStroke(0.5.dp, Color(55, 55, 60))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                RobloxClassIcon(className = obj, iconSize = 20.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = obj.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// New Project Dialog
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit // (name, template)
) {
    var projectName by remember { mutableStateOf("My Adventure UI") }
    var selectedTemplate by remember { mutableStateOf("Main Menu Dashboard") }
    
    val templates = listOf("Empty Screen", "Main Menu Dashboard", "Item Shop Grid", "Player HUD Status")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Roblox UI Project", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Starting Template:", fontSize = 11.sp, color = Color.Gray)
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .border(1.dp, Color(45, 45, 50))
                ) {
                    items(templates) { temp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedTemplate == temp) Color(0, 162, 255, 30) else Color.Transparent)
                                .clickable { selectedTemplate = temp }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedTemplate == temp),
                                onClick = { selectedTemplate = temp },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0, 162, 255))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(temp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = when (temp) {
                                        "Empty Screen" -> "Start fresh with a blank canvas Frame."
                                        "Main Menu Dashboard" -> "Prebuilt beautiful stylized start menu."
                                        "Item Shop Grid" -> "Grid layout with scrolling item buy buttons."
                                        else -> "Top-left player status frame with dynamic HP bar."
                                    },
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(projectName, selectedTemplate); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
            ) {
                Text("Create Project", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Open Saved Project Dialog
@Composable
fun OpenProjectDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onLoadProject: (ProjectEntity) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Roblox GUI Project", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved projects found.", color = Color.Gray, fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(45, 45, 50))
                    ) {
                        items(projects) { project ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLoadProject(project); onDismiss() }
                                    .padding(8.dp)
                                    .border(
                                        width = 0.5.dp,
                                        color = Color(50, 50, 55),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(241, 196, 15))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(project.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(project.description, fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteProject(project.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Export Luau Script Dialog
@Composable
fun ExportLuauDialog(
    luauCode: String,
    rojoBundle: String = "",
    uiScalePercent: Int = 100,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedMode by remember { mutableStateOf(0) }
    val selectedCode = if (selectedMode == 0) luauCode else rojoBundle.ifBlank { luauCode }
    val shareTitle = if (selectedMode == 0) "Chia se ma Luau" else "Chia se Rojo bundle"
    val codeFontSize = (9f * (uiScalePercent / 100f)).coerceIn(8f, 13f).sp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Export Luau / Rojo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("DPI-aware preview • UI scale $uiScalePercent%", fontSize = 10.sp, color = Color(0xFF8C929C))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, selectedCode)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, shareTitle)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(46, 204, 113)
                        )
                    }
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(selectedCode)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = Color(0, 162, 255)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(32, 34, 38), RoundedCornerShape(4.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExportModeButton(
                        text = "Luau",
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    ExportModeButton(
                        text = "Rojo",
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(22, 24, 28), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(45, 45, 50), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (selectedMode == 0) "Runtime LocalScript" else "Rojo project files", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("UI $uiScalePercent%  •  ${selectedCode.lines().size} lines", color = Color(0xFF8C929C), fontSize = 10.sp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 330.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(15, 15, 18))
                        .border(1.dp, Color(45, 45, 50), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = selectedCode,
                        color = Color(46, 204, 113),
                        fontSize = codeFontSize,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, selectedCode)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, shareTitle)
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(46, 204, 113))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chia sẻ", color = Color.White)
                }
                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(selectedCode)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
                ) {
                    Text("Copy & Close", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExportModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0, 162, 255) else Color.Transparent,
            contentColor = if (selected) Color.White else Color(190, 196, 204)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorDialog(
    obj: RobloxObject,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var source by remember { mutableStateOf(obj.properties["Source"] as? String ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RobloxClassIcon(className = obj.className, iconSize = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Editing: ${obj.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Script Source:", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0, 162, 255),
                        unfocusedBorderColor = Color(60, 60, 65),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(15, 15, 18),
                        unfocusedContainerColor = Color(15, 15, 18)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(source); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
            ) {
                Text("Save Script", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GradientPickerDialog(
    initialColorString: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    lang: String = "vi"
) {
    // Parse initial string: "r,g,b to r,g,b" or "pos:r,g,b;pos:r,g,b"
    val initialStops = remember(initialColorString) {
        try {
            if (initialColorString.contains(";")) {
                initialColorString.split(";").map { part ->
                    val segments = part.split(":")
                    val pos = segments[0].toFloat()
                    val rgb = segments[1].split(",").map { it.trim().toInt() }
                    GradientStop(pos, Color(rgb[0], rgb[1], rgb[2]))
                }
            } else if (initialColorString.contains(" to ")) {
                val parts = initialColorString.split(" to ")
                val c1 = parts[0].split(",").map { it.trim().toInt() }
                val c2 = parts[1].split(",").map { it.trim().toInt() }
                listOf(
                    GradientStop(0f, Color(c1[0], c1[1], c1[2])),
                    GradientStop(1f, Color(c2[0], c2[1], c2[2]))
                )
            } else {
                listOf(GradientStop(0f, Color.White), GradientStop(1f, Color.Black))
            }
        } catch (e: Exception) {
            listOf(GradientStop(0f, Color.White), GradientStop(1f, Color.Black))
        }
    }

    var stops by remember { mutableStateOf(initialStops.sortedBy { it.position }) }
    var selectedStopIndex by remember { mutableStateOf(0) }

    // Floating offsets for dragging
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(24, 24, 28)),
            border = BorderStroke(1.dp, Color(60, 60, 65)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Drag Title Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(32, 32, 38))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFFEC4899))),
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Locales.translate("adv_gradient_title", lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }

                // Inner content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Main Gradient Showcase Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(60, 60, 65)),
                        colors = CardDefaults.cardColors(containerColor = Color(28, 28, 32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(Locales.translate("gradient_preview", lang), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            
                            // Main Gradient Strip
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colorStops = stops.map { it.position to it.color }.toTypedArray()
                                        )
                                    )
                                    .border(1.dp, Color(50, 50, 55), RoundedCornerShape(6.dp))
                            )
                        }
                    }

                    // Interactive Stops Control Strip (DPI-Responsive & Drag-Optimized)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(60, 60, 65)),
                        colors = CardDefaults.cardColors(containerColor = Color(20, 20, 24)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Locales.translate("gradient_track", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Spacer(Modifier.weight(1f))
                                Text(Locales.translate("click_to_add", lang), fontSize = 10.sp, color = Color(0, 162, 255))
                            }
                            
                            // Interactive slider track
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .pointerInput(stops) {
                                        detectTapGestures { offset ->
                                            val w = size.width.toFloat()
                                            val pos = (offset.x / w).coerceIn(0f, 1f)
                                            // Check if clicked near an existing stop
                                            val threshold = 0.05f
                                            val existing = stops.find { Math.abs(it.position - pos) < threshold }
                                            if (existing == null) {
                                                val newColor = interpolateColor(stops, pos)
                                                val updated = (stops + GradientStop(pos, newColor)).sortedBy { it.position }
                                                stops = updated
                                                selectedStopIndex = updated.indexOfFirst { Math.abs(it.position - pos) < 0.001f }
                                            } else {
                                                selectedStopIndex = stops.indexOf(existing)
                                            }
                                        }
                                    }
                            ) {
                                val trackWidthPx = constraints.maxWidth.toFloat()
                                val trackWidthDp = maxWidth
                                
                                // Visual horizontal timeline track line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(45, 45, 50))
                                )

                                // Render stops markers
                                stops.forEachIndexed { index, stop ->
                                    val isSelected = index == selectedStopIndex
                                    val offsetDp = (stop.position * trackWidthDp.value).dp - 12.dp
                                    
                                    Box(
                                        modifier = Modifier
                                            .offset(x = offsetDp)
                                            .size(24.dp)
                                            .align(Alignment.CenterStart)
                                            .pointerInput(index) {
                                                detectDragGestures(
                                                    onDragStart = { selectedStopIndex = index },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val deltaPct = dragAmount.x / trackWidthPx
                                                        val rawPos = (stops[index].position + deltaPct).coerceIn(0f, 1f)
                                                        
                                                        // Maintain order or re-sort
                                                        val updatedStops = stops.toMutableList().apply {
                                                            this[index] = this[index].copy(position = rawPos)
                                                        }.sortedBy { it.position }
                                                        
                                                        stops = updatedStops
                                                        selectedStopIndex = updatedStops.indexOfFirst { 
                                                            Math.abs(it.position - rawPos) < 0.005f && it.color == stop.color 
                                                        }.coerceIn(0, updatedStops.size - 1)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Glow or circle ring
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(
                                                    if (isSelected) Color(0, 162, 255) else Color(40, 40, 45),
                                                    CircleShape
                                                )
                                                .border(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) Color.White else Color.Gray,
                                                    CircleShape
                                                )
                                        )
                                        
                                        // Color sample dot inside
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(stop.color, CircleShape)
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            // Stop selection chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                stops.forEachIndexed { index, stop ->
                                    val isSelected = index == selectedStopIndex
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0, 162, 255).copy(alpha = 0.2f) else Color(30, 30, 35))
                                            .border(1.dp, if (isSelected) Color(0, 162, 255) else Color(50, 50, 55), RoundedCornerShape(8.dp))
                                            .clickable { selectedStopIndex = index }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(stop.color, CircleShape)
                                                .border(1.dp, Color.White.copy(0.4f), CircleShape)
                                        )
                                        Text(
                                            text = "${Locales.translate("point", lang)} ${index + 1} (${(stop.position * 100).roundToInt()}%)",
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected Stop Color Customization Area
                    if (selectedStopIndex in stops.indices) {
                        val currentStop = stops[selectedStopIndex]
                        
                        // Parse RGB to HSV for visual picker
                        val hsv = remember(currentStop.color) { colorToHSV(currentStop.color) }
                        var hue by remember(currentStop.color) { mutableStateOf(hsv[0]) }
                        var saturation by remember(currentStop.color) { mutableStateOf(hsv[1]) }
                        var value by remember(currentStop.color) { mutableStateOf(hsv[2]) }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(65, 65, 70)),
                            colors = CardDefaults.cardColors(containerColor = Color(28, 28, 32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp), 
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header of current stop settings
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(currentStop.color, RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${Locales.translate("stop_color_settings", lang)} (${selectedStopIndex + 1})", 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (stops.size > 2) {
                                        IconButton(
                                            onClick = {
                                                stops = stops.toMutableList().apply { removeAt(selectedStopIndex) }
                                                selectedStopIndex = 0
                                            }, 
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Precise Stop Position Input / Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(Locales.translate("stop_position", lang), fontSize = 11.sp, color = Color.Gray)
                                        Spacer(Modifier.weight(1f))
                                        Text("${(currentStop.position * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0, 162, 255), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentStop.position,
                                        onValueChange = { newPos ->
                                            stops = stops.toMutableList().apply {
                                                this[selectedStopIndex] = this[selectedStopIndex].copy(position = newPos)
                                            }.sortedBy { it.position }
                                            selectedStopIndex = stops.indexOfFirst { 
                                                Math.abs(it.position - newPos) < 0.005f && it.color == currentStop.color 
                                            }.coerceIn(0, stops.size - 1)
                                        },
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = Color(0, 162, 255),
                                            inactiveTrackColor = Color(55, 55, 60),
                                            thumbColor = Color.White
                                        )
                                    )
                                }

                                Divider(color = Color(50, 50, 55), thickness = 1.dp)

                                // VISUAL COLOR PICKER: Hue, Saturation, Value sliders (Figma Style)
                                Text(Locales.translate("visual_color_mixer", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                                // 1. Hue Spectrum Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row {
                                        Text(Locales.translate("hue", lang), fontSize = 10.sp, color = Color.LightGray)
                                        Spacer(Modifier.weight(1f))
                                        Text("${hue.roundToInt()}°", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    val hueColors = listOf(
                                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                    UpgradedSlider(
                                        value = hue,
                                        onValueChange = { newHue ->
                                            hue = newHue
                                            val updatedColor = hsvToColor(hue, saturation, value)
                                            updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                        },
                                        range = 0f..360f,
                                        trackBrush = Brush.horizontalGradient(hueColors)
                                    )
                                }

                                // 2. Saturation Slider (Gradually saturates the color)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row {
                                        Text(Locales.translate("saturation", lang), fontSize = 10.sp, color = Color.LightGray)
                                        Spacer(Modifier.weight(1f))
                                        Text("${(saturation * 100).roundToInt()}%", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    val satStart = hsvToColor(hue, 0f, value)
                                    val satEnd = hsvToColor(hue, 1f, value)
                                    UpgradedSlider(
                                        value = saturation,
                                        onValueChange = { newSat ->
                                            saturation = newSat
                                            val updatedColor = hsvToColor(hue, saturation, value)
                                            updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                        },
                                        range = 0f..1f,
                                        trackBrush = Brush.horizontalGradient(listOf(satStart, satEnd))
                                    )
                                }

                                // 3. Brightness / Value Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row {
                                        Text(Locales.translate("brightness", lang), fontSize = 10.sp, color = Color.LightGray)
                                        Spacer(Modifier.weight(1f))
                                        Text("${(value * 100).roundToInt()}%", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    val valStart = hsvToColor(hue, saturation, 0f)
                                    val valEnd = hsvToColor(hue, saturation, 1f)
                                    UpgradedSlider(
                                        value = value,
                                        onValueChange = { newVal ->
                                            value = newVal
                                            val updatedColor = hsvToColor(hue, saturation, value)
                                            updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                        },
                                        range = 0f..1f,
                                        trackBrush = Brush.horizontalGradient(listOf(valStart, valEnd))
                                    )
                                }

                                // RGB & Hex input panel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val r = (currentStop.color.red * 255).roundToInt()
                                    val g = (currentStop.color.green * 255).roundToInt()
                                    val b = (currentStop.color.blue * 255).roundToInt()

                                    // Hex input (Figma style)
                                    var hexText by remember(currentStop.color) { mutableStateOf(colorToHex(currentStop.color)) }
                                    Column(
                                        modifier = Modifier.weight(1.3f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        BasicTextField(
                                            value = hexText,
                                            onValueChange = { input ->
                                                hexText = input
                                                val parsed = hexToColor(input)
                                                if (parsed != null) {
                                                    updateStopColor(stops, selectedStopIndex, parsed) { stops = it }
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp),
                                            cursorBrush = SolidColor(Color(0, 162, 255)),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(20, 20, 24), RoundedCornerShape(4.dp))
                                                        .border(1.dp, Color(70, 70, 75), RoundedCornerShape(4.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(Locales.translate("hex", lang), fontSize = 8.sp, color = Color.Gray)
                                    }

                                    RGBFieldDark("R", r) { nv -> updateStopColor(stops, selectedStopIndex, Color(nv, g, b)) { stops = it } }
                                    RGBFieldDark("G", g) { nv -> updateStopColor(stops, selectedStopIndex, Color(r, nv, b)) { stops = it } }
                                    RGBFieldDark("B", b) { nv -> updateStopColor(stops, selectedStopIndex, Color(r, g, nv)) { stops = it } }
                                }
                            }
                        }
                    }

                    // Save / Cancel Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text(Locales.translate("cancel", lang), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                val result = stops.joinToString(";") { 
                                    "${it.position}:${(it.color.red * 255).roundToInt()},${(it.color.green * 255).roundToInt()},${(it.color.blue * 255).roundToInt()}" 
                                }
                                onSave(result)
                                onDismiss() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(Locales.translate("save_close", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradientPickerDialogOld(
    initialColorString: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    lang: String = "vi"
) {
    // Parse initial string: "r,g,b to r,g,b" or "pos:r,g,b;pos:r,g,b"
    val initialStops = remember(initialColorString) {
        try {
            if (initialColorString.contains(";")) {
                initialColorString.split(";").map { part ->
                    val segments = part.split(":")
                    val pos = segments[0].toFloat()
                    val rgb = segments[1].split(",").map { it.trim().toInt() }
                    GradientStop(pos, Color(rgb[0], rgb[1], rgb[2]))
                }
            } else if (initialColorString.contains(" to ")) {
                val parts = initialColorString.split(" to ")
                val c1 = parts[0].split(",").map { it.trim().toInt() }
                val c2 = parts[1].split(",").map { it.trim().toInt() }
                listOf(
                    GradientStop(0f, Color(c1[0], c1[1], c1[2])),
                    GradientStop(1f, Color(c2[0], c2[1], c2[2]))
                )
            } else {
                listOf(GradientStop(0f, Color.White), GradientStop(1f, Color.Black))
            }
        } catch (e: Exception) {
            listOf(GradientStop(0f, Color.White), GradientStop(1f, Color.Black))
        }
    }

    var stops by remember { mutableStateOf(initialStops.sortedBy { it.position }) }
    var selectedStopIndex by remember { mutableStateOf(0) }

    // Custom dark Figma-like design dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFFEC4899))),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Advanced Gradient Stop Editor", 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Gradient Showcase Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(60, 60, 65)),
                    colors = CardDefaults.cardColors(containerColor = Color(28, 28, 32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Gradient Preview", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        
                        // Main Gradient Strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colorStops = stops.map { it.position to it.color }.toTypedArray()
                                    )
                                )
                                .border(1.dp, Color(50, 50, 55), RoundedCornerShape(8.dp))
                        )
                    }
                }

                // Interactive Stops Control Strip (DPI-Responsive & Drag-Optimized)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(60, 60, 65)),
                    colors = CardDefaults.cardColors(containerColor = Color(20, 20, 24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gradient Stops Track", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Spacer(Modifier.weight(1f))
                            Text("Click track to add • Drag nodes", fontSize = 10.sp, color = Color(0, 162, 255))
                        }
                        
                        // Interactive slider track
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .pointerInput(stops) {
                                    detectTapGestures { offset ->
                                        val w = size.width.toFloat()
                                        val pos = (offset.x / w).coerceIn(0f, 1f)
                                        // Check if clicked near an existing stop
                                        val threshold = 0.05f
                                        val existing = stops.find { Math.abs(it.position - pos) < threshold }
                                        if (existing == null) {
                                            val newColor = interpolateColor(stops, pos)
                                            val updated = (stops + GradientStop(pos, newColor)).sortedBy { it.position }
                                            stops = updated
                                            selectedStopIndex = updated.indexOfFirst { Math.abs(it.position - pos) < 0.001f }
                                        } else {
                                            selectedStopIndex = stops.indexOf(existing)
                                        }
                                    }
                                }
                        ) {
                            val trackWidthPx = constraints.maxWidth.toFloat()
                            val trackWidthDp = maxWidth
                            
                            // Visual horizontal timeline track line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(45, 45, 50))
                            )

                            // Render stops markers
                            stops.forEachIndexed { index, stop ->
                                val isSelected = index == selectedStopIndex
                                val offsetDp = (stop.position * trackWidthDp.value).dp - 12.dp
                                
                                Box(
                                    modifier = Modifier
                                        .offset(x = offsetDp)
                                        .size(24.dp)
                                        .align(Alignment.CenterStart)
                                        .pointerInput(index) {
                                            detectDragGestures(
                                                onDragStart = { selectedStopIndex = index },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val deltaPct = dragAmount.x / trackWidthPx
                                                    val rawPos = (stops[index].position + deltaPct).coerceIn(0f, 1f)
                                                    
                                                    // Maintain order or re-sort
                                                    val updatedStops = stops.toMutableList().apply {
                                                        this[index] = this[index].copy(position = rawPos)
                                                    }.sortedBy { it.position }
                                                    
                                                    stops = updatedStops
                                                    selectedStopIndex = updatedStops.indexOfFirst { 
                                                        Math.abs(it.position - rawPos) < 0.005f && it.color == stop.color 
                                                    }.coerceIn(0, updatedStops.size - 1)
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Glow or circle ring
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                if (isSelected) Color(0, 162, 255) else Color(40, 40, 45),
                                                CircleShape
                                            )
                                            .border(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) Color.White else Color.Gray,
                                                CircleShape
                                            )
                                    )
                                    
                                    // Color sample dot inside
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(stop.color, CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        // Stop selection chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            stops.forEachIndexed { index, stop ->
                                val isSelected = index == selectedStopIndex
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0, 162, 255).copy(alpha = 0.2f) else Color(30, 30, 35))
                                        .border(1.dp, if (isSelected) Color(0, 162, 255) else Color(50, 50, 55), RoundedCornerShape(8.dp))
                                        .clickable { selectedStopIndex = index }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(stop.color, CircleShape)
                                            .border(1.dp, Color.White.copy(0.4f), CircleShape)
                                    )
                                    Text(
                                        text = "Point ${index + 1} (${(stop.position * 100).roundToInt()}%)",
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected Stop Color Customization Area
                if (selectedStopIndex in stops.indices) {
                    val currentStop = stops[selectedStopIndex]
                    
                    // Parse RGB to HSV for visual picker
                    val hsv = remember(currentStop.color) { colorToHSV(currentStop.color) }
                    var hue by remember(currentStop.color) { mutableStateOf(hsv[0]) }
                    var saturation by remember(currentStop.color) { mutableStateOf(hsv[1]) }
                    var value by remember(currentStop.color) { mutableStateOf(hsv[2]) }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(65, 65, 70)),
                        colors = CardDefaults.cardColors(containerColor = Color(28, 28, 32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp), 
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header of current stop settings
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(currentStop.color, RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(4.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Stop Color Settings (${selectedStopIndex + 1})", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Spacer(Modifier.weight(1f))
                                if (stops.size > 2) {
                                    IconButton(
                                        onClick = {
                                            stops = stops.toMutableList().apply { removeAt(selectedStopIndex) }
                                            selectedStopIndex = 0
                                        }, 
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Precise Stop Position Input / Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop Position", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(Modifier.weight(1f))
                                    Text("${(currentStop.position * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0, 162, 255), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = currentStop.position,
                                    onValueChange = { newPos ->
                                        stops = stops.toMutableList().apply {
                                            this[selectedStopIndex] = this[selectedStopIndex].copy(position = newPos)
                                        }.sortedBy { it.position }
                                        selectedStopIndex = stops.indexOfFirst { 
                                            Math.abs(it.position - newPos) < 0.005f && it.color == currentStop.color 
                                        }.coerceIn(0, stops.size - 1)
                                    },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0, 162, 255),
                                        inactiveTrackColor = Color(55, 55, 60),
                                        thumbColor = Color.White
                                    )
                                )
                            }

                            Divider(color = Color(50, 50, 55), thickness = 1.dp)

                            // VISUAL COLOR PICKER: Hue, Saturation, Value sliders (Figma Style)
                            Text("Visual Color Mixer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                            // 1. Hue Spectrum Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row {
                                    Text("Hue", fontSize = 10.sp, color = Color.LightGray)
                                    Spacer(Modifier.weight(1f))
                                    Text("${hue.roundToInt()}°", fontSize = 10.sp, color = Color.Gray)
                                }
                                val hueColors = listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                                UpgradedSlider(
                                    value = hue,
                                    onValueChange = { newHue ->
                                        hue = newHue
                                        val updatedColor = hsvToColor(hue, saturation, value)
                                        updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                    },
                                    range = 0f..360f,
                                    trackBrush = Brush.horizontalGradient(hueColors)
                                )
                            }

                            // 2. Saturation Slider (Gradually saturates the color)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row {
                                    Text("Saturation", fontSize = 10.sp, color = Color.LightGray)
                                    Spacer(Modifier.weight(1f))
                                    Text("${(saturation * 100).roundToInt()}%", fontSize = 10.sp, color = Color.Gray)
                                }
                                val satStart = hsvToColor(hue, 0f, value)
                                val satEnd = hsvToColor(hue, 1f, value)
                                UpgradedSlider(
                                    value = saturation,
                                    onValueChange = { newSat ->
                                        saturation = newSat
                                        val updatedColor = hsvToColor(hue, saturation, value)
                                        updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                    },
                                    range = 0f..1f,
                                    trackBrush = Brush.horizontalGradient(listOf(satStart, satEnd))
                                )
                            }

                            // 3. Brightness / Value Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row {
                                    Text("Brightness", fontSize = 10.sp, color = Color.LightGray)
                                    Spacer(Modifier.weight(1f))
                                    Text("${(value * 100).roundToInt()}%", fontSize = 10.sp, color = Color.Gray)
                                }
                                val valStart = hsvToColor(hue, saturation, 0f)
                                val valEnd = hsvToColor(hue, saturation, 1f)
                                UpgradedSlider(
                                    value = value,
                                    onValueChange = { newVal ->
                                        value = newVal
                                        val updatedColor = hsvToColor(hue, saturation, value)
                                        updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                    },
                                    range = 0f..1f,
                                    trackBrush = Brush.horizontalGradient(listOf(valStart, valEnd))
                                )
                            }

                            // RGB & Hex input panel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val r = (currentStop.color.red * 255).roundToInt()
                                val g = (currentStop.color.green * 255).roundToInt()
                                val b = (currentStop.color.blue * 255).roundToInt()

                                // Hex input (Figma style)
                                var hexText by remember(currentStop.color) { mutableStateOf(colorToHex(currentStop.color)) }
                                OutlinedTextField(
                                    value = hexText,
                                    onValueChange = { input ->
                                        hexText = input
                                        val parsed = hexToColor(input)
                                        if (parsed != null) {
                                            updateStopColor(stops, selectedStopIndex, parsed) { stops = it }
                                        }
                                    },
                                    label = { Text("HEX", fontSize = 9.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0, 162, 255),
                                        unfocusedBorderColor = Color(70, 70, 75),
                                        focusedContainerColor = Color(20, 20, 24),
                                        unfocusedContainerColor = Color(20, 20, 24)
                                    ),
                                    modifier = Modifier.weight(1.3f)
                                )

                                RGBFieldDark("R", r) { nv -> updateStopColor(stops, selectedStopIndex, Color(nv, g, b)) { stops = it } }
                                RGBFieldDark("G", g) { nv -> updateStopColor(stops, selectedStopIndex, Color(r, nv, b)) { stops = it } }
                                RGBFieldDark("B", b) { nv -> updateStopColor(stops, selectedStopIndex, Color(r, g, nv)) { stops = it } }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val result = stops.joinToString(";") { 
                        "${it.position}:${(it.color.red * 255).roundToInt()},${(it.color.green * 255).roundToInt()},${(it.color.blue * 255).roundToInt()}" 
                    }
                    onSave(result)
                    onDismiss() 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Gradient", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        },
        containerColor = Color(24, 24, 28)
    )
}

@Composable
fun UpgradedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedRange<Float>,
    trackBrush: Brush,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(5.dp))
                .background(trackBrush)
                .border(1.dp, Color(60, 60, 65), RoundedCornerShape(5.dp))
        )

        // Thumb
        val progress = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        val thumbOffset = (progress * maxWidth.value).dp - 8.dp
        
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .align(Alignment.CenterStart)
                .background(Color.White, CircleShape)
                .border(2.dp, Color(0, 162, 255), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaPercent = dragAmount.x / widthPx
                        val newValue = (value + deltaPercent * (range.endInclusive - range.start))
                            .coerceIn(range.start, range.endInclusive)
                        onValueChange(newValue)
                    }
                }
        )
    }
}

@Composable
fun RGBFieldDark(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var textState by remember(value) { mutableStateOf(value.toString()) }
    Column(
        modifier = Modifier.width(46.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicTextField(
            value = textState,
            onValueChange = {
                textState = it
                val nv = it.toIntOrNull()?.coerceIn(0, 255)
                if (nv != null) {
                    onValueChange(nv)
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            cursorBrush = SolidColor(Color(0, 162, 255)),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(20, 20, 24), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(70, 70, 75), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 8.sp, color = Color.Gray)
    }
}

// HSV / Hex Helper converters
private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val x = c * (1 - Math.abs((hue / 60f) % 2 - 1))
    val m = value - c
    val (r, g, b) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        ((r + m) * 255f).roundToInt().coerceIn(0, 255),
        ((g + m) * 255f).roundToInt().coerceIn(0, 255),
        ((b + m) * 255f).roundToInt().coerceIn(0, 255)
    )
}

private fun colorToHSV(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    var h = 0f
    if (delta > 0f) {
        h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        h *= 60f
        if (h < 0f) h += 360f
    }

    val s = if (max == 0f) 0f else delta / max
    val v = max

    return floatArrayOf(h, s, v)
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun hexToColor(hex: String): Color? {
    val sanitized = hex.trim().removePrefix("#")
    if (sanitized.length != 6) return null
    return try {
        val r = sanitized.substring(0, 2).toInt(16)
        val g = sanitized.substring(2, 4).toInt(16)
        val b = sanitized.substring(4, 6).toInt(16)
        Color(r, g, b)
    } catch (e: Exception) {
        null
    }
}

data class GradientStop(val position: Float, val color: Color)

private fun interpolateColor(stops: List<GradientStop>, pos: Float): Color {
    if (stops.isEmpty()) return Color.White
    if (pos <= stops.first().position) return stops.first().color
    if (pos >= stops.last().position) return stops.last().color

    for (i in 0 until stops.size - 1) {
        val s1 = stops[i]
        val s2 = stops[i+1]
        if (pos >= s1.position && pos <= s2.position) {
            val t = (pos - s1.position) / (s2.position - s1.position)
            return Color(
                red = s1.color.red + (s2.color.red - s1.color.red) * t,
                green = s1.color.green + (s2.color.green - s1.color.green) * t,
                blue = s1.color.blue + (s2.color.blue - s1.color.blue) * t,
                alpha = s1.color.alpha + (s2.color.alpha - s1.color.alpha) * t
            )
        }
    }
    return Color.White
}

private fun updateStopColor(stops: List<GradientStop>, index: Int, color: Color, onUpdate: (List<GradientStop>) -> Unit) {
    val newList = stops.toMutableList()
    newList[index] = newList[index].copy(color = color)
    onUpdate(newList)
}
