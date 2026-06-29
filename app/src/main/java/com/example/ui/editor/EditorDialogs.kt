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
import com.example.data.model.RobloxObject
import com.example.data.model.RobloxClass
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity

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
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Export Roblox Luau Script", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(luauCode)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        tint = Color(0, 162, 255)
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(15, 15, 18))
                    .border(1.dp, Color(45, 45, 50), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = luauCode,
                    color = Color(46, 204, 113), // Code Green
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { clipboardManager.setText(AnnotatedString(luauCode)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
            ) {
                Text("Copy & Close", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    onSave: (String) -> Unit
) {
    var gradientString by remember { mutableStateOf(initialColorString) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Gradient (Color1 to Color2)", fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gradientString,
                    onValueChange = { gradientString = it },
                    label = { Text("Gradient (e.g. 255,0,0 to 0,0,255)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Helper: Use 'R,G,B to R,G,B' format", fontSize = 10.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(gradientString); onDismiss() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
