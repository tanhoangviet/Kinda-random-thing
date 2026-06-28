package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*

@Composable
fun PropertiesPanel(
    selectedObj: RobloxObject?,
    parentName: String?,
    onUpdateProperty: (String, String, Any) -> Unit, // (id, propName, value)
    onConvertOffsetToScale: (String) -> Unit,
    onConvertScaleToOffset: (String) -> Unit,
    onApplyAnchorPreset: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedObj == null) {
        Box(
            modifier = modifier
                .background(Color(25, 25, 30))
                .border(1.dp, Color(45, 45, 50))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No Object Selected",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap an item in explorer or canvas",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }
        return
    }

    val scrollState = rememberScrollState()
    var activeColorProp by remember { mutableStateOf<Pair<String, Color3>?>(null) } // (propName, Color3)

    Column(
        modifier = modifier
            .background(Color(25, 25, 30))
            .border(1.dp, Color(45, 45, 50))
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROPERTIES",
                color = Color(0, 162, 255), // Roblox blue accent
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = selectedObj.className.name,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Basic Properties Group
        PropertySection(title = "Basic") {
            PropertyRow(label = "Name") {
                OutlinedTextField(
                    value = selectedObj.name,
                    onValueChange = { onUpdateProperty(selectedObj.id, "Name", it) }, // Note: VM rename handler does rename, here we update basic field
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0, 162, 255),
                        unfocusedBorderColor = Color(55, 55, 60),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(18, 18, 22),
                        unfocusedContainerColor = Color(18, 18, 22)
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                )
            }

            PropertyRow(label = "Parent") {
                Text(
                    text = parentName ?: "nil",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            val visible = selectedObj.properties["Visible"] as? Boolean ?: true
            PropertyRow(label = "Visible") {
                Switch(
                    checked = visible,
                    onCheckedChange = { onUpdateProperty(selectedObj.id, "Visible", it) },
                    modifier = Modifier.scale(0.7f)
                )
            }

            val active = selectedObj.properties["Active"] as? Boolean ?: true
            PropertyRow(label = "Active") {
                Switch(
                    checked = active,
                    onCheckedChange = { onUpdateProperty(selectedObj.id, "Active", it) },
                    modifier = Modifier.scale(0.7f)
                )
            }

            val zIndex = selectedObj.properties["ZIndex"] as? Int ?: 1
            PropertyRow(label = "ZIndex") {
                StepperInput(
                    value = zIndex,
                    onValueChange = { onUpdateProperty(selectedObj.id, "ZIndex", it) },
                    min = 1,
                    max = 100
                )
            }
        }

        // Transform Properties Group
        val hasSizeAndPos = selectedObj.className in listOf(
            RobloxClass.Frame, RobloxClass.TextLabel, RobloxClass.TextButton,
            RobloxClass.ImageLabel, RobloxClass.ImageButton, RobloxClass.ScrollingFrame,
            RobloxClass.ViewportFrame
        )
        if (hasSizeAndPos) {
            PropertySection(title = "Transform") {
                val pos = selectedObj.properties["Position"] as? UDim2 ?: UDim2(0f, 0, 0f, 0)
                UDim2Editor(
                    label = "Position",
                    udim2 = pos,
                    onValueChange = { onUpdateProperty(selectedObj.id, "Position", it) }
                )

                val size = selectedObj.properties["Size"] as? UDim2 ?: UDim2(0f, 0, 0f, 0)
                UDim2Editor(
                    label = "Size",
                    udim2 = size,
                    onValueChange = { onUpdateProperty(selectedObj.id, "Size", it) }
                )

                val anchor = selectedObj.properties["AnchorPoint"] as? Vector2 ?: Vector2(0f, 0f)
                Vector2Editor(
                    label = "AnchorPoint",
                    vector2 = anchor,
                    onValueChange = { onUpdateProperty(selectedObj.id, "AnchorPoint", it) }
                )

                // Anchor Preset Tool
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anchor Preset", fontSize = 10.sp, color = Color.Gray)
                    var expandedPresets by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expandedPresets = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(50, 50, 55))
                        ) {
                            Text("Select...", fontSize = 9.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = expandedPresets,
                            onDismissRequest = { expandedPresets = false }
                        ) {
                            listOf("Top Left", "Top Center", "Center", "Bottom Right", "Top Right", "Bottom Left").forEach { pr ->
                                DropdownMenuItem(
                                    text = { Text(pr, fontSize = 10.sp) },
                                    onClick = {
                                        onApplyAnchorPreset(selectedObj.id, pr)
                                        expandedPresets = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Convert Scale/Offset buttons
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onConvertOffsetToScale(selectedObj.id) },
                        modifier = Modifier.weight(1f).height(28.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
                    ) {
                        Text("Offset ➔ Scale", fontSize = 8.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onConvertScaleToOffset(selectedObj.id) },
                        modifier = Modifier.weight(1f).height(28.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
                    ) {
                        Text("Scale ➔ Offset", fontSize = 8.sp, color = Color.White)
                    }
                }
            }
        }

        // Appearance Group (BackgroundColor, Transparency, Border, etc.)
        PropertySection(title = "Appearance") {
            if (selectedObj.properties.containsKey("BackgroundColor3")) {
                val bgCol = selectedObj.properties["BackgroundColor3"] as? Color3 ?: Color3(255, 255, 255)
                PropertyRow(label = "Background") {
                    Box(
                        modifier = Modifier
                            .size(34.dp, 22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(bgCol.r, bgCol.g, bgCol.b))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { activeColorProp = Pair("BackgroundColor3", bgCol) }
                    )
                }
            }

            if (selectedObj.properties.containsKey("BackgroundTransparency")) {
                val trans = selectedObj.properties["BackgroundTransparency"] as? Float ?: 0f
                PropertyRow(label = "Transparency") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = trans,
                            onValueChange = { onUpdateProperty(selectedObj.id, "BackgroundTransparency", it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(100.dp).height(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.2f", trans),
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            if (selectedObj.properties.containsKey("BorderSizePixel")) {
                val bSize = selectedObj.properties["BorderSizePixel"] as? Int ?: 1
                PropertyRow(label = "BorderSize") {
                    StepperInput(
                        value = bSize,
                        onValueChange = { onUpdateProperty(selectedObj.id, "BorderSizePixel", it) },
                        min = 0,
                        max = 10
                    )
                }
            }
        }

        // Text Properties (only for Text elements)
        val isTextClass = selectedObj.className in listOf(RobloxClass.TextLabel, RobloxClass.TextButton)
        if (isTextClass) {
            PropertySection(title = "Text Settings") {
                val text = selectedObj.properties["Text"] as? String ?: "Text"
                PropertyRow(label = "Text") {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { onUpdateProperty(selectedObj.id, "Text", it) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0, 162, 255),
                            unfocusedBorderColor = Color(55, 55, 60),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(18, 18, 22),
                            unfocusedContainerColor = Color(18, 18, 22)
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                    )
                }

                val textCol = selectedObj.properties["TextColor3"] as? Color3 ?: Color3(255, 255, 255)
                PropertyRow(label = "Text Color") {
                    Box(
                        modifier = Modifier
                            .size(34.dp, 22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(textCol.r, textCol.g, textCol.b))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { activeColorProp = Pair("TextColor3", textCol) }
                    )
                }

                val textSize = selectedObj.properties["TextSize"] as? Int ?: 14
                PropertyRow(label = "Text Size") {
                    StepperInput(
                        value = textSize,
                        onValueChange = { onUpdateProperty(selectedObj.id, "TextSize", it) },
                        min = 6,
                        max = 96
                    )
                }

                val textTrans = selectedObj.properties["TextTransparency"] as? Float ?: 0f
                PropertyRow(label = "Text Trans") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = textTrans,
                            onValueChange = { onUpdateProperty(selectedObj.id, "TextTransparency", it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(80.dp).height(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.2f", textTrans),
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }

                val font = selectedObj.properties["Font"] as? String ?: "SourceSans"
                var fontExpanded by remember { mutableStateOf(false) }
                PropertyRow(label = "Font") {
                    Box {
                        Button(
                            onClick = { fontExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(50, 50, 55))
                        ) {
                            Text(font, fontSize = 9.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = fontExpanded,
                            onDismissRequest = { fontExpanded = false }
                        ) {
                            listOf("SourceSans", "SourceSansBold", "SourceSansItalic", "Gotham", "Arial", "Bodoni", "SciFi").forEach { fName ->
                                DropdownMenuItem(
                                    text = { Text(fName, fontSize = 10.sp) },
                                    onClick = {
                                        onUpdateProperty(selectedObj.id, "Font", fName)
                                        fontExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                val textScaled = selectedObj.properties["TextScaled"] as? Boolean ?: false
                PropertyRow(label = "TextScaled") {
                    Switch(
                        checked = textScaled,
                        onCheckedChange = { onUpdateProperty(selectedObj.id, "TextScaled", it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
        }

        // Layout Constraints (UIListLayout, UIGridLayout, etc.)
        val isLayoutClass = selectedObj.className in listOf(RobloxClass.UIListLayout, RobloxClass.UIGridLayout, RobloxClass.UIPadding, RobloxClass.UICorner, RobloxClass.UIStroke)
        if (isLayoutClass) {
            PropertySection(title = "Layout Configurations") {
                if (selectedObj.properties.containsKey("FillDirection")) {
                    val dir = selectedObj.properties["FillDirection"] as? String ?: "Vertical"
                    var dirExpanded by remember { mutableStateOf(false) }
                    PropertyRow(label = "Direction") {
                        Box {
                            Button(
                                onClick = { dirExpanded = true },
                                modifier = Modifier.height(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(50, 50, 55))
                            ) {
                                Text(dir, fontSize = 9.sp)
                            }
                            DropdownMenu(expanded = dirExpanded, onDismissRequest = { dirExpanded = false }) {
                                listOf("Vertical", "Horizontal").forEach { d ->
                                    DropdownMenuItem(text = { Text(d) }, onClick = {
                                        onUpdateProperty(selectedObj.id, "FillDirection", d)
                                        dirExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }

                if (selectedObj.properties.containsKey("CornerRadius")) {
                    val radius = selectedObj.properties["CornerRadius"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
                    PropertyRow(label = "Corner Radius") {
                        StepperInput(
                            value = radius.offsetX,
                            onValueChange = { onUpdateProperty(selectedObj.id, "CornerRadius", radius.copy(offsetX = it)) },
                            min = 0,
                            max = 100
                        )
                    }
                }

                if (selectedObj.properties.containsKey("Thickness")) {
                    val strokeThick = selectedObj.properties["Thickness"] as? Int ?: 1
                    PropertyRow(label = "Thickness") {
                        StepperInput(value = strokeThick, onValueChange = {
                            onUpdateProperty(selectedObj.id, "Thickness", it)
                        }, min = 1, max = 20)
                    }
                }
            }
        }

        // Advanced Panel
        PropertySection(title = "Advanced") {
            if (selectedObj.properties.containsKey("ClipsDescendants")) {
                val clips = selectedObj.properties["ClipsDescendants"] as? Boolean ?: false
                PropertyRow(label = "ClipsDesc") {
                    Switch(
                        checked = clips,
                        onCheckedChange = { onUpdateProperty(selectedObj.id, "ClipsDescendants", it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }

            val lOrder = selectedObj.properties["LayoutOrder"] as? Int ?: 0
            PropertyRow(label = "LayoutOrder") {
                StepperInput(
                    value = lOrder,
                    onValueChange = { onUpdateProperty(selectedObj.id, "LayoutOrder", it) },
                    min = -100,
                    max = 100
                )
            }
        }
    }

    // Custom RGB Color Picker Dialog
    if (activeColorProp != null) {
        val (propName, curCol) = activeColorProp!!
        ColorPickerDialog(
            currentValue = curCol,
            onDismiss = { activeColorProp = null },
            onColorSelect = { newCol ->
                onUpdateProperty(selectedObj.id, propName, newCol)
                activeColorProp = null
            }
        )
    }
}

@Composable
fun PropertySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(40, 40, 45), RoundedCornerShape(4.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropDown else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 4.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PropertyRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(180, 180, 185),
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.CenterEnd
        ) {
            content()
        }
    }
}

@Composable
fun UDim2Editor(label: String, udim2: UDim2, onValueChange: (UDim2) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // X components
            CoordinateInputBox(
                label = "Scale X",
                value = udim2.scaleX.toString(),
                onValueChange = {
                    val s = it.toFloatOrNull() ?: udim2.scaleX
                    onValueChange(udim2.copy(scaleX = s))
                },
                modifier = Modifier.weight(1f)
            )
            CoordinateInputBox(
                label = "Offset X",
                value = udim2.offsetX.toString(),
                onValueChange = {
                    val o = it.toIntOrNull() ?: udim2.offsetX
                    onValueChange(udim2.copy(offsetX = o))
                },
                modifier = Modifier.weight(1f)
            )
            // Y components
            CoordinateInputBox(
                label = "Scale Y",
                value = udim2.scaleY.toString(),
                onValueChange = {
                    val s = it.toFloatOrNull() ?: udim2.scaleY
                    onValueChange(udim2.copy(scaleY = s))
                },
                modifier = Modifier.weight(1f)
            )
            CoordinateInputBox(
                label = "Offset Y",
                value = udim2.offsetY.toString(),
                onValueChange = {
                    val o = it.toIntOrNull() ?: udim2.offsetY
                    onValueChange(udim2.copy(offsetY = o))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun Vector2Editor(label: String, vector2: Vector2, onValueChange: (Vector2) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CoordinateInputBox(
                label = "X",
                value = vector2.x.toString(),
                onValueChange = {
                    val x = it.toFloatOrNull() ?: vector2.x
                    onValueChange(vector2.copy(x = x))
                },
                modifier = Modifier.weight(1f)
            )
            CoordinateInputBox(
                label = "Y",
                value = vector2.y.toString(),
                onValueChange = {
                    val y = it.toFloatOrNull() ?: vector2.y
                    onValueChange(vector2.copy(y = y))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CoordinateInputBox(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.height(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0, 162, 255),
                unfocusedBorderColor = Color(50, 50, 55),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(15, 15, 18),
                unfocusedContainerColor = Color(15, 15, 18)
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp)
        )
        Text(label, fontSize = 7.sp, color = Color.Gray)
    }
}

@Composable
fun StepperInput(value: Int, onValueChange: (Int) -> Unit, min: Int, max: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { if (value > min) onValueChange(value - 1) },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Text(
            text = value.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 16.dp),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = { if (value < max) onValueChange(value + 1) },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

// Custom native Color Picker Composable Dialog
@Composable
fun ColorPickerDialog(
    currentValue: Color3,
    onDismiss: () -> Unit,
    onColorSelect: (Color3) -> Unit
) {
    var r by remember { mutableStateOf(currentValue.r) }
    var g by remember { mutableStateOf(currentValue.g) }
    var b by remember { mutableStateOf(currentValue.b) }

    val presetColors = listOf(
        Color3(231, 76, 60),  // Red
        Color3(230, 126, 34), // Orange
        Color3(241, 196, 15), // Yellow
        Color3(46, 204, 113), // Green
        Color3(26, 188, 156), // Teal
        Color3(52, 152, 219), // Blue
        Color3(155, 89, 182), // Purple
        Color3(255, 255, 255),// White
        Color3(149, 165, 166),// Gray
        Color3(30, 30, 30)    // Dark
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color3 Select (RGB)", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Color preview block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(r, g, b))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RGB($r, $g, $b)",
                        color = if ((r+g+b)/3 > 128) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Preset palette grid
                Text("Presets:", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetColors.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(preset.r, preset.g, preset.b))
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                .clickable {
                                    r = preset.r
                                    g = preset.g
                                    b = preset.b
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom RGB Sliders
                ColorSlider(label = "Red (R)", value = r, onValueChange = { r = it }, color = Color.Red)
                ColorSlider(label = "Green (G)", value = g, onValueChange = { g = it }, color = Color.Green)
                ColorSlider(label = "Blue (B)", value = b, onValueChange = { b = it }, color = Color.Blue)
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelect(Color3(r, g, b)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
            ) {
                Text("Select", color = Color.White)
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
fun ColorSlider(label: String, value: Int, onValueChange: (Int) -> Unit, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 10.sp, color = Color.White)
            Text(value.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f)
            ),
            modifier = Modifier.height(14.dp)
        )
    }
}
