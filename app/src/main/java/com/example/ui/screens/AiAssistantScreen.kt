package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiDocumentAssistant
import com.example.core.ai.ExtractedEntities
import com.example.core.ocr.OcrEngine
import com.example.core.pdf.PdfToolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ScannedDocument
import com.example.ui.ScanSessionViewModel
import com.example.ui.components.BannerAdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    documentId: Long,
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val customKey = viewModel.prefs.customAiApiKey

    var document by remember { mutableStateOf<ScannedDocument?>(null) }
    var docText by remember { mutableStateOf("") }
    var isDocLoading by remember { mutableStateOf(true) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Summary, 1: Key Points, 2: Entities, 3: Q&A, 4: Translate

    // State for Summary
    var summaryContent by remember { mutableStateOf("") }
    var summaryStatus by remember { mutableStateOf("") }
    var isSummaryLoading by remember { mutableStateOf(false) }
    var isDetailedSummary by remember { mutableStateOf(false) }

    // State for Key Points
    var keyPointsContent by remember { mutableStateOf("") }
    var keyPointsStatus by remember { mutableStateOf("") }
    var isKeyPointsLoading by remember { mutableStateOf(false) }

    // State for Entities
    var entities by remember { mutableStateOf<ExtractedEntities?>(null) }
    var entitiesStatus by remember { mutableStateOf("") }
    var isEntitiesLoading by remember { mutableStateOf(false) }

    // State for Q&A
    var questionInput by remember { mutableStateOf("") }
    var answerContent by remember { mutableStateOf("") }
    var answerStatus by remember { mutableStateOf("") }
    var isAnswerLoading by remember { mutableStateOf(false) }

    // State for Translation
    var selectedLanguage by remember { mutableStateOf("Spanish") }
    var translatedContent by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        withContext(Dispatchers.IO) {
            val doc = db.documentDao().getDocumentByIdOnce(documentId)
            document = doc

            var text = doc?.ocrText ?: ""
            if (text.isBlank()) {
                val pages = db.documentDao().getPagesForDocumentOnce(documentId)
                val files = pages.map { File(it.imagePath) }.filter { it.exists() }
                if (files.isNotEmpty()) {
                    val ocrRes = OcrEngine.recognizeTextFromMultipleFiles(files)
                    text = ocrRes.fullText
                    if (text.isNotBlank()) {
                        viewModel.updateDocumentOcrText(documentId, text)
                    }
                } else if (doc?.pdfFilePath != null) {
                    val pdfFile = File(doc.pdfFilePath)
                    if (pdfFile.exists()) {
                        val extracted = PdfToolManager.convertPdfToImages(context, pdfFile)
                        val ocrRes = OcrEngine.recognizeTextFromMultipleFiles(extracted)
                        text = ocrRes.fullText
                        if (text.isNotBlank()) {
                            viewModel.updateDocumentOcrText(documentId, text)
                        }
                    }
                }
            }

            docText = text
            isDocLoading = false

            // Auto-load summary initially
            if (text.isNotBlank()) {
                isSummaryLoading = true
                val res = AiDocumentAssistant.summarizeDocument(text, false, customKey)
                summaryContent = res.content
                summaryStatus = res.statusMessage
                isSummaryLoading = false
            }
        }
    }

    fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Output", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share AI Output via"))
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("ai_assistant_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Document Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        if (summaryContent.isBlank() && docText.isNotBlank()) {
                            scope.launch {
                                isSummaryLoading = true
                                val res = AiDocumentAssistant.summarizeDocument(docText, isDetailedSummary, customKey)
                                summaryContent = res.content
                                summaryStatus = res.statusMessage
                                isSummaryLoading = false
                            }
                        }
                    },
                    text = { Text("Summary") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (keyPointsContent.isBlank() && docText.isNotBlank()) {
                            scope.launch {
                                isKeyPointsLoading = true
                                val res = AiDocumentAssistant.extractKeyPoints(docText, customKey)
                                keyPointsContent = res.content
                                keyPointsStatus = res.statusMessage
                                isKeyPointsLoading = false
                            }
                        }
                    },
                    text = { Text("Points") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        if (entities == null && docText.isNotBlank()) {
                            scope.launch {
                                isEntitiesLoading = true
                                val (ent, res) = AiDocumentAssistant.extractEntities(docText, customKey)
                                entities = ent
                                entitiesStatus = res.statusMessage
                                isEntitiesLoading = false
                            }
                        }
                    },
                    text = { Text("Entities") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Ask Q&A") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Translate") }
                )
            }

            if (isDocLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Extracting document context…")
                    }
                }
            } else if (docText.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No text content found in document for AI analysis. Please run OCR or scan a clearer page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        // 0: SUMMARY TAB
                        0 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isDetailedSummary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            isDetailedSummary = false
                                            scope.launch {
                                                isSummaryLoading = true
                                                val res = AiDocumentAssistant.summarizeDocument(docText, false, customKey)
                                                summaryContent = res.content
                                                summaryStatus = res.statusMessage
                                                isSummaryLoading = false
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "Executive",
                                            color = if (!isDetailedSummary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDetailedSummary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            isDetailedSummary = true
                                            scope.launch {
                                                isSummaryLoading = true
                                                val res = AiDocumentAssistant.summarizeDocument(docText, true, customKey)
                                                summaryContent = res.content
                                                summaryStatus = res.statusMessage
                                                isSummaryLoading = false
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "Detailed",
                                            color = if (isDetailedSummary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (summaryContent.isNotBlank()) {
                                    Row {
                                        IconButton(onClick = { copyText(summaryContent) }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                        }
                                        IconButton(onClick = { shareText(summaryContent) }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share")
                                        }
                                    }
                                }
                            }

                            if (isSummaryLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = summaryContent,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 24.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = summaryStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 1: KEY POINTS TAB
                        1 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Key Takeaways & Highlights",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (keyPointsContent.isNotBlank()) {
                                    Row {
                                        IconButton(onClick = { copyText(keyPointsContent) }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                        }
                                        IconButton(onClick = { shareText(keyPointsContent) }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share")
                                        }
                                    }
                                }
                            }

                            if (isKeyPointsLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = keyPointsContent,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = keyPointsStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 2: ENTITIES TAB
                        2 -> {
                            Text(
                                text = "Structured Extracted Entities",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (isEntitiesLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (entities != null) {
                                EntityCategoryCard(title = "Dates", items = entities!!.dates, onCopy = { copyText(it) })
                                EntityCategoryCard(title = "Amounts & Currency", items = entities!!.amounts, onCopy = { copyText(it) })
                                EntityCategoryCard(title = "Emails", items = entities!!.emails, onCopy = { copyText(it) })
                                EntityCategoryCard(title = "Phone Numbers", items = entities!!.phoneNumbers, onCopy = { copyText(it) })
                                EntityCategoryCard(title = "Names & Signatories", items = entities!!.names, onCopy = { copyText(it) })

                                Text(
                                    text = entitiesStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 3: ASK Q&A TAB
                        3 -> {
                            Text(
                                text = "Ask anything about this document:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = questionInput,
                                    onValueChange = { questionInput = it },
                                    placeholder = { Text("e.g. When is the due date?") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (questionInput.isNotBlank()) {
                                            scope.launch {
                                                isAnswerLoading = true
                                                val res = AiDocumentAssistant.askDocumentQuestion(
                                                    docText,
                                                    questionInput,
                                                    customKey
                                                )
                                                answerContent = res.content
                                                answerStatus = res.statusMessage
                                                isAnswerLoading = false
                                            }
                                        }
                                    },
                                    enabled = questionInput.isNotBlank() && !isAnswerLoading
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Ask",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isAnswerLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (answerContent.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = answerContent,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = answerStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // 4: TRANSLATE TAB
                        4 -> {
                            Text(
                                text = "Translate document content:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            val languages = listOf("Spanish", "French", "German", "Arabic", "Urdu", "Hindi", "Chinese")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                languages.take(4).forEach { lang ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedLanguage == lang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { selectedLanguage = lang }
                                    ) {
                                        Text(
                                            text = lang,
                                            color = if (selectedLanguage == lang) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isTranslating = true
                                        val res = AiDocumentAssistant.translateText(docText, selectedLanguage, customKey)
                                        translatedContent = res.content
                                        isTranslating = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Translate to $selectedLanguage")
                            }

                            if (isTranslating) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (translatedContent.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = translatedContent,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntityCategoryCard(
    title: String,
    items: List<String>,
    onCopy: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$title (${items.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (items.isEmpty()) {
                Text(
                    text = "None detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { onCopy(item) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
