package com.finuts.data.import.ocr

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
    companion object {
        private const val SCALE_FACTOR = 2.0
    }

    actual suspend fun extractPages(pdfData: ByteArray): List<com.finuts.data.import.ocr.PdfPage> =
        withContext(Dispatchers.IO) {
            val nsData = pdfData.toNSData()
            val document = PDFDocument(data = nsData)
                ?: throw PdfExtractionException("Failed to create PDF document from data")

            val pageCount = document.pageCount.toInt()
            if (pageCount == 0) {
                throw PdfExtractionException("PDF document contains no pages")
            }

            val pages = mutableListOf<com.finuts.data.import.ocr.PdfPage>()

            for (i in 0 until pageCount) {
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

                val imageData = renderPageToImage(pdfPage, width, height)

                pages.add(
                    com.finuts.data.import.ocr.PdfPage(
                        index = i,
                        width = width,
                        height = height,
                        imageData = imageData
                    )
                )
            }

            pages
        }

    private fun renderPageToImage(
        pdfPage: PDFPage,
        width: Int,
        height: Int
    ): ByteArray {
        val size = CGSizeMake(width.toDouble(), height.toDouble())

        UIGraphicsBeginImageContextWithOptions(size, true, 1.0)
        try {
            val context = UIGraphicsGetCurrentContext()
                ?: throw PdfExtractionException("Failed to create graphics context")

            // Fill with white background
            platform.CoreGraphics.CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            platform.CoreGraphics.CGContextFillRect(
                context,
                CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
            )

            // Scale context
            platform.CoreGraphics.CGContextScaleCTM(
                context,
                SCALE_FACTOR,
                SCALE_FACTOR
            )

            // Draw PDF page
            pdfPage.drawWithBox(
                platform.PDFKit.kPDFDisplayBoxMediaBox,
                toContext = context
            )

            val image = UIGraphicsGetImageFromCurrentImageContext()
                ?: throw PdfExtractionException("Failed to get image from context")

            val pngData = UIImagePNGRepresentation(image)
                ?: throw PdfExtractionException("Failed to convert image to PNG")

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
