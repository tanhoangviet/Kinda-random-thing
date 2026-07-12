package com.example.ui.editor

import com.example.data.model.*
import java.util.Locale

object LuauGenerator {

    fun generate(
        root: RobloxObject,
        style: String = "Standard",
        includeMountCode: Boolean = true,
        canvasWidth: Int = 1280,
        canvasHeight: Int = 720
    ): String {
        val sb = StringBuilder()
        val needsCustomAssetLoader = hasExternalImage(root)
        
        sb.append("--!strict\n")
        sb.append("--[[\n")
        sb.append("    Generated with Roblox UI Designer Mobile")
        if (style.isNotBlank()) {
            sb.append(" ($style)")
        }
        sb.append("\n")
        sb.append("    ----------------------------------------\n")
        sb.append("    - Strict Luau, StyLua-friendly formatting, and Darklua-ready output.\n")
        sb.append("    - GUI coordinates keep their scale and offset so exports stay DPI-safe.\n")
        sb.append("]]\n\n")
        if (includeMountCode) {
            sb.append("local Players = game:GetService(\"Players\")\n")
            sb.append("local player = Players.LocalPlayer :: Player\n")
            sb.append("local playerGui = player:WaitForChild(\"PlayerGui\") :: PlayerGui\n\n")
        } else {
            sb.append("-- Paste this in a LocalScript inside your ScreenGui\n\n")
        }

        if (needsCustomAssetLoader) {
            appendCustomAssetLoader(sb)
        }
        
        sb.append("local GUI: {[string]: any} = {}\n\n")
        
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
        
        fun generateObjectCode(
            obj: RobloxObject,
            parentKey: String?,
            parentW: Float,
            parentH: Float,
            depth: Int
        ) {
            val key = getObjectKey(obj)
            val objectBounds = resolveObjectExportBounds(obj, parentW, parentH)
            
            sb.append("-- ${obj.className.name}: ${obj.name}\n")
            if (parentKey == null && !includeMountCode) {
                sb.append("GUI[\"$key\"] = script.Parent -- Root element assuming script is inside it\n")
            } else {
                sb.append("GUI[\"$key\"] = Instance.new(\"${obj.className.name}\")\n")
                sb.append("GUI[\"$key\"][\"Name\"] = ${formatLuauString(obj.name)}\n")
            }
            
            // Assign properties
            obj.properties.forEach { (name, value) ->
                val exportName = normalizeExportPropertyName(obj.className, name)
                if (isPropertyRelevant(obj.className, exportName)) {
                    if (obj.className in listOf(RobloxClass.ImageLabel, RobloxClass.ImageButton) && exportName == "Image" && value is String) {
                        val customAssetCode = formatCustomImageAssetAssignment(key, value)
                        if (customAssetCode != null) {
                            sb.append(customAssetCode)
                            return@forEach
                        }
                    }
                    if (obj.className == RobloxClass.Path2D && exportName == "ControlPoints" && value is String) {
                        sb.append(formatPath2DControlPoints(key, value))
                        return@forEach
                    }
                    val formatted = formatPropertyValue(
                        className = obj.className,
                        propName = exportName,
                        value = value
                    )
                    if (formatted != null) {
                        sb.append("GUI[\"$key\"][\"$exportName\"] = $formatted\n")
                    }
                }
            }
            if (obj.className == RobloxClass.ScreenGui) {
                appendScreenGuiExportDefaults(sb, key, obj)
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
            obj.children.forEachIndexed { index, child ->
                generateObjectCode(
                    obj = child,
                    parentKey = key,
                    parentW = objectBounds.first,
                    parentH = objectBounds.second,
                    depth = depth + 1
                )
            }
        }
        
        generateObjectCode(
            obj = root,
            parentKey = null,
            parentW = canvasWidth.coerceAtLeast(1).toFloat(),
            parentH = canvasHeight.coerceAtLeast(1).toFloat(),
            depth = 0
        )
        
        sb.append("return GUI\n")
        return sb.toString()
    }

    fun generateRojoBundle(
        root: RobloxObject,
        projectName: String = "Vanilla UI",
        canvasWidth: Int = 1280,
        canvasHeight: Int = 720
    ): String {
        val safeProjectName = projectName.ifBlank { "Vanilla UI" }
        val scriptFileName = "${safeFileName(root.name)}.client.lua"
        val defaultProjectJson = """
{
  "name": ${formatJsonString(safeProjectName)},
  "tree": {
    "${'$'}className": "DataModel",
    "StarterPlayer": {
      "${'$'}className": "StarterPlayer",
      "StarterPlayerScripts": {
        "${'$'}className": "StarterPlayerScripts",
        "BuildVanillaUI": {
          "${'$'}path": "src/client/$scriptFileName"
        }
      }
    }
  }
}
""".trimIndent()

        return buildString {
            appendLine("-- Rojo portable export")
            appendLine("-- Tao 2 file ben duoi trong project Rojo cua ban.")
            appendLine()
            appendLine("===== default.project.json =====")
            appendLine(defaultProjectJson)
            appendLine()
            appendLine("===== src/client/$scriptFileName =====")
            append(
                generate(
                    root = root,
                    style = "Rojo",
                    includeMountCode = true,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight
                )
            )
        }
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

    private fun normalizeExportPropertyName(className: RobloxClass, propName: String): String {
        if (className != RobloxClass.UICorner) return propName
        return when (propName) {
            "TopLeft" -> "TopLeftRadius"
            "TopRight" -> "TopRightRadius"
            "BottomLeft" -> "BottomLeftRadius"
            "BottomRight" -> "BottomRightRadius"
            else -> propName
        }
    }

    private fun appendScreenGuiExportDefaults(sb: StringBuilder, key: String, obj: RobloxObject) {
        if (!obj.properties.containsKey("IgnoreGuiInset")) {
            sb.append("GUI[\"$key\"][\"IgnoreGuiInset\"] = true\n")
        }
        if (!obj.properties.containsKey("DisplayOrder")) {
            sb.append("GUI[\"$key\"][\"DisplayOrder\"] = 999\n")
        }
        if (!obj.properties.containsKey("ZIndexBehavior")) {
            sb.append("GUI[\"$key\"][\"ZIndexBehavior\"] = Enum.ZIndexBehavior.Sibling\n")
        }
    }

    private fun formatNumberSequence(sequenceStr: String): String {
        return try {
            val keypoints = sequenceStr
                .split(';')
                .mapNotNull { part ->
                    val segments = part.split(':')
                    if (segments.size < 2) return@mapNotNull null
                    val time = segments[0].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
                    val value = segments[1].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
                    time to value
                }
                .distinctBy { it.first }
                .sortedBy { it.first }
                .toMutableList()

            if (keypoints.isEmpty()) {
                keypoints.add(0f to 0f)
                keypoints.add(1f to 0f)
            }
            if (keypoints.first().first != 0f) {
                keypoints.add(0, 0f to keypoints.first().second)
            }
            if (keypoints.last().first != 1f) {
                keypoints.add(1f to keypoints.last().second)
            }

            val formatted = keypoints.joinToString(",\n\t") { (time, value) ->
                "NumberSequenceKeypoint.new(${time.toLuauNumber()}, ${value.toLuauNumber()})"
            }
            "NumberSequence.new({\n\t$formatted\n})"
        } catch (e: Exception) {
            "NumberSequence.new(0)"
        }
    }
    
    private fun formatPropertyValue(
        className: RobloxClass,
        propName: String,
        value: Any
    ): String? {
        if (className == RobloxClass.UIGradient && propName == "Color" && value is String) {
            return formatGradientColor(value)
        }
        if (className == RobloxClass.UIGradient && propName == "Transparency" && value is Number) {
            return "NumberSequence.new(${value.toLuauNumber()})"
        }
        if (className == RobloxClass.UIGradient && propName == "Transparency" && value is String) {
            return formatNumberSequence(value)
        }
        if (isUDimProperty(className, propName) && value is UDim2) {
            return formatUDim(propName, value)
        }
        if (shouldNormalizeScaleProperty(className, propName) && value is UDim2) {
            return formatExportUDim2(value)
        }
        if (propName == "ZIndex" && value is Int) {
            return value.toString()
        }
        
        val enumProperties = mapOf(
            "Font" to "Enum.Font.",
            "TextXAlignment" to "Enum.TextXAlignment.",
            "TextYAlignment" to "Enum.TextYAlignment.",
            "ScaleType" to "Enum.ScaleType.",
            "FillDirection" to "Enum.FillDirection.",
            "HorizontalAlignment" to "Enum.HorizontalAlignment.",
            "VerticalAlignment" to "Enum.VerticalAlignment.",
            "SortOrder" to "Enum.SortOrder.",
            "ScrollingDirection" to "Enum.ScrollingDirection.",
            "AspectType" to "Enum.AspectType.",
            "DominantAxis" to "Enum.DominantAxis.",
            "ApplyStrokeMode" to "Enum.ApplyStrokeMode.",
            "ZIndexBehavior" to "Enum.ZIndexBehavior."
        )

        if (propName in enumProperties.keys && value is String) {
            return enumProperties[propName] + value
        }
        
        return when (value) {
            is Boolean -> if (value) "true" else "false"
            is Int -> value.toString()
            is Float -> value.toLuauNumber()
            is Double -> value.toLuauNumber()
            is String -> formatLuauString(value, multiline = propName == "Source")
            is UDim2 -> formatExportUDim2(value)
            is Vector2 -> "Vector2.new(${value.x}, ${value.y})"
            is Color3 -> "Color3.fromRGB(${value.r}, ${value.g}, ${value.b})"
            else -> null
        }
    }

    private fun resolveObjectExportBounds(obj: RobloxObject, parentW: Float, parentH: Float): Pair<Float, Float> {
        val size = obj.properties["Size"] as? UDim2 ?: return parentW.coerceAtLeast(1f) to parentH.coerceAtLeast(1f)
        val width = ((size.scaleX * parentW) + size.offsetX).coerceAtLeast(1f)
        val height = ((size.scaleY * parentH) + size.offsetY).coerceAtLeast(1f)
        return width to height
    }

    private fun shouldNormalizeScaleProperty(className: RobloxClass, propName: String): Boolean {
        return propName in listOf("Position", "Size", "CanvasSize") && className in listOf(
            RobloxClass.Frame,
            RobloxClass.TextLabel,
            RobloxClass.TextButton,
            RobloxClass.ImageLabel,
            RobloxClass.ImageButton,
            RobloxClass.ScrollingFrame,
            RobloxClass.ViewportFrame
        )
    }

    private fun formatExportUDim2(value: UDim2): String {
        val scaleX = value.scaleX
        val scaleY = value.scaleY
        val offsetX = value.offsetX
        val offsetY = value.offsetY
        return when {
            offsetX == 0 && offsetY == 0 -> "UDim2.fromScale(${scaleX.toLuauNumber()}, ${scaleY.toLuauNumber()})"
            scaleX == 0f && scaleY == 0f -> "UDim2.fromOffset($offsetX, $offsetY)"
            else -> "UDim2.new(${scaleX.toLuauNumber()}, $offsetX, ${scaleY.toLuauNumber()}, $offsetY)"
        }
    }

    private fun formatCustomImageAssetAssignment(key: String, image: String): String? {
        val trimmed = image.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return null
        }

        val fileName = safeAssetFileName(trimmed)
        val assetPath = fileName
        return "GUI[\"$key\"][\"Image\"] = loadCustomImage(${formatLuauString(trimmed)}, ${formatLuauString(assetPath)})\n"
    }

    private fun formatPath2DControlPoints(key: String, points: String): String {
        val parsedPoints = parsePath2DPoints(points)
        if (parsedPoints.size < 2) {
            return "-- Path2D ${key}: skipped control points because at least two points are required\n"
        }

        return buildString {
            appendLine("GUI[\"$key\"]:SetControlPoints({")
            parsedPoints.forEach { (x, y) ->
                appendLine("\tPath2DControlPoint.new(UDim2.new(${x.toLuauNumber()}, 0, ${y.toLuauNumber()}, 0), UDim2.new(0, 0, 0, 0), UDim2.new(0, 0, 0, 0)),")
            }
            appendLine("})")
        }
    }

    private fun parsePath2DPoints(points: String): List<Pair<Float, Float>> {
        return points
            .split(';')
            .mapNotNull { rawPoint ->
                val parts = rawPoint.trim().split(',')
                if (parts.size < 2) return@mapNotNull null
                val x = parts[0].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
                val y = parts[1].trim().toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
                x to y
            }
    }

    private fun appendCustomAssetLoader(sb: StringBuilder) {
        sb.appendLine("local executorEnv = getfenv() :: any")
        sb.appendLine("local writefileFn = executorEnv.writefile")
        sb.appendLine("local readfileFn = executorEnv.readfile")
        sb.appendLine("local isfileFn = executorEnv.isfile")
        sb.appendLine("local getCustomAssetFn = executorEnv.getcustomasset or executorEnv.GetCustomAsset")
        sb.appendLine()
        sb.appendLine("local function loadCustomImage(imageUrl: string, imagePath: string): string")
        sb.appendLine("\tif typeof(writefileFn) == \"function\" and typeof(readfileFn) == \"function\" and typeof(isfileFn) == \"function\" and typeof(getCustomAssetFn) == \"function\" then")
        sb.appendLine("\t\tlocal hasFile = isfileFn(imagePath)")
        sb.appendLine("\t\tlocal cachedData = if hasFile then readfileFn(imagePath) else nil")
        sb.appendLine("\t\tif not hasFile or type(cachedData) ~= \"string\" or #cachedData == 0 then")
        sb.appendLine("\t\t\tlocal ok, result = pcall(function()")
        sb.appendLine("\t\t\t\treturn (game :: any):HttpGet(imageUrl)")
        sb.appendLine("\t\t\tend)")
        sb.appendLine("\t\t\tif ok and type(result) == \"string\" and #result > 0 then")
        sb.appendLine("\t\t\t\twritefileFn(imagePath, result)")
        sb.appendLine("\t\t\telse")
        sb.appendLine("\t\t\t\twarn(\"Vanilla image download failed\", imageUrl, result)")
        sb.appendLine("\t\t\tend")
        sb.appendLine("\t\tend")
        sb.appendLine("\t\treturn getCustomAssetFn(imagePath)")
        sb.appendLine("\tend")
        sb.appendLine("\treturn imageUrl")
        sb.appendLine("end")
        sb.appendLine()
    }

    private fun isUDimProperty(className: RobloxClass, propName: String): Boolean {
        return when (className) {
            RobloxClass.UIListLayout -> propName == "Padding"
            RobloxClass.UIPadding -> propName in listOf("PaddingTop", "PaddingBottom", "PaddingLeft", "PaddingRight")
            RobloxClass.UICorner -> propName in listOf(
                "CornerRadius",
                "TopLeftRadius",
                "TopRightRadius",
                "BottomLeftRadius",
                "BottomRightRadius"
            )
            else -> false
        }
    }

    private fun formatUDim(propName: String, value: UDim2): String {
        val useY = propName in listOf("Padding", "PaddingTop", "PaddingBottom")
        val scale = if (useY) value.scaleY else value.scaleX
        val offset = if (useY) value.offsetY else value.offsetX
        return "UDim.new($scale, $offset)"
    }

    private fun formatLuauString(value: String, multiline: Boolean = false): String {
        if (multiline && value.contains('\n')) {
            var equals = ""
            while (value.contains("]$equals]")) {
                equals += "="
            }
            return "[$equals[\n$value\n]$equals]"
        }

        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

    private fun Number.toLuauNumber(): String {
        val doubleValue = toDouble()
        if (doubleValue.isNaN() || doubleValue.isInfinite()) return "0"
        return String.format(Locale.US, "%.6f", doubleValue)
            .trimEnd('0')
            .trimEnd('.')
            .ifEmpty { "0" }
    }

    private fun formatJsonString(value: String): String {
        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

    private fun safeFileName(value: String): String {
        return value
            .lowercase(Locale.US)
            .replace("[^a-z0-9_-]+".toRegex(), "-")
            .trim('-')
            .ifBlank { "vanilla-ui" }
    }

    private fun safeAssetFileName(value: String): String {
        val extension = value
            .substringBefore('?')
            .substringAfterLast('.', "png")
            .lowercase(Locale.US)
            .takeIf { it.matches("[a-z0-9]{2,5}".toRegex()) }
            ?: "png"
        return "vanilla_asset_${Integer.toHexString(value.hashCode())}.$extension"
    }

    private fun hasExternalImage(obj: RobloxObject): Boolean {
        val image = obj.properties["Image"] as? String
        if (image != null && (image.startsWith("http://", ignoreCase = true) || image.startsWith("https://", ignoreCase = true))) {
            return true
        }
        return obj.children.any { hasExternalImage(it) }
    }
    
    private fun isPropertyRelevant(className: RobloxClass, propName: String): Boolean {
        val layoutOnlyProps = listOf("FillDirection", "HorizontalAlignment", "VerticalAlignment", "Padding", "SortOrder", "CellPadding", "CellSize")
        val paddingOnlyProps = listOf("PaddingTop", "PaddingBottom", "PaddingLeft", "PaddingRight")
        val cornerOnlyProps = listOf(
            "CornerRadius",
            "TopLeftRadius",
            "TopRightRadius",
            "BottomLeftRadius",
            "BottomRightRadius"
        )
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
        if (propName == "Visible" || propName == "ZIndex") {
            return isGuiObject || className == RobloxClass.Path2D
        }
        if (propName == "Active" || propName == "LayoutOrder") {
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
            RobloxClass.ScreenGui -> return propName in listOf(
                "ResetOnSpawn",
                "Enabled",
                "IgnoreGuiInset",
                "DisplayOrder",
                "ZIndexBehavior"
            )
            RobloxClass.Folder -> return false
            RobloxClass.Frame -> return propName in frameOnlyProps || propName == "ClipsDescendants"
            RobloxClass.TextLabel -> return propName in frameOnlyProps || propName in textOnlyProps
            RobloxClass.TextButton -> return propName in frameOnlyProps || propName in textOnlyProps || propName == "AutoButtonColor"
            RobloxClass.ImageLabel -> return propName in frameOnlyProps || propName in imageOnlyProps
            RobloxClass.ImageButton -> return propName in frameOnlyProps || propName in imageOnlyProps || propName == "AutoButtonColor"
            RobloxClass.ScrollingFrame -> return propName in frameOnlyProps || propName in listOf("CanvasSize", "ScrollBarThickness", "ScrollingDirection", "ClipsDescendants")
            RobloxClass.ViewportFrame -> return propName in frameOnlyProps || propName == "ImageTransparency"
            RobloxClass.Path2D -> return propName in listOf("Visible", "ZIndex", "Color3", "Transparency", "Thickness", "Closed", "ControlPoints")
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
