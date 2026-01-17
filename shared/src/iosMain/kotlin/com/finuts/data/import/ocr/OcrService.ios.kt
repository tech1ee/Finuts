package com.finuts.data.import.ocr

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageRef
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * iOS implementation of OcrService using Vision Framework.
 *
 * Uses VNRecognizeTextRequest which supports 18+ languages
 * including Russian (Cyrillic), English, and Kazakh.
 *
 * Requires iOS 13+ for basic text recognition.
 * iOS 15+ recommended for improved accuracy.
 *
 * IMPORTANT: Vision's performRequests is SYNCHRONOUS and blocks the thread.
 * We use withContext(Dispatchers.IO) to run on background thread.
 */
@OptIn(ExperimentalForeignApi::class)
actual class OcrService {
    private val log = Logger.withTag("OcrService")

    companion object {
        private val RECOGNITION_LANGUAGES = listOf("ru-RU", "en-US", "kk-KZ")
    }

    actual suspend fun recognizeText(imageData: ByteArray): OcrResult {
        log.d { "recognizeText() START - imageData=${imageData.size} bytes" }
        return withContext(Dispatchers.IO) {
            log.d { "Inside Dispatchers.IO context" }
            val nsData = imageData.toNSData()
            log.d { "Created NSData" }
            val uiImage = UIImage.imageWithData(nsData)
                ?: throw OcrException("Failed to create UIImage from data")
            log.d { "Created UIImage" }

            val cgImage = uiImage.CGImage
                ?: throw OcrException("Failed to get CGImage from UIImage")
            log.d { "Got CGImage, performing text recognition..." }

            val result = performTextRecognition(cgImage)
            log.d { "Recognition complete, text length=${result.fullText.length}" }
            result
        }
    }

    private fun performTextRecognition(cgImage: CGImageRef): OcrResult {
        log.d { "performTextRecognition() START" }
        var resultBlocks: List<OcrBlock> = emptyList()
        var recognitionError: NSError? = null

        log.v { "Creating VNRecognizeTextRequest..." }
        val request = VNRecognizeTextRequest { request, error ->
            log.d { "VNRecognizeTextRequest callback triggered" }
            if (error != null) {
                log.e { "VNRecognizeTextRequest ERROR: ${error.localizedDescription}" }
                recognitionError = error
                return@VNRecognizeTextRequest
            }

            val observations = request?.results
                ?.filterIsInstance<VNRecognizedTextObservation>()
                ?: emptyList()
            log.d { "Found ${observations.size} text observations" }

            resultBlocks = observations.mapNotNull { observation ->
                processObservation(observation)
            }
            log.d { "Processed ${resultBlocks.size} text blocks" }
        }

        // Configure recognition
        log.v { "Configuring recognition..." }
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.usesLanguageCorrection = true
        request.setRecognitionLanguages(RECOGNITION_LANGUAGES)

        // Create handler and perform request (SYNCHRONOUS call)
        log.v { "Creating VNImageRequestHandler..." }
        val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())

        log.d { "Calling performRequests() - SYNCHRONOUS..." }
        try {
            handler.performRequests(listOf(request), error = null)
            log.d { "performRequests() completed" }
        } catch (e: Exception) {
            log.e(e) { "performRequests() EXCEPTION: ${e.message}" }
            throw OcrException("Vision request failed: ${e.message}", e)
        }

        // Check for errors after synchronous execution
        recognitionError?.let { error ->
            log.e { "Recognition error: ${error.localizedDescription}" }
            throw OcrException("OCR failed: ${error.localizedDescription}")
        }

        val fullText = resultBlocks.joinToString("\n") { it.text }
        val avgConfidence = if (resultBlocks.isNotEmpty()) {
            resultBlocks.map { it.confidence }.average().toFloat()
        } else {
            0f
        }

        log.d { "performTextRecognition() DONE - text length=${fullText.length}, confidence=$avgConfidence" }
        return OcrResult(
            fullText = fullText,
            blocks = resultBlocks,
            overallConfidence = avgConfidence
        )
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
