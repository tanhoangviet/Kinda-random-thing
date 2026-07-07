package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.liquidGlass(
    cornerRadius: Dp,
    tint: Color = Color.White,
    opacity: Float = 0.18f,
    strokeOpacity: Float = 0.30f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val base = tint.copy(alpha = opacity.coerceIn(0.05f, 0.34f))
    val edge = Color.White.copy(alpha = strokeOpacity.coerceIn(0.08f, 0.48f))
    val shadow = Color.Black.copy(alpha = 0.20f)

    return this
        .clip(shape)
        .drawWithContent {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        base,
                        tint.copy(alpha = 0.08f),
                        shadow.copy(alpha = 0.16f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.02f),
                    radius = size.minDimension * 0.9f
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )
            drawContent()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(size.width * 0.08f, 1.dp.toPx()),
                end = Offset(size.width * 0.92f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        .border(BorderStroke(1.dp, edge), shape)
}

@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    tint: Color = Color.White,
    opacity: Float = 0.18f,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(cornerRadius = cornerRadius, tint = tint, opacity = opacity)
            .padding(contentPadding),
        content = content
    )
}
