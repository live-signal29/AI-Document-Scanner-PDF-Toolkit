package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.pdf.PdfToolManager
import com.example.ui.ScanSessionViewModel
import com.example.ui.components.BannerAdView
import com.example.ui.components.HighDensityBottomNav
import com.example.ui.components.HighDensityNavTab
import com.example.ui.components.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = onNavigateBack,
    onNavigateToFiles: () -> Unit = {},
    onAiDocument: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }

    // Helper to copy content uri to temp file
    suspend fun copyUriToTempFile(uri: Uri, suffix: String = ".pdf"): File = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
        val temp = File(context.cacheDir, "temp_${System.currentTimeMillis()}$suffix")
        FileOutputStream(temp).use { out -> input?.copyTo(out) }
        input?.close()
        temp
    }

    // PDF Compressor Launcher
    val compressLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingMessage = "Compressing PDF document…"
                try {
                    val temp = copyUriToTempFile(uri)
                    val compressed = PdfToolManager.compressPdf(context, temp, targetQualityRatio = 0.6f)
                    val originalSize = temp.length()
                    val newSize = compressed.length()
                    temp.delete()

                    resultFile = compressed
                    showResultDialog = true
                    Toast.makeText(
                        context,
                        "Compressed from ${formatFileSize(originalSize)} to ${formatFileSize(newSize)}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Compression failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // PDF Merge Launcher (Multiple documents)
    val mergeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.size >= 2) {
            scope.launch {
                isProcessing = true
                processingMessage = "Merging ${uris.size} PDF files…"
                try {
                    val files = uris.map { copyUriToTempFile(it) }
                    val merged = PdfToolManager.mergePdfs(context, files, "Merged_Document")
                    files.forEach { it.delete() }

                    resultFile = merged
                    showResultDialog = true
                    Toast.makeText(context, "Merged successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Merge failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        } else if (uris.isNotEmpty()) {
            Toast.makeText(context, "Please select at least 2 PDF files to merge.", Toast.LENGTH_SHORT).show()
        }
    }

    // PDF to Images Launcher
    val pdfToImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingMessage = "Converting PDF pages into images…"
                try {
                    val temp = copyUriToTempFile(uri)
                    val images = PdfToolManager.convertPdfToImages(context, temp)
                    temp.delete()

                    if (images.isNotEmpty()) {
                        PdfToolManager.shareFiles(context, images, mimeType = "image/jpeg")
                        Toast.makeText(context, "Extracted ${images.size} page image(s)!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Extraction failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // JPG to PDF Launcher
    val jpgToPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                processingMessage = "Converting images to PDF…"
                try {
                    val files = uris.map { copyUriToTempFile(it, ".jpg") }
                    val pdf = PdfToolManager.createPdfFromImageFiles(context, files, "Images_Document")
                    files.forEach { it.delete() }

                    resultFile = pdf
                    showResultDialog = true
                    Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Conversion failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    if (showResultDialog && resultFile != null) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("Processing Complete") },
            text = {
                Column {
                    Text(
                        text = "File: ${resultFile!!.name}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Size: ${formatFileSize(resultFile!!.length())}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultDialog = false
                        PdfToolManager.sharePdf(context, resultFile!!)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("pdf_tools_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PDF & Document Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BannerAdView(
                    isTestMode = viewModel.prefs.isAdMobTestMode,
                    isProUser = viewModel.prefs.isProUser
                )
                HighDensityBottomNav(
                    currentTab = HighDensityNavTab.TOOLS,
                    onTabSelected = { tab ->
                        when (tab) {
                            HighDensityNavTab.HOME -> onNavigateToHome()
                            HighDensityNavTab.FILES -> onNavigateToFiles()
                            HighDensityNavTab.TOOLS -> { /* Already on Tools */ }
                            HighDensityNavTab.ASK_AI -> onAiDocument(0L)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = processingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Quick Document Actions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    PdfToolItem(
                        title = "Compress PDF",
                        description = "Reduce PDF file size while preserving readability",
                        icon = Icons.Default.Compress,
                        onClick = { compressLauncher.launch(arrayOf("application/pdf")) },
                        testTag = "tool_compress_pdf"
                    )
                }

                item {
                    PdfToolItem(
                        title = "Merge PDFs",
                        description = "Combine two or more PDF documents into a single file",
                        icon = Icons.Default.MergeType,
                        onClick = { mergeLauncher.launch(arrayOf("application/pdf")) },
                        testTag = "tool_merge_pdfs"
                    )
                }

                item {
                    PdfToolItem(
                        title = "PDF to JPG",
                        description = "Extract all pages of a PDF document as high-resolution images",
                        icon = Icons.Default.Image,
                        onClick = { pdfToImagesLauncher.launch(arrayOf("application/pdf")) },
                        testTag = "tool_pdf_to_jpg"
                    )
                }

                item {
                    PdfToolItem(
                        title = "JPG to PDF",
                        description = "Select multiple photos and compile them into a PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            jpgToPdfLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        testTag = "tool_jpg_to_pdf"
                    )
                }
            }
        }
    }
}

@Composable
fun PdfToolItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
