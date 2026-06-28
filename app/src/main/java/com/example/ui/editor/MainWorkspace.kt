package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.model.RobloxClass
import com.example.data.model.RobloxObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWorkspace(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ViewModel Observables
    val rootObj by viewModel.rootObject.collectAsState()
    val selectedId by viewModel.selectedObjectId.collectAsState()
    val selectedObj by viewModel.selectedObject.collectAsState()
    
    val projectName by viewModel.currentProjectName.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState()
    val snapToGrid by viewModel.snapToGrid.collectAsState()
    val gridSize by viewModel.gridSize.collectAsState()
    val devicePreview by viewModel.devicePreviewType.collectAsState()
    val screenWidth by viewModel.screenWidth.collectAsState()
    val screenHeight by viewModel.screenHeight.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()

    // Dialog trigger states
    var showInsertDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenProjectDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var generatedLuauCode by remember { mutableStateOf("") }

    // Canvas panning/zooming state
    var canvasScale by remember { mutableStateOf(0.6f) }
    var canvasOffsetX by remember { mutableStateOf(0f) }
    var canvasOffsetY by remember { mutableStateOf(0f) }

    // Floating toolbar quick actions
    var showQuickMenu by remember { mutableStateOf(true) }

    // Scaffold with a clean dark theme
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Custom Roblox-like Cube Logo
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(230, 126, 34), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("R", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ROBLOX UI DESIGNER",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Active: $projectName ($devicePreview)",
                                color = Color(0, 162, 255),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    // Toolbar controls
                    ToolbarIconButton(
                        icon = Icons.Default.AddBox,
                        text = "New",
                        onClick = { showNewProjectDialog = true }
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Folder,
                        text = "Open",
                        onClick = { showOpenProjectDialog = true }
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Save,
                        text = "Save",
                        onClick = { viewModel.saveProjectToLocal() }
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Code,
                        text = "Export",
                        onClick = {
                            generatedLuauCode = LuauGenerator.generate(rootObj)
                            showExportDialog = true
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.width(6.dp))

                    ToolbarIconButton(
                        icon = Icons.Default.Undo,
                        text = "Undo",
                        onClick = { viewModel.undo() }
                    )
                    ToolbarIconButton(
                        icon = Icons.Default.Redo,
                        text = "Redo",
                        onClick = { viewModel.redo() }
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.width(6.dp))

                    // Grid toggles
                    ToolbarIconToggle(
                        icon = Icons.Default.GridOn,
                        checked = showGrid,
                        text = "Grid",
                        onCheckedChange = { viewModel.setShowGrid(it) }
                    )
                    
                    // Edit/Preview selector
                    Button(
                        onClick = { viewModel.togglePreviewMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPreviewMode) Color(46, 204, 113) else Color(50, 50, 55)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPreviewMode) "Previewing" else "Editing",
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(20, 20, 22),
                    titleContentColor = Color.White
                ),
                modifier = Modifier.height(48.dp)
            )
        },
        containerColor = Color(15, 15, 15)
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left Panel (Dex Explorer hierarchy)
            AnimatedVisibility(
                visible = !isPreviewMode,
                modifier = Modifier
                    .width(210.dp)
                    .fillMaxHeight()
            ) {
                DexExplorerPanel(
                    root = rootObj,
                    selectedId = selectedId,
                    onSelect = { viewModel.selectObject(it) },
                    onDelete = { viewModel.deleteObject(it) },
                    onDuplicate = { viewModel.duplicateObject(it) },
                    onRename = { id, name -> viewModel.renameObject(id, name) },
                    onMove = { id, up -> viewModel.moveObjectInHierarchy(id, up) },
                    onCopy = { viewModel.copyObject(it) },
                    onPaste = { viewModel.pasteObject(it) }
                )
            }

            // Center Workspace Area (The Viewport canvas)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(18, 18, 18))
                    .border(0.5.dp, Color(40, 40, 45))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            canvasScale = (canvasScale * zoom).coerceIn(0.2f, 3.0f)
                            canvasOffsetX += pan.x
                            canvasOffsetY += pan.y
                        }
                    }
            ) {
                // Interactive background viewport
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = canvasScale,
                            scaleY = canvasScale,
                            translationX = canvasOffsetX,
                            translationY = canvasOffsetY
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    RobloxCanvasPreview(
                        root = rootObj,
                        selectedId = selectedId,
                        onSelect = { viewModel.selectObject(it) },
                        onMoveOrResize = { id, pos, size -> viewModel.updateProperty(id, "Position", pos); viewModel.updateProperty(id, "Size", size) },
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        scaleFactor = canvasScale,
                        showGrid = showGrid,
                        gridSize = gridSize,
                        isPreviewMode = isPreviewMode
                    )
                }

                // Interactive Overlays
                
                // Bottom control panel (Zoom and Device selector)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color(30, 30, 35, 220), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(60, 60, 65, 150), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom UI
                    Text("Scale: ${(canvasScale * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                    IconButton(onClick = { canvasScale = (canvasScale + 0.1f).coerceIn(0.2f, 3.0f) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { canvasScale = (canvasScale - 0.1f).coerceIn(0.2f, 3.0f) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { canvasScale = 0.6f; canvasOffsetX = 0f; canvasOffsetY = 0f }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset View", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    VerticalDivider(modifier = Modifier.height(14.dp), color = Color.Gray)

                    // Device Preview drop-down selection
                    var showDevices by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showDevices = true }
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0, 162, 255), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(devicePreview, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = showDevices, onDismissRequest = { showDevices = false }) {
                            listOf("Phone 16:9", "Phone 20:9", "Tablet", "Desktop").forEach { dev ->
                                DropdownMenuItem(
                                    text = { Text(dev, fontSize = 11.sp) },
                                    onClick = { viewModel.setDevicePreview(dev); showDevices = false }
                                )
                            }
                        }
                    }
                }

                // Quick Floating Mini Toolbar (Duplicate, Delete, Insert Object etc.)
                if (!isPreviewMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .background(Color(25, 25, 30, 240), RoundedCornerShape(30.dp))
                            .border(1.dp, Color(60, 60, 65, 180), RoundedCornerShape(30.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showInsertDialog = true },
                            modifier = Modifier.size(28.dp).background(Color(0, 162, 255), RoundedCornerShape(100.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Insert Object", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        
                        VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 4.dp), color = Color.Gray)

                        IconButton(
                            onClick = { selectedId?.let { viewModel.duplicateObject(it) } },
                            enabled = selectedId != null && selectedId != rootObj.id,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ControlPointDuplicate, contentDescription = "Duplicate", tint = if (selectedId != null && selectedId != rootObj.id) Color.White else Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { selectedId?.let { viewModel.deleteObject(it) } },
                            enabled = selectedId != null && selectedId != rootObj.id,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (selectedId != null && selectedId != rootObj.id) Color.Red else Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Right Panel (Properties Inspector)
            AnimatedVisibility(
                visible = !isPreviewMode,
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
            ) {
                val parentName = selectedId?.let { id ->
                    findParentNameInTree(rootObj, id)
                }
                PropertiesPanel(
                    selectedObj = selectedObj,
                    parentName = parentName,
                    onUpdateProperty = { id, name, value -> viewModel.updateProperty(id, name, value) },
                    onConvertOffsetToScale = { viewModel.convertOffsetToScale(it) },
                    onConvertScaleToOffset = { viewModel.convertScaleToOffset(it) },
                    onApplyAnchorPreset = { id, pr -> viewModel.applyAnchorPreset(id, pr) }
                )
            }
        }
    }

    // Modal dialogues
    if (showInsertDialog) {
        InsertObjectDialog(
            onDismiss = { showInsertDialog = false },
            onInsert = { viewModel.insertObject(it) }
        )
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, temp -> viewModel.createNewProject(name, temp) }
        )
    }

    if (showOpenProjectDialog) {
        OpenProjectDialog(
            projects = savedProjects,
            onDismiss = { showOpenProjectDialog = false },
            onLoadProject = { viewModel.loadProject(it) },
            onDeleteProject = { viewModel.deleteProject(it) }
        )
    }

    if (showExportDialog) {
        ExportLuauDialog(
            luauCode = generatedLuauCode,
            onDismiss = { showExportDialog = false }
        )
    }
}

// Utility: Toolbar icon button
@Composable
fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = text, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(1.dp))
            Text(text, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

// Utility: Toolbar toggle button
@Composable
fun ToolbarIconToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit
) {
    IconButton(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.size(34.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (checked) Color(0, 162, 255) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(text, fontSize = 7.sp, color = if (checked) Color(0, 162, 255) else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

// Utility function to locate a parent's name
private fun findParentNameInTree(root: RobloxObject, childId: String): String? {
    if (root.id == childId) return "nil"
    for (child in root.children) {
        if (child.id == childId) return root.name
        val name = findParentNameInTree(child, childId)
        if (name != null) return name
    }
    return null
}
