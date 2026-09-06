package com.example.core.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.core.image.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object PdfToolManager {

    // Standard A4 dimensions in PDF points (72 points per inch)
    const val A4_WIDTH = 595
    const val A4_HEIGHT = 842

    /**
     * Creates a PDF from a list of image files.
     */
    suspend fun createPdfFromImageFiles(
        context: Context,
        imageFiles: List<File>,
        outputTitle: String,
        quality: String = "MEDIUM"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val dir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val sanitizedTitle = outputTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outputFile = File(dir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        // Compression downscaling based on quality setting
        val maxDimension = when (quality.uppercase()) {
            "HIGH" -> 2048
            "LOW" -> 1024
            else -> 1600
        }

        imageFiles.forEachIndexed { index, file ->
            val bitmap = ImageProcessor.decodeBitmapSafe(file, maxDimension = maxDimension) ?: return@forEachIndexed
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Fill white background
            canvas.drawColor(Color.WHITE)

            // Compute aspect fit into A4 page margins (20pt margin)
            val margin = 20f
            val availW = A4_WIDTH - (margin * 2)
            val availH = A4_HEIGHT - (margin * 2)

            val imgW = bitmap.width.toFloat()
            val imgH = bitmap.height.toFloat()
            val scale = minOf(availW / imgW, availH / imgH)

            val drawW = imgW * scale
            val drawH = imgH * scale
            val left = margin + (availW - drawW) / 2f
            val top = margin + (availH - drawH) / 2f

            val destRect = RectF(left, top, left + drawW, top + drawH)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, null, destRect, paint)

            pdfDocument.finishPage(page)
            bitmap.recycle()
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    /**
     * Converts a PDF to a list of high-resolution JPG images.
     */
    suspend fun convertPdfToImages(
        context: Context,
        pdfFile: File,
        targetDpiScale: Float = 2.0f
    ): List<File> = withContext(Dispatchers.IO) {
        val resultImages = mutableListOf<File>()
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val outputDir = File(context.filesDir, "extracted_images").apply { if (!exists()) mkdirs() }

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val width = (page.width * targetDpiScale).toInt()
            val height = (page.height * targetDpiScale).toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val imageFile = File(outputDir, "${pdfFile.nameWithoutExtension}_page_${i + 1}.jpg")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
            resultImages.add(imageFile)
        }

        renderer.close()
        pfd.close()
        resultImages
    }

    /**
     * Compresses a PDF file by rendering each page and re-encoding at reduced scale and quality.
     */
    suspend fun compressPdf(
        context: Context,
        srcPdf: File,
        targetQualityRatio: Float = 0.65f
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val compressedFile = File(dir, "compressed_${srcPdf.nameWithoutExtension}_${System.currentTimeMillis()}.pdf")

        val pfd = ParcelFileDescriptor.open(srcPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pdfDocument = PdfDocument()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val renderScale = 1.3f * targetQualityRatio
            val bWidth = (page.width * renderScale).toInt().coerceAtLeast(400)
            val bHeight = (page.height * renderScale).toInt().coerceAtLeast(600)

            val bitmap = Bitmap.createBitmap(bWidth, bHeight, Bitmap.Config.ARGB_8888)
            val canvasB = Canvas(bitmap)
            canvasB.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
            val docPage = pdfDocument.startPage(pageInfo)
            val docCanvas = docPage.canvas
            docCanvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            docCanvas.drawBitmap(bitmap, null, Rect(0, 0, page.width, page.height), paint)
            pdfDocument.finishPage(docPage)

            bitmap.recycle()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(compressedFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        compressedFile
    }

    /**
     * Merges multiple PDF files into one combined PDF.
     */
    suspend fun mergePdfs(
        context: Context,
        pdfFiles: List<File>,
        outputTitle: String
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(dir, "merged_${outputTitle}_${System.currentTimeMillis()}.pdf")
        val mergedDocument = PdfDocument()
        var globalPageIndex = 1

        for (pdf in pdfFiles) {
            val pfd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, globalPageIndex++).create()
                val docPage = mergedDocument.startPage(pageInfo)
                val docCanvas = docPage.canvas
                docCanvas.drawColor(Color.WHITE)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                docCanvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), paint)
                mergedDocument.finishPage(docPage)
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()
        }

        FileOutputStream(outputFile).use { out ->
            mergedDocument.writeTo(out)
        }
        mergedDocument.close()
        outputFile
    }

    /**
     * Splits a PDF into a new PDF containing only the specified page indices (0-based).
     */
    suspend fun splitPdf(
        context: Context,
        srcPdf: File,
        selectedPages: List<Int>,
        outputTitle: String
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(dir, "split_${outputTitle}_${System.currentTimeMillis()}.pdf")

        val pfd = ParcelFileDescriptor.open(srcPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pdfDocument = PdfDocument()

        selectedPages.forEachIndexed { outIndex, pageIndex ->
            if (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val width = page.width
                val height = page.height

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, outIndex + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val docCanvas = docPage.canvas
                docCanvas.drawColor(Color.WHITE)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                docCanvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), paint)
                pdfDocument.finishPage(docPage)
                bitmap.recycle()
            }
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    /**
     * Share a PDF file via system Intent.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF via"))
    }

    /**
     * Share multiple files.
     */
    fun shareFiles(context: Context, files: List<File>, mimeType: String = "image/jpeg") {
        if (files.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (f in files) {
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share files via"))
    }

    /**
     * Prints a PDF document using Android PrintManager.
     */
    fun printPdf(context: Context, pdfFile: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(pdfFile.nameWithoutExtension, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Helper to render the first page of a PDF as a thumbnail.
     */
    suspend fun renderPdfThumbnail(context: Context, pdfFile: File): File? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext null
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) {
                renderer.close()
                pfd.close()
                return@withContext null
            }
            val page = renderer.openPage(0)
            val scale = 0.5f
            val width = (page.width * scale).toInt().coerceAtLeast(200)
            val height = (page.height * scale).toInt().coerceAtLeast(200)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()

            val thumbFile = File(context.filesDir, "thumbs").apply { if (!exists()) mkdirs() }
            val dest = File(thumbFile, "thumb_${pdfFile.nameWithoutExtension}.jpg")
            FileOutputStream(dest).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            dest
        } catch (e: Exception) {
            null
        }
    }
}
