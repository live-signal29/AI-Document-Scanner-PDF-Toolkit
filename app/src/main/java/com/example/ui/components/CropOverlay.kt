package com.example.ui.components

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.image.DocumentQuad
import kotlin.math.hypot

enum class CornerDrag {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT
}

@Composable
fun CropOverlay(
    quad: DocumentQuad,
    imageWidth: Float,
    imageHeight: Float,
    onQuadChanged: (DocumentQuad) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("crop_overlay")) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return@BoxWithConstraints
        }

        // Calculate aspect fit scale and offsets
        val scale = minOf(viewWidth / imageWidth, viewHeight / imageHeight)
        val drawnWidth = imageWidth * scale
        val drawnHeight = imageHeight * scale
        val offsetX = (viewWidth - drawnWidth) / 2f
        val offsetY = (viewHeight - drawnHeight) / 2f

        // Convert image points to screen coordinates
        fun toScreen(pt: PointF): Offset {
            return Offset(offsetX + pt.x * scale, offsetY + pt.y * scale)
        }

        // Convert screen coordinates back to image points
        fun toImage(offset: Offset): PointF {
            val imgX = ((offset.x - offsetX) / scale).coerceIn(0f, imageWidth)
            val imgY = ((offset.y - offsetY) / scale).coerceIn(0f, imageHeight)
            return PointF(imgX, imgY)
        }

        var activeCorner by remember { mutableStateOf(CornerDrag.NONE) }
        val density = LocalDensity.current
        val touchRadiusPx = with(density) { 48.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(quad, scale) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val sTl = toScreen(quad.topLeft)
                            val sTr = toScreen(quad.topRight)
                            val sBr = toScreen(quad.bottomRight)
                            val sBl = toScreen(quad.bottomLeft)

                            val dTl = hypot(startOffset.x - sTl.x, startOffset.y - sTl.y)
                            val dTr = hypot(startOffset.x - sTr.x, startOffset.y - sTr.y)
                            val dBr = hypot(startOffset.x - sBr.x, startOffset.y - sBr.y)
                            val dBl = hypot(startOffset.x - sBl.x, startOffset.y - sBl.y)

                            val minD = minOf(dTl, dTr, dBr, dBl)
                            activeCorner = when {
                                minD > touchRadiusPx * 1.5f -> CornerDrag.NONE
                                minD == dTl -> CornerDrag.TOP_LEFT
                                minD == dTr -> CornerDrag.TOP_RIGHT
                                minD == dBr -> CornerDrag.BOTTOM_RIGHT
                                else -> CornerDrag.BOTTOM_LEFT
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeCorner == CornerDrag.NONE) return@detectDragGestures

                            val currentPt = when (activeCorner) {
                                CornerDrag.TOP_LEFT -> toScreen(quad.topLeft)
                                CornerDrag.TOP_RIGHT -> toScreen(quad.topRight)
                                CornerDrag.BOTTOM_RIGHT -> toScreen(quad.bottomRight)
                                CornerDrag.BOTTOM_LEFT -> toScreen(quad.bottomLeft)
                                CornerDrag.NONE -> Offset.Zero
                            }

                            val newScreenPos = currentPt + dragAmount
                            val newImgPt = toImage(newScreenPos)

                            val newQuad = when (activeCorner) {
                                CornerDrag.TOP_LEFT -> quad.copy(topLeft = newImgPt)
                                CornerDrag.TOP_RIGHT -> quad.copy(topRight = newImgPt)
                                CornerDrag.BOTTOM_RIGHT -> quad.copy(bottomRight = newImgPt)
                                CornerDrag.BOTTOM_LEFT -> quad.copy(bottomLeft = newImgPt)
                                CornerDrag.NONE -> quad
                            }
                            onQuadChanged(newQuad)
                        },
                        onDragEnd = { activeCorner = CornerDrag.NONE },
                        onDragCancel = { activeCorner = CornerDrag.NONE }
                    )
                }
        ) {
            val sTl = toScreen(quad.topLeft)
            val sTr = toScreen(quad.topRight)
            val sBr = toScreen(quad.bottomRight)
            val sBl = toScreen(quad.bottomLeft)

            // Draw quadrilateral border path
            val quadPath = Path().apply {
                moveTo(sTl.x, sTl.y)
                lineTo(sTr.x, sTr.y)
                lineTo(sBr.x, sBr.y)
                lineTo(sBl.x, sBl.y)
                close()
            }

            // Draw semi-transparent background outside or bounding polygon
            drawPath(
                path = quadPath,
                color = Color(0x330F52BA)
            )

            // Draw high-contrast edge lines
            drawPath(
                path = quadPath,
                color = Color(0xFF0F52BA),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // Draw Rule-of-Thirds Grid inside quad
            for (i in 1..2) {
                val fraction = i / 3f
                // Top to Bottom guide
                val topGuide = Offset(
                    sTl.x + (sTr.x - sTl.x) * fraction,
                    sTl.y + (sTr.y - sTl.y) * fraction
                )
                val bottomGuide = Offset(
                    sBl.x + (sBr.x - sBl.x) * fraction,
                    sBl.y + (sBr.y - sBl.y) * fraction
                )
                drawLine(
                    color = Color(0x66FFFFFF),
                    start = topGuide,
                    end = bottomGuide,
                    strokeWidth = 2f
                )

                // Left to Right guide
                val leftGuide = Offset(
                    sTl.x + (sBl.x - sTl.x) * fraction,
                    sTl.y + (sBl.y - sTl.y) * fraction
                )
                val rightGuide = Offset(
                    sTr.x + (sBr.x - sTr.x) * fraction,
                    sTr.y + (sBr.y - sTr.y) * fraction
                )
                drawLine(
                    color = Color(0x66FFFFFF),
                    start = leftGuide,
                    end = rightGuide,
                    strokeWidth = 2f
                )
            }

            // Draw 4 corner handles
            val corners = listOf(sTl, sTr, sBr, sBl)
            corners.forEach { corner ->
                // Outer ring
                drawCircle(
                    color = Color.White,
                    radius = 24f,
                    center = corner
                )
                // Inner blue circle
                drawCircle(
                    color = Color(0xFF0F52BA),
                    radius = 16f,
                    center = corner
                )
            }
        }
    }
}
