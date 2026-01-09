package com.finuts.data.import.ocr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGImageRef
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of OcrService using Vision Framework.
 *
 * Uses VNRecognizeTextRequest which supports 18+ languages
 * including Russian (Cyrillic), English, and Kazakh.
 *
 * Requires iOS 13+ for basic text recognition.
 * iOS 15+ recommended for improved accuracy.
 */
@OptIn(ExperimentalForeignApi::class)
actual class OcrService {
    companion object {
        private val RECOGNITION_LANGUAGES = listOf("ru-RU", "en-US", "kk-KZ")
    }

    actual suspend fun recognizeText(imageData: ByteArray): OcrResult =
        suspendCancellableCoroutine { continuation ->
            try {
                val nsData = imageData.toNSData()
                val uiImage = UIImage.imageWithData(nsData)
                    ?: throw OcrException("Failed to create UIImage from data")

                val cgImage = uiImage.CGImage
                    ?: throw OcrException("Failed to get CGImage from UIImage")

                performTextRecognition(cgImage) { result, error ->
                    if (error != null) {
                        continuation.resumeWithException(
                            OcrException("OCR failed: ${error.localizedDescription}")
                        )
                    } else if (result != null) {
                        continuation.resume(result)
                    } else {
                        continuation.resumeWithException(
                            OcrException("OCR returned no results")
                        )
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(
                    OcrException("OCR processing failed: ${e.message}", e)
                )
            }
        }

    private fun performTextRecognition(
        cgImage: CGImageRef,
        completion: (OcrResult?, NSError?) -> Unit
    ) {
        val request = VNRecognizeTextRequest { request, error ->
            if (error != null) {
                completion(null, error)
                return@VNRecognizeTextRequest
            }

            val observations = request?.results
                ?.filterIsInstance<VNRecognizedTextObservation>()
                ?: emptyList()

            val blocks = observations.mapNotNull { observation ->
                processObservation(observation)
            }

            val fullText = blocks.joinToString("\n") { it.text }
            val avgConfidence = if (blocks.isNotEmpty()) {
                blocks.map { it.confidence }.average().toFloat()
            } else {
                0f
            }

            val result = OcrResult(
                fullText = fullText,
                blocks = blocks,
                overallConfidence = avgConfidence
            )

            completion(result, null)
        }

        // Configure recognition
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.usesLanguageCorrection = true
        request.setRecognitionLanguages(RECOGNITION_LANGUAGES)

        // Create handler and perform request
        val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())

        try {
            handler.performRequests(listOf(request), error = null)
        } catch (e: Exception) {
            completion(null, null)
        }
    }

    private fun processObservation(observation: VNRecognizedTextObservation): OcrBlock? {
        val candidates = observation.topCandidates(1u)
        val topCandidate = candidates.firstOrNull() ?: return null

        @Suppress("UNCHECKED_CAST")
        val text = (topCandidate as? platform.Vision.VNRecognizedText)?.string ?: return null

        val boundingBox = observation.boundingBox

        // Use useContents to access CGRect struct fields
        val (x, y, width, height) = boundingBox.useContents {
            listOf(
                origin.x.toFloat(),
                origin.y.toFloat(),
                size.width.toFloat(),
                size.height.toFloat()
            )
        }

        return OcrBlock(
            text = text,
            confidence = observation.confidence,
            boundingBox = BoundingBox(
                x = x,
                y = y,
                width = width,
                height = height
            )
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = this.size.toULong()
        )
    }
}
