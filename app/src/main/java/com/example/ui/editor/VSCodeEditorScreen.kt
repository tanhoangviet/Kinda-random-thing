package com.example.ui.editor

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RobloxClass

@Composable
fun VSCodeEditorScreen(
    scriptName: String,
    className: RobloxClass,
    contextClassName: RobloxClass? = null,
    initialSource: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var sourceText by remember(initialSource) { mutableStateOf(initialSource) }
    var savedSnapshot by remember(initialSource) { mutableStateOf(initialSource) }
    val lineCount = remember(sourceText) { sourceText.lines().size.coerceAtLeast(1) }
    val hasUnsavedChanges = sourceText != savedSnapshot
    val editorScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    var styleCheckResult by remember(initialSource) { mutableStateOf<LuauStyleCheckResult?>(null) }
    val scriptKind = if (className == RobloxClass.LocalScript) "LocalScript" else "ModuleScript"
    val fileName = if (className == RobloxClass.LocalScript) "$scriptName.client.lua" else "$scriptName.lua"
    val isPath2DContext = className == RobloxClass.LocalScript && contextClassName == RobloxClass.Path2D
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 900 || configuration.screenHeightDp < 520
    val sidebarWidth = if (isCompact) 152.dp else 224.dp
    val editorMinWidth = if (isCompact) 360.dp else 900.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111318))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1D2027))
                .border(0.5.dp, Color(0xFF343842))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorTrafficDot(Color(0xFFFF5F57))
            Spacer(modifier = Modifier.width(8.dp))
            EditorTrafficDot(Color(0xFFFFBD2E))
            Spacer(modifier = Modifier.width(8.dp))
            EditorTrafficDot(Color(0xFF28C840))
            Spacer(modifier = Modifier.width(14.dp))
            RobloxClassIcon(className = className, iconSize = 20.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName + if (hasUnsavedChanges) " *" else "",
                    color = if (hasUnsavedChanges) Color(0xFF7DD3FC) else Color(0xFFE7EAEE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(scriptKind, color = Color(0xFF8C929C), fontSize = 10.sp)
            }

            EditorToolButton(
                icon = Icons.Default.ContentCopy,
                label = "Copy",
                onClick = { clipboardManager.setText(AnnotatedString(sourceText)) }
            )
            EditorToolButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sourceText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Chia se ma script"))
                }
            )
            EditorToolButton(
                icon = Icons.Default.CheckCircle,
                label = "StyLua",
                onClick = { styleCheckResult = runStyluaStyleCheck(sourceText) }
            )
            Button(
                onClick = {
                    onSave(sourceText)
                    savedSnapshot = sourceText
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasUnsavedChanges) Color(0xFF0A84FF) else Color(0xFF343842),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFE7EAEE))
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .background(Color(0xFF181B22))
                    .border(0.5.dp, Color(0xFF343842))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SCRIPT", color = Color(0xFF8C929C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF222631), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RobloxClassIcon(className = className, iconSize = 22.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            scriptName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(scriptKind, color = Color(0xFF8C929C), fontSize = 10.sp)
                    }
                }

                EditorMetric("Lines", lineCount.toString())
                EditorMetric("Chars", sourceText.length.toString())
                EditorMetric("Mode", if (isPath2DContext) "Path2D" else "Luau")
                EditorMetric("Context", contextClassName?.name ?: "None")

                StyleCheckPanel(styleCheckResult)

                Text("SNIPPETS", color = Color(0xFF8C929C), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                SnippetButton("example code") { sourceText = localScriptExampleCode(scriptName, contextClassName, className) }
                SnippetButton("print") { sourceText += "\nprint(\"Hello from $scriptName\")" }
                SnippetButton("task.wait") { sourceText += "\ntask.wait(1)" }
                if (className == RobloxClass.LocalScript) {
                    SnippetButton("PlayerGui") { sourceText += "\nlocal playerGui = game.Players.LocalPlayer:WaitForChild(\"PlayerGui\")" }
                    SnippetButton("GUI tween") { sourceText += "\n\n" + guiTweenSnippet() }
                    if (isPath2DContext) {
                        Text("PATH2D HELPERS", color = Color(0xFF8C929C), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        SnippetButton("path animate") { sourceText = path2DAnimationSnippet() }
                        SnippetButton("path sample") { sourceText += "\n\n" + path2DSampleSnippet() }
                    }
                } else {
                    SnippetButton("module fn") { sourceText += "\nfunction Module.new()\n\treturn {}\nend" }
                }

                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { styleCheckResult = runStyluaStyleCheck(sourceText) },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    border = BorderStroke(1.dp, Color(0xFF3D4148))
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("StyLua check", fontSize = 11.sp)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color(0xFF171A21))
                        .border(0.5.dp, Color(0xFF343842)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 180.dp, max = 320.dp)
                            .background(Color(0xFF111318))
                            .border(0.5.dp, Color(0xFF343842))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RobloxClassIcon(className = className, iconSize = 16.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(fileName, color = Color(0xFFE7EAEE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF8C929C), modifier = Modifier.size(16.dp).clickable { onBack() })
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF111318))
                        .verticalScroll(editorScroll)
                        .horizontalScroll(horizontalScroll)
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .padding(end = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lineCount) {
                            Text(
                                text = i.toString(),
                                color = Color(0xFF5F6673),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    BasicTextField(
                        value = sourceText,
                        onValueChange = { sourceText = it },
                        modifier = Modifier
                            .widthIn(min = editorMinWidth)
                            .padding(end = 24.dp),
                        textStyle = TextStyle(
                            color = Color(0xFFE7EAEE),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8))
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Color(0xFF0A84FF))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$scriptKind  |  UTF-8  |  Luau", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Lines $lineCount  |  ${if (hasUnsavedChanges) "Unsaved" else "Saved"}", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = Color(0xFFE7EAEE), modifier = Modifier.size(18.dp))
            Text(label, color = Color(0xFF8C929C), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SnippetButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF222631))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFE7EAEE), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun EditorMetric(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222631), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF8C929C), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StyleCheckPanel(result: LuauStyleCheckResult?) {
    if (result == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222631))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF5F6673), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text("StyLua check not run", color = Color(0xFF8C929C), fontSize = 10.sp)
        }
        return
    }

    val accent = if (result.ok) Color(0xFF28C840) else Color(0xFFFFBD2E)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222631), RoundedCornerShape(6.dp))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(accent, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(result.summary, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        result.diagnostics.take(3).forEach { diagnostic ->
            Text(
                text = "L${diagnostic.line}: ${diagnostic.message}",
                color = Color(0xFFC6CED8),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorTrafficDot(color: Color) {
    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
}

private fun localScriptExampleCode(
    scriptName: String,
    contextClassName: RobloxClass?,
    className: RobloxClass
): String {
    if (className == RobloxClass.ModuleScript) {
        return """
--!strict
local ${scriptName.replace("[^A-Za-z0-9_]".toRegex(), "").ifBlank { "Module" }} = {}

function ${scriptName.replace("[^A-Za-z0-9_]".toRegex(), "").ifBlank { "Module" }}.init()
	print("Module ready")
end

return ${scriptName.replace("[^A-Za-z0-9_]".toRegex(), "").ifBlank { "Module" }}
""".trimIndent()
    }

    return if (contextClassName == RobloxClass.Path2D) {
        path2DAnimationSnippet()
    } else {
        """
--!strict
local TweenService = game:GetService("TweenService")

local guiObject = script.Parent :: GuiObject
local tweenInfo = TweenInfo.new(0.28, Enum.EasingStyle.Quad, Enum.EasingDirection.Out)
local originalSize = guiObject.Size

local pulse = TweenService:Create(guiObject, tweenInfo, {
	Size = originalSize + UDim2.fromOffset(10, 6),
	BackgroundTransparency = 0.08,
})

pulse:Play()
""".trimIndent()
    }
}

private fun guiTweenSnippet(): String {
    return """
local TweenService = game:GetService("TweenService")
local guiObject = script.Parent :: GuiObject

local tween = TweenService:Create(guiObject, TweenInfo.new(0.35), {
	Position = guiObject.Position + UDim2.fromOffset(0, -12),
})

tween:Play()
""".trimIndent()
}

private fun path2DSampleSnippet(): String {
    return """
local path = script.Parent :: Path2D

local ok, position = pcall(function()
	return path:GetPositionOnCurveArcLength(0.5)
end)

if ok then
	print("Midpoint", position)
end
""".trimIndent()
}

private fun path2DAnimationSnippet(): String {
    return """
--!strict
local RunService = game:GetService("RunService")

local path = script.Parent :: Path2D
local marker = Instance.new("Frame")
marker.Name = "PathMarker"
marker.Size = UDim2.fromOffset(14, 14)
marker.AnchorPoint = Vector2.new(0.5, 0.5)
marker.BackgroundColor3 = path.Color3
marker.BorderSizePixel = 0
marker.Parent = path.Parent

local corner = Instance.new("UICorner")
corner.CornerRadius = UDim.new(1, 0)
corner.Parent = marker

local elapsed = 0
local connection: RBXScriptConnection?

connection = RunService.RenderStepped:Connect(function(deltaTime)
	elapsed += deltaTime
	local alpha = (math.sin(elapsed * 1.6) + 1) / 2

	local ok, position = pcall(function()
		return path:GetPositionOnCurveArcLength(alpha)
	end)

	if ok then
		marker.Position = position
	end
end)

script.Destroying:Connect(function()
	if connection then
		connection:Disconnect()
	end
	marker:Destroy()
end)
""".trimIndent()
}
