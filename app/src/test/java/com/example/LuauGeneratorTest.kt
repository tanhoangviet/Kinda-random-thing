package com.example

import com.example.data.model.Color3
import com.example.data.model.RobloxClass
import com.example.data.model.RobloxObject
import com.example.data.model.UDim2
import com.example.ui.editor.LuauGenerator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuauGeneratorTest {

  @Test
  fun export_usesRobloxPropertyTypes() {
    val root = RobloxObject(
      id = "root",
      className = RobloxClass.ScreenGui,
      name = "Main Gui",
      properties = mapOf("ResetOnSpawn" to true, "Enabled" to true),
      children = listOf(
        RobloxObject(
          id = "frame",
          className = RobloxClass.Frame,
          name = "Frame \"A\"",
          properties = mapOf(
            "Size" to UDim2(0f, 100, 0f, 80),
            "Position" to UDim2(0f, 8, 0f, 12),
            "BackgroundColor3" to Color3(12, 34, 56),
            "BackgroundTransparency" to 0.25f,
            "BorderSizePixel" to 0,
          ),
          children = listOf(
            RobloxObject(
              id = "corner",
              className = RobloxClass.UICorner,
              name = "UICorner",
              properties = mapOf("CornerRadius" to UDim2(0f, 8, 0f, 8)),
            ),
            RobloxObject(
              id = "padding",
              className = RobloxClass.UIPadding,
              name = "UIPadding",
              properties = mapOf("PaddingLeft" to UDim2(0f, 12, 0f, 12)),
            ),
            RobloxObject(
              id = "label",
              className = RobloxClass.TextLabel,
              name = "Label",
              properties = mapOf(
                "Size" to UDim2(1f, 0, 0f, 24),
                "Position" to UDim2(0f, 0, 0f, 0),
                "Text" to "Hello\nWorld",
                "TextColor3" to Color3(255, 255, 255),
                "TextSize" to 14,
                "TextXAlignment" to "Left",
                "TextYAlignment" to "Top",
              ),
            ),
          ),
        ),
      ),
    )

    val code = LuauGenerator.generate(root)

    assertTrue(code.contains("GUI[\"GUI_UICorner\"][\"CornerRadius\"] = UDim.new(0.0, 8)"))
    assertTrue(code.contains("GUI[\"GUI_UIPadding\"][\"PaddingLeft\"] = UDim.new(0.0, 12)"))
    assertTrue(code.contains("Enum.TextXAlignment.Left"))
    assertTrue(code.contains("Enum.TextYAlignment.Top"))
    assertTrue(code.contains("Frame \\\"A\\\""))
    assertTrue(code.contains("Hello\\nWorld"))
    assertFalse(code.contains("CornerRadius\"] = UDim2.new"))
  }

  @Test
  fun rojoBundle_containsPortableProjectFiles() {
    val root = RobloxObject(
      id = "root",
      className = RobloxClass.ScreenGui,
      name = "Main Gui",
      properties = mapOf("ResetOnSpawn" to true, "Enabled" to true),
    )

    val bundle = LuauGenerator.generateRojoBundle(root, "Vanilla Project")

    assertTrue(bundle.contains("===== default.project.json ====="))
    assertTrue(bundle.contains("\"${'$'}className\": \"DataModel\""))
    assertTrue(bundle.contains("\"${'$'}path\": \"src/client/main-gui.client.lua\""))
    assertTrue(bundle.contains("===== src/client/main-gui.client.lua ====="))
    assertTrue(bundle.contains("local playerGui = player:WaitForChild(\"PlayerGui\")"))
  }
}
