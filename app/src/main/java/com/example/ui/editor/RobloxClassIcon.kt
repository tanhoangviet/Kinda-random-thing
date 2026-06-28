package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RobloxClass

@Composable
fun RobloxClassIcon(
    className: RobloxClass,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp
) {
    Box(
        modifier = modifier
            .size(iconSize)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        when (className) {
            RobloxClass.ScreenGui -> {
                // Monitor/Screen icon (grey frame, blue top bar)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(45, 45, 50))
                        .border(1.dp, Color(120, 120, 130), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0, 162, 255))
                    )
                }
            }
            RobloxClass.Frame -> {
                // Square Outline
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, Color(220, 220, 230), RoundedCornerShape(2.dp))
                        .background(Color(255, 255, 255, 15))
                )
            }
            RobloxClass.TextLabel -> {
                // T inside light frame (Greenish/Cyan "T")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(35, 175, 145))
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 9.sp
                    )
                }
            }
            RobloxClass.TextButton -> {
                // White "T" inside Blue button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0, 130, 230))
                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 9.sp
                    )
                }
            }
            RobloxClass.ImageLabel -> {
                // Picture frame with mountains
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(100, 200, 255), RoundedCornerShape(2.dp))
                        .background(Color(30, 40, 50))
                ) {
                    // Draw a sun
                    drawCircle(
                        color = Color(255, 200, 0),
                        radius = 2.dp.toPx(),
                        center = Offset(10.dp.toPx(), 4.dp.toPx())
                    )
                    // Draw mountain
                    val path = Path().apply {
                        moveTo(1.dp.toPx(), 13.dp.toPx())
                        lineTo(6.dp.toPx(), 6.dp.toPx())
                        lineTo(10.dp.toPx(), 10.dp.toPx())
                        lineTo(13.dp.toPx(), 13.dp.toPx())
                        close()
                    }
                    drawPath(path, Color(0, 162, 255))
                }
            }
            RobloxClass.ImageButton -> {
                // ImageLabel with Cursor
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(255, 100, 150), RoundedCornerShape(2.dp))
                            .background(Color(30, 40, 50))
                    ) {
                        // Draw mountain
                        val path = Path().apply {
                            moveTo(1.dp.toPx(), 13.dp.toPx())
                            lineTo(6.dp.toPx(), 6.dp.toPx())
                            lineTo(10.dp.toPx(), 10.dp.toPx())
                            lineTo(13.dp.toPx(), 13.dp.toPx())
                            close()
                        }
                        drawPath(path, Color(255, 80, 120))
                    }
                    // Tiny cursor arrow drawn on top
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cursorPath = Path().apply {
                            moveTo(8.dp.toPx(), 8.dp.toPx())
                            lineTo(13.dp.toPx(), 10.dp.toPx())
                            lineTo(11.dp.toPx(), 11.dp.toPx())
                            lineTo(13.dp.toPx(), 13.dp.toPx())
                            lineTo(12.dp.toPx(), 14.dp.toPx())
                            lineTo(10.dp.toPx(), 12.dp.toPx())
                            lineTo(8.dp.toPx(), 13.dp.toPx())
                            close()
                        }
                        drawPath(cursorPath, Color.White)
                        drawPath(cursorPath, Color.Black, style = Stroke(width = 0.5.dp.toPx()))
                    }
                }
            }
            RobloxClass.ScrollingFrame -> {
                // Scrollable container layout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(180, 180, 190), RoundedCornerShape(2.dp))
                        .background(Color(45, 45, 50))
                ) {
                    // Vertical scrollbar on the right side
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(Color(80, 80, 85))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(Color(200, 200, 210))
                        )
                    }
                }
            }
            RobloxClass.ViewportFrame -> {
                // 3D cube inside frame
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, Color(0, 180, 255), RoundedCornerShape(2.dp))
                        .background(Color(20, 20, 25))
                ) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = 4.dp.toPx()
                    // Draw a simple isometric 3D wireframe box
                    drawLine(Color(0, 180, 255, 180), Offset(cx, cy - r), Offset(cx + r * 0.86f, cy - r * 0.5f), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx + r * 0.86f, cy - r * 0.5f), Offset(cx + r * 0.86f, cy + r * 0.5f), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx + r * 0.86f, cy + r * 0.5f), Offset(cx, cy + r), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx, cy + r), Offset(cx - r * 0.86f, cy + r * 0.5f), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx - r * 0.86f, cy + r * 0.5f), Offset(cx - r * 0.86f, cy - r * 0.5f), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx - r * 0.86f, cy - r * 0.5f), Offset(cx, cy - r), strokeWidth = 1.dp.toPx())
                    
                    drawLine(Color(0, 180, 255, 180), Offset(cx, cy), Offset(cx, cy - r), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx, cy), Offset(cx + r * 0.86f, cy + r * 0.5f), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0, 180, 255, 180), Offset(cx, cy), Offset(cx - r * 0.86f, cy + r * 0.5f), strokeWidth = 1.dp.toPx())
                }
            }
            RobloxClass.UIListLayout -> {
                // Stack of items
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(2.dp)
                                .background(Color(0, 162, 255))
                        )
                    }
                }
            }
            RobloxClass.UIGridLayout -> {
                // 2x2 squares
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.size(4.dp).border(1.dp, Color(0, 162, 255)).background(Color(0, 162, 255, 30)))
                        Box(modifier = Modifier.size(4.dp).border(1.dp, Color(0, 162, 255)).background(Color(0, 162, 255, 30)))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.size(4.dp).border(1.dp, Color(0, 162, 255)).background(Color(0, 162, 255, 30)))
                        Box(modifier = Modifier.size(4.dp).border(1.dp, Color(0, 162, 255)).background(Color(0, 162, 255, 30)))
                    }
                }
            }
            RobloxClass.UIPadding -> {
                // Dotted border box with padded box inside
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer dashed border
                    val stroke = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
                    )
                    drawRect(Color(0, 200, 120), size = Size(size.width, size.height), style = stroke)
                    
                    // Inner box showing offset bounds
                    val inset = 3.dp.toPx()
                    drawRect(
                        Color(0, 200, 120, 80),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2)
                    )
                }
            }
            RobloxClass.UICorner -> {
                // Rounded corner arc highlight
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 1.5.dp.toPx()
                    // Draw normal edges
                    drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = strokeWidth)
                    drawLine(Color.Gray, Offset(size.width, size.height), Offset(size.width, 0f), strokeWidth = strokeWidth)
                    
                    // Highlight rounded corner on top-left
                    drawArc(
                        color = Color(255, 85, 85),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(0f, 0f),
                        size = Size(10.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(Color(255, 85, 85), Offset(0f, 5.dp.toPx()), Offset(0f, size.height), strokeWidth = strokeWidth)
                    drawLine(Color(255, 85, 85), Offset(5.dp.toPx(), 0f), Offset(size.width, 0f), strokeWidth = strokeWidth)
                }
            }
            RobloxClass.UIStroke -> {
                // Outer stroke highlighting
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.5.dp, Color(255, 170, 0), RoundedCornerShape(2.dp))
                )
            }
            RobloxClass.UIGradient -> {
                // Multi-color rainbow linear gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(255, 60, 60),
                                    Color(255, 180, 0),
                                    Color(60, 220, 60),
                                    Color(0, 162, 255)
                                )
                            )
                        )
                )
            }
            RobloxClass.UIScale -> {
                // Diagonal scale arrows or double squares
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 1.dp.toPx())
                    // Draw outer box in grey, inner smaller in orange
                    drawRect(Color.Gray, size = Size(size.width, size.height), style = stroke)
                    drawRect(
                        Color(255, 120, 0),
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // Diagonal arrow line
                    drawLine(Color(255, 120, 0), Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            }
            RobloxClass.UIAspectRatioConstraint -> {
                // Width/Height ratio constraint lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 1.dp.toPx())
                    drawRect(Color(180, 100, 255), size = Size(size.width, size.height), style = stroke)
                    // Vertical and horizontal arrows
                    drawLine(Color(180, 100, 255), Offset(size.width / 2, 2.dp.toPx()), Offset(size.width / 2, size.height - 2.dp.toPx()), strokeWidth = 1.dp.toPx())
                    drawLine(Color(180, 100, 255), Offset(2.dp.toPx(), size.height / 2), Offset(size.width - 2.dp.toPx(), size.height / 2), strokeWidth = 1.dp.toPx())
                }
            }
            RobloxClass.LocalScript -> {
                // Blue script icon (Scroll / sheet with blue gear/circle)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(240, 240, 245))
                        .border(0.5.dp, Color.Gray, RoundedCornerShape(1.dp))
                ) {
                    // Draw some small black lines representing code
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.dp).background(Color.DarkGray))
                        Box(modifier = Modifier.fillMaxWidth(0.8f).height(1.dp).background(Color.DarkGray))
                        Box(modifier = Modifier.fillMaxWidth(0.4f).height(1.dp).background(Color.DarkGray))
                    }
                    // Tiny blue badge on bottom right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(5.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0, 120, 255))
                    )
                }
            }
            RobloxClass.ModuleScript -> {
                // Yellow script icon (Scroll / sheet with yellow gear/circle)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(240, 240, 245))
                        .border(0.5.dp, Color.Gray, RoundedCornerShape(1.dp))
                ) {
                    // Draw code lines
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.dp).background(Color.DarkGray))
                        Box(modifier = Modifier.fillMaxWidth(0.8f).height(1.dp).background(Color.DarkGray))
                        Box(modifier = Modifier.fillMaxWidth(0.4f).height(1.dp).background(Color.DarkGray))
                    }
                    // Tiny yellow badge on bottom right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(5.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(255, 204, 0))
                    )
                }
            }
        }
    }
}
