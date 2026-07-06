@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RobloxClass
import com.example.data.model.RobloxObject

@Composable
fun DexExplorerPanel(
    root: RobloxObject,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onMove: (String, Boolean) -> Unit, // (id, up)
    onCopy: (String) -> Unit,
    onPaste: (String) -> Unit,
    onReparent: (String, String) -> Unit = { _, _ -> },
    onInsertChild: (String, RobloxClass) -> Unit = { _, _ -> },
    onOpenScript: (String) -> Unit = {},
    onToggleDragMode: () -> Unit = {},
    onMinimize: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf<RobloxClass?>(null) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var toolbarNodeId by remember { mutableStateOf<String?>(null) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }

    // Track expanded state of nodes
    val expandedNodes = remember { mutableStateMapOf<String, Boolean>().apply { put(root.id, true) } }

    // Dialogue for renaming
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .background(Color(0xFF25272C))
            .border(1.dp, Color(0xFF3D4148))
            .padding(6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.vanilla_action_explorer),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Explorer",
                    color = Color(0xFFE7EAEE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showFilterDropdown = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Classes",
                        tint = if (classFilter != null) Color(0, 162, 255) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    DropdownMenu(
                        expanded = showFilterDropdown,
                        onDismissRequest = { showFilterDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Classes", fontSize = 12.sp) },
                            onClick = { classFilter = null; showFilterDropdown = false }
                        )
                        RobloxClass.values().forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.name, fontSize = 12.sp) },
                                onClick = { classFilter = cls; showFilterDropdown = false }
                            )
                        }
                    }
                }
                if (onMinimize != null) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Minimize Explorer",
                            tint = Color(0xFFA6ACB3),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Explorer", fontSize = 11.sp, color = Color.Gray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0, 162, 255),
                unfocusedBorderColor = Color(0xFF3D4148),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1C1E22),
                unfocusedContainerColor = Color(0xFF1C1E22)
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hierarchy list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                RenderExplorerNode(
                    node = root,
                    depth = 0,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onRenameRequest = { id, name -> renamingId = id; renameInput = name },
                    onMove = onMove,
                    onCopy = onCopy,
                    onPaste = onPaste,
                    onReparent = onReparent,
                    onInsertChild = onInsertChild,
                    onOpenScript = onOpenScript,
                    expandedNodes = expandedNodes,
                    searchQuery = searchQuery,
                    classFilter = classFilter,
                    onToggleDragMode = onToggleDragMode,
                    toolbarNodeId = toolbarNodeId,
                    onToolbarNodeChange = { toolbarNodeId = it },
                    draggingNodeId = draggingNodeId,
                    dropTargetId = dropTargetId,
                    rowBounds = rowBounds,
                    onDragStart = { id ->
                        draggingNodeId = id
                        toolbarNodeId = id
                        dropTargetId = null
                    },
                    onDragPosition = { id, position ->
                        dragPosition = position
                        dropTargetId = rowBounds.entries
                            .firstOrNull { (targetId, bounds) ->
                                targetId != id && bounds.containsPoint(position)
                            }
                            ?.key
                    },
                    onDragEnd = {
                        val sourceId = draggingNodeId
                        val targetId = dropTargetId ?: dragPosition?.let { position ->
                            rowBounds.entries
                                .firstOrNull { (candidateId, bounds) ->
                                    candidateId != sourceId && bounds.containsPoint(position)
                                }
                                ?.key
                        }
                        if (sourceId != null && targetId != null) {
                            expandedNodes[targetId] = true
                            onReparent(sourceId, targetId)
                        }
                        draggingNodeId = null
                        dragPosition = null
                        dropTargetId = null
                    },
                    onDragCancel = {
                        draggingNodeId = null
                        dragPosition = null
                        dropTargetId = null
                    }
                )
            }
        }
    }

    // Rename Dialog
    if (renamingId != null) {
        AlertDialog(
            onDismissRequest = { renamingId = null },
            title = { Text("Rename Object", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("New Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRename(renamingId!!, renameInput)
                        renamingId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
                ) {
                    Text("Rename", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun Rect.containsPoint(offset: Offset): Boolean {
    return offset.x >= left && offset.x <= right && offset.y >= top && offset.y <= bottom
}

@Composable
fun RenderExplorerNode(
    node: RobloxObject,
    depth: Int,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onRenameRequest: (String, String) -> Unit,
    onMove: (String, Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onPaste: (String) -> Unit,
    onReparent: (String, String) -> Unit,
    onInsertChild: (String, RobloxClass) -> Unit,
    onOpenScript: (String) -> Unit,
    expandedNodes: MutableMap<String, Boolean>,
    searchQuery: String,
    classFilter: RobloxClass?,
    onToggleDragMode: () -> Unit,
    toolbarNodeId: String?,
    onToolbarNodeChange: (String?) -> Unit,
    draggingNodeId: String?,
    dropTargetId: String?,
    rowBounds: MutableMap<String, Rect>,
    onDragStart: (String) -> Unit,
    onDragPosition: (String, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val isSelected = node.id == selectedId
    val isExpanded = expandedNodes[node.id] ?: false
    val hasChildren = node.children.isNotEmpty()

    // Filtering logic
    val matchesSearch = searchQuery.isEmpty() || node.name.contains(searchQuery, ignoreCase = true)
    val matchesFilter = classFilter == null || node.className == classFilter
    val shouldRenderThisNode = matchesSearch && matchesFilter

    var showContextMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val isDropTarget = dropTargetId == node.id && draggingNodeId != null && draggingNodeId != node.id

    Column(modifier = Modifier.fillMaxWidth()) {
        if (shouldRenderThisNode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(
                        color = when {
                            isDropTarget -> Color(0xFF22C55E).copy(alpha = 0.18f)
                            isSelected -> Color(0xFF0A84FF).copy(alpha = 0.22f)
                            draggingNodeId == node.id -> Color(0xFF8FBFF8).copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(3.dp)
                    )
                    .onGloballyPositioned { coordinates ->
                        rowCoordinates = coordinates
                        rowBounds[node.id] = coordinates.boundsInWindow()
                    }
                    .combinedClickable(
                        onClick = {
                            onSelect(node.id)
                            if (toolbarNodeId != node.id) onToolbarNodeChange(null)
                        },
                        onDoubleClick = {
                            if (node.className == RobloxClass.LocalScript || node.className == RobloxClass.ModuleScript) {
                                onOpenScript(node.id)
                            } else {
                                onSelect(node.id)
                                onToggleDragMode()
                            }
                        },
                        onLongClick = {
                            onSelect(node.id)
                            onToolbarNodeChange(node.id)
                        }
                    )
                    .pointerInput(node.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { startOffset ->
                                onSelect(node.id)
                                onDragStart(node.id)
                                rowCoordinates?.localToWindow(startOffset)?.let { onDragPosition(node.id, it) }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                rowCoordinates?.localToWindow(change.position)?.let { onDragPosition(node.id, it) }
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel
                        )
                    }
                    .padding(start = (depth * 12).dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand / Collapse icon
                if (hasChildren) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.PlayArrow,
                        contentDescription = "Expand",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { expandedNodes[node.id] = !isExpanded }
                    )
                } else {
                    Spacer(modifier = Modifier.size(16.dp))
                }

                // Class Icon Placeholder
                RobloxClassIcon(
                    className = node.className,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Object name
                Text(
                    text = node.name,
                    color = if (isSelected) Color.White else Color(220, 220, 220),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Direct control buttons (quick access)
                if (isSelected) {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = toolbarNodeId == node.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth * 12 + 24).dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
                        .background(Color(0xFF1B1D22), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF3D4148), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { showAddMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Insert Child", tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            listOf(
                                RobloxClass.Frame,
                                RobloxClass.TextLabel,
                                RobloxClass.TextButton,
                                RobloxClass.ImageLabel,
                                RobloxClass.ImageButton,
                                RobloxClass.UIGradient,
                                RobloxClass.UICorner,
                                RobloxClass.UIStroke,
                                RobloxClass.LocalScript,
                                RobloxClass.ModuleScript
                            ).forEach { cls ->
                                DropdownMenuItem(
                                    leadingIcon = { RobloxClassIcon(className = cls, iconSize = 16.dp) },
                                    text = { Text(cls.name, fontSize = 11.sp) },
                                    onClick = {
                                        expandedNodes[node.id] = true
                                        onInsertChild(node.id, cls)
                                        showAddMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onCopy(node.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFA6ACB3), modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onPaste(node.id); expandedNodes[node.id] = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Inside", tint = Color(0xFFA6ACB3), modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = { onDuplicate(node.id) },
                        enabled = node.className != RobloxClass.ScreenGui,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ControlPointDuplicate,
                            contentDescription = "Duplicate",
                            tint = if (node.className != RobloxClass.ScreenGui) Color(0xFFA6ACB3) else Color(0xFF5E636B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = { onRenameRequest(node.id, node.name) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFFA6ACB3), modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (draggingNodeId == node.id) "Drop on target" else "Hold + drag to parent",
                        color = Color(0xFF7E858F),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }

            // Context Menu Dropdown
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                if (node.className == RobloxClass.LocalScript || node.className == RobloxClass.ModuleScript) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        text = { Text("Open Script", fontSize = 11.sp) },
                        onClick = { onOpenScript(node.id); showContextMenu = false }
                    )
                    Divider()
                }
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    text = { Text("Rename", fontSize = 11.sp) },
                    onClick = { onRenameRequest(node.id, node.name); showContextMenu = false }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    text = { Text("Copy Object", fontSize = 11.sp) },
                    onClick = { onCopy(node.id); showContextMenu = false }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    text = { Text("Paste Inside", fontSize = 11.sp) },
                    onClick = { onPaste(node.id); showContextMenu = false }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ControlPointDuplicate, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    text = { Text("Duplicate", fontSize = 11.sp) },
                    onClick = { onDuplicate(node.id); showContextMenu = false }
                )
                Divider()
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    text = { Text("Move Up", fontSize = 11.sp) },
                    onClick = { onMove(node.id, true); showContextMenu = false }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    text = { Text("Move Down", fontSize = 11.sp) },
                    onClick = { onMove(node.id, false); showContextMenu = false }
                )
                Divider()
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red) },
                    text = { Text("Delete", fontSize = 11.sp, color = Color.Red) },
                    onClick = { onDelete(node.id); showContextMenu = false }
                )
            }
        }

        // Render Children Nodes recursively
        if (hasChildren && (isExpanded || searchQuery.isNotEmpty())) {
            node.children.forEach { child ->
                RenderExplorerNode(
                    node = child,
                    depth = depth + 1,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onRenameRequest = onRenameRequest,
                    onMove = onMove,
                    onCopy = onCopy,
                    onPaste = onPaste,
                    onReparent = onReparent,
                    onInsertChild = onInsertChild,
                    onOpenScript = onOpenScript,
                    expandedNodes = expandedNodes,
                    searchQuery = searchQuery,
                    classFilter = classFilter,
                    onToggleDragMode = onToggleDragMode,
                    toolbarNodeId = toolbarNodeId,
                    onToolbarNodeChange = onToolbarNodeChange,
                    draggingNodeId = draggingNodeId,
                    dropTargetId = dropTargetId,
                    rowBounds = rowBounds,
                    onDragStart = onDragStart,
                    onDragPosition = onDragPosition,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            }
        }
    }
}

fun getClassEmojiIcon(className: RobloxClass): String {
    return when (className) {
        RobloxClass.ScreenGui -> "🖥️"
        RobloxClass.Frame -> "🔲"
        RobloxClass.TextLabel -> "🔤"
        RobloxClass.TextButton -> "🔘"
        RobloxClass.ImageLabel -> "🖼️"
        RobloxClass.ImageButton -> "🖱️"
        RobloxClass.ScrollingFrame -> "📜"
        RobloxClass.ViewportFrame -> "👁️"
        RobloxClass.UIListLayout -> "🗂️"
        RobloxClass.UIGridLayout -> "🎛️"
        RobloxClass.UIPadding -> "📦"
        RobloxClass.UICorner -> "⚪"
        RobloxClass.UIStroke -> "➖"
        RobloxClass.UIGradient -> "🌈"
        RobloxClass.UIScale -> "🔍"
        RobloxClass.UIAspectRatioConstraint -> "📐"
        RobloxClass.LocalScript -> "🔵"
        RobloxClass.ModuleScript -> "🟡"
        RobloxClass.UIShadow -> "🌑"
    }
}
