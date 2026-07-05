package com.example.ui.editor

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*

@Composable
fun PropertiesPanel(
    selectedObj: RobloxObject?,
    parentName: String?,
    onUpdateProperty: (String, String, Any) -> Unit, // (id, propName, value)
    onConvertOffsetToScale: (String) -> Unit,
    onConvertScaleToOffset: (String) -> Unit,
    onApplyAnchorPreset: (String, String) -> Unit,
    onOpenScript: (String) -> Unit = {},
    lang: String = "vi",
    onMinimize: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (selectedObj == null) {
        Box(
            modifier = modifier
                .background(Color(0xFF25272C))
                .border(1.dp, Color(0xFF3D4148))
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
    var activeGradientProp by remember { mutableStateOf<Pair<String, String>?>(null) } // (propName, String)

    Column(
        modifier = modifier
            .background(Color(0xFF25272C))
            .border(1.dp, Color(0xFF3D4148))
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.vanilla_action_properties),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Properties",
                    color = Color(0xFFE7EAEE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedObj.className.name,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                if (onMinimize != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Minimize Properties",
                            tint = Color(0xFFA6ACB3),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Basic Properties Group
        PropertySection(title = "Basic") {
            PropertyRow(label = "Name") {
                RobloxTextField(
                    value = selectedObj.name,
                    onValueChange = { onUpdateProperty(selectedObj.id, "Name", it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
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

            val isGuiObject = selectedObj.className in listOf(
                RobloxClass.Frame, RobloxClass.TextLabel, RobloxClass.TextButton,
                RobloxClass.ImageLabel, RobloxClass.ImageButton, RobloxClass.ScrollingFrame,
                RobloxClass.ViewportFrame
            )

            if (isGuiObject) {
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

            if (selectedObj.className == RobloxClass.LocalScript || selectedObj.className == RobloxClass.ModuleScript) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onOpenScript(selectedObj.id) },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Locales.translate("edit_script", lang), fontSize = 10.sp)
                }
            }
        }

        // Transform Properties Group
        val hasSizeAndPos = selectedObj.className in listOf(
            RobloxClass.Frame, RobloxClass.TextLabel, RobloxClass.TextButton,
            RobloxClass.ImageLabel, RobloxClass.ImageButton, RobloxClass.ScrollingFrame,
            RobloxClass.ViewportFrame
        )
        if (hasSizeAndPos) {
            PropertySection(title = Locales.translate("transform", lang)) {
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
                    Text(Locales.translate("anchor_preset", lang), fontSize = 10.sp, color = Color.Gray)
                    var expandedPresets by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expandedPresets = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(50, 50, 55))
                        ) {
                            Text(Locales.translate("select", lang), fontSize = 9.sp, color = Color.White)
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
                        Text(Locales.translate("offset_scale", lang), fontSize = 8.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onConvertScaleToOffset(selectedObj.id) },
                        modifier = Modifier.weight(1f).height(28.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0, 162, 255))
                    ) {
                        Text(Locales.translate("scale_offset", lang), fontSize = 8.sp, color = Color.White)
                    }
                }
            }
        }

        // Appearance Group (BackgroundColor, Transparency, Border, etc.)
        if (selectedObj.className == RobloxClass.UIGradient) {
            PropertySection(title = "Gradient") {
                val colorStr = selectedObj.properties["Color"] as? String ?: "255,255,255 to 150,150,150"
                val stops = try {
                    if (colorStr.contains(";")) {
                        colorStr.split(";").map { part ->
                            val segments = part.split(":")
                            val pos = segments[0].toFloat().coerceIn(0f, 1f)
                            val rgb = segments[1].split(",").map { it.trim().toInt() }
                            pos to Color(rgb[0], rgb[1], rgb[2])
                        }.sortedBy { it.first }.toTypedArray()
                    } else {
                        val colors = colorStr.split(" to ").map { part ->
                            val rgb = part.split(",").map { it.trim().toInt() }
                            Color(rgb[0], rgb[1], rgb[2])
                        }
                        arrayOf(0f to colors[0], 1f to colors[1])
                    }
                } catch (e: Exception) {
                    arrayOf(0f to Color.White, 1f to Color.Gray)
                }

                PropertyRow(label = "Color") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(colorStops = stops))
                            .border(1.dp, Color(60, 60, 65), RoundedCornerShape(8.dp))
                            .clickable { activeGradientProp = "Color" to colorStr },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            stops.forEach { (_, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, androidx.compose.foundation.shape.CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.7f), androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                        Text(
                            "Edit Gradient Wheel",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (selectedObj.properties.containsKey("Rotation")) {
                    val rotation = selectedObj.properties["Rotation"] as? Float ?: 0f
                    PropertyRow(label = "Rotation") {
                        StepperInput(
                            value = rotation.toInt(),
                            onValueChange = { onUpdateProperty(selectedObj.id, "Rotation", it.toFloat()) },
                            min = 0,
                            max = 360
                        )
                    }
                }
            }
        }

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

            if (selectedObj.properties.containsKey("Color") && selectedObj.properties["Color"] is Color3) {
                val col = selectedObj.properties["Color"] as? Color3 ?: Color3(255, 255, 255)
                PropertyRow(label = "Color") {
                    Box(
                        modifier = Modifier
                            .size(34.dp, 22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(col.r, col.g, col.b))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { activeColorProp = Pair("Color", col) }
                    )
                }
            }

            if (selectedObj.properties.containsKey("Transparency") && selectedObj.properties["Transparency"] is Float) {
                val trans = selectedObj.properties["Transparency"] as? Float ?: 0f
                PropertyRow(label = "Transparency") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = trans,
                            onValueChange = { onUpdateProperty(selectedObj.id, "Transparency", it) },
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
        }

        // Text Properties (only for Text elements)
        val isTextClass = selectedObj.className in listOf(RobloxClass.TextLabel, RobloxClass.TextButton)
        if (isTextClass) {
            PropertySection(title = "Text Settings") {
                val text = selectedObj.properties["Text"] as? String ?: "Text"
                PropertyRow(label = "Text") {
                    RobloxTextField(
                        value = text,
                        onValueChange = { onUpdateProperty(selectedObj.id, "Text", it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
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

                val textWrapped = selectedObj.properties["TextWrapped"] as? Boolean ?: true
                PropertyRow(label = "TextWrapped") {
                    Switch(
                        checked = textWrapped,
                        onCheckedChange = { onUpdateProperty(selectedObj.id, "TextWrapped", it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }

                val richText = selectedObj.properties["RichText"] as? Boolean ?: false
                PropertyRow(label = "RichText") {
                    Switch(
                        checked = richText,
                        onCheckedChange = { onUpdateProperty(selectedObj.id, "RichText", it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }

                val textXAlignment = selectedObj.properties["TextXAlignment"] as? String ?: "Center"
                PropertyRow(label = "Text X") {
                    StudioDropdownField(
                        value = textXAlignment,
                        options = listOf("Left", "Center", "Right"),
                        onValueChange = { onUpdateProperty(selectedObj.id, "TextXAlignment", it) }
                    )
                }

                val textYAlignment = selectedObj.properties["TextYAlignment"] as? String ?: "Center"
                PropertyRow(label = "Text Y") {
                    StudioDropdownField(
                        value = textYAlignment,
                        options = listOf("Top", "Center", "Bottom"),
                        onValueChange = { onUpdateProperty(selectedObj.id, "TextYAlignment", it) }
                    )
                }
            }
        }

        // Layout Constraints (UIListLayout, UIGridLayout, etc.)
        val isLayoutClass = selectedObj.className in listOf(RobloxClass.UIListLayout, RobloxClass.UIGridLayout, RobloxClass.UIPadding, RobloxClass.UICorner, RobloxClass.UIStroke, RobloxClass.UIShadow)
        if (isLayoutClass) {
            PropertySection(title = "Layout Configurations") {
                if (selectedObj.properties.containsKey("FillDirection")) {
                    val dir = selectedObj.properties["FillDirection"] as? String ?: "Vertical"
                    PropertyRow(label = "Direction") {
                        StudioDropdownField(
                            value = dir,
                            options = listOf("Vertical", "Horizontal"),
                            onValueChange = { onUpdateProperty(selectedObj.id, "FillDirection", it) }
                        )
                    }
                }

                if (selectedObj.properties.containsKey("Padding")) {
                    val padding = selectedObj.properties["Padding"] as? UDim2 ?: UDim2(0f, 0, 0f, 8)
                    PropertyRow(label = "Padding") {
                        StepperInput(
                            value = padding.offsetY,
                            onValueChange = { onUpdateProperty(selectedObj.id, "Padding", padding.copy(offsetY = it)) },
                            min = 0,
                            max = 200
                        )
                    }
                }

                if (selectedObj.properties.containsKey("CellSize")) {
                    val cellSize = selectedObj.properties["CellSize"] as? UDim2 ?: UDim2(0f, 80, 0f, 80)
                    UDim2Editor(
                        label = "CellSize",
                        udim2 = cellSize,
                        onValueChange = { onUpdateProperty(selectedObj.id, "CellSize", it) }
                    )
                }

                if (selectedObj.properties.containsKey("CellPadding")) {
                    val cellPadding = selectedObj.properties["CellPadding"] as? UDim2 ?: UDim2(0f, 6, 0f, 6)
                    UDim2Editor(
                        label = "CellPadding",
                        udim2 = cellPadding,
                        onValueChange = { onUpdateProperty(selectedObj.id, "CellPadding", it) }
                    )
                }

                listOf(
                    "PaddingTop" to "Pad Top",
                    "PaddingBottom" to "Pad Bottom",
                    "PaddingLeft" to "Pad Left",
                    "PaddingRight" to "Pad Right"
                ).forEach { (propName, label) ->
                    if (selectedObj.properties.containsKey(propName)) {
                        val padding = selectedObj.properties[propName] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
                        PropertyRow(label = label) {
                            StepperInput(
                                value = if (propName == "PaddingLeft" || propName == "PaddingRight") padding.offsetX else padding.offsetY,
                                onValueChange = { onUpdateProperty(selectedObj.id, propName, padding.copy(offsetX = it, offsetY = it)) },
                                min = 0,
                                max = 200
                            )
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

                if (selectedObj.properties.containsKey("TopLeft")) {
                    val radius = selectedObj.properties["TopLeft"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
                    PropertyRow(label = "Top Left Corner") {
                        StepperInput(
                            value = radius.offsetX,
                            onValueChange = { onUpdateProperty(selectedObj.id, "TopLeft", radius.copy(offsetX = it)) },
                            min = 0,
                            max = 100
                        )
                    }
                }

                if (selectedObj.properties.containsKey("TopRight")) {
                    val radius = selectedObj.properties["TopRight"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
                    PropertyRow(label = "Top Right Corner") {
                        StepperInput(
                            value = radius.offsetX,
                            onValueChange = { onUpdateProperty(selectedObj.id, "TopRight", radius.copy(offsetX = it)) },
                            min = 0,
                            max = 100
                        )
                    }
                }

                if (selectedObj.properties.containsKey("BottomLeft")) {
                    val radius = selectedObj.properties["BottomLeft"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
                    PropertyRow(label = "Bottom Left Corner") {
                        StepperInput(
                            value = radius.offsetX,
                            onValueChange = { onUpdateProperty(selectedObj.id, "BottomLeft", radius.copy(offsetX = it)) },
                            min = 0,
                            max = 100
                        )
                    }
                }

                if (selectedObj.properties.containsKey("BottomRight")) {
                    val radius = selectedObj.properties["BottomRight"] as? UDim2 ?: UDim2(0f, 8, 0f, 8)
                    PropertyRow(label = "Bottom Right Corner") {
                        StepperInput(
                            value = radius.offsetX,
                            onValueChange = { onUpdateProperty(selectedObj.id, "BottomRight", radius.copy(offsetX = it)) },
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

                if (selectedObj.properties.containsKey("Blur")) {
                    val blurVal = selectedObj.properties["Blur"] as? Int ?: 8
                    PropertyRow(label = "Blur") {
                        StepperInput(value = blurVal, onValueChange = {
                            onUpdateProperty(selectedObj.id, "Blur", it)
                        }, min = 0, max = 100)
                    }
                }

                if (selectedObj.properties.containsKey("Spread")) {
                    val spreadVal = selectedObj.properties["Spread"] as? Int ?: 0
                    PropertyRow(label = "Spread") {
                        StepperInput(value = spreadVal, onValueChange = {
                            onUpdateProperty(selectedObj.id, "Spread", it)
                        }, min = -100, max = 100)
                    }
                }

                if (selectedObj.properties.containsKey("Enabled")) {
                    val enabled = selectedObj.properties["Enabled"] as? Boolean ?: true
                    PropertyRow(label = "Enabled") {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { onUpdateProperty(selectedObj.id, "Enabled", it) },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }

                if (selectedObj.properties.containsKey("Offset")) {
                    val offset = selectedObj.properties["Offset"] as? Vector2 ?: Vector2(0f, 4f)
                    Vector2Editor(
                        label = "Offset",
                        vector2 = offset,
                        onValueChange = { onUpdateProperty(selectedObj.id, "Offset", it) }
                    )
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

            val isGuiObject = selectedObj.className in listOf(
                RobloxClass.Frame, RobloxClass.TextLabel, RobloxClass.TextButton,
                RobloxClass.ImageLabel, RobloxClass.ImageButton, RobloxClass.ScrollingFrame,
                RobloxClass.ViewportFrame
            )
            if (isGuiObject) {
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

    // Gradient Color Picker Dialog
    if (activeGradientProp != null) {
        val (propName, curStr) = activeGradientProp!!
        GradientPickerDialog(
            initialColorString = curStr,
            onDismiss = { activeGradientProp = null },
            onSave = { newStr ->
                onUpdateProperty(selectedObj.id, propName, newStr)
                activeGradientProp = null
            },
            lang = lang
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
                .height(30.dp)
                .background(Color(0xFF30333A), RoundedCornerShape(3.dp))
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
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
            modifier = Modifier.weight(1.35f),
            contentAlignment = Alignment.CenterEnd
        ) {
            content()
        }
    }
}

@Composable
fun StudioDropdownField(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32353B))
        ) {
            Text(value, fontSize = 10.sp, color = Color.White, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 11.sp) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
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
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            cursorBrush = SolidColor(Color(0, 162, 255)),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1C1E22), RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFF3D4148), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(2.dp))
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
            modifier = Modifier.size(28.dp)
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
            modifier = Modifier.size(28.dp)
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

@Composable
fun RobloxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
        modifier = modifier,
        cursorBrush = SolidColor(Color(0, 162, 255)),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(18, 18, 22), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(55, 55, 60), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.Gray, fontSize = 10.sp)
                }
                innerTextField()
            }
        }
    )
}
