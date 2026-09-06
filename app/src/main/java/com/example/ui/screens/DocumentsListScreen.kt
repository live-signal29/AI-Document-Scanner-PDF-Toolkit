package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.pdf.PdfToolManager
import com.example.data.local.ScannedDocument
import com.example.ui.ScanSessionViewModel
import com.example.ui.SortOrder
import com.example.ui.components.BannerAdView
import com.example.ui.components.DocumentCard
import com.example.ui.components.HighDensityBottomNav
import com.example.ui.components.HighDensityNavTab
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsListScreen(
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onOcrDocument: (Long) -> Unit,
    onAiDocument: (Long) -> Unit,
    onNavigateToHome: () -> Unit = onNavigateBack,
    onNavigateToTools: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDocuments by viewModel.activeDocuments.collectAsState()
    val trashDocuments by viewModel.trashDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Trash
    var showSortMenu by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Are you sure you want to permanently delete all items in the trash?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashDialog = false
                        viewModel.emptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("documents_list_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "My Documents" else "Trash Bin",
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
                    if (selectedTab == 0) {
                        IconButton(onClick = { viewModel.toggleGridView() }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List"
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Newest First") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.NEWEST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest First") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.OLDEST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.NAME)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest)") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.SIZE)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { showEmptyTrashDialog = true },
                            enabled = trashDocuments.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Empty Trash")
                        }
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
                    currentTab = HighDensityNavTab.FILES,
                    onTabSelected = { tab ->
                        when (tab) {
                            HighDensityNavTab.HOME -> onNavigateToHome()
                            HighDensityNavTab.FILES -> { /* Already on Files */ }
                            HighDensityNavTab.TOOLS -> onNavigateToTools()
                            HighDensityNavTab.ASK_AI -> onAiDocument(0L)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Active vs Trash
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active (${activeDocuments.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Trash (${trashDocuments.size})") }
                )
            }

            // Search Bar (Active Tab only)
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by name or OCR text…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("doc_search_field")
                )
            }

            // Documents List / Grid
            val currentList = if (selectedTab == 0) activeDocuments else trashDocuments

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No documents found." else "Trash is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isGridView && selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            isGridView = true,
                            onClick = { onOpenDocument(doc.id) },
                            onShare = {
                                doc.pdfFilePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) PdfToolManager.sharePdf(context, file)
                                }
                            },
                            onOcr = { onOcrDocument(doc.id) },
                            onAiAssistant = { onAiDocument(doc.id) },
                            onRename = { /* Handle rename */ },
                            onDelete = { viewModel.moveToTrash(doc.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            isGridView = false,
                            onClick = {
                                if (selectedTab == 0) onOpenDocument(doc.id)
                            },
                            onShare = {
                                doc.pdfFilePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) PdfToolManager.sharePdf(context, file)
                                }
                            },
                            onOcr = { onOcrDocument(doc.id) },
                            onAiAssistant = { onAiDocument(doc.id) },
                            onRename = { /* Rename */ },
                            onDelete = {
                                if (selectedTab == 0) {
                                    viewModel.moveToTrash(doc.id)
                                } else {
                                    viewModel.deleteDocumentPermanently(doc)
                                }
                            },
                            onRestore = if (selectedTab == 1) {
                                { viewModel.restoreFromTrash(doc.id) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
