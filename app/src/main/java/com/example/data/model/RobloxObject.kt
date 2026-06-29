package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class RobloxClass {
    ScreenGui,
    Frame,
    TextLabel,
    TextButton,
    ImageLabel,
    ImageButton,
    ScrollingFrame,
    ViewportFrame,
    UIListLayout,
    UIGridLayout,
    UIPadding,
    UICorner,
    UIStroke,
    UIGradient,
    UIScale,
    UIAspectRatioConstraint,
    LocalScript,
    ModuleScript,
    UIShadow
}

data class UDim2(val scaleX: Float, val offsetX: Int, val scaleY: Float, val offsetY: Int) {
    override fun toString(): String = "{$scaleX, $offsetX}, {$scaleY, $offsetY}"
    fun toLuau(): String = "UDim2.new($scaleX, $offsetX, $scaleY, $offsetY)"
}

data class Vector2(val x: Float, val y: Float) {
    override fun toString(): String = "$x, $y"
    fun toLuau(): String = "Vector2.new($x, $y)"
}

data class Color3(val r: Int, val g: Int, val b: Int) {
    override fun toString(): String = "$r, $g, $b"
    fun toLuau(): String = "Color3.fromRGB($r, $g, $b)"
    fun toHex(): String = String.format("#%02X%02X%02X", r, g, b)
}

data class RobloxObject(
    val id: String,
    val className: RobloxClass,
    val name: String,
    val properties: Map<String, Any>,
    val children: List<RobloxObject> = emptyList()
)

fun RobloxObject.toJSONObject(): JSONObject {
    val obj = JSONObject()
    obj.put("id", id)
    obj.put("className", className.name)
    obj.put("name", name)
    
    val propsJson = JSONObject()
    properties.forEach { (k, v) ->
        when (v) {
            is UDim2 -> {
                val u = JSONObject()
                u.put("scaleX", v.scaleX)
                u.put("offsetX", v.offsetX)
                u.put("scaleY", v.scaleY)
                u.put("offsetY", v.offsetY)
                propsJson.put(k, u)
            }
            is Vector2 -> {
                val vec = JSONObject()
                vec.put("x", v.x)
                vec.put("y", v.y)
                propsJson.put(k, vec)
            }
            is Color3 -> {
                val col = JSONObject()
                col.put("r", v.r)
                col.put("g", v.g)
                col.put("b", v.b)
                propsJson.put(k, col)
            }
            else -> {
                propsJson.put(k, v)
            }
        }
    }
    obj.put("properties", propsJson)
    
    val childrenArray = JSONArray()
    children.forEach { childrenArray.put(it.toJSONObject()) }
    obj.put("children", childrenArray)
    
    return obj
}

fun JSONObjectToRobloxObject(obj: JSONObject): RobloxObject {
    val id = obj.getString("id")
    val className = RobloxClass.valueOf(obj.getString("className"))
    val name = obj.getString("name")
    
    val propsJson = obj.getJSONObject("properties")
    val properties = mutableMapOf<String, Any>()
    propsJson.keys().forEach { k ->
        val v = propsJson.get(k)
        if (v is JSONObject) {
            if (v.has("scaleX") && v.has("offsetX") && v.has("scaleY") && v.has("offsetY")) {
                properties[k] = UDim2(
                    v.getDouble("scaleX").toFloat(),
                    v.getInt("offsetX"),
                    v.getDouble("scaleY").toFloat(),
                    v.getInt("offsetY")
                )
            } else if (v.has("x") && v.has("y")) {
                properties[k] = Vector2(
                    v.getDouble("x").toFloat(),
                    v.getDouble("y").toFloat()
                )
            } else if (v.has("r") && v.has("g") && v.has("b")) {
                properties[k] = Color3(
                    v.getInt("r"),
                    v.getInt("g"),
                    v.getInt("b")
                )
            } else {
                properties[k] = v.toString()
            }
        } else {
            // Numbers in JSON can be Double or Int
            if (v is Double) {
                // If it can be parsed as Int
                if (v == v.toInt().toDouble()) {
                    properties[k] = v.toInt()
                } else {
                    properties[k] = v.toFloat()
                }
            } else if (v is Int) {
                properties[k] = v
            } else {
                properties[k] = v
            }
        }
    }
    
    val childrenList = mutableListOf<RobloxObject>()
    if (obj.has("children")) {
        val childrenArray = obj.getJSONArray("children")
        for (i in 0 until childrenArray.length()) {
            childrenList.add(JSONObjectToRobloxObject(childrenArray.getJSONObject(i)))
        }
    }
    
    return RobloxObject(id, className, name, properties, childrenList)
}

fun createDefaultObject(className: RobloxClass, customName: String? = null): RobloxObject {
    val id = "obj_" + java.util.UUID.randomUUID().toString().replace("-", "").take(8)
    val defaultName = customName ?: className.name
    val properties = mutableMapOf<String, Any>()
    
    // Core structural values for GUI Objects (subclasses of GuiObject)
    val isGuiObject = className in setOf(
        RobloxClass.Frame,
        RobloxClass.TextLabel,
        RobloxClass.TextButton,
        RobloxClass.ImageLabel,
        RobloxClass.ImageButton,
        RobloxClass.ScrollingFrame,
        RobloxClass.ViewportFrame
    )
    if (isGuiObject) {
        properties["Visible"] = true
        properties["Active"] = true
        properties["ZIndex"] = 1
        properties["LayoutOrder"] = 0
    }
    
    when (className) {
        RobloxClass.ScreenGui -> {
            properties["ResetOnSpawn"] = true
            properties["Enabled"] = true
        }
        RobloxClass.Frame -> {
            properties["Size"] = UDim2(0.3f, 0, 0.3f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(45, 45, 48)
            properties["BackgroundTransparency"] = 0f
            properties["BorderSizePixel"] = 1
            properties["ClipsDescendants"] = false
        }
        RobloxClass.TextLabel -> {
            properties["Size"] = UDim2(0.2f, 0, 0.1f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(255, 255, 255)
            properties["BackgroundTransparency"] = 1f
            properties["BorderSizePixel"] = 0
            properties["Text"] = "Label Text"
            properties["TextColor3"] = Color3(255, 255, 255)
            properties["TextSize"] = 14
            properties["TextTransparency"] = 0f
            properties["TextWrapped"] = true
            properties["TextScaled"] = false
            properties["Font"] = "SourceSans"
            properties["TextXAlignment"] = "Center"
            properties["TextYAlignment"] = "Center"
            properties["RichText"] = false
        }
        RobloxClass.TextButton -> {
            properties["Size"] = UDim2(0.2f, 0, 0.08f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(0, 162, 255)
            properties["BackgroundTransparency"] = 0f
            properties["BorderSizePixel"] = 0
            properties["Text"] = "Click Me"
            properties["TextColor3"] = Color3(255, 255, 255)
            properties["TextSize"] = 14
            properties["TextTransparency"] = 0f
            properties["TextWrapped"] = true
            properties["TextScaled"] = false
            properties["Font"] = "SourceSansBold"
            properties["TextXAlignment"] = "Center"
            properties["TextYAlignment"] = "Center"
            properties["RichText"] = false
            properties["AutoButtonColor"] = true
        }
        RobloxClass.ImageLabel -> {
            properties["Size"] = UDim2(0.15f, 0, 0.15f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(255, 255, 255)
            properties["BackgroundTransparency"] = 1f
            properties["BorderSizePixel"] = 0
            properties["Image"] = "rbxassetid://0"
            properties["ImageTransparency"] = 0f
            properties["ScaleType"] = "Stretch"
        }
        RobloxClass.ImageButton -> {
            properties["Size"] = UDim2(0.15f, 0, 0.15f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(255, 255, 255)
            properties["BackgroundTransparency"] = 1f
            properties["BorderSizePixel"] = 0
            properties["Image"] = "rbxassetid://0"
            properties["ImageTransparency"] = 0f
            properties["ScaleType"] = "Stretch"
            properties["AutoButtonColor"] = true
        }
        RobloxClass.ScrollingFrame -> {
            properties["Size"] = UDim2(0.4f, 0, 0.4f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(35, 35, 35)
            properties["BackgroundTransparency"] = 0f
            properties["BorderSizePixel"] = 1
            properties["CanvasSize"] = UDim2(0f, 0, 2f, 0)
            properties["ScrollBarThickness"] = 8
            properties["ScrollingDirection"] = "Y"
            properties["ClipsDescendants"] = true
        }
        RobloxClass.ViewportFrame -> {
            properties["Size"] = UDim2(0.3f, 0, 0.3f, 0)
            properties["Position"] = UDim2(0.1f, 0, 0.1f, 0)
            properties["AnchorPoint"] = Vector2(0f, 0f)
            properties["BackgroundColor3"] = Color3(0, 0, 0)
            properties["BackgroundTransparency"] = 0f
            properties["BorderSizePixel"] = 1
            properties["ImageTransparency"] = 0f
        }
        RobloxClass.UIListLayout -> {
            properties["FillDirection"] = "Vertical"
            properties["HorizontalAlignment"] = "Left"
            properties["VerticalAlignment"] = "Top"
            properties["Padding"] = UDim2(0f, 0, 0f, 8)
            properties["SortOrder"] = "LayoutOrder"
        }
        RobloxClass.UIGridLayout -> {
            properties["CellPadding"] = UDim2(0f, 6, 0f, 6)
            properties["CellSize"] = UDim2(0f, 80, 0f, 80)
            properties["FillDirection"] = "Horizontal"
            properties["SortOrder"] = "LayoutOrder"
        }
        RobloxClass.UIPadding -> {
            properties["PaddingTop"] = UDim2(0f, 8, 0f, 8)
            properties["PaddingBottom"] = UDim2(0f, 8, 0f, 8)
            properties["PaddingLeft"] = UDim2(0f, 8, 0f, 8)
            properties["PaddingRight"] = UDim2(0f, 8, 0f, 8)
        }
        RobloxClass.UICorner -> {
            properties["CornerRadius"] = UDim2(0f, 8, 0f, 8)
            properties["TopLeft"] = UDim2(0f, 8, 0f, 8)
            properties["TopRight"] = UDim2(0f, 8, 0f, 8)
            properties["BottomLeft"] = UDim2(0f, 8, 0f, 8)
            properties["BottomRight"] = UDim2(0f, 8, 0f, 8)
        }
        RobloxClass.UIShadow -> {
            properties["Color"] = Color3(0, 0, 0)
            properties["Transparency"] = 0.5f
            properties["Blur"] = 8
            properties["Spread"] = 0
            properties["Offset"] = Vector2(0f, 4f)
            properties["Enabled"] = true
        }
        RobloxClass.UIStroke -> {
            properties["Color"] = Color3(255, 255, 255)
            properties["Thickness"] = 2
            properties["Transparency"] = 0f
            properties["ApplyStrokeMode"] = "Contextual"
        }
        RobloxClass.UIGradient -> {
            properties["Color"] = "255,255,255 to 150,150,150"
            properties["Rotation"] = 90f
            properties["Transparency"] = 0f
        }
        RobloxClass.UIScale -> {
            properties["Scale"] = 1f
        }
        RobloxClass.UIAspectRatioConstraint -> {
            properties["AspectRatio"] = 1.0f
            properties["AspectType"] = "FitWithinMaxSize"
            properties["DominantAxis"] = "Width"
        }
        RobloxClass.LocalScript -> {
            properties["Source"] = "-- LocalScript logic here\nprint('Hello from script!')"
        }
        RobloxClass.ModuleScript -> {
            properties["Source"] = "local Module = {}\n\nfunction Module.doSomething()\n\tprint('Doing something!')\nend\n\nreturn Module"
        }
    }
    
    return RobloxObject(id, className, defaultName, properties)
}
