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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val arrowControlsEnabled by viewModel.arrowControlsEnabled.collectAsState()
    val arrowStepPx by viewModel.arrowStepPx.collectAsState()
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
    var showMainMenu by remember { mutableStateOf(true) }
    var activeScrollModeId by remember { mutableStateOf<String?>(null) }
    val activeScrollObj = remember(rootObj, activeScrollModeId) {
        activeScrollModeId
            ?.let { viewModel.findObjectById(rootObj, it) }
            ?.takeIf { it.className == RobloxClass.ScrollingFrame }
    }
    LaunchedEffect(activeScrollModeId, activeScrollObj) {
        if (activeScrollModeId != null && activeScrollObj == null) {
            activeScrollModeId = null
        }
    }
    val activeCanvasRoot = activeScrollObj ?: rootObj
    val activeCanvasWidth = remember(activeScrollObj, screenWidth) {
        activeScrollObj?.let { scrollObj ->
            val size = scrollObj.properties["Size"] as? com.example.data.model.UDim2
            (((size?.scaleX ?: 1f) * screenWidth) + (size?.offsetX ?: 0))
                .roundToInt()
                .coerceAtLeast(screenWidth)
        } ?: screenWidth
    }
    val activeCanvasHeight = remember(activeScrollObj, screenHeight) {
        activeScrollObj?.let { scrollObj ->
            val size = scrollObj.properties["Size"] as? com.example.data.model.UDim2
            val frameHeight = (((size?.scaleY ?: 1f) * screenHeight) + (size?.offsetY ?: 0)).coerceAtLeast(160f)
            val canvasSize = scrollObj.properties["CanvasSize"] as? com.example.data.model.UDim2
            (((canvasSize?.scaleY ?: 2f) * frameHeight) + (canvasSize?.offsetY ?: 0))
                .roundToInt()
                .coerceAtLeast(frameHeight.roundToInt())
                .coerceAtLeast(screenHeight)
        } ?: screenHeight
    }

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
        ((configuration.screenWidthDp - sidePanelReserve).coerceAtLeast(320)).toFloat() / activeCanvasWidth.coerceAtLeast(1).toFloat(),
        ((configuration.screenHeightDp - topReserve - bottomReserve).coerceAtLeast(240)).toFloat() / activeCanvasHeight.coerceAtLeast(1).toFloat()
    ).coerceIn(0.2f, 1.2f)
    var canvasScale by remember { mutableStateOf(fitCanvasScale) }
    var canvasOffsetX by remember { mutableStateOf(0f) }
    var canvasOffsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp, activeCanvasWidth, activeCanvasHeight, isCompact) {
        canvasScale = fitCanvasScale
        canvasOffsetX = 0f
        canvasOffsetY = 0f
    }

    fun openExportDialog() {
        generatedLuauCode = LuauGenerator.generate(
            root = rootObj,
            canvasWidth = screenWidth,
            canvasHeight = screenHeight
        )
        generatedRojoBundle = LuauGenerator.generateRojoBundle(
            root = rootObj,
            projectName = projectName,
            canvasWidth = screenWidth,
            canvasHeight = screenHeight
        )
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
	        if (showMainMenu) {
	            ProjectLauncherScreen(
	                projectName = projectName,
	                savedProjects = savedProjects,
	                themeColors = themeColors,
	                onContinue = { showMainMenu = false },
	                onNewProject = { showNewProjectDialog = true },
	                onOpenProject = { showOpenProjectDialog = true },
	                onLoadProject = {
	                    viewModel.loadProject(it)
	                    activeScrollModeId = null
	                    showMainMenu = false
	                }
	            )
	        } else {
	        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopbarVisible) {
                StudioTopBar(
                    projectName = projectName,
                    isCompact = isCompact,
                    savedProjects = savedProjects,
	                    showGrid = showGrid,
	                    isPreviewMode = isPreviewMode,
	                    showCompactMenu = showCompactMenu,
	                    themeColors = themeColors,
	                    canUndo = canUndo,
	                    canRedo = canRedo,
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
	                        StudioTextureOverlay(
	                            modifier = Modifier.matchParentSize(),
	                            accent = themeColors.accent
	                        )
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
	                                root = activeCanvasRoot,
	                                selectedId = selectedId,
	                                onSelect = { viewModel.selectObject(it) },
	                                onMoveOrResize = { id, pos, size -> viewModel.updateTransform(id, pos, size) },
	                                screenWidth = activeCanvasWidth,
	                                screenHeight = activeCanvasHeight,
	                                scaleFactor = canvasScale,
	                                showGrid = showGrid,
	                                snapToGrid = snapToGrid,
	                                gridSize = gridSize,
	                                isPreviewMode = isPreviewMode,
	                                useSingleDragMode = useSingleDragMode,
	                                onToggleDragMode = { viewModel.setUseSingleDragMode(!useSingleDragMode) },
	                                onOpenScrollMode = { id ->
	                                    activeScrollModeId = id
	                                    canvasScale = fitCanvasScale
	                                    canvasOffsetX = 0f
	                                    canvasOffsetY = 0f
	                                }
	                            )
	                        }

	                        CanvasModeTabs(
	                            activeScrollObj = activeScrollObj,
	                            accent = themeColors.accent,
	                            onRootMode = { activeScrollModeId = null },
	                            onCloseScrollMode = { activeScrollModeId = null },
	                            modifier = Modifier
	                                .align(Alignment.TopStart)
	                                .padding(10.dp)
	                        )

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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.vanilla_action_device), contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Roblox Scale", fontSize = 11.sp, color = Color(0xFFE7EAEE), fontWeight = FontWeight.Bold)
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

	                        if (!isPreviewMode && arrowControlsEnabled && selectedObj?.properties?.containsKey("Position") == true && selectedObj?.properties?.containsKey("Size") == true) {
	                            TransformNudgePad(
	                                stepPx = arrowStepPx,
	                                accent = themeColors.accent,
	                                onMove = { dx, dy -> viewModel.nudgeSelectedObject(dx, dy) },
	                                onResize = { dw, dh -> viewModel.resizeSelectedObject(dw, dh) },
	                                modifier = Modifier
	                                    .align(Alignment.BottomEnd)
	                                    .padding(12.dp)
	                            )
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
	                canvasScale = canvasScale,
	                canvasModeLabel = activeScrollObj?.let { "Scrolling: ${it.name}" } ?: "ScreenGui",
	                showGrid = showGrid,
                snapToGrid = snapToGrid
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
	            onCreate = { name, temp ->
	                viewModel.createNewProject(name, temp)
	                activeScrollModeId = null
	                showMainMenu = false
	            }
	        )
    }

    if (showOpenProjectDialog) {
        OpenProjectDialog(
	            projects = savedProjects,
	            onDismiss = { showOpenProjectDialog = false },
	            onLoadProject = {
	                viewModel.loadProject(it)
	                activeScrollModeId = null
	                showMainMenu = false
	            },
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
	            arrowControlsEnabled = arrowControlsEnabled,
	            arrowStepPx = arrowStepPx,
	            onUseSingleDragModeChange = { viewModel.setUseSingleDragMode(it) },
            onTopbarVisibleChange = { viewModel.setTopbarVisible(it) },
            onShowGridChange = { viewModel.setShowGrid(it) },
            onSnapToGridChange = { viewModel.setSnapToGrid(it) },
            onGridSizeChange = { viewModel.setGridSize(it) },
	            onUiScalePercentChange = { viewModel.setUiScalePercent(it) },
	            onThemeChange = { viewModel.setStudioTheme(it) },
	            onArrowControlsEnabledChange = { viewModel.setArrowControlsEnabled(it) },
	            onArrowStepPxChange = { viewModel.setArrowStepPx(it) },
	            onResetUi = {
	                viewModel.setUiScalePercent(40)
	                viewModel.setStudioTheme("Studio Dark")
	                viewModel.setArrowControlsEnabled(true)
	                viewModel.setArrowStepPx(5)
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
                    contextClassName = viewModel.findParentOfObject(rootObj, scriptId)?.className,
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
private fun ProjectLauncherScreen(
    projectName: String,
    savedProjects: List<ProjectEntity>,
    themeColors: StudioThemeColors,
    onContinue: () -> Unit,
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onLoadProject: (ProjectEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101318))
    ) {
        StudioTextureOverlay(modifier = Modifier.matchParentSize(), accent = themeColors.accent)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight()
                    .background(Color(0xD9141820), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(8.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.vanilla_studio),
                        contentDescription = "Vanilla",
                        modifier = Modifier.size(58.dp)
                    )
                    Text("Vanilla Studio UI", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Project menu", color = themeColors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onContinue,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue: $projectName", maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onNewProject,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenProject,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open", fontSize = 12.sp)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.38f)
                    .fillMaxHeight()
                    .background(Color(0xD91A2029), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF303743), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Projects", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${savedProjects.size} saved", color = Color(0xFF8E96A3), fontSize = 11.sp)
                }
                if (savedProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF11151B), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No local projects yet", color = Color(0xFF8E96A3), fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        savedProjects.take(8).forEach { project ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .background(Color(0xFF11151B), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(6.dp))
                                    .clickable { onLoadProject(project) }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(project.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(project.description, color = Color(0xFF8E96A3), fontSize = 9.sp, maxLines = 1)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E96A3), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTextureOverlay(
    modifier: Modifier = Modifier,
    accent: Color
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF151922), Color(0xFF101319)),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        val lineColor = accent.copy(alpha = 0.045f)
        val darkLine = Color.Black.copy(alpha = 0.12f)
        var x = -size.height
        while (x < size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, size.height),
                end = Offset(x + size.height, 0f),
                strokeWidth = 1f
            )
            x += 28f
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = darkLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += 32f
        }
    }
}

@Composable
private fun CanvasModeTabs(
    activeScrollObj: RobloxObject?,
    accent: Color,
    onRootMode: () -> Unit,
    onCloseScrollMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xEE171C23), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF303743), RoundedCornerShape(6.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CanvasModeTab(label = "ScreenGui", selected = activeScrollObj == null, accent = accent, onClick = onRootMode)
        activeScrollObj?.let { scrollObj ->
            CanvasModeTab(
                label = "Scrolling: ${scrollObj.name}",
                selected = true,
                accent = Color(0xFF28C840),
                onClick = {}
            )
            IconButton(onClick = onCloseScrollMode, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Scrolling Mode", tint = Color(0xFFC6CED8), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun CanvasModeTab(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.copy(alpha = 0.2f) else Color.Transparent,
        contentColor = if (selected) Color.White else Color(0xFFA6ACB3),
        shape = RoundedCornerShape(4.dp),
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.5f)) else null
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TransformNudgePad(
    stepPx: Int,
    accent: Color,
    onMove: (Int, Int) -> Unit,
    onResize: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(178.dp)
            .background(Color(0xF0181C23), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF303743), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Step $stepPx px", color = Color(0xFFC6CED8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ArrowCluster(title = "Move", accent = accent, onUp = { onMove(0, -stepPx) }, onDown = { onMove(0, stepPx) }, onLeft = { onMove(-stepPx, 0) }, onRight = { onMove(stepPx, 0) }, modifier = Modifier.weight(1f))
            SizeCluster(title = "Size", accent = accent, onWidthDown = { onResize(-stepPx, 0) }, onWidthUp = { onResize(stepPx, 0) }, onHeightDown = { onResize(0, -stepPx) }, onHeightUp = { onResize(0, stepPx) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArrowCluster(
    title: String,
    accent: Color,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color(0xFF8E96A3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        TinyIconButton(Icons.Default.KeyboardArrowUp, accent, onUp)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TinyIconButton(Icons.Default.KeyboardArrowLeft, accent, onLeft)
            TinyIconButton(Icons.Default.KeyboardArrowRight, accent, onRight)
        }
        TinyIconButton(Icons.Default.KeyboardArrowDown, accent, onDown)
    }
}

@Composable
private fun SizeCluster(
    title: String,
    accent: Color,
    onWidthDown: () -> Unit,
    onWidthUp: () -> Unit,
    onHeightDown: () -> Unit,
    onHeightUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color(0xFF8E96A3), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TinyIconButton(Icons.Default.Remove, accent, onWidthDown)
            TinyIconButton(Icons.Default.Add, accent, onWidthUp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TinyTextButton("H-", accent, onHeightDown)
            TinyTextButton("H+", accent, onHeightUp)
        }
    }
}

@Composable
private fun TinyIconButton(icon: ImageVector, accent: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TinyTextButton(text: String, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = accent.copy(alpha = 0.15f),
        contentColor = Color.White,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.size(width = 28.dp, height = 28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudioTopBar(
    projectName: String,
    isCompact: Boolean,
    savedProjects: List<ProjectEntity>,
    showGrid: Boolean,
	    isPreviewMode: Boolean,
	    showCompactMenu: Boolean,
	    themeColors: StudioThemeColors,
	    canUndo: Boolean,
	    canRedo: Boolean,
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
        color = Color(0xFF111419),
        border = BorderStroke(0.5.dp, Color(0xFF252B34))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 48.dp else 52.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF111419), themeColors.topBar.copy(alpha = 0.82f), Color(0xFF111419))
                    )
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ProjectTitleMenu(
                    projectName = projectName,
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
	                    enabled = canUndo,
	                    onClick = onUndo
	                )
	                ToolbarIconButton(
	                    iconRes = R.drawable.vanilla_action_redo,
	                    text = "Redo",
	                    enabled = canRedo,
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
                    containerColor = if (isPreviewMode) Color(0xFF176B45) else Color(0xFF202631),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .height(36.dp)
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
                            text = "  |  Roblox Scale  |  Scale Export",
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
	    arrowControlsEnabled: Boolean,
	    arrowStepPx: Int,
	    onUseSingleDragModeChange: (Boolean) -> Unit,
    onTopbarVisibleChange: (Boolean) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onSnapToGridChange: (Boolean) -> Unit,
    onGridSizeChange: (Int) -> Unit,
	    onUiScalePercentChange: (Int) -> Unit,
	    onThemeChange: (String) -> Unit,
	    onArrowControlsEnabledChange: (Boolean) -> Unit,
	    onArrowStepPxChange: (Int) -> Unit,
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
	                        listOf("General", "Theme", "DPI", "Controls", "Grid", "Render", "Export").forEach { item ->
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
	                            "Controls" -> {
	                                MacSettingsToggle("Arrow move/resize", "Hiện pad mũi tên để dịch chuyển và resize bằng bước cố định.", arrowControlsEnabled, onArrowControlsEnabledChange)
	                                SettingsInfoRow("Arrow step", "$arrowStepPx px")
	                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
	                                    OutlinedButton(onClick = { onArrowStepPxChange((arrowStepPx - 1).coerceAtLeast(1)) }) { Text("-") }
	                                    Slider(
	                                        value = arrowStepPx.toFloat(),
	                                        onValueChange = { onArrowStepPxChange(it.roundToInt()) },
	                                        valueRange = 1f..64f,
	                                        steps = 62,
	                                        modifier = Modifier.weight(1f),
	                                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
	                                    )
	                                    OutlinedButton(onClick = { onArrowStepPxChange((arrowStepPx + 1).coerceAtMost(64)) }) { Text("+") }
	                                }
	                                SettingsInfoRow("Resize arrows", "Giữ object rồi dùng cụm Size để tăng/giảm Width và Height.")
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
    canvasScale: Float,
    canvasModeLabel: String,
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
	        Text("Roblox Scale", color = Color(0xFFA6ACB3), fontSize = 10.sp)
	        Text(canvasModeLabel, color = Color(0xFF8FBFF8), fontSize = 10.sp, maxLines = 1)
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color(0x00111111),
        contentColor = if (enabled) Color(0xFFE7EAEE) else Color(0xFF6F7680),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
	                Image(
	                    painter = painterResource(id = iconRes),
	                    contentDescription = text,
	                    modifier = Modifier.size(18.dp),
	                    alpha = if (enabled) 1f else 0.35f
	                )
	            } else if (icon != null) {
	                Icon(imageVector = icon, contentDescription = text, tint = if (enabled) Color(0xFFE7EAEE) else Color(0xFF6F7680), modifier = Modifier.size(18.dp))
	            }
	            Text(text, fontSize = 10.sp, color = if (enabled) Color(0xFFC6CED8) else Color(0xFF6F7680), fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = if (checked) Color(0xFF0A84FF).copy(alpha = 0.14f) else Color(0x00111111),
        contentColor = if (checked) Color(0xFF9AD7FF) else Color(0xFFC6CED8),
        shape = RoundedCornerShape(6.dp),
        border = if (checked) BorderStroke(1.dp, Color(0xFF0A84FF).copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(18.dp),
                    alpha = if (checked) 1f else 0.45f
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (checked) Color(0xFF8FBFF8) else Color(0xFFA6ACB3),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(text, fontSize = 10.sp, color = if (checked) Color(0xFF9AD7FF) else Color(0xFFC6CED8), fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun StudioToolbarDivider() {
    Spacer(modifier = Modifier.width(3.dp))
    VerticalDivider(modifier = Modifier.height(24.dp), color = Color(0xFF303743))
    Spacer(modifier = Modifier.width(3.dp))
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
