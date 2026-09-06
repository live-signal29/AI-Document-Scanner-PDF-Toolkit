package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0L,
    val pdfFilePath: String? = null,
    val thumbnailPath: String? = null,
    val ocrText: String? = null,
    val aiSummary: String? = null,
    val tags: String = "",
    val isTrash: Boolean = false
)
