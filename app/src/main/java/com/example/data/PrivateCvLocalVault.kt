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
}
