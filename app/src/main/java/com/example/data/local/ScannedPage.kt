package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = ScannedDocument::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class ScannedPage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long = 0,
    val pageIndex: Int = 0,
    val imagePath: String,
    val originalImagePath: String? = null,
    val rotationDegrees: Int = 0,
    val filterMode: String = "DOCUMENT", // ORIGINAL, DOCUMENT, BW, GRAYSCALE
    val ocrText: String? = null
)
