package com.finuts.data.import.ocr

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation

/**
 * iOS implementation of PdfTextExtractor using PDFKit.
 *
 * Renders PDF pages to UIImage at high resolution for OCR processing.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PdfTextExtractor {
    private val log = Logger.withTag("PdfTextExtractor")

    companion object {
        private const val SCALE_FACTOR = 2.0
    }

    actual suspend fun extractPages(pdfData: ByteArray): List<com.finuts.data.import.ocr.PdfPage> {
        log.d { "extractPages() START - size=${pdfData.size}" }
        return withContext(Dispatchers.IO) {
            log.d { "Inside Dispatchers.IO context" }
            val nsData = pdfData.toNSData()
            log.d { "Created NSData" }
            val document = PDFDocument(data = nsData)
                ?: throw PdfExtractionException("Failed to create PDF document from data")
            log.d { "Created PDFDocument" }

            val pageCount = document.pageCount.toInt()
            log.d { "pageCount=$pageCount" }
            if (pageCount == 0) {
                throw PdfExtractionException("PDF document contains no pages")
            }

            val pages = mutableListOf<com.finuts.data.import.ocr.PdfPage>()

            for (i in 0 until pageCount) {
                log.d { "Processing page ${i + 1}/$pageCount..." }
                val pdfPage = document.pageAtIndex(i.toULong())
                    ?: throw PdfExtractionException("Failed to get page at index $i")

                val bounds = pdfPage.boundsForBox(
                    platform.PDFKit.kPDFDisplayBoxMediaBox
                )

                // Use useContents to access CGRect struct fields
                val (boundsWidth, boundsHeight) = bounds.useContents {
                    size.width to size.height
                }

                val width = (boundsWidth * SCALE_FACTOR).toInt()
                val height = (boundsHeight * SCALE_FACTOR).toInt()
                log.d { "Page $i size: ${width}x${height}, rendering..." }

                val imageData = renderPageToImage(pdfPage, width, height)
                log.d { "Page $i rendered, imageData=${imageData.size} bytes" }

                pages.add(
                    com.finuts.data.import.ocr.PdfPage(
                        index = i,
                        width = width,
                        height = height,
                        imageData = imageData
                    )
                )
            }

            log.d { "All ${pages.size} pages extracted" }
            pages
        }
    }

    private fun renderPageToImage(
        pdfPage: PDFPage,
        width: Int,
        height: Int
    ): ByteArray {
        log.d { "renderPageToImage() START - ${width}x${height}" }
        val size = CGSizeMake(width.toDouble(), height.toDouble())

        log.v { "UIGraphicsBeginImageContextWithOptions..." }
        UIGraphicsBeginImageContextWithOptions(size, true, 1.0)
        try {
            log.v { "UIGraphicsGetCurrentContext..." }
            val context = UIGraphicsGetCurrentContext()
                ?: throw PdfExtractionException("Failed to create graphics context")

            // Fill with white background
            log.v { "Filling white background..." }
            platform.CoreGraphics.CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            platform.CoreGraphics.CGContextFillRect(
                context,
                CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
            )

            // CRITICAL: Transform coordinate system!
            // PDF uses bottom-left origin, UIKit uses top-left origin.
            // Without this transformation, the image renders upside down → OCR reads garbage.
            //
            // Steps:
            // 1. Translate to top of the image (move origin up by full height)
            // 2. Flip Y axis (scale Y by -1)
            // 3. Then apply our scale factor
            log.d { "Applying coordinate transformation (flip Y axis)..." }
            platform.CoreGraphics.CGContextTranslateCTM(context, 0.0, height.toDouble())
            platform.CoreGraphics.CGContextScaleCTM(context, SCALE_FACTOR, -SCALE_FACTOR)

            // Draw PDF page
            log.d { "Drawing PDF page to context..." }
            pdfPage.drawWithBox(
                platform.PDFKit.kPDFDisplayBoxMediaBox,
                toContext = context
            )
            log.d { "PDF page drawn" }

            log.v { "UIGraphicsGetImageFromCurrentImageContext..." }
            val image = UIGraphicsGetImageFromCurrentImageContext()
                ?: throw PdfExtractionException("Failed to get image from context")

            log.v { "UIImagePNGRepresentation..." }
            val pngData = UIImagePNGRepresentation(image)
                ?: throw PdfExtractionException("Failed to convert image to PNG")
            log.d { "PNG data size: ${pngData.length} bytes" }

            return pngData.toByteArray()
        } finally {
            UIGraphicsEndImageContext()
        }
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

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            platform.posix.memcpy(
                pinned.addressOf(0),
                this.bytes,
                this.length
            )
        }
    }
    return bytes
}
