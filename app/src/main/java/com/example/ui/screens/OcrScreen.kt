package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ocr.OcrEngine
import com.example.core.ocr.OcrResult
import com.example.core.pdf.PdfToolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedDocument
import com.example.ui.ScanSessionViewModel
import com.example.ui.components.BannerAdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    documentId: Long,
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAi: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var document by remember { mutableStateOf<ScannedDocument?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var ocrResult by remember { mutableStateOf<OcrResult?>(null) }
    var isProcessing by remember { mutableStateOf(true) }
    var progressMessage by remember { mutableStateOf("Initializing OCR Engine…") }

    LaunchedEffect(documentId) {
        withContext(Dispatchers.IO) {
            val doc = db.documentDao().getDocumentByIdOnce(documentId)
            document = doc

            // Check if document already has saved OCR text
            if (!doc?.ocrText.isNullOrBlank()) {
                ocrText = doc!!.ocrText!!
                isProcessing = false
                return@withContext
            }

            // Retrieve pages or extract from PDF
            val pages = db.documentDao().getPagesForDocumentOnce(documentId)
            val filesToOcr = mutableListOf<File>()

            if (pages.isNotEmpty()) {
                pages.forEach { p ->
                    val f = File(p.imagePath)
                    if (f.exists()) filesToOcr.add(f)
                }
            } else if (doc?.pdfFilePath != null) {
                val pdfFile = File(doc.pdfFilePath)
                if (pdfFile.exists()) {
                    progressMessage = "Rendering PDF pages for OCR…"
                    val extracted = PdfToolManager.convertPdfToImages(context, pdfFile)
                    filesToOcr.addAll(extracted)
                }
            }

            if (filesToOcr.isNotEmpty()) {
                progressMessage = "Extracting text with ML Kit on-device…"
                val result = OcrEngine.recognizeTextFromMultipleFiles(filesToOcr) { current, total ->
                    progressMessage = "Extracting page $current of $total…"
                }
                ocrResult = result
                ocrText = result.fullText.ifEmpty { "No legible text found in this document." }
                if (result.fullText.isNotBlank()) {
                    viewModel.updateDocumentOcrText(documentId, result.fullText)
                }
            } else {
                ocrText = "No document images found for OCR."
            }
            isProcessing = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("ocr_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OCR Text Recognition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("OCR Text", ocrText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        enabled = ocrText.isNotBlank(),
                        modifier = Modifier.testTag("ocr_copy_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, ocrText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Text via"))
                        },
                        enabled = ocrText.isNotBlank(),
                        modifier = Modifier.testTag("ocr_share_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            BannerAdView(
                isTestMode = viewModel.prefs.isAdMobTestMode,
                isProUser = viewModel.prefs.isProUser
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "100% On-Device & Private",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // OCR Stats & Privacy Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "OFFLINE OCR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val conf = ((ocrResult?.confidenceEstimate ?: 0.95f) * 100).toInt()
                            Text(
                                text = "$conf% Confidence",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${ocrResult?.wordCount ?: ocrText.split(" ").size} words",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Editable OCR Text Field
                OutlinedTextField(
                    value = ocrText,
                    onValueChange = {
                        ocrText = it
                        viewModel.updateDocumentOcrText(documentId, it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("ocr_text_field"),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Recognized Document Text") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Save & AI Analysis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateDocumentOcrText(documentId, ocrText)
                            Toast.makeText(context, "OCR text saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_save_btn"),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Text")
                    }

                    Button(
                        onClick = { onNavigateToAi(documentId) },
                        modifier = Modifier.weight(1.3f).testTag("ocr_to_ai_btn"),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze with AI")
                    }
                }
            }
        }
    }
}
