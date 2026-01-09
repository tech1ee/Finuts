package com.finuts.data.import.ocr

import android.content.Context
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Manages Tesseract OCR language training data files.
 *
 * Downloads language files on-demand from GitHub and stores them
 * in the app's private directory (context.filesDir/tessdata/).
 *
 * Supported languages:
 * - Russian (rus) - 15MB - Primary for Kazakhstan/CIS
 * - Kazakh (kaz) - 12MB - Secondary for Kazakhstan
 * - English (eng) - 12MB - Fallback
 */
class TessLanguageManager(private val context: Context) {

    private val logger = Logger.withTag("TessLanguageManager")
    private val tessDataDir: File by lazy {
        File(context.filesDir, TESSDATA_DIR).apply { mkdirs() }
    }

    /**
     * Get the path to tessdata directory for Tesseract initialization.
     * Note: Tesseract expects the PARENT directory, not tessdata/ itself.
     */
    fun getTessDataPath(): String = context.filesDir.absolutePath

    /**
     * Check if a language is available (downloaded).
     */
    fun isLanguageAvailable(language: String): Boolean {
        val file = File(tessDataDir, "$language.traineddata")
        return file.exists() && file.length() > MIN_FILE_SIZE
    }

    /**
     * Get list of available (downloaded) languages.
     */
    fun getAvailableLanguages(): List<String> {
        return tessDataDir.listFiles()
            ?.filter { it.name.endsWith(".traineddata") }
            ?.filter { it.length() > MIN_FILE_SIZE }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    /**
     * Ensure a language is available, downloading if necessary.
     *
     * @param language Language code (e.g., "rus", "eng", "kaz")
     * @param onProgress Optional callback for download progress (0.0 to 1.0)
     * @return Result with tessdata path on success
     */
    suspend fun ensureLanguage(
        language: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(tessDataDir, "$language.traineddata")

            if (file.exists() && file.length() > MIN_FILE_SIZE) {
                logger.d { "Language $language already available" }
                return@withContext Result.success(getTessDataPath())
            }

            val url = LANGUAGE_URLS[language]
                ?: return@withContext Result.failure(
                    OcrException("Unknown language: $language. Supported: ${LANGUAGE_URLS.keys}")
                )

            logger.i { "Downloading $language.traineddata..." }
            downloadFile(url, file, onProgress)
            logger.i { "Downloaded $language.traineddata (${file.length() / 1024 / 1024}MB)" }

            Result.success(getTessDataPath())
        } catch (e: Exception) {
            logger.e(e) { "Failed to ensure language $language" }
            Result.failure(OcrException("Failed to download language $language: ${e.message}", e))
        }
    }

    /**
     * Download multiple languages in sequence.
     */
    suspend fun ensureLanguages(
        languages: List<String>,
        onProgress: ((String, Float) -> Unit)? = null
    ): Result<String> {
        for (language in languages) {
            val result = ensureLanguage(language) { progress ->
                onProgress?.invoke(language, progress)
            }
            if (result.isFailure) {
                return result
            }
        }
        return Result.success(getTessDataPath())
    }

    /**
     * Delete a downloaded language file.
     */
    fun deleteLanguage(language: String): Boolean {
        val file = File(tessDataDir, "$language.traineddata")
        return if (file.exists()) {
            file.delete().also {
                logger.d { "Deleted $language.traineddata: $it" }
            }
        } else {
            true
        }
    }

    /**
     * Get total size of downloaded language files.
     */
    fun getTotalDownloadedSize(): Long {
        return tessDataDir.listFiles()
            ?.filter { it.name.endsWith(".traineddata") }
            ?.sumOf { it.length() }
            ?: 0L
    }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: ((Float) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection().apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

        val contentLength = connection.contentLength.toLong()
        var downloaded = 0L
        val buffer = ByteArray(BUFFER_SIZE)

        // Use temp file to avoid partial downloads
        val tempFile = File(destination.parent, "${destination.name}.tmp")

        try {
            connection.getInputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        if (contentLength > 0) {
                            val progress = downloaded.toFloat() / contentLength
                            onProgress?.invoke(progress)
                        }

                        bytesRead = input.read(buffer)
                    }
                }
            }

            // Rename temp file to final destination
            if (!tempFile.renameTo(destination)) {
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }
        } finally {
            // Clean up temp file on failure
            if (tempFile.exists() && !destination.exists()) {
                tempFile.delete()
            }
        }
    }

    companion object {
        private const val TESSDATA_DIR = "tessdata"
        private const val MIN_FILE_SIZE = 1_000_000L // 1MB minimum
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000

        /**
         * Download URLs for Tesseract language training data.
         * Source: https://github.com/UB-Mannheim/tesseract/wiki
         */
        val LANGUAGE_URLS = mapOf(
            "rus" to "https://github.com/tesseract-ocr/tessdata/raw/main/rus.traineddata",
            "kaz" to "https://github.com/tesseract-ocr/tessdata/raw/main/kaz.traineddata",
            "eng" to "https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata"
        )

        /**
         * Expected file sizes for validation.
         */
        val LANGUAGE_SIZES = mapOf(
            "rus" to 15_728_640L,  // ~15MB
            "kaz" to 12_582_912L,  // ~12MB
            "eng" to 12_582_912L   // ~12MB
        )
    }
}
