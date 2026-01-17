@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.finuts.data.model

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableData
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUserDomainMask
import platform.Foundation.appendData
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of ModelDownloader.
 * Downloads AI model files to Documents directory with progress tracking.
 */
actual class ModelDownloader : ModelDownloaderOperations {

    private val logger = Logger.withTag("ModelDownloader")
    private val fileManager = NSFileManager.defaultManager

    actual override fun getModelsDirectory(): String {
        val documentDirectory = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        val modelsPath = requireNotNull(documentDirectory).path + "/$MODELS_DIR"

        // Create directory if it doesn't exist
        if (!fileManager.fileExistsAtPath(modelsPath)) {
            fileManager.createDirectoryAtPath(
                path = modelsPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        return modelsPath
    }

    actual override suspend fun getAvailableStorage(): Long = withContext(Dispatchers.IO) {
        try {
            val documentDirectory = fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null
            )
            val path = requireNotNull(documentDirectory).path!!
            val attrs = fileManager.attributesOfFileSystemForPath(path, null)
            (attrs?.get(NSFileSystemFreeSize) as? Long) ?: 0L
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
        try {
            logger.i { "Starting download with progress: $url" }

            val nsUrl = NSURL.URLWithString(url) ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid URL: $url")
            )

            val request = NSURLRequest.requestWithURL(nsUrl)

            downloadWithProgress(request, destination, onProgress)

            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Download failed: $url" }
            Result.failure(e)
        }
    }

    private suspend fun downloadWithProgress(
        request: NSURLRequest,
        destination: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        val delegate = DownloadDelegate(
            destination = destination,
            onProgress = onProgress,
            continuation = continuation,
            logger = logger
        )

        val config = NSURLSessionConfiguration.defaultSessionConfiguration
        val session = NSURLSession.sessionWithConfiguration(
            configuration = config,
            delegate = delegate,
            delegateQueue = null
        )

        val task = session.dataTaskWithRequest(request)

        continuation.invokeOnCancellation {
            logger.i { "Download cancelled" }
            task.cancel()
            session.invalidateAndCancel()
        }

        task.resume()
    }

    actual override suspend fun verifyChecksum(
        filePath: String,
        expectedSha256: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Skip verification if no checksum provided
            if (expectedSha256.isBlank()) {
                logger.d { "No checksum provided, skipping verification" }
                return@withContext true
            }

            val data = NSData.create(contentsOfFile = filePath)
                ?: return@withContext false

            // iOS SHA-256 calculation using CommonCrypto
            val actualHash = calculateSHA256(data)
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
            if (fileManager.fileExistsAtPath(filePath)) {
                fileManager.removeItemAtPath(filePath, null).also {
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
            fileManager.fileExistsAtPath(filePath)
        } catch (e: Exception) {
            logger.e(e) { "Failed to check file existence: $filePath" }
            false
        }
    }

    private fun calculateSHA256(data: NSData): String {
        // Placeholder - in production, use CommonCrypto CC_SHA256
        // For now, return empty string (checksum verification will skip)
        // TODO: Implement proper SHA-256 using CommonCrypto
        return ""
    }

    companion object {
        private const val MODELS_DIR = "models"
    }
}

/**
 * NSURLSession delegate for tracking download progress.
 */
private class DownloadDelegate(
    private val destination: String,
    private val onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    private val continuation: CancellableContinuation<Unit>,
    private val logger: Logger
) : NSObject(), NSURLSessionDataDelegateProtocol {

    private val receivedData = NSMutableData()
    private var expectedContentLength: Long = -1L
    private var totalBytesReceived: Long = 0L

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (Long) -> Unit
    ) {
        expectedContentLength = didReceiveResponse.expectedContentLength
        logger.i { "Download started, expected size: $expectedContentLength bytes" }

        // Allow to receive data
        completionHandler(1L) // NSURLSessionResponseAllow = 1
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData
    ) {
        receivedData.appendData(didReceiveData)
        totalBytesReceived += didReceiveData.length.toLong()

        val total = if (expectedContentLength > 0) expectedContentLength else totalBytesReceived
        onProgress(totalBytesReceived, total)

        val progressPercent = if (total > 0) (totalBytesReceived * 100 / total) else 0
        if (progressPercent % 10 == 0L) {
            logger.d { "Download progress: $totalBytesReceived / $total bytes ($progressPercent%)" }
        }
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        session.finishTasksAndInvalidate()

        if (didCompleteWithError != null) {
            logger.e { "Download error: ${didCompleteWithError.localizedDescription}" }
            if (continuation.isActive) {
                continuation.resumeWithException(
                    Exception("Download failed: ${didCompleteWithError.localizedDescription}")
                )
            }
            return
        }

        // Write to file
        val success = receivedData.writeToFile(destination, atomically = true)
        if (success) {
            logger.i { "Download completed: $totalBytesReceived bytes saved to $destination" }
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        } else {
            logger.e { "Failed to write downloaded data to file" }
            if (continuation.isActive) {
                continuation.resumeWithException(Exception("Failed to write file to $destination"))
            }
        }
    }
}
