package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE isTrash = 0 ORDER BY createdAt DESC")
    fun getAllActiveDocuments(): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM documents WHERE isTrash = 0 AND (title LIKE '%' || :query || '%' OR ocrText LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchActiveDocuments(query: String): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM documents WHERE isTrash = 1 ORDER BY updatedAt DESC")
    fun getTrashDocuments(): Flow<List<ScannedDocument>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): Flow<ScannedDocument?>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentByIdOnce(id: Long): ScannedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument): Long

    @Update
    suspend fun updateDocument(document: ScannedDocument)

    @Delete
    suspend fun deleteDocument(document: ScannedDocument)

    @Query("UPDATE documents SET isTrash = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isTrash = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM documents WHERE isTrash = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<ScannedPage>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesForDocumentOnce(documentId: Long): List<ScannedPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<ScannedPage>)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)
}
