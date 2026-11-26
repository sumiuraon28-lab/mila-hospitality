package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.auth.CandidateValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PickedCvDocument(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

object PrivateCvLocalVault {
    suspend fun readPickedCv(context: Context, uri: Uri): Result<PickedCvDocument> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var displayName = "candidate_cv.pdf"
            var reportedSize = -1L

            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) {
                            displayName = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: displayName
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            reportedSize = cursor.getLong(sizeIdx)
                        }
                    }
                }

            val rawMime = resolver.getType(uri)
            val inferredMime = CandidateValidator.inferMimeType(displayName, rawMime)

            if (reportedSize > CandidateValidator.MAX_CV_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("CV file size must not exceed 10 MB.")
                )
            }

            val bytes = resolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: return@withContext Result.failure(
                IllegalStateException("Unable to read the selected CV file.")
            )

            CandidateValidator.validateCvFile(displayName, inferredMime, bytes.size.toLong())?.let { err ->
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            Result.success(
                PickedCvDocument(
                    fileName = CandidateValidator.sanitizeCvFileName(displayName),
                    mimeType = inferredMime,
                    bytes = bytes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveOwnerOnlyCopy(
        context: Context,
        userId: String,
        storagePath: String,
        bytes: ByteArray,
        previousStoragePath: String?
    ) = withContext(Dispatchers.IO) {
        val rootDir = File(context.filesDir, "private_cvs/$userId")
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        if (!previousStoragePath.isNullOrBlank()) {
            val prevFileName = previousStoragePath.substringAfterLast('/')
            File(rootDir, prevFileName).delete()
        }
        val targetFileName = storagePath.substringAfterLast('/')
        File(rootDir, targetFileName).writeBytes(bytes)
    }

    suspend fun deleteOwnerOnlyCopy(
        context: Context,
        userId: String,
        storagePath: String?
    ) = withContext(Dispatchers.IO) {
        val rootDir = File(context.filesDir, "private_cvs/$userId")
        if (!rootDir.exists()) return@withContext
        if (storagePath.isNullOrBlank()) {
            rootDir.listFiles()?.forEach { it.delete() }
        } else {
            File(rootDir, storagePath.substringAfterLast('/')).delete()
        }
    }

    suspend fun readOwnerOnlyCopy(
        context: Context,
        userId: String,
        storagePath: String?
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        val rootDir = File(context.filesDir, "private_cvs/$userId")
        if (!rootDir.exists()) return@withContext null
        val candidateFile = if (!storagePath.isNullOrBlank()) {
            File(rootDir, storagePath.substringAfterLast('/'))
        } else {
            rootDir.listFiles()?.firstOrNull()
        }
        candidateFile?.takeIf { it.exists() && it.isFile }?.readBytes()
    }

    data class SavedAdminCvDownload(
        val fileName: String,
        val mimeType: String,
        val shareableUri: Uri,
        val savedToDeviceDownloads: Boolean
    )

    suspend fun saveDownloadedCvForAdmin(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<SavedAdminCvDownload> = withContext(Dispatchers.IO) {
        try {
            val safeName = CandidateValidator.sanitizeCvFileName(fileName)
            val resolvedMime = CandidateValidator.inferMimeType(safeName, mimeType)

            // 1. Save into public Downloads/MILA_CVs on Android 10+ (zero runtime permissions required)
            var savedToDownloads = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, resolvedMime)
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/MILA_CVs")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val downloadUri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                if (downloadUri != null) {
                    resolver.openOutputStream(downloadUri)?.use { out ->
                        out.write(bytes)
                    }
                    values.clear()
                    values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(downloadUri, values, null, null)
                    savedToDownloads = true
                }
            }

            // 2. Write a shareable copy into cache/cv_downloads for immediate FileProvider viewing
            val cacheDir = File(context.cacheDir, "cv_downloads").apply {
                if (!exists()) mkdirs()
            }
            val cacheFile = File(cacheDir, safeName)
            cacheFile.writeBytes(bytes)

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            Result.success(
                SavedAdminCvDownload(
                    fileName = safeName,
                    mimeType = resolvedMime,
                    shareableUri = contentUri,
                    savedToDeviceDownloads = savedToDownloads
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
