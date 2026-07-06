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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Color3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        StudioDialogSurface(
            title = "Insert Object",
            subtitle = "Choose an instance type for the selected parent",
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.66f)
                .heightIn(min = 320.dp, max = 420.dp)
                .widthIn(max = 680.dp),
            footer = {
                StudioGhostButton("Close", onDismiss)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF11151B), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(6.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .background(if (selectedCategory == cat) Color(0xFF0A84FF).copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (selectedCategory == cat) Color(0xFFF4F7FB) else Color(0xFF8E96A3),
                                fontWeight = if (selectedCategory == cat) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(objectsForCategory) { obj ->
                        Surface(
                            onClick = { onInsert(obj); onDismiss() },
                            color = Color(0xFF171C23),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E3540))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RobloxClassIcon(className = obj, iconSize = 22.dp)
                                Text(
                                    text = obj.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF4F7FB),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// New Project Dialog
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit // (name, template)
) {
    var projectName by remember { mutableStateOf("New Interface") }
    var selectedTemplate by remember { mutableStateOf("Main Menu Dashboard") }
    
    val templates = listOf(
        ProjectTemplateOption("Empty Screen", "Blank canvas with ScreenGui root", RobloxClass.ScreenGui),
        ProjectTemplateOption("Main Menu Dashboard", "Hero frame, buttons, gradient polish", RobloxClass.Frame),
        ProjectTemplateOption("Item Shop Grid", "Responsive shop layout and cards", RobloxClass.UIGridLayout),
        ProjectTemplateOption("Player HUD Status", "Health, coins, status widgets", RobloxClass.TextLabel)
    )
    val selectedTemplateOption = templates.firstOrNull { it.name == selectedTemplate } ?: templates.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        StudioDialogSurface(
            title = "New UI Project",
            subtitle = "Create a local autosaved Roblox GUI workspace",
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .heightIn(min = 392.dp, max = 500.dp)
                .widthIn(max = 760.dp),
            footer = {
                StudioGhostButton("Cancel", onDismiss)
                StudioPrimaryButton(
                    label = "Create",
                    enabled = projectName.isNotBlank(),
                    onClick = {
                        onCreate(projectName.ifBlank { "New Project" }, selectedTemplate)
                        onDismiss()
                    }
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.15f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioFieldLabel("Project name")
                    CompactStudioTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Template", fontSize = 11.sp, color = Color(0xFF8E96A3), fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        templates.forEach { template ->
                            TemplateChoiceRow(
                                template = template,
                                selected = selectedTemplate == template.name,
                                onClick = { selectedTemplate = template.name }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .heightIn(min = 286.dp)
                        .background(Color(0xFF11151B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Preview", color = Color(0xFFF4F7FB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .background(Color(0xFF090D12), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF303844), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.78f)
                                .fillMaxHeight(0.56f)
                                .background(Brush.linearGradient(listOf(Color(0xFF0A84FF).copy(alpha = 0.34f), Color(0xFF27D17F).copy(alpha = 0.22f))), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF54B6FF).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            repeat(4) { index ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (index == 1) 18.dp else 12.dp)
                                        .align(Alignment.Bottom)
                                        .background(if (index == 1) Color(0xFF0A84FF) else Color(0xFF252C36), RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RobloxClassIcon(className = selectedTemplateOption.iconClass, iconSize = 22.dp)
                        Column {
                            Text(selectedTemplateOption.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(selectedTemplateOption.description, color = Color(0xFF8E96A3), fontSize = 9.sp, maxLines = 2)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2E3540))
                    Text("Autosave local storage is enabled. DPI starts at 40% for mobile landscape.", color = Color(0xFF8E96A3), fontSize = 9.sp, lineHeight = 12.sp)
                }
            }
        }
    }
}

private data class ProjectTemplateOption(
    val name: String,
    val description: String,
    val iconClass: RobloxClass
)

@Composable
private fun StudioDialogSurface(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF161A21),
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF333A45))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xFF1E232B))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color(0xFFF4F7FB), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(subtitle, color = Color(0xFF8E96A3), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA8B0BC), modifier = Modifier.size(18.dp))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color(0xFF11151B))
                    .border(0.5.dp, Color(0xFF2E3540))
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = footer
            )
        }
    }
}

@Composable
private fun StudioFieldLabel(text: String) {
    Text(text, color = Color(0xFF8E96A3), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun CompactStudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color(0xFFF4F7FB),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        ),
        cursorBrush = SolidColor(Color(0xFF0A84FF)),
        modifier = modifier.height(44.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1319), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF333A45), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                innerTextField()
            }
        }
    )
}

@Composable
private fun TemplateChoiceRow(
    template: ProjectTemplateOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(if (selected) Color(0xFF0A84FF).copy(alpha = 0.16f) else Color(0xFF11151B), RoundedCornerShape(6.dp))
            .border(1.dp, if (selected) Color(0xFF0A84FF).copy(alpha = 0.65f) else Color(0xFF2E3540), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (selected) Color(0xFF0A84FF) else Color.Transparent, CircleShape)
                .border(1.5.dp, if (selected) Color(0xFF69C3FF) else Color(0xFF68727F), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
            }
        }
        RobloxClassIcon(className = template.iconClass, iconSize = 20.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(template.name, color = Color(0xFFF4F7FB), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(template.description, color = Color(0xFF8E96A3), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StudioGhostButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFF3A4350)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC6CED8)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StudioPrimaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0A84FF),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF26313E),
            disabledContentColor = Color(0xFF7D8794)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// Open Saved Project Dialog
@Composable
fun OpenProjectDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onLoadProject: (ProjectEntity) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        StudioDialogSurface(
            title = "Project Manager",
            subtitle = "${projects.size} local project${if (projects.size == 1) "" else "s"} saved on this device",
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .heightIn(min = 340.dp, max = 520.dp)
                .widthIn(max = 720.dp),
            footer = {
                StudioGhostButton("Close", onDismiss)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 230.dp, max = 360.dp)
            ) {
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF11151B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF5C6673), modifier = Modifier.size(34.dp))
                            Text("No saved projects", color = Color(0xFFC6CED8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Create a project and it will autosave here.", color = Color(0xFF8E96A3), fontSize = 10.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF11151B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(projects) { project ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .background(Color(0xFF171C23), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF2E3540), RoundedCornerShape(6.dp))
                                    .clickable { onLoadProject(project); onDismiss() }
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(project.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF4F7FB), maxLines = 1)
                                        Text(project.description, fontSize = 9.sp, color = Color(0xFF8E96A3), maxLines = 1)
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteProject(project.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5F57),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Export Luau Script Dialog
@Composable
fun ExportLuauDialog(
    luauCode: String,
    rojoBundle: String = "",
    uiScalePercent: Int = 40,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var selectedMode by remember { mutableStateOf(0) }
    val selectedCode = if (selectedMode == 0) luauCode else rojoBundle.ifBlank { luauCode }
    val shareTitle = if (selectedMode == 0) "Chia se ma Luau" else "Chia se Rojo bundle"
    val codeFontSize = (9f * (uiScalePercent / 100f)).coerceIn(8f, 13f).sp
    val accent = if (selectedMode == 0) Color(0xFF0A84FF) else Color(0xFF28C840)

    fun shareSelectedCode() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, selectedCode)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, shareTitle)
        context.startActivity(shareIntent)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f)
                .widthIn(max = 900.dp),
            color = Color(0xFF1C1E24),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF3A3F4A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0xFF262A32))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExportTrafficDot(Color(0xFFFF5F57))
                    Spacer(modifier = Modifier.width(8.dp))
                    ExportTrafficDot(Color(0xFFFFBD2E))
                    Spacer(modifier = Modifier.width(8.dp))
                    ExportTrafficDot(Color(0xFF28C840))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export GUI", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "DPI-aware • UI $uiScalePercent% • ${selectedCode.lines().size} lines",
                            color = Color(0xFFA6ACB3),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { selectedMode = 0 }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "New Luau tab", tint = Color(0xFFE7EAEE))
                    }
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(selectedCode)) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFE7EAEE))
                    }
                    IconButton(onClick = { shareSelectedCode() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFFE7EAEE))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFE7EAEE))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF20232A))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportTabButton(
                        label = "Luau",
                        selected = selectedMode == 0,
                        accent = Color(0xFF0A84FF),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = 0 }
                    )
                    ExportTabButton(
                        label = "Rojo",
                        selected = selectedMode == 1,
                        accent = Color(0xFF28C840),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = 1 }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181A20))
                        .border(0.5.dp, Color(0xFF343842))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedMode == 0) "Runtime LocalScript" else "Rojo project files",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Tab ${selectedMode + 1}/2", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F1117))
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedCode,
                        color = if (selectedMode == 0) Color(0xFF7CFFB2) else Color(0xFF9AD0FF),
                        fontSize = codeFontSize,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF20232A))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF4B5260))
                    ) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { shareSelectedCode() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28C840), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { clipboardManager.setText(AnnotatedString(selectedCode)) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportTrafficDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun ExportTabButton(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        color = if (selected) accent.copy(alpha = 0.22f) else Color(0xFF2A2E37),
        contentColor = if (selected) Color.White else Color(0xFFA6ACB3),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) accent else Color(0xFF343842))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        StudioDialogSurface(
            title = "Script Editor",
            subtitle = "${obj.className.name}  |  ${obj.name}",
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .heightIn(min = 430.dp, max = 560.dp)
                .widthIn(max = 840.dp),
            footer = {
                StudioGhostButton("Cancel", onDismiss)
                StudioPrimaryButton(
                    label = "Save Script",
                    onClick = {
                        onSave(source)
                        onDismiss()
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Color(0xFF0F1319), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF303743), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RobloxClassIcon(className = obj.className, iconSize = 20.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = obj.className.name,
                        color = Color(0xFFF4F7FB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${source.lines().size} lines",
                        color = Color(0xFF8E96A3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0A84FF),
                        unfocusedBorderColor = Color(0xFF2E3540),
                        focusedTextColor = Color(0xFFE7EAEE),
                        unfocusedTextColor = Color(0xFFE7EAEE),
                        cursorColor = Color(0xFF0A84FF),
                        focusedContainerColor = Color(0xFF090D12),
                        unfocusedContainerColor = Color(0xFF090D12)
                    ),
                    shape = RoundedCornerShape(5.dp)
                )
            }
        }
    }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A21)),
            border = BorderStroke(1.dp, Color(0xFF333A45)),
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .widthIn(max = 520.dp)
                .heightIn(max = 580.dp)
                .padding(8.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Drag Title Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color(0xFF1E232B))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                        .padding(horizontal = 12.dp)
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F7FB)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA8B0BC), modifier = Modifier.size(16.dp))
                    }
                }

                // Inner content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Main Gradient Showcase Card
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3540)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF11151B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(Locales.translate("gradient_preview", lang), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8E96A3))
                            
                            // Main Gradient Strip
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colorStops = stops.map { it.position to it.color }.toTypedArray()
                                        )
                                    )
                                    .border(1.dp, Color(0xFF303743), RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    val gradientPresets = listOf(
                        listOf(GradientStop(0f, Color(0xFF0EA5E9)), GradientStop(1f, Color(0xFF22C55E))),
                        listOf(GradientStop(0f, Color(0xFFF97316)), GradientStop(1f, Color(0xFFEF4444))),
                        listOf(GradientStop(0f, Color(0xFF111827)), GradientStop(1f, Color(0xFFE5E7EB))),
                        listOf(GradientStop(0f, Color(0xFF8B5CF6)), GradientStop(0.5f, Color(0xFFEC4899)), GradientStop(1f, Color(0xFFF59E0B))),
                        listOf(GradientStop(0f, Color(0xFF14B8A6)), GradientStop(1f, Color(0xFF1D4ED8)))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gradientPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.horizontalGradient(colorStops = preset.map { it.position to it.color }.toTypedArray()))
                                    .border(1.dp, Color(0xFF303743), RoundedCornerShape(4.dp))
                                    .clickable {
                                        stops = preset
                                        selectedStopIndex = 0
                                    }
                            )
                        }
                    }

                    // Interactive Stops Control Strip (DPI-Responsive & Drag-Optimized)
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3540)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF11151B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Locales.translate("gradient_track", lang), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8E96A3))
                                Spacer(Modifier.weight(1f))
                                Text(Locales.translate("click_to_add", lang), fontSize = 10.sp, color = Color(0xFF0A84FF))
                            }
                            
                            // Interactive slider track
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
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
                                        .background(Color(0xFF303743))
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
                                                    if (isSelected) Color(0xFF0A84FF) else Color(0xFF202631),
                                                    CircleShape
                                                )
                                                .border(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) Color.White else Color(0xFF8E96A3),
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
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                stops.forEachIndexed { index, stop ->
                                    val isSelected = index == selectedStopIndex
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.2f) else Color(0xFF202631))
                                            .border(1.dp, if (isSelected) Color(0xFF0A84FF) else Color(0xFF303743), RoundedCornerShape(4.dp))
                                            .clickable { selectedStopIndex = index }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                                            color = if (isSelected) Color.White else Color(0xFFC6CED8),
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
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E3540)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF11151B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        Text(Locales.translate("stop_position", lang), fontSize = 11.sp, color = Color(0xFF8E96A3))
                                        Spacer(Modifier.weight(1f))
                                        Text("${(currentStop.position * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold)
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
                                            activeTrackColor = Color(0xFF0A84FF),
                                            inactiveTrackColor = Color(0xFF303743),
                                            thumbColor = Color.White
                                        )
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF2E3540), thickness = 1.dp)

                                // VISUAL COLOR PICKER: circular hue/saturation wheel + value sliders.
                                Text(Locales.translate("visual_color_mixer", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E96A3))

                                CircularColorWheel(
                                    hue = hue,
                                    saturation = saturation,
                                    value = value,
                                    onColorChange = { newHue, newSat ->
                                        hue = newHue
                                        saturation = newSat
                                        val updatedColor = hsvToColor(hue, saturation, value)
                                        updateStopColor(stops, selectedStopIndex, updatedColor) { stops = it }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(112.dp)
                                )

                                // 1. Hue Spectrum Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row {
                                        Text(Locales.translate("hue", lang), fontSize = 10.sp, color = Color(0xFFC6CED8))
                                        Spacer(Modifier.weight(1f))
                                        Text("${hue.roundToInt()}°", fontSize = 10.sp, color = Color(0xFF8E96A3))
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
                                        Text(Locales.translate("saturation", lang), fontSize = 10.sp, color = Color(0xFFC6CED8))
                                        Spacer(Modifier.weight(1f))
                                        Text("${(saturation * 100).roundToInt()}%", fontSize = 10.sp, color = Color(0xFF8E96A3))
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
                                        Text(Locales.translate("brightness", lang), fontSize = 10.sp, color = Color(0xFFC6CED8))
                                        Spacer(Modifier.weight(1f))
                                        Text("${(value * 100).roundToInt()}%", fontSize = 10.sp, color = Color(0xFF8E96A3))
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
                                                        .background(Color(0xFF0F1319), RoundedCornerShape(4.dp))
                                                        .border(1.dp, Color(0xFF303743), RoundedCornerShape(4.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(Locales.translate("hex", lang), fontSize = 8.sp, color = Color(0xFF8E96A3))
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
                        StudioGhostButton(Locales.translate("cancel", lang), onDismiss)
                        StudioPrimaryButton(
                            label = Locales.translate("save_close", lang),
                            onClick = { 
                                val result = stops.joinToString(";") { 
                                    "${it.position}:${(it.color.red * 255).roundToInt()},${(it.color.green * 255).roundToInt()},${(it.color.blue * 255).roundToInt()}" 
                                }
                                onSave(result)
                                onDismiss() 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red
    )

    fun updateFromOffset(offset: Offset, width: Float, height: Float) {
        val radius = minOf(width, height) / 2f
        if (radius <= 0f) return
        val center = Offset(width / 2f, height / 2f)
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val distance = sqrt(dx * dx + dy * dy)
        val nextSaturation = (distance / radius).coerceIn(0f, 1f)
        val nextHue = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
        onColorChange(nextHue, nextSaturation)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        updateFromOffset(offset, size.width.toFloat(), size.height.toFloat())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        updateFromOffset(change.position, size.width.toFloat(), size.height.toFloat())
                    }
                }
        ) {
            val radius = minOf(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.sweepGradient(colors = hueColors, center = center),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(24, 24, 28),
                radius = radius * 0.18f,
                center = center
            )

            val markerAngle = (hue / 180f * PI.toFloat())
            val markerDistance = saturation.coerceIn(0f, 1f) * radius
            val markerCenter = Offset(
                x = center.x + cos(markerAngle) * markerDistance,
                y = center.y + sin(markerAngle) * markerDistance
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 11.dp.toPx(),
                center = markerCenter
            )
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = markerCenter
            )
            drawCircle(
                color = hsvToColor(hue, saturation, value),
                radius = 6.dp.toPx(),
                center = markerCenter
            )
        }
    }
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
