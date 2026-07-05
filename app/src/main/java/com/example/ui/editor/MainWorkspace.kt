package com.example.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RobloxClass
import com.example.data.model.RobloxObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWorkspace(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 900 || configuration.screenHeightDp < 520
    
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
    val activeScriptId by viewModel.activeScriptId.collectAsState()

    val useSingleDragMode by viewModel.useSingleDragMode.collectAsState()
    val isTopbarVisible by viewModel.isTopbarVisible.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val language by viewModel.language.collectAsState()

    // Dialog trigger states
    var showInsertDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenProjectDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCompactMenu by remember { mutableStateOf(false) }
    var generatedLuauCode by remember { mutableStateOf("") }

    // Canvas panning/zooming state
    var canvasScale by remember(isCompact) { mutableStateOf(if (isCompact) 0.38f else 0.6f) }
    var canvasOffsetX by remember { mutableStateOf(0f) }
    var canvasOffsetY by remember { mutableStateOf(0f) }

    var activeTabMobile by remember { mutableStateOf("Viewport") }

    // Scaffold with a clean dark theme
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopbarVisible) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.vanilla_studio),
                                contentDescription = "Vanilla Studio",
                                modifier = Modifier
                                    .size(if (isCompact) 22.dp else 28.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "VANILLA STUDIO UI",
                                    color = Color(0xFFE7EAEE),
                                    fontSize = if (isCompact) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp
                                )
                                if (!isCompact) {
                                    Text(
                                        text = "$projectName  |  $devicePreview",
                                        color = Color(0xFF8FBFF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        ToolbarIconButton(
                            iconRes = R.drawable.vanilla_action_new,
                            text = "New",
                            onClick = { showNewProjectDialog = true }
                        )
                        ToolbarIconButton(
                            iconRes = R.drawable.vanilla_action_save,
                            text = "Save",
                            onClick = { viewModel.saveProjectToLocal() }
                        )

                        if (!isCompact) {
                            ToolbarIconButton(
                                iconRes = R.drawable.vanilla_action_open,
                                text = "Open",
                                onClick = { showOpenProjectDialog = true }
                            )
                            ToolbarIconButton(
                                iconRes = R.drawable.vanilla_action_code,
                                text = "Export",
                                onClick = {
                                    generatedLuauCode = LuauGenerator.generate(rootObj)
                                    showExportDialog = true
                                }
                            )

                            StudioToolbarDivider()

                            ToolbarIconButton(
                                iconRes = R.drawable.vanilla_action_undo,
                                text = "Undo",
                                onClick = { viewModel.undo() }
                            )
                            ToolbarIconButton(
                                iconRes = R.drawable.vanilla_action_redo,
                                text = "Redo",
                                onClick = { viewModel.redo() }
                            )

                            StudioToolbarDivider()

                            ToolbarIconToggle(
                                iconRes = R.drawable.vanilla_action_grid,
                                checked = showGrid,
                                text = "Grid",
                                onCheckedChange = { viewModel.setShowGrid(it) }
                            )
                            ToolbarIconButton(
                                iconRes = R.drawable.vanilla_action_settings,
                                text = "Settings",
                                onClick = { viewModel.setShowSettingsDialog(true) }
                            )
                        } else {
                            Box {
                                ToolbarIconButton(
                                    icon = Icons.Default.MoreVert,
                                    text = "More",
                                    onClick = { showCompactMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showCompactMenu,
                                    onDismissRequest = { showCompactMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Image(painterResource(R.drawable.vanilla_action_open), null, Modifier.size(20.dp))
                                        },
                                        text = { Text("Open") },
                                        onClick = {
                                            showOpenProjectDialog = true
                                            showCompactMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Image(painterResource(R.drawable.vanilla_action_code), null, Modifier.size(20.dp))
                                        },
                                        text = { Text("Export Luau") },
                                        onClick = {
                                            generatedLuauCode = LuauGenerator.generate(rootObj)
                                            showExportDialog = true
                                            showCompactMenu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Image(painterResource(R.drawable.vanilla_action_undo), null, Modifier.size(20.dp))
                                        },
                                        text = { Text("Undo") },
                                        onClick = {
                                            viewModel.undo()
                                            showCompactMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Image(painterResource(R.drawable.vanilla_action_redo), null, Modifier.size(20.dp))
                                        },
                                        text = { Text("Redo") },
                                        onClick = {
                                            viewModel.redo()
                                            showCompactMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Image(painterResource(R.drawable.vanilla_action_grid), null, Modifier.size(20.dp))
                                        },
                                        text = { Text(if (showGrid) "Hide Grid" else "Show Grid") },
                                        onClick = {
                                            viewModel.setShowGrid(!showGrid)
                                            showCompactMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Image(painterResource(R.drawable.vanilla_action_settings), null, Modifier.size(20.dp))
                                        },
                                        text = { Text("Settings") },
                                        onClick = {
                                            viewModel.setShowSettingsDialog(true)
                                            showCompactMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.togglePreviewMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPreviewMode) Color(0xFF1F7A4D) else Color(0xFF32353B),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            if (!isCompact) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPreviewMode) "Preview" else "Edit",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF202124),
                        titleContentColor = Color(0xFFE7EAEE)
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        containerColor = Color(0xFF18191C)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (!isTopbarVisible) Modifier.statusBarsPadding() else Modifier)
        ) {
            // Main Content Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 1. Left Panel (Dex Explorer hierarchy)
                // Visible on Desktop if not in preview mode, or on Mobile if active tab is Explorer
                if ((!isCompact && !isPreviewMode) || (isCompact && activeTabMobile == "Explorer")) {
                    Box(
                        modifier = Modifier
                            .then(if (!isCompact) Modifier.width(210.dp) else Modifier.weight(1f))
                            .fillMaxHeight()
                    ) {
                        DexExplorerPanel(
                            root = rootObj,
                            selectedId = selectedId,
                            onSelect = { 
                                viewModel.selectObject(it)
                                if (isCompact) {
                                    activeTabMobile = "Viewport"
                                }
                            },
                            onDelete = { viewModel.deleteObject(it) },
                            onDuplicate = { viewModel.duplicateObject(it) },
                            onRename = { id, name -> viewModel.renameObject(id, name) },
                            onMove = { id, up -> viewModel.moveObjectInHierarchy(id, up) },
                            onCopy = { viewModel.copyObject(it) },
                            onPaste = { viewModel.pasteObject(it) },
                            onOpenScript = { viewModel.openScriptEditor(it) },
                            onToggleDragMode = { viewModel.setUseSingleDragMode(!useSingleDragMode) }
                        )
                    }
                }

                // 2. Center Workspace Area (The Viewport canvas)
                // Always visible on Desktop, or on Mobile if active tab is Viewport
                if (!isCompact || activeTabMobile == "Viewport") {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF191B1F))
                            .border(0.5.dp, Color(0xFF3D4148))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        viewModel.selectObject(null)
                                    }
                                )
                            }
                            .pointerInput(selectedId, useSingleDragMode) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (zoom != 1.0f) {
                                        // Deselect selected object when starting zoom
                                        viewModel.selectObject(null)
                                    }
                                    
                                    // Lock camera movement if single-drag mode is activated and an object is selected
                                    val isCameraLocked = selectedId != null && useSingleDragMode
                                    if (!isCameraLocked) {
                                        canvasScale = (canvasScale * zoom).coerceIn(0.2f, 3.0f)
                                        canvasOffsetX += pan.x
                                        canvasOffsetY += pan.y
                                    }
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
                                onMoveOrResize = { id, pos, size -> viewModel.updateTransform(id, pos, size) },
                                screenWidth = screenWidth,
                                screenHeight = screenHeight,
                                scaleFactor = canvasScale,
                                showGrid = showGrid,
                                snapToGrid = snapToGrid,
                                gridSize = gridSize,
                                isPreviewMode = isPreviewMode,
                                useSingleDragMode = useSingleDragMode,
                                onToggleDragMode = { viewModel.setUseSingleDragMode(!useSingleDragMode) }
                            )
                        }

                        // Floating quick settings overlay at the top-right corner of the workspace
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color(0xDD25272C), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF3D4148), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.setTopbarVisible(!isTopbarVisible) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTopbarVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Topbar",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setShowSettingsDialog(true) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.vanilla_action_settings),
                                        contentDescription = "Open Settings",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Bottom control panel (Zoom and Device selector)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(Color(0xEE25272C), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF3D4148), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Zoom UI
                            Text("Scale ${(canvasScale * 100).toInt()}%", color = Color(0xFFE7EAEE), fontSize = 11.sp)
                            IconButton(onClick = { canvasScale = (canvasScale + 0.1f).coerceIn(0.2f, 3.0f) }, modifier = Modifier.size(44.dp)) {
                                Image(painterResource(R.drawable.vanilla_action_zoom_in), contentDescription = "Zoom In", modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { canvasScale = (canvasScale - 0.1f).coerceIn(0.2f, 3.0f) }, modifier = Modifier.size(44.dp)) {
                                Image(painterResource(R.drawable.vanilla_action_zoom_out), contentDescription = "Zoom Out", modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { canvasScale = if (isCompact) 0.38f else 0.6f; canvasOffsetX = 0f; canvasOffsetY = 0f }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset View", tint = Color(0xFFE7EAEE), modifier = Modifier.size(20.dp))
                            }

                            VerticalDivider(modifier = Modifier.height(14.dp), color = Color.Gray)

                            // Device Preview drop-down selection
                            var showDevices by remember { mutableStateOf(false) }
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showDevices = true }
                                ) {
                                    Image(painterResource(R.drawable.vanilla_action_device), contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(devicePreview, fontSize = 11.sp, color = Color(0xFFE7EAEE), fontWeight = FontWeight.Bold)
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
                                    .background(Color(0xF225272C), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF3D4148), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showInsertDialog = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Image(painterResource(R.drawable.vanilla_action_insert), contentDescription = "Insert Object", modifier = Modifier.size(24.dp))
                                }
                                
                                VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 4.dp), color = Color.Gray)

                                IconButton(
                                    onClick = { selectedId?.let { viewModel.duplicateObject(it) } },
                                    enabled = selectedId != null && selectedId != rootObj.id,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Image(painterResource(R.drawable.vanilla_action_duplicate), contentDescription = "Duplicate", modifier = Modifier.size(22.dp), alpha = if (selectedId != null && selectedId != rootObj.id) 1f else 0.38f)
                                }
                                IconButton(
                                    onClick = { selectedId?.let { viewModel.deleteObject(it) } },
                                    enabled = selectedId != null && selectedId != rootObj.id,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Image(painterResource(R.drawable.vanilla_action_delete), contentDescription = "Delete", modifier = Modifier.size(22.dp), alpha = if (selectedId != null && selectedId != rootObj.id) 1f else 0.38f)
                                }
                            }
                        }
                    }
                }

                // 3. Right Panel (Properties Inspector)
                // Visible on Desktop if not in preview mode, or on Mobile if active tab is Properties
                if ((!isCompact && !isPreviewMode) || (isCompact && activeTabMobile == "Properties")) {
                    Box(
                        modifier = Modifier
                            .then(if (!isCompact) Modifier.width(230.dp) else Modifier.weight(1f))
                            .fillMaxHeight()
                    ) {
                        val parentName = selectedId?.let { id ->
                            findParentNameInTree(rootObj, id)
                        }
                        PropertiesPanel(
                            selectedObj = selectedObj,
                            parentName = parentName,
                            onUpdateProperty = { id, name, value ->
                                if (name == "Name" && value is String) {
                                    viewModel.renameObject(id, value)
                                } else {
                                    viewModel.updateProperty(id, name, value)
                                }
                            },
                            onConvertOffsetToScale = { viewModel.convertOffsetToScale(it) },
                            onConvertScaleToOffset = { viewModel.convertScaleToOffset(it) },
                            onApplyAnchorPreset = { id, pr -> viewModel.applyAnchorPreset(id, pr) },
                            onOpenScript = { viewModel.openScriptEditor(it) },
                            lang = language
                        )
                    }
                }
            }

            // Bottom Navigation tabs on small screens (DPI-adaptive)
            if (isCompact) {
                NavigationBar(
                    containerColor = Color(0xFF202124),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = activeTabMobile == "Explorer",
                        onClick = { activeTabMobile = "Explorer" },
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.vanilla_action_explorer),
                                contentDescription = "Explorer",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Explorer", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFF8FBFF8),
                            indicatorColor = Color(0xFF0A84FF).copy(alpha = 0.18f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color(0xFFA6ACB3)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTabMobile == "Viewport",
                        onClick = { activeTabMobile = "Viewport" },
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.vanilla_action_ui),
                                contentDescription = "Viewport",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Viewport", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFF8FBFF8),
                            indicatorColor = Color(0xFF0A84FF).copy(alpha = 0.18f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color(0xFFA6ACB3)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTabMobile == "Properties",
                        onClick = { activeTabMobile = "Properties" },
                        icon = {
                            Image(
                                painter = painterResource(id = R.drawable.vanilla_action_properties),
                                contentDescription = "Properties",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Properties", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFF8FBFF8),
                            indicatorColor = Color(0xFF0A84FF).copy(alpha = 0.18f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color(0xFFA6ACB3)
                        )
                    )
                }
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

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowSettingsDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0, 162, 255),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cài Đặt Workspace",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Chế độ Kéo Đơn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chế độ Kéo Đơn (4 góc)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Khóa di chuyển camera khi chọn UI. Kéo 4 góc để co giãn tự do.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = useSingleDragMode,
                            onCheckedChange = { viewModel.setUseSingleDragMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0, 162, 255)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    // 2. Hiện Topbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hiển thị thanh công cụ (Topbar)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ẩn đi để tăng không gian thiết kế.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isTopbarVisible,
                            onCheckedChange = { viewModel.setTopbarVisible(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0, 162, 255)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    // 3. Grid Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Căn theo lưới (Snap to Grid)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tự động gắn tọa độ khi di chuyển/co giãn.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = snapToGrid,
                            onCheckedChange = { viewModel.setSnapToGrid(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0, 162, 255)
                            )
                        )
                    }
                    
                    if (snapToGrid) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kích thước Lưới (pixels)",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { viewModel.setGridSize((gridSize - 4).coerceAtLeast(4)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(40, 40, 45)),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("-", color = Color.White)
                                }
                                Text(
                                    text = "$gridSize px",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Button(
                                    onClick = { viewModel.setGridSize((gridSize + 4).coerceAtMost(64)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(40, 40, 45)),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+", color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setShowSettingsDialog(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
                ) {
                    Text("Hoàn tất", color = Color.White)
                }
            },
            containerColor = Color(24, 24, 28),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    activeScriptId?.let { scriptId ->
        viewModel.findObjectById(rootObj, scriptId)?.let { scriptObj ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1E1E)
            ) {
                VSCodeEditorScreen(
                    scriptName = scriptObj.name,
                    className = scriptObj.className,
                    initialSource = scriptObj.properties["Source"] as? String ?: "",
                    onSave = { newSrc -> viewModel.updateProperty(scriptId, "Source", newSrc) },
                    onBack = { viewModel.closeScriptEditor() }
                )
            }
        }
    }
    } // Close root Box
}

// Utility: Toolbar icon button
@Composable
fun ToolbarIconButton(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    text: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(22.dp)
                )
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = text, tint = Color(0xFFE7EAEE), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text, fontSize = 9.sp, color = Color(0xFFA6ACB3), fontWeight = FontWeight.SemiBold)
        }
    }
}

// Utility: Toolbar toggle button
@Composable
fun ToolbarIconToggle(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit
) {
    IconButton(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.size(48.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(22.dp),
                    alpha = if (checked) 1f else 0.45f
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (checked) Color(0xFF8FBFF8) else Color(0xFFA6ACB3),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text, fontSize = 9.sp, color = if (checked) Color(0xFF8FBFF8) else Color(0xFFA6ACB3), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StudioToolbarDivider() {
    Spacer(modifier = Modifier.width(4.dp))
    VerticalDivider(modifier = Modifier.height(32.dp), color = Color(0xFF3D4148))
    Spacer(modifier = Modifier.width(4.dp))
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
