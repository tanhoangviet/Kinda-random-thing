package com.example.ui.editor

import com.example.data.model.*
import java.util.Locale

object LuauGenerator {

    fun generate(root: RobloxObject, style: String = "Standard", includeMountCode: Boolean = true): String {
        val sb = StringBuilder()
        
        if (includeMountCode) {
            sb.append("-- Generated with Roblox UI Designer Mobile\n")
            sb.append("local Players = game:GetService(\"Players\")\n")
            sb.append("local player = Players.LocalPlayer\n")
            sb.append("local playerGui = player:WaitForChild(\"PlayerGui\")\n\n")
        } else {
            sb.append("-- Generated with Roblox UI Designer Mobile\n-- Paste this in a LocalScript inside your ScreenGui\n\n")
        }
        
        val varNames = mutableMapOf<String, String>()
        val nameCounters = mutableMapOf<String, Int>()
        
        fun getSafeVarName(obj: RobloxObject): String {
            val existing = varNames[obj.id]
            if (existing != null) return existing
            
            // Generate clean variable names
            var base = obj.name.replace("[^a-zA-Z0-9]".toRegex(), "")
                .replaceFirstChar { it.lowercase(Locale.ROOT) }
            if (base.isEmpty()) base = "instance"
            
            // Ensure variable name doesn't start with numbers
            if (base[0].isDigit()) {
                base = "obj_$base"
            }
            
            // Roblox keywords check
            val keywords = listOf("local", "function", "end", "if", "then", "else", "elseif", "for", "while", "do", "repeat", "until", "return", "nil", "true", "false", "and", "or", "not")
            if (base in keywords) {
                base = "${base}Obj"
            }
            
            val count = nameCounters[base] ?: 0
            val uniqueName = if (count == 0) base else "${base}_$count"
            nameCounters[base] = count + 1
            
            varNames[obj.id] = uniqueName
            return uniqueName
        }
        
        fun generateObjectCode(obj: RobloxObject, parentVar: String?) {
            val varName = getSafeVarName(obj)
            
            // Instance creation
            if (parentVar == null && !includeMountCode) {
                sb.append("-- Root element: ${obj.name}\n")
                sb.append("local $varName = script.Parent -- Assuming running inside the Root\n")
            } else {
                sb.append("local $varName = Instance.new(\"${obj.className}\")\n")
                sb.append("$varName.Name = \"${obj.name}\"\n")
            }
            
            // Assign properties
            obj.properties.forEach { (name, value) ->
                if (isPropertyRelevant(obj.className, name)) {
                    val formatted = formatPropertyValue(value)
                    if (formatted != null) {
                        sb.append("$varName.$name = $formatted\n")
                    }
                }
            }
            
            // Assign parent
            if (parentVar == null) {
                if (includeMountCode && obj.className == RobloxClass.ScreenGui) {
                    sb.append("$varName.Parent = playerGui\n")
                }
            } else {
                sb.append("$varName.Parent = $parentVar\n")
            }
            sb.append("\n")
            
            // Generate for children
            obj.children.forEach { child ->
                generateObjectCode(child, varName)
            }
        }
        
        generateObjectCode(root, null)
        
        return sb.toString()
    }
    
    private fun isPropertyRelevant(className: RobloxClass, propName: String): Boolean {
        // Safe list to exclude properties that aren't valid for that class
        val layoutOnlyProps = listOf("FillDirection", "HorizontalAlignment", "VerticalAlignment", "Padding", "SortOrder", "CellPadding", "CellSize")
        val paddingOnlyProps = listOf("PaddingTop", "PaddingBottom", "PaddingLeft", "PaddingRight")
        val cornerOnlyProps = listOf("CornerRadius", "TopLeft", "TopRight", "BottomLeft", "BottomRight")
        val strokeOnlyProps = listOf("Color", "Thickness", "Transparency", "ApplyStrokeMode")
        val textOnlyProps = listOf("Text", "TextColor3", "TextSize", "TextTransparency", "TextWrapped", "TextScaled", "Font", "TextXAlignment", "TextYAlignment", "RichText")
        val imageOnlyProps = listOf("Image", "ImageTransparency", "ScaleType")
        val frameOnlyProps = listOf("BackgroundColor3", "BackgroundTransparency", "BorderSizePixel")
        val shadowOnlyProps = listOf("Color", "Transparency", "Blur", "Spread", "Offset", "Enabled")
        
        if (propName == "Visible" || propName == "Active" || propName == "ZIndex" || propName == "LayoutOrder") return true
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
    
    private fun formatPropertyValue(value: Any): String? {
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
}
