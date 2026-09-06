package com.example.core.ocr

import android.content.Context
import android.graphics.Bitmap
import com.example.core.image.ImageProcessor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class OcrResult(
    val fullText: String,
    val wordCount: Int,
    val lineCount: Int,
    val blockCount: Int,
    val confidenceEstimate: Float,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Recognizes text from a Bitmap on-device and offline.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult =
        withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            val blocks = visionText.textBlocks
                            var lines = 0
                            var words = 0
                            var totalConfidence = 0f
                            var elementCount = 0

                            for (block in blocks) {
                                for (line in block.lines) {
                                    lines++
                                    for (element in line.elements) {
                                        words++
                                        val conf = element.confidence ?: 0.9f
                                        totalConfidence += conf
                                        elementCount++
                                    }
                                }
                            }

                            val avgConfidence = if (elementCount > 0) totalConfidence / elementCount else 0.95f
                            continuation.resume(
                                OcrResult(
                                    fullText = text.trim(),
                                    wordCount = words,
                                    lineCount = lines,
                                    blockCount = blocks.size,
                                    confidenceEstimate = avgConfidence,
                                    isSuccess = true
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(
                                OcrResult(
                                    fullText = "",
                                    wordCount = 0,
                                    lineCount = 0,
                                    blockCount = 0,
                                    confidenceEstimate = 0f,
                                    isSuccess = false,
                                    errorMessage = e.localizedMessage ?: "OCR processing failed"
                                )
                            )
                        }
                } catch (e: Exception) {
                    continuation.resume(
                        OcrResult(
                            fullText = "",
                            wordCount = 0,
                            lineCount = 0,
                            blockCount = 0,
                            confidenceEstimate = 0f,
                            isSuccess = false,
                            errorMessage = e.localizedMessage ?: "OCR initialization error"
                        )
                    )
                }
            }
        }

    /**
     * Recognizes text from an image file.
     */
    suspend fun recognizeTextFromFile(file: File): OcrResult = withContext(Dispatchers.IO) {
        val bitmap = ImageProcessor.decodeBitmapSafe(file, maxDimension = 2048)
            ?: return@withContext OcrResult(
                fullText = "",
                wordCount = 0,
                lineCount = 0,
                blockCount = 0,
                confidenceEstimate = 0f,
                isSuccess = false,
                errorMessage = "Could not load image file for OCR"
            )

        val result = recognizeTextFromBitmap(bitmap)
        bitmap.recycle()
        result
    }

    /**
     * Performs OCR across multiple page files and aggregates the text.
     */
    suspend fun recognizeTextFromMultipleFiles(
        files: List<File>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): OcrResult = withContext(Dispatchers.IO) {
        val fullBuilder = StringBuilder()
        var totalWords = 0
        var totalLines = 0
        var totalBlocks = 0
        var confidenceSum = 0f
        var pageCount = 0

        files.forEachIndexed { index, file ->
            onProgress(index + 1, files.size)
            val result = recognizeTextFromFile(file)
            if (result.isSuccess && result.fullText.isNotEmpty()) {
                if (fullBuilder.isNotEmpty()) {
                    fullBuilder.append("\n\n--- Page ${index + 1} ---\n\n")
                }
                fullBuilder.append(result.fullText)
                totalWords += result.wordCount
                totalLines += result.lineCount
                totalBlocks += result.blockCount
                confidenceSum += result.confidenceEstimate
                pageCount++
            }
        }

        val avgConf = if (pageCount > 0) confidenceSum / pageCount else 0f
        OcrResult(
            fullText = fullBuilder.toString(),
            wordCount = totalWords,
            lineCount = totalLines,
            blockCount = totalBlocks,
            confidenceEstimate = avgConf,
            isSuccess = true
        )
    }
}
