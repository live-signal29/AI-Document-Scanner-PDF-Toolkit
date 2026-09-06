package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.analytics.AppAnalytics
import com.example.core.image.DocumentQuad
import com.example.core.image.FilterMode
import com.example.core.image.ImageProcessor
import com.example.core.pdf.PdfToolManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.ScannedDocument
import com.example.data.local.ScannedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PageDraft(
    val originalFile: File,
    val processedFile: File,
    val quad: DocumentQuad,
    val rotation: Int = 0,
    val filter: FilterMode = FilterMode.DOCUMENT,
    val ocrText: String? = null
)

enum class SortOrder {
    NEWEST, OLDEST, NAME, SIZE
}

class ScanSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val docDao = db.documentDao()
    val prefs = PreferencesManager(application)

    // Current Scan Session Draft Pages
    private val _draftPages = MutableStateFlow<List<PageDraft>>(emptyList())
    val draftPages: StateFlow<List<PageDraft>> = _draftPages.asStateFlow()

    private val _activeEditPageIndex = MutableStateFlow<Int>(0)
    val activeEditPageIndex: StateFlow<Int> = _activeEditPageIndex.asStateFlow()

    // Temporary working bitmap for crop screen
    private val _currentEditBitmap = MutableStateFlow<Bitmap?>(null)
    val currentEditBitmap: StateFlow<Bitmap?> = _currentEditBitmap.asStateFlow()

    private val _currentEditQuad = MutableStateFlow<DocumentQuad?>(null)
    val currentEditQuad: StateFlow<DocumentQuad?> = _currentEditQuad.asStateFlow()

    private val _currentEditFilter = MutableStateFlow(FilterMode.DOCUMENT)
    val currentEditFilter: StateFlow<FilterMode> = _currentEditFilter.asStateFlow()

    private val _currentEditRotation = MutableStateFlow(0)
    val currentEditRotation: StateFlow<Int> = _currentEditRotation.asStateFlow()

    // Processing & UI Feedback states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Documents Screen State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(
        try { SortOrder.valueOf(prefs.sortOption) } catch (e: Exception) { SortOrder.NEWEST }
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.isGridView)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Active Documents Stream with search and sort
    val activeDocuments: StateFlow<List<ScannedDocument>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                docDao.getAllActiveDocuments()
            } else {
                docDao.searchActiveDocuments(query)
            }
        }
        .combine(_sortOrder) { list, sort ->
            when (sort) {
                SortOrder.NEWEST -> list.sortedByDescending { it.createdAt }
                SortOrder.OLDEST -> list.sortedBy { it.createdAt }
                SortOrder.NAME -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
                SortOrder.SIZE -> list.sortedByDescending { it.fileSizeBytes }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashDocuments: StateFlow<List<ScannedDocument>> = docDao.getTrashDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        prefs.sortOption = order.name
    }

    fun toggleGridView() {
        val newVal = !_isGridView.value
        _isGridView.value = newVal
        prefs.isGridView = newVal
    }

    // =========================================================================
    // SCANNING & DRAFT MANAGEMENT
    // =========================================================================

    fun clearDraftSession() {
        _draftPages.value = emptyList()
        _activeEditPageIndex.value = 0
        _currentEditBitmap.value = null
        _currentEditQuad.value = null
    }

    /**
     * Add captured page from camera.
     */
    fun addCapturedImage(imageFile: File, onReadyForCrop: (pageIndex: Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bitmap = ImageProcessor.decodeBitmapSafe(imageFile)
                if (bitmap != null) {
                    val quad = ImageProcessor.detectDocumentCorners(bitmap)
                    val processedBitmap = ImageProcessor.cropAndWarpPerspective(bitmap, quad)
                    val filtered = ImageProcessor.applyFilter(processedBitmap, FilterMode.DOCUMENT)
                    val processedFile = ImageProcessor.saveBitmapToFile(
                        getApplication(),
                        filtered,
                        quality = 90
                    )
                    processedBitmap.recycle()
                    filtered.recycle()
                    bitmap.recycle()

                    val draft = PageDraft(
                        originalFile = imageFile,
                        processedFile = processedFile,
                        quad = quad,
                        rotation = 0,
                        filter = FilterMode.DOCUMENT
                    )

                    val updated = _draftPages.value.toMutableList().apply { add(draft) }
                    _draftPages.value = updated
                    val newIndex = updated.size - 1
                    _activeEditPageIndex.value = newIndex
                    AppAnalytics.logEvent("scan_completed", mapOf("page_count" to updated.size.toString()))
                    onReadyForCrop(newIndex)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to process captured page: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add images imported from gallery.
     */
    fun importImages(uris: List<Uri>, onCompleted: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val newPages = mutableListOf<PageDraft>()

                for (uri in uris) {
                    val bitmap = ImageProcessor.decodeUriSafe(context, uri) ?: continue
                    val origFile = ImageProcessor.saveBitmapToFile(context, bitmap, subDir = "originals")
                    val quad = ImageProcessor.detectDocumentCorners(bitmap)
                    val warped = ImageProcessor.cropAndWarpPerspective(bitmap, quad)
                    val filtered = ImageProcessor.applyFilter(warped, FilterMode.DOCUMENT)
                    val procFile = ImageProcessor.saveBitmapToFile(context, filtered, subDir = "scans")

                    bitmap.recycle()
                    warped.recycle()
                    filtered.recycle()

                    newPages.add(
                        PageDraft(
                            originalFile = origFile,
                            processedFile = procFile,
                            quad = quad,
                            rotation = 0,
                            filter = FilterMode.DOCUMENT
                        )
                    )
                }

                if (newPages.isNotEmpty()) {
                    val combined = _draftPages.value + newPages
                    _draftPages.value = combined
                    AppAnalytics.logEvent("import_images", mapOf("count" to newPages.size.toString()))
                    onCompleted()
                } else {
                    _statusMessage.value = "Could not load selected images"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Setup single page for Crop & Filter editing screen.
     */
    fun loadPageForEditing(index: Int) {
        val pages = _draftPages.value
        if (index !in pages.indices) return

        _activeEditPageIndex.value = index
        val page = pages[index]
        _currentEditFilter.value = page.filter
        _currentEditRotation.value = page.rotation

        viewModelScope.launch {
            _isLoading.value = true
            val bitmap = ImageProcessor.decodeBitmapSafe(page.originalFile)
            _currentEditBitmap.value = bitmap
            _currentEditQuad.value = page.quad
            _isLoading.value = false
        }
    }

    fun updateCurrentQuad(quad: DocumentQuad) {
        _currentEditQuad.value = quad
    }

    fun autoDetectCurrentQuad() {
        val bmp = _currentEditBitmap.value ?: return
        val quad = ImageProcessor.detectDocumentCorners(bmp)
        _currentEditQuad.value = quad
    }

    fun resetQuadToFull() {
        val bmp = _currentEditBitmap.value ?: return
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        _currentEditQuad.value = DocumentQuad(
            topLeft = PointF(0f, 0f),
            topRight = PointF(w, 0f),
            bottomRight = PointF(w, h),
            bottomLeft = PointF(0f, h)
        )
    }

    fun rotateCurrentPage() {
        _currentEditRotation.value = (_currentEditRotation.value + 90) % 360
    }

    fun setCurrentFilter(mode: FilterMode) {
        _currentEditFilter.value = mode
    }

    /**
     * Apply crop, rotation, and filter to current page and update draft.
     */
    fun saveCurrentPageEdits(onSaved: () -> Unit) {
        val index = _activeEditPageIndex.value
        val pages = _draftPages.value
        if (index !in pages.indices) return

        val origBitmap = _currentEditBitmap.value ?: return
        val quad = _currentEditQuad.value ?: return
        val filter = _currentEditFilter.value
        val rotation = _currentEditRotation.value

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Perspective Crop
                val warped = ImageProcessor.cropAndWarpPerspective(origBitmap, quad)
                // 2. Rotate if needed
                val rotated = if (rotation != 0) ImageProcessor.rotateBitmap(warped, rotation.toFloat()) else warped
                // 3. Filter
                val filtered = ImageProcessor.applyFilter(rotated, filter)

                val context = getApplication<Application>()
                val newProcessedFile = ImageProcessor.saveBitmapToFile(context, filtered, subDir = "scans")

                if (warped != rotated) warped.recycle()
                rotated.recycle()
                filtered.recycle()

                val updatedPage = pages[index].copy(
                    processedFile = newProcessedFile,
                    quad = quad,
                    rotation = rotation,
                    filter = filter
                )

                val newList = pages.toMutableList()
                newList[index] = updatedPage
                _draftPages.value = newList
                onSaved()
            } catch (e: Exception) {
                _statusMessage.value = "Failed to save page edits: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val list = _draftPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _draftPages.value = list
        }
    }

    fun deleteDraftPage(index: Int) {
        val list = _draftPages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _draftPages.value = list
        }
    }

    /**
     * Finalize scanning session, create PDF, and save ScannedDocument into Room DB.
     */
    fun saveDraftAsDocument(
        title: String,
        onSuccess: (documentId: Long) -> Unit
    ) {
        val pages = _draftPages.value
        if (pages.isEmpty()) {
            _statusMessage.value = "Cannot save empty document"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val docTitle = title.ifBlank {
                    "Scan " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                }

                // 1. Create PDF
                val imageFiles = pages.map { it.processedFile }
                val pdfFile = PdfToolManager.createPdfFromImageFiles(
                    context,
                    imageFiles,
                    docTitle,
                    quality = prefs.pdfQuality
                )

                // 2. Generate thumbnail
                val thumbFile = PdfToolManager.renderPdfThumbnail(context, pdfFile)

                // 3. Save Document to Room
                val doc = ScannedDocument(
                    title = docTitle,
                    pageCount = pages.size,
                    fileSizeBytes = pdfFile.length(),
                    pdfFilePath = pdfFile.absolutePath,
                    thumbnailPath = thumbFile?.absolutePath ?: imageFiles.firstOrNull()?.absolutePath
                )
                val docId = docDao.insertDocument(doc)

                // 4. Save individual pages
                val pageEntities = pages.mapIndexed { idx, p ->
                    ScannedPage(
                        documentId = docId,
                        pageIndex = idx,
                        imagePath = p.processedFile.absolutePath,
                        originalImagePath = p.originalFile.absolutePath,
                        rotationDegrees = p.rotation,
                        filterMode = p.filter.name
                    )
                }
                docDao.insertPages(pageEntities)

                AppAnalytics.logEvent(
                    "pdf_created",
                    mapOf("page_count" to pages.size.toString(), "size" to pdfFile.length().toString())
                )

                clearDraftSession()
                _statusMessage.value = "PDF saved successfully!"
                onSuccess(docId)
            } catch (e: Exception) {
                _statusMessage.value = "Failed to save PDF: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =========================================================================
    // DOCUMENT MANAGEMENT OPERATIONS
    // =========================================================================

    fun moveToTrash(id: Long) {
        viewModelScope.launch {
            docDao.moveToTrash(id)
            _statusMessage.value = "Document moved to Trash"
        }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch {
            docDao.restoreFromTrash(id)
            _statusMessage.value = "Document restored"
        }
    }

    fun deleteDocumentPermanently(document: ScannedDocument) {
        viewModelScope.launch {
            // Delete files from disk
            document.pdfFilePath?.let { File(it).delete() }
            document.thumbnailPath?.let { File(it).delete() }
            docDao.deleteDocument(document)
            _statusMessage.value = "Document permanently deleted"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashList = docDao.getTrashDocuments()
            docDao.emptyTrash()
            _statusMessage.value = "Trash emptied"
        }
    }

    fun renameDocument(id: Long, newTitle: String) {
        viewModelScope.launch {
            val doc = docDao.getDocumentByIdOnce(id) ?: return@launch
            docDao.updateDocument(doc.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            _statusMessage.value = "Document renamed"
        }
    }

    fun updateDocumentOcrText(id: Long, text: String) {
        viewModelScope.launch {
            val doc = docDao.getDocumentByIdOnce(id) ?: return@launch
            docDao.updateDocument(doc.copy(ocrText = text, updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateDocumentAiSummary(id: Long, summary: String) {
        viewModelScope.launch {
            val doc = docDao.getDocumentByIdOnce(id) ?: return@launch
            docDao.updateDocument(doc.copy(aiSummary = summary, updatedAt = System.currentTimeMillis()))
        }
    }
}
