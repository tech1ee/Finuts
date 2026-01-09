package com.finuts.data.import.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Android implementation of PdfTextExtractor using PdfRenderer.
 *
 * Converts PDF pages to bitmap images for OCR processing.
 * Renders at 2x scale (approximately 300 DPI) for better OCR quality.
 */
actual class PdfTextExtractor(
    private val context: Context
) {
    companion object {
        private const val SCALE_FACTOR = 2.0f
        private const val IMAGE_QUALITY = 100
    }

    actual suspend fun extractPages(pdfData: ByteArray): List<PdfPage> =
        withContext(Dispatchers.IO) {
            val tempFile = createTempFile(pdfData)

            try {
                val fileDescriptor = ParcelFileDescriptor.open(
                    tempFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )

                val renderer = PdfRenderer(fileDescriptor)
                val pages = mutableListOf<PdfPage>()

                try {
                    for (i in 0 until renderer.pageCount) {
                        val page = extractSinglePage(renderer, i)
                        pages.add(page)
                    }
                } finally {
                    renderer.close()
                    fileDescriptor.close()
                }

                pages
            } catch (e: Exception) {
                throw PdfExtractionException(
                    "Failed to extract PDF pages: ${e.message}",
                    e
                )
            } finally {
                tempFile.delete()
            }
        }

    private fun createTempFile(pdfData: ByteArray): File {
        val tempFile = File.createTempFile("import_", ".pdf", context.cacheDir)
        tempFile.writeBytes(pdfData)
        return tempFile
    }

    private fun extractSinglePage(renderer: PdfRenderer, pageIndex: Int): PdfPage {
        val page = renderer.openPage(pageIndex)

        try {
            val width = (page.width * SCALE_FACTOR).toInt()
            val height = (page.height * SCALE_FACTOR).toInt()

            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            val imageData = bitmapToByteArray(bitmap)
            bitmap.recycle()

            return PdfPage(
                index = pageIndex,
                width = width,
                height = height,
                imageData = imageData
            )
        } finally {
            page.close()
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, stream)
        return stream.toByteArray()
    }
}
