package com.example.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.ProjectEntity
import com.example.data.model.RobloxClass
import com.example.data.model.RobloxObject
import kotlin.math.min
import kotlin.math.roundToInt

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
    val uiScalePercent by viewModel.uiScalePercent.collectAsState()
    val studioTheme by viewModel.studioTheme.collectAsState()
    val baseDensity = LocalDensity.current
    val densityScale = (uiScalePercent / 100f).coerceIn(0.4f, 1.4f)
    val themeColors = remember(studioTheme) { studioThemePalette(studioTheme) }

    // Dialog trigger states
    var showInsertDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenProjectDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCompactMenu by remember { mutableStateOf(false) }
    var generatedLuauCode by remember { mutableStateOf("") }
    var generatedRojoBundle by remember { mutableStateOf("") }
    var explorerMinimized by remember(isCompact) { mutableStateOf(false) }
    var propertiesMinimized by remember(isCompact) { mutableStateOf(false) }

    // Canvas panning/zooming state
    val explorerPanelWidth = if (isCompact) 216.dp else 248.dp
    val propertiesPanelWidth = if (isCompact) 256.dp else 304.dp
    val collapsedRailWidth = 52.dp
    val logicalSidePanelReserve = if (!isPreviewMode) {
        (if (explorerMinimized) 52 else if (isCompact) 216 else 248) +
            (if (propertiesMinimized) 52 else if (isCompact) 256 else 304)
    } else {
        0
    }
    val sidePanelReserve = (logicalSidePanelReserve * densityScale).roundToInt()
    val bottomReserve = (28 * densityScale).roundToInt()
    val topReserve = ((if (isTopbarVisible) 56 else 8) * densityScale).roundToInt()
    val fitCanvasScale = min(
        ((configuration.screenWidthDp - sidePanelReserve).coerceAtLeast(320)).toFloat() / screenWidth.coerceAtLeast(1).toFloat(),
        ((configuration.screenHeightDp - topReserve - bottomReserve).coerceAtLeast(240)).toFloat() / screenHeight.coerceAtLeast(1).toFloat()
    ).coerceIn(0.2f, 1.2f)
    var canvasScale by remember { mutableStateOf(fitCanvasScale) }
    var canvasOffsetX by remember { mutableStateOf(0f) }
    var canvasOffsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp, screenWidth, screenHeight, isCompact) {
        canvasScale = fitCanvasScale
        canvasOffsetX = 0f
        canvasOffsetY = 0f
    }

    fun openExportDialog() {
        generatedLuauCode = LuauGenerator.generate(rootObj)
        generatedRojoBundle = LuauGenerator.generateRojoBundle(rootObj, projectName)
        showExportDialog = true
    }

    // Scaffold with a clean dark theme
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * densityScale,
            fontScale = baseDensity.fontScale
        )
    ) {
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopbarVisible) {
                StudioTopBar(
                    projectName = projectName,
                    devicePreview = devicePreview,
                    isCompact = isCompact,
                    savedProjects = savedProjects,
                    showGrid = showGrid,
                    isPreviewMode = isPreviewMode,
                    showCompactMenu = showCompactMenu,
                    themeColors = themeColors,
                    onCompactMenuChange = { showCompactMenu = it },
                    onNewProject = { showNewProjectDialog = true },
                    onSaveProject = { viewModel.saveProjectToLocal() },
                    onOpenProject = { showOpenProjectDialog = true },
                    onExport = { openExportDialog() },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onToggleGrid = { viewModel.setShowGrid(!showGrid) },
                    onSettings = { viewModel.setShowSettingsDialog(true) },
                    onTogglePreview = { viewModel.togglePreviewMode() },
                    onLoadProject = { viewModel.loadProject(it) }
                )
            }
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Content Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!isPreviewMode) {
                    if (explorerMinimized) {
                        StudioSidebarRail(
                            iconRes = R.drawable.vanilla_action_explorer,
                            label = "Explorer",
                            expandIcon = Icons.Default.ChevronRight,
                            onExpand = { explorerMinimized = false },
                            modifier = Modifier.width(collapsedRailWidth)
                        )
                    } else {
                        DexExplorerPanel(
                            root = rootObj,
                            selectedId = selectedId,
                            onSelect = { viewModel.selectObject(it) },
                            onDelete = { viewModel.deleteObject(it) },
                            onDuplicate = { viewModel.duplicateObject(it) },
                            onRename = { id, name -> viewModel.renameObject(id, name) },
                            onMove = { id, up -> viewModel.moveObjectInHierarchy(id, up) },
                            onCopy = { viewModel.copyObject(it) },
                            onPaste = { viewModel.pasteObject(it) },
                            onReparent = { id, targetParentId -> viewModel.reparentObject(id, targetParentId) },
                            onInsertChild = { parentId, className -> viewModel.insertObjectInto(parentId, className) },
                            onOpenScript = { viewModel.openScriptEditor(it) },
                            onToggleDragMode = { viewModel.setUseSingleDragMode(!useSingleDragMode) },
                            onMinimize = { explorerMinimized = true },
                            modifier = Modifier
                                .width(explorerPanelWidth)
                                .fillMaxHeight()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                            .fillMaxHeight()
                        .background(themeColors.background)
                        .border(0.5.dp, Color(0xFF3D4148))
                        .clipToBounds()
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
                                .align(Alignment.Center)
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

                        // Floating topbar toggle. Settings moved to the macOS-style settings window.
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
                            IconButton(onClick = { canvasScale = fitCanvasScale; canvasOffsetX = 0f; canvasOffsetY = 0f }, modifier = Modifier.size(44.dp)) {
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

                if (!isPreviewMode) {
                    if (propertiesMinimized) {
                        StudioSidebarRail(
                            iconRes = R.drawable.vanilla_action_properties,
                            label = "Properties",
                            expandIcon = Icons.Default.ChevronLeft,
                            onExpand = { propertiesMinimized = false },
                            modifier = Modifier.width(collapsedRailWidth)
                        )
                    } else {
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
                            lang = language,
                            onMinimize = { propertiesMinimized = true },
                            modifier = Modifier
                                .width(propertiesPanelWidth)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            StudioStatusBar(
                selectedObj = selectedObj,
                projectName = projectName,
                devicePreview = devicePreview,
                canvasScale = canvasScale,
                showGrid = showGrid,
                snapToGrid = snapToGrid
            )
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
            rojoBundle = generatedRojoBundle,
            uiScalePercent = uiScalePercent,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showSettingsDialog) {
        MacSettingsDialog(
            useSingleDragMode = useSingleDragMode,
            isTopbarVisible = isTopbarVisible,
            showGrid = showGrid,
            snapToGrid = snapToGrid,
            gridSize = gridSize,
            uiScalePercent = uiScalePercent,
            studioTheme = studioTheme,
            onUseSingleDragModeChange = { viewModel.setUseSingleDragMode(it) },
            onTopbarVisibleChange = { viewModel.setTopbarVisible(it) },
            onShowGridChange = { viewModel.setShowGrid(it) },
            onSnapToGridChange = { viewModel.setSnapToGrid(it) },
            onGridSizeChange = { viewModel.setGridSize(it) },
            onUiScalePercentChange = { viewModel.setUiScalePercent(it) },
            onThemeChange = { viewModel.setStudioTheme(it) },
            onResetUi = {
                viewModel.setUiScalePercent(40)
                viewModel.setStudioTheme("Studio Dark")
            },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
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
}

private data class StudioThemeColors(
    val name: String,
    val topBar: Color,
    val background: Color,
    val panel: Color,
    val accent: Color
)

private fun studioThemePalette(theme: String): StudioThemeColors {
    return when (theme) {
        "Midnight" -> StudioThemeColors(theme, Color(0xFF111827), Color(0xFF0B1020), Color(0xFF151B2D), Color(0xFF7DD3FC))
        "Graphite" -> StudioThemeColors(theme, Color(0xFF242424), Color(0xFF181818), Color(0xFF2B2B2B), Color(0xFFB7C0CC))
        "Vanilla Blue" -> StudioThemeColors(theme, Color(0xFF172233), Color(0xFF111820), Color(0xFF202A36), Color(0xFF38BDF8))
        else -> StudioThemeColors("Studio Dark", Color(0xFF202124), Color(0xFF18191C), Color(0xFF25272C), Color(0xFF8FBFF8))
    }
}

@Composable
private fun StudioTopBar(
    projectName: String,
    devicePreview: String,
    isCompact: Boolean,
    savedProjects: List<ProjectEntity>,
    showGrid: Boolean,
    isPreviewMode: Boolean,
    showCompactMenu: Boolean,
    themeColors: StudioThemeColors,
    onCompactMenuChange: (Boolean) -> Unit,
    onNewProject: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenProject: () -> Unit,
    onExport: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleGrid: () -> Unit,
    onSettings: () -> Unit,
    onTogglePreview: () -> Unit,
    onLoadProject: (ProjectEntity) -> Unit
) {
    Surface(
        color = themeColors.topBar,
        border = BorderStroke(0.5.dp, Color(0xFF343842))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 52.dp else 56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ProjectTitleMenu(
                    projectName = projectName,
                    devicePreview = devicePreview,
                    isCompact = isCompact,
                    savedProjects = savedProjects,
                    accentColor = themeColors.accent,
                    onNewProject = onNewProject,
                    onOpenProject = onOpenProject,
                    onSaveProject = onSaveProject,
                    onExport = onExport,
                    onLoadProject = onLoadProject
                )
            }

            ToolbarIconButton(
                iconRes = R.drawable.vanilla_action_new,
                text = "New",
                onClick = onNewProject
            )
            ToolbarIconButton(
                iconRes = R.drawable.vanilla_action_save,
                text = "Save",
                onClick = onSaveProject
            )

            if (!isCompact) {
                ToolbarIconButton(
                    iconRes = R.drawable.vanilla_action_open,
                    text = "Open",
                    onClick = onOpenProject
                )
                ToolbarIconButton(
                    iconRes = R.drawable.vanilla_action_code,
                    text = "Export",
                    onClick = onExport
                )

                StudioToolbarDivider()

                ToolbarIconButton(
                    iconRes = R.drawable.vanilla_action_undo,
                    text = "Undo",
                    onClick = onUndo
                )
                ToolbarIconButton(
                    iconRes = R.drawable.vanilla_action_redo,
                    text = "Redo",
                    onClick = onRedo
                )

                StudioToolbarDivider()

                ToolbarIconToggle(
                    iconRes = R.drawable.vanilla_action_grid,
                    checked = showGrid,
                    text = "Grid",
                    onCheckedChange = { onToggleGrid() }
                )
                ToolbarIconButton(
                    iconRes = R.drawable.vanilla_action_settings,
                    text = "Settings",
                    onClick = onSettings
                )
            } else {
                Box {
                    ToolbarIconButton(
                        icon = Icons.Default.MoreVert,
                        text = "More",
                        onClick = { onCompactMenuChange(true) }
                    )
                    DropdownMenu(
                        expanded = showCompactMenu,
                        onDismissRequest = { onCompactMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(painterResource(R.drawable.vanilla_action_open), null, Modifier.size(20.dp))
                            },
                            text = { Text("Open") },
                            onClick = {
                                onOpenProject()
                                onCompactMenuChange(false)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(painterResource(R.drawable.vanilla_action_code), null, Modifier.size(20.dp))
                            },
                            text = { Text("Export Luau") },
                            onClick = {
                                onExport()
                                onCompactMenuChange(false)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(painterResource(R.drawable.vanilla_action_undo), null, Modifier.size(20.dp))
                            },
                            text = { Text("Undo") },
                            onClick = {
                                onUndo()
                                onCompactMenuChange(false)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(painterResource(R.drawable.vanilla_action_redo), null, Modifier.size(20.dp))
                            },
                            text = { Text("Redo") },
                            onClick = {
                                onRedo()
                                onCompactMenuChange(false)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(painterResource(R.drawable.vanilla_action_grid), null, Modifier.size(20.dp))
                            },
                            text = { Text(if (showGrid) "Hide Grid" else "Show Grid") },
                            onClick = {
                                onToggleGrid()
                                onCompactMenuChange(false)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Image(painterResource(R.drawable.vanilla_action_settings), null, Modifier.size(20.dp))
                            },
                            text = { Text("Settings") },
                            onClick = {
                                onSettings()
                                onCompactMenuChange(false)
                            }
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onTogglePreview,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPreviewMode) Color(0xFF1F7A4D) else Color(0xFF32353B),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .padding(start = 4.dp)
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
        }
    }
}

@Composable
private fun ProjectTitleMenu(
    projectName: String,
    devicePreview: String,
    isCompact: Boolean,
    savedProjects: List<ProjectEntity>,
    accentColor: Color,
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onSaveProject: () -> Unit,
    onExport: () -> Unit,
    onLoadProject: (ProjectEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.vanilla_studio),
                contentDescription = "Vanilla Studio",
                modifier = Modifier.size(if (isCompact) 22.dp else 28.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "ROBLOX STUDIO UI",
                    color = Color(0xFFE7EAEE),
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
                if (!isCompact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = projectName,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  |  $devicePreview  |  Landscape",
                            color = Color(0xFFA6ACB3),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Project menu", tint = Color(0xFFA6ACB3), modifier = Modifier.size(18.dp))
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                leadingIcon = { Image(painterResource(R.drawable.vanilla_action_new), null, Modifier.size(18.dp)) },
                text = { Text("Tạo project mới") },
                onClick = {
                    expanded = false
                    onNewProject()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Image(painterResource(R.drawable.vanilla_action_open), null, Modifier.size(18.dp)) },
                text = { Text("Mở project manager") },
                onClick = {
                    expanded = false
                    onOpenProject()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Image(painterResource(R.drawable.vanilla_action_save), null, Modifier.size(18.dp)) },
                text = { Text("Lưu project hiện tại") },
                onClick = {
                    expanded = false
                    onSaveProject()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Image(painterResource(R.drawable.vanilla_action_code), null, Modifier.size(18.dp)) },
                text = { Text("Export Luau / Rojo") },
                onClick = {
                    expanded = false
                    onExport()
                }
            )
            if (savedProjects.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "RECENT PROJECTS",
                    color = Color(0xFF8C929C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                savedProjects.take(6).forEach { project ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(project.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(project.description, fontSize = 10.sp, color = Color(0xFF8C929C), maxLines = 1)
                            }
                        },
                        onClick = {
                            expanded = false
                            onLoadProject(project)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MacSettingsDialog(
    useSingleDragMode: Boolean,
    isTopbarVisible: Boolean,
    showGrid: Boolean,
    snapToGrid: Boolean,
    gridSize: Int,
    uiScalePercent: Int,
    studioTheme: String,
    onUseSingleDragModeChange: (Boolean) -> Unit,
    onTopbarVisibleChange: (Boolean) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onSnapToGridChange: (Boolean) -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onUiScalePercentChange: (Int) -> Unit,
    onThemeChange: (String) -> Unit,
    onResetUi: () -> Unit,
    onDismiss: () -> Unit
) {
    var section by remember { mutableStateOf("General") }
    val accent = studioThemePalette(studioTheme).accent
    val contentScroll = rememberScrollState()
    val themeScroll = rememberScrollState()

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.86f)
                .widthIn(max = 760.dp)
                .heightIn(min = 420.dp, max = 640.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
            color = Color(0xFF202124),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, Color(0xFF3D4148))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF2B2D33))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MacTrafficDot(Color(0xFFFF5F57))
                    Spacer(modifier = Modifier.width(8.dp))
                    MacTrafficDot(Color(0xFFFFBD2E))
                    Spacer(modifier = Modifier.width(8.dp))
                    MacTrafficDot(Color(0xFF28C840))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("UI $uiScalePercent%", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Settings", tint = Color(0xFFA6ACB3), modifier = Modifier.size(18.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF25272C))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("General", "Theme", "DPI", "Grid", "Render", "Export").forEach { item ->
                            MacSettingsSidebarItem(
                                label = item,
                                selected = section == item,
                                accent = accent,
                                onClick = { section = item }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onResetUi,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Reset UI", fontSize = 11.sp)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(contentScroll)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(section, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        when (section) {
                            "Theme" -> {
                                Text("Chọn theme editor", color = Color(0xFFA6ACB3), fontSize = 12.sp)
                                Row(
                                    modifier = Modifier.horizontalScroll(themeScroll),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf("Studio Dark", "Midnight", "Graphite", "Vanilla Blue").forEach { theme ->
                                        ThemeChoiceChip(
                                            label = theme,
                                            selected = studioTheme == theme,
                                            color = studioThemePalette(theme).accent,
                                            onClick = { onThemeChange(theme) }
                                        )
                                    }
                                }
                            }
                            "DPI" -> {
                                SettingsInfoRow("UI scale", "Default 40%. Chỉnh % giao diện cho từng máy, tablet hoặc phone landscape.")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Slider(
                                        value = uiScalePercent.toFloat(),
                                        onValueChange = { raw ->
                                            onUiScalePercentChange((raw / 5f).roundToInt() * 5)
                                        },
                                        valueRange = 40f..140f,
                                        steps = 19,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                                    )
                                    Text("$uiScalePercent%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                                }
                                SettingsInfoRow("Fullscreen", "System status/navigation bar được ẩn bằng immersive mode.")
                                SettingsInfoRow("Canvas fit", "Viewport fit tính theo DPI scale để không chừa dư khoảng trống.")
                            }
                            "Grid" -> {
                                MacSettingsToggle("Show grid", "Hiện lưới trong viewport.", showGrid, onShowGridChange)
                                MacSettingsToggle("Snap to grid", "Gắn position/size theo grid khi kéo.", snapToGrid, onSnapToGridChange)
                                SettingsInfoRow("Grid size", "$gridSize px")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { onGridSizeChange((gridSize - 4).coerceAtLeast(4)) }) { Text("-") }
                                    Slider(
                                        value = gridSize.toFloat(),
                                        onValueChange = { onGridSizeChange((it / 4f).roundToInt() * 4) },
                                        valueRange = 4f..64f,
                                        steps = 14,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                                    )
                                    OutlinedButton(onClick = { onGridSizeChange((gridSize + 4).coerceAtMost(64)) }) { Text("+") }
                                }
                            }
                            "Render" -> {
                                SettingsInfoRow("Viewport renderer", "High-detail grid, safe area, animated 3D cube và viewport chrome.")
                                SettingsInfoRow("Render target", "Landscape canvas optimized for Roblox Studio style editing.")
                                SettingsInfoRow("Preview scale", "Canvas zoom và DPI scale tách riêng để edit chính xác hơn.")
                                SettingsInfoRow("Mobile support", "Touch pan/zoom, compact sidebars, minimized rails.")
                            }
                            "Export" -> {
                                SettingsInfoRow("Export window", "Custom GUI có topbar, close, copy/share và tabs Luau/Rojo.")
                                SettingsInfoRow("Luau tab", "Runtime LocalScript xuất trực tiếp để paste vào Roblox Studio.")
                                SettingsInfoRow("Rojo tab", "Bundle file structure cho project Rojo.")
                                SettingsInfoRow("DPI preview", "Code preview giữ font readable kể cả UI 40%.")
                            }
                            else -> {
                                MacSettingsToggle("Single drag mode", "Khóa camera khi chọn object, kéo góc để resize.", useSingleDragMode, onUseSingleDragModeChange)
                                MacSettingsToggle("Show topbar", "Ẩn/hiện topbar để lấy thêm không gian edit.", isTopbarVisible, onTopbarVisibleChange)
                                SettingsInfoRow("Floating settings", "Cửa sổ này có thể kéo bằng topbar và không bo góc.")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacTrafficDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun MacSettingsSidebarItem(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) Color.White else Color(0xFFA6ACB3), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun MacSettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(Color(0xFF2B2D33), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Color(0xFFA6ACB3), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.82f)
        )
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2D33), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color(0xFFA6ACB3), fontSize = 11.sp, modifier = Modifier.weight(1.25f))
    }
}

@Composable
private fun ThemeChoiceChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .background(if (selected) color.copy(alpha = 0.2f) else Color(0xFF2B2D33), RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) color else Color(0xFF3D4148), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun StudioSidebarRail(
    iconRes: Int,
    label: String,
    expandIcon: ImageVector,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF25272C))
            .border(1.dp, Color(0xFF3D4148))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onExpand,
            modifier = Modifier.size(48.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(22.dp)
                )
                Icon(
                    imageVector = expandIcon,
                    contentDescription = null,
                    tint = Color(0xFFA6ACB3),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioStatusBar(
    selectedObj: RobloxObject?,
    projectName: String,
    devicePreview: String,
    canvasScale: Float,
    showGrid: Boolean,
    snapToGrid: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFF202124))
            .border(0.5.dp, Color(0xFF3D4148))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = projectName,
            color = Color(0xFFE7EAEE),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = selectedObj?.let { "${it.className.name}: ${it.name}" } ?: "No selection",
            color = Color(0xFFA6ACB3),
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.weight(1.4f)
        )
        Text(devicePreview, color = Color(0xFFA6ACB3), fontSize = 10.sp)
        Text("Zoom ${(canvasScale * 100).toInt()}%", color = Color(0xFFA6ACB3), fontSize = 10.sp)
        Text(
            text = if (showGrid) "Grid on" else "Grid off",
            color = if (showGrid) Color(0xFF8FBFF8) else Color(0xFFA6ACB3),
            fontSize = 10.sp
        )
        Text(
            text = if (snapToGrid) "Snap on" else "Snap off",
            color = if (snapToGrid) Color(0xFF8FBFF8) else Color(0xFFA6ACB3),
            fontSize = 10.sp
        )
    }
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
