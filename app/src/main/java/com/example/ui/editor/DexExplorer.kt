@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onToggleDragMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf<RobloxClass?>(null) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    
    // Track expanded state of nodes
    val expandedNodes = remember { mutableStateMapOf<String, Boolean>().apply { put(root.id, true) } }
    
    // Dialogue for renaming
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .background(Color(30, 30, 35))
            .border(1.dp, Color(45, 45, 50))
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DEX EXPLORER",
                color = Color(241, 196, 15), // Yellow DarkDex highlight
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { showFilterDropdown = true },
                modifier = Modifier.size(24.dp)
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
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...", fontSize = 11.sp, color = Color.Gray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0, 162, 255),
                unfocusedBorderColor = Color(60, 60, 65),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(20, 20, 25),
                unfocusedContainerColor = Color(20, 20, 25)
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
                    expandedNodes = expandedNodes,
                    searchQuery = searchQuery,
                    classFilter = classFilter,
                    onToggleDragMode = onToggleDragMode
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
    expandedNodes: MutableMap<String, Boolean>,
    searchQuery: String,
    classFilter: RobloxClass?,
    onToggleDragMode: () -> Unit
) {
    val isSelected = node.id == selectedId
    val isExpanded = expandedNodes[node.id] ?: false
    val hasChildren = node.children.isNotEmpty()
    
    // Filtering logic
    val matchesSearch = searchQuery.isEmpty() || node.name.contains(searchQuery, ignoreCase = true)
    val matchesFilter = classFilter == null || node.className == classFilter
    val shouldRenderThisNode = matchesSearch && matchesFilter

    var showContextMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (shouldRenderThisNode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(if (isSelected) Color(0, 162, 255, 60) else Color.Transparent)
                    .combinedClickable(
                        onClick = { onSelect(node.id) },
                        onDoubleClick = {
                            onSelect(node.id)
                            onToggleDragMode()
                        },
                        onLongClick = { showContextMenu = true }
                    )
                    .padding(start = (depth * 10).dp, end = 4.dp),
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
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Direct control buttons (quick access)
                if (isSelected) {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(20.dp)
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

            // Context Menu Dropdown
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
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
                    expandedNodes = expandedNodes,
                    searchQuery = searchQuery,
                    classFilter = classFilter,
                    onToggleDragMode = onToggleDragMode
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
    }
}
