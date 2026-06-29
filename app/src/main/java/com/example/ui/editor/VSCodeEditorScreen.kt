package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RobloxClass

@Composable
fun VSCodeEditorScreen(
    scriptName: String,
    className: RobloxClass,
    initialSource: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var sourceText by remember(initialSource) { mutableStateOf(initialSource) }
    val lineCount = remember(sourceText) { sourceText.lines().size.coerceAtLeast(1) }
    var hasUnsavedChanges by remember(initialSource, sourceText) { mutableStateOf(sourceText != initialSource) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // VS Code Top Bar (Tab & Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF252526)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tab Header
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color(0xFF1E1E1E))
                    .border(width = 1.dp, color = Color(0xFF333333))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RobloxClassIcon(className = className, iconSize = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$scriptName.lua" + if (hasUnsavedChanges) " •" else "",
                    color = if (hasUnsavedChanges) Color(0xFF00A2FF) else Color(0xFFD4D4D4),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(14.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = Color(0xFF858585),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onBack() }
                )
            }

            // Action Buttons (Save & Back)
            Row(
                modifier = Modifier.padding(end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onSave(sourceText)
                        hasUnsavedChanges = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasUnsavedChanges) Color(0xFF007ACC) else Color(0xFF3A3D41)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lưu mã", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCCCCCC)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFF555555))),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quay lại", fontSize = 11.sp)
                }
            }
        }

        // Code Editor Body (Line numbers + Text Area)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
        ) {
            // Line Numbers Column
            Column(
                modifier = Modifier
                    .widthIn(min = 44.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = i.toString(),
                        color = Color(0xFF858585),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }

            // Editor Text Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                BasicTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = Color(0xFFD4D4D4),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF528BFF))
                )
            }
        }

        // VS Code Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0xFF007ACC))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("⚡ Luau (Roblox Studio Mode)", color = Color.White, fontSize = 11.sp)
                Text("UTF-8", color = Color.White, fontSize = 11.sp)
            }
            Text("Ln $lineCount, Col ${sourceText.length}", color = Color.White, fontSize = 11.sp)
        }
    }
}
