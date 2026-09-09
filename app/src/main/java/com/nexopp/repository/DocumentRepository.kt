package com.nexopp.repository

import com.nexopp.format.model.Document
import java.io.File

/**
 * Interface decoupling document lifecycle and I/O from storage implementations.
 * Enables clean multi-device sync, local storage, future desktop PC client, and automated unit testing.
 */
interface DocumentRepository {
    suspend fun listDocuments(includeTrashed: Boolean = false): List<DocumentMetadata>
    suspend fun getDocumentMetadata(fileName: String): DocumentMetadata?
    suspend fun updateDocumentMetadata(metadata: DocumentMetadata): Boolean
    
    suspend fun loadDocument(fileName: String): Result<Document>
    suspend fun saveDocument(fileName: String, document: Document): Result<File>
    
    suspend fun renameDocument(oldFileName: String, newFileName: String): Result<Unit>
    suspend fun moveToTrash(fileName: String): Result<Unit>
    suspend fun restoreFromTrash(fileName: String): Result<Unit>
    suspend fun permanentlyDelete(fileName: String): Result<Unit>
    
    suspend fun calculateContentHash(fileName: String): String
    fun getDocumentFile(fileName: String): File
}
