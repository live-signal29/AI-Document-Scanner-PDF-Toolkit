package com.example.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class FilterMode {
    ORIGINAL,
    DOCUMENT,
    BW,
    GRAYSCALE
}

data class DocumentQuad(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
)

object ImageProcessor {

    /**
     * Decode a bitmap from file with memory-safe downsampling.
     */
    suspend fun decodeBitmapSafe(
        file: File,
        maxDimension: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var inSampleSize = 1
            val maxSide = max(options.outWidth, options.outHeight)
            while (maxSide / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decode bitmap from URI with memory-safe downsampling.
     */
    suspend fun decodeUriSafe(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var input: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            var inSampleSize = 1
            val maxSide = max(options.outWidth, options.outHeight)
            while (maxSide / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            input = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Detects document corners in a bitmap.
     * Uses luminance thresholding & convex bounding heuristic, or fallback to an inset quadrilateral.
     */
    fun detectDocumentCorners(bitmap: Bitmap): DocumentQuad {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // Smart edge detection: compute edge energy on downscaled grid
        val sampleW = 64
        val sampleH = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        var minX = sampleW
        var maxX = 0
        var minY = sampleH
        var maxY = 0

        val threshold = 180
        var darkPixelCount = 0

        for (y in 2 until sampleH - 2) {
            for (x in 2 until sampleW - 2) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                // If pixel has contrast difference with neighbors
                val rightPixel = scaled.getPixel(x + 1, y)
                val rightLum = (0.299 * Color.red(rightPixel) + 0.587 * Color.green(rightPixel) + 0.114 * Color.blue(rightPixel)).toInt()

                if (Math.abs(lum - rightLum) > 30 || lum < threshold) {
                    minX = min(minX, x)
                    maxX = max(maxX, x)
                    minY = min(minY, y)
                    maxY = max(maxY, y)
                    darkPixelCount++
                }
            }
        }
        scaled.recycle()

        // If detected valid bounding region that covers between 20% and 95% of the frame
        val detectedAreaFraction = (maxX - minX).toFloat() * (maxY - minY).toFloat() / (sampleW * sampleH)
        if (darkPixelCount > 80 && detectedAreaFraction in 0.2f..0.96f) {
            val scaleX = width / sampleW
            val scaleY = height / sampleH

            val left = minX * scaleX
            val right = maxX * scaleX
            val top = minY * scaleY
            val bottom = maxY * scaleY

            return DocumentQuad(
                topLeft = PointF(max(0f, left), max(0f, top)),
                topRight = PointF(min(width, right), max(0f, top)),
                bottomRight = PointF(min(width, right), min(height, bottom)),
                bottomLeft = PointF(max(0f, left), min(height, bottom))
            )
        }

        // Default: 5% inset quadrilateral representing a standard document frame
        val marginX = width * 0.05f
        val marginY = height * 0.05f
        return DocumentQuad(
            topLeft = PointF(marginX, marginY),
            topRight = PointF(width - marginX, marginY),
            bottomRight = PointF(width - marginX, height - marginY),
            bottomLeft = PointF(marginX, height - marginY)
        )
    }

    /**
     * Performs perspective correction / warp from 4 corners into a rectangular bitmap.
     */
    suspend fun cropAndWarpPerspective(
        srcBitmap: Bitmap,
        quad: DocumentQuad
    ): Bitmap = withContext(Dispatchers.Default) {
        // Calculate destination width and height based on Euclidean distance
        val widthTop = hypot((quad.topRight.x - quad.topLeft.x).toDouble(), (quad.topRight.y - quad.topLeft.y).toDouble())
        val widthBottom = hypot((quad.bottomRight.x - quad.bottomLeft.x).toDouble(), (quad.bottomRight.y - quad.bottomLeft.y).toDouble())
        val targetWidth = max(widthTop, widthBottom).toInt().coerceAtLeast(100)

        val heightLeft = hypot((quad.bottomLeft.x - quad.topLeft.x).toDouble(), (quad.bottomLeft.y - quad.topLeft.y).toDouble())
        val heightRight = hypot((quad.bottomRight.x - quad.topRight.x).toDouble(), (quad.bottomRight.y - quad.topRight.y).toDouble())
        val targetHeight = max(heightLeft, heightRight).toInt().coerceAtLeast(100)

        val srcPoints = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y,
            quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y,
            quad.bottomLeft.x, quad.bottomLeft.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val destBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(srcBitmap, matrix, paint)
        destBitmap
    }

    /**
     * Applies filter to bitmap.
     */
    suspend fun applyFilter(bitmap: Bitmap, mode: FilterMode): Bitmap = withContext(Dispatchers.Default) {
        if (mode == FilterMode.ORIGINAL) {
            return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (mode) {
            FilterMode.GRAYSCALE -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            FilterMode.DOCUMENT -> {
                // Magic Color Filter: Increases contrast, enhances text sharpness, cleans paper background
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.4f, 0f, 0f, 0f, -30f,
                        0f, 1.4f, 0f, 0f, -30f,
                        0f, 0f, 1.4f, 0f, -30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            FilterMode.BW -> {
                // High contrast Black & White thresholding
                val cm = ColorMatrix().apply {
                    setSaturation(0f)
                }
                // High contrast curve
                val contrast = 3.5f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            FilterMode.ORIGINAL -> {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
        }
        result
    }

    /**
     * Rotate bitmap by degrees (90, 180, 270).
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Save bitmap to private storage file.
     */
    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        quality: Int = 90,
        subDir: String = "scans"
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
        val file = File(dir, "page_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        file
    }
}
