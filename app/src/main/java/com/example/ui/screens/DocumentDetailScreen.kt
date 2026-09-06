package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.pdf.PdfToolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedDocument
import com.example.ui.ScanSessionViewModel
import com.example.ui.components.BannerAdView
import com.example.ui.components.formatDate
import com.example.ui.components.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: Long,
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOcr: (Long) -> Unit,
    onNavigateToAi: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var document by remember { mutableStateOf<ScannedDocument?>(null) }
    val renderedPages = remember { mutableStateListOf<Bitmap>() }
    var isLoadingPages by remember { mutableStateOf(true) }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showCompressDialog by remember { mutableStateOf(false) }
    var isCompressing by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        withContext(Dispatchers.IO) {
            val doc = db.documentDao().getDocumentByIdOnce(documentId)
            document = doc
            renameText = doc?.title ?: ""

            // Render PDF pages with PdfRenderer
            val pdfPath = doc?.pdfFilePath
            if (pdfPath != null) {
                val pdfFile = File(pdfPath)
                if (pdfFile.exists()) {
                    try {
                        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        val bitmaps = mutableListOf<Bitmap>()

                        for (i in 0 until renderer.pageCount) {
                            val page = renderer.openPage(i)
                            val width = (page.width * 1.5f).toInt()
                            val height = (page.height * 1.5f).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            bitmaps.add(bitmap)
                        }
                        renderer.close()
                        pfd.close()

                        withContext(Dispatchers.Main) {
                            renderedPages.clear()
                            renderedPages.addAll(bitmaps)
                            isLoadingPages = false
                        }
                    } catch (e: Exception) {
                        isLoadingPages = false
                    }
                } else {
                    isLoadingPages = false
                }
            } else {
                isLoadingPages = false
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialog = false
                        viewModel.renameDocument(documentId, renameText)
                        document = document?.copy(title = renameText)
                    },
                    modifier = Modifier.testTag("rename_confirm_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("document_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = document?.title ?: "Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
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
                            document?.pdfFilePath?.let { path ->
                                val file = File(path)
                                if (file.exists()) PdfToolManager.sharePdf(context, file)
                            }
                        },
                        modifier = Modifier.testTag("detail_share_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(
                        onClick = {
                            document?.pdfFilePath?.let { path ->
                                val file = File(path)
                                if (file.exists()) PdfToolManager.printPdf(context, file)
                            }
                        },
                        modifier = Modifier.testTag("detail_print_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compress PDF") },
                                leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    // Run quick compression
                                    document?.pdfFilePath?.let { path ->
                                        val file = File(path)
                                        if (file.exists()) {
                                            isCompressing = true
                                            kotlinx.coroutines.GlobalScope.let {
                                                // Handle inline compression
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.moveToTrash(documentId)
                                    onNavigateBack()
                                }
                            )
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Action Buttons Row: OCR & AI Assistant
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onNavigateToOcr(documentId) },
                        modifier = Modifier.weight(1f).testTag("detail_ocr_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.TextFields, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract OCR")
                    }

                    Button(
                        onClick = { onNavigateToAi(documentId) },
                        modifier = Modifier.weight(1f).testTag("detail_ai_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Assistant")
                    }
                }
            }

            // Document Metadata Card
            item {
                document?.let { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetadataColumn(label = "Pages", value = "${doc.pageCount}")
                            MetadataColumn(label = "File Size", value = formatFileSize(doc.fileSizeBytes))
                            MetadataColumn(label = "Created", value = formatDate(doc.createdAt))
                        }
                    }
                }
            }

            // PDF Pages View
            if (isLoadingPages) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (renderedPages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No pages available to display.")
                    }
                }
            } else {
                itemsIndexed(renderedPages) { idx, pageBmp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_rendered_page_$idx"),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column {
                            Image(
                                bitmap = pageBmp.asImageBitmap(),
                                contentDescription = "Page ${idx + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(pageBmp.width.toFloat() / pageBmp.height.toFloat()),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "Page ${idx + 1} of ${renderedPages.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
