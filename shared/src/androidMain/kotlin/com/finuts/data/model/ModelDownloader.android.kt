package com.finuts.data.model

import android.content.Context
import android.os.StatFs
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Android implementation of ModelDownloader.
 * Downloads AI model files to app's private storage.
 */
actual class ModelDownloader(private val context: Context) : ModelDownloaderOperations {

    private val logger = Logger.withTag("ModelDownloader")
    private val modelsDir: File by lazy {
        File(context.filesDir, MODELS_DIR).apply { mkdirs() }
    }

    actual override fun getModelsDirectory(): String = modelsDir.absolutePath

    actual override suspend fun getAvailableStorage(): Long = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            logger.e(e) { "Failed to get available storage" }
            0L
        }
    }

    actual override suspend fun download(
        url: String,
        destination: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val destFile = File(destination)
        val tempFile = File(destination + TEMP_SUFFIX)

        try {
            logger.i { "Starting download: $url" }

            val connection = URL(url).openConnection().apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val totalBytes = connection.contentLengthLong
            var downloaded = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            connection.getInputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(downloaded, totalBytes)
                        bytesRead = input.read(buffer)
                    }
                }
            }

            // Atomic rename
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            logger.i { "Download completed: ${destFile.length()} bytes" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Download failed: $url" }
            tempFile.delete()
            Result.failure(e)
        }
    }

    actual override suspend fun verifyChecksum(
        filePath: String,
        expectedSha256: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val file = File(filePath)

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead = fis.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = fis.read(buffer)
                }
            }

            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            val matches = actualHash.equals(expectedSha256, ignoreCase = true)

            if (!matches) {
                logger.w { "Checksum mismatch: expected $expectedSha256, got $actualHash" }
            }

            matches
        } catch (e: Exception) {
            logger.e(e) { "Checksum verification failed" }
            false
        }
    }

    actual override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete().also {
                    logger.d { "Deleted file $filePath: $it" }
                }
            } else {
                true
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete file: $filePath" }
            false
        }
    }

    actual override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).exists()
        } catch (e: Exception) {
            logger.e(e) { "Failed to check file existence: $filePath" }
            false
        }
    }

    companion object {
        private const val MODELS_DIR = "models"
        private const val TEMP_SUFFIX = ".tmp"
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 120_000 // 2 min for large files
    }
}
