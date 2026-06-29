package com.example.ui.editor

import com.example.data.model.*
import java.util.Locale

object LuauGenerator {

    fun generate(root: RobloxObject, style: String = "Standard", includeMountCode: Boolean = true): String {
        val sb = StringBuilder()
        
        sb.append("--[[\n")
        sb.append("    Generated with Roblox UI Designer Mobile\n")
        sb.append("    ----------------------------------------\n")
        sb.append("    - Hướng dẫn: Bạn có thể sử dụng StyLua để tự động định dạng (format) lại đoạn mã này.\n")
        sb.append("    - Hướng dẫn: Sử dụng Darklua để build/bundle, tối ưu hóa hoặc rút gọn (minify) code.\n")
        sb.append("]]\n\n")
        if (includeMountCode) {
            sb.append("local Players = game:GetService(\"Players\")\n")
            sb.append("local player = Players.LocalPlayer\n")
            sb.append("local playerGui = player:WaitForChild(\"PlayerGui\")\n\n")
        } else {
            sb.append("-- Paste this in a LocalScript inside your ScreenGui\n\n")
        }
        
        sb.append("local GUI = {}\n\n")
        
        val keys = mutableMapOf<String, String>()
        val keyCounters = mutableMapOf<String, Int>()
        
        fun getObjectKey(obj: RobloxObject): String {
            val existing = keys[obj.id]
            if (existing != null) return existing
            
            // Generate a clean identifier name for the table key
            val cleaned = obj.name.replace("[^a-zA-Z0-9_]".toRegex(), "")
            val base = "GUI_" + if (cleaned.isEmpty()) obj.className.name else cleaned
            
            val count = keyCounters[base] ?: 0
            val uniqueKey = if (count == 0) base else "${base}_$count"
            keyCounters[base] = count + 1
            
            keys[obj.id] = uniqueKey
            return uniqueKey
        }
        
        fun generateObjectCode(obj: RobloxObject, parentKey: String?) {
            val key = getObjectKey(obj)
            
            sb.append("-- ${obj.className.name}: ${obj.name}\n")
            if (parentKey == null && !includeMountCode) {
                sb.append("GUI[\"$key\"] = script.Parent -- Root element assuming script is inside it\n")
            } else {
                sb.append("GUI[\"$key\"] = Instance.new(\"${obj.className.name}\")\n")
                sb.append("GUI[\"$key\"][\"Name\"] = \"${obj.name}\"\n")
            }
            
            // Assign properties
            obj.properties.forEach { (name, value) ->
                if (isPropertyRelevant(obj.className, name)) {
                    val formatted = formatPropertyValue(obj.className, name, value)
                    if (formatted != null) {
                        sb.append("GUI[\"$key\"][\"$name\"] = $formatted\n")
                    }
                }
            }
            
            // Assign parent
            if (parentKey == null) {
                if (includeMountCode && obj.className == RobloxClass.ScreenGui) {
                    sb.append("GUI[\"$key\"][\"Parent\"] = playerGui\n")
                }
            } else {
                sb.append("GUI[\"$key\"][\"Parent\"] = GUI[\"$parentKey\"]\n")
            }
            sb.append("\n")
            
            // Generate for children
            obj.children.forEach { child ->
                generateObjectCode(child, key)
            }
        }
        
        generateObjectCode(root, null)
        
        sb.append("return GUI\n")
        return sb.toString()
    }
    
    private fun formatGradientColor(colorStr: String): String {
        return try {
            if (colorStr.contains(";")) {
                val keypoints = colorStr.split(";").map { part ->
                    val segments = part.split(":")
                    val pos = segments[0].toFloat()
                    val rgb = segments[1].split(",").map { it.trim().toInt() }
                    "ColorSequenceKeypoint.new($pos, Color3.fromRGB(${rgb[0]}, ${rgb[1]}, ${rgb[2]}))"
                }
                "ColorSequence.new({\n\t" + keypoints.joinToString(",\n\t") + "\n})"
            } else {
                val colors = colorStr.split(" to ").map { part ->
                    val rgb = part.split(",").map { it.trim().toInt() }
                    "Color3.fromRGB(${rgb[0]}, ${rgb[1]}, ${rgb[2]})"
                }
                "ColorSequence.new(${colors[0]}, ${colors[1]})"
            }
        } catch (e: Exception) {
            "ColorSequence.new(Color3.fromRGB(255, 255, 255), Color3.fromRGB(150, 150, 150))"
        }
    }
    
    private fun formatPropertyValue(className: RobloxClass, propName: String, value: Any): String? {
        if (className == RobloxClass.UIGradient && propName == "Color" && value is String) {
            return formatGradientColor(value)
        }
        
        val enumProperties = mapOf(
            "Font" to "Enum.Font.",
            "ScaleType" to "Enum.ScaleType.",
            "FillDirection" to "Enum.FillDirection.",
            "HorizontalAlignment" to "Enum.HorizontalAlignment.",
            "VerticalAlignment" to "Enum.VerticalAlignment.",
            "SortOrder" to "Enum.SortOrder.",
            "ScrollingDirection" to "Enum.ScrollingDirection.",
            "AspectType" to "Enum.AspectType.",
            "DominantAxis" to "Enum.DominantAxis.",
            "ApplyStrokeMode" to "Enum.ApplyStrokeMode."
        )

        if (propName in enumProperties.keys && value is String) {
            return enumProperties[propName] + value
        }
        
        return when (value) {
            is Boolean -> if (value) "true" else "false"
            is Int -> value.toString()
            is Float -> value.toString()
            is Double -> value.toString()
            is String -> "\"${value.replace("\"", "\\\"")}\""
            is UDim2 -> "UDim2.new(${value.scaleX}, ${value.offsetX}, ${value.scaleY}, ${value.offsetY})"
            is Vector2 -> "Vector2.new(${value.x}, ${value.y})"
            is Color3 -> "Color3.fromRGB(${value.r}, ${value.g}, ${value.b})"
            else -> null
        }
    }
    
    private fun isPropertyRelevant(className: RobloxClass, propName: String): Boolean {
        val layoutOnlyProps = listOf("FillDirection", "HorizontalAlignment", "VerticalAlignment", "Padding", "SortOrder", "CellPadding", "CellSize")
        val paddingOnlyProps = listOf("PaddingTop", "PaddingBottom", "PaddingLeft", "PaddingRight")
        val cornerOnlyProps = listOf("CornerRadius", "TopLeft", "TopRight", "BottomLeft", "BottomRight")
        val strokeOnlyProps = listOf("Color", "Thickness", "Transparency", "ApplyStrokeMode")
        val textOnlyProps = listOf("Text", "TextColor3", "TextSize", "TextTransparency", "TextWrapped", "TextScaled", "Font", "TextXAlignment", "TextYAlignment", "RichText")
        val imageOnlyProps = listOf("Image", "ImageTransparency", "ScaleType")
        val frameOnlyProps = listOf("BackgroundColor3", "BackgroundTransparency", "BorderSizePixel")
        val shadowOnlyProps = listOf("Color", "Transparency", "Blur", "Spread", "Offset", "Enabled")
        
        val isGuiObject = className in listOf(
            RobloxClass.Frame, RobloxClass.TextLabel, RobloxClass.TextButton,
            RobloxClass.ImageLabel, RobloxClass.ImageButton, RobloxClass.ScrollingFrame,
            RobloxClass.ViewportFrame
        )
        if (propName == "Visible" || propName == "Active" || propName == "ZIndex" || propName == "LayoutOrder") {
            return isGuiObject
        }
        if (propName == "Size" || propName == "Position" || propName == "AnchorPoint") {
            return className in listOf(
                RobloxClass.Frame, RobloxClass.TextLabel, RobloxClass.TextButton,
                RobloxClass.ImageLabel, RobloxClass.ImageButton, RobloxClass.ScrollingFrame,
                RobloxClass.ViewportFrame
            )
        }
        
        when (className) {
            RobloxClass.ScreenGui -> return propName in listOf("ResetOnSpawn", "Enabled")
            RobloxClass.Frame -> return propName in frameOnlyProps || propName == "ClipsDescendants"
            RobloxClass.TextLabel -> return propName in frameOnlyProps || propName in textOnlyProps
            RobloxClass.TextButton -> return propName in frameOnlyProps || propName in textOnlyProps || propName == "AutoButtonColor"
            RobloxClass.ImageLabel -> return propName in frameOnlyProps || propName in imageOnlyProps
            RobloxClass.ImageButton -> return propName in frameOnlyProps || propName in imageOnlyProps || propName == "AutoButtonColor"
            RobloxClass.ScrollingFrame -> return propName in frameOnlyProps || propName in listOf("CanvasSize", "ScrollBarThickness", "ScrollingDirection", "ClipsDescendants")
            RobloxClass.ViewportFrame -> return propName in frameOnlyProps || propName == "ImageTransparency"
            RobloxClass.UIListLayout -> return propName in layoutOnlyProps
            RobloxClass.UIGridLayout -> return propName in layoutOnlyProps
            RobloxClass.UIPadding -> return propName in paddingOnlyProps
            RobloxClass.UICorner -> return propName in cornerOnlyProps
            RobloxClass.UIStroke -> return propName in strokeOnlyProps
            RobloxClass.UIGradient -> return propName in listOf("Color", "Rotation", "Transparency")
            RobloxClass.UIScale -> return propName == "Scale"
            RobloxClass.UIAspectRatioConstraint -> return propName in listOf("AspectRatio", "AspectType", "DominantAxis")
            RobloxClass.LocalScript, RobloxClass.ModuleScript -> return propName == "Source"
            RobloxClass.UIShadow -> return propName in shadowOnlyProps
        }
    }
}
