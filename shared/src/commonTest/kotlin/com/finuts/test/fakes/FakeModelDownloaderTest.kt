package com.finuts.test.fakes

import com.finuts.test.BaseTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeModelDownloaderTest : BaseTest() {

    @Test
    fun `getModelsDirectory returns configured path`() {
        val downloader = FakeModelDownloader(modelsDirectory = "/custom/path")

        assertEquals("/custom/path", downloader.getModelsDirectory())
    }

    @Test
    fun `getAvailableStorage returns configured value`() = runTest {
        val downloader = FakeModelDownloader(availableStorage = 5_000_000_000L)

        assertEquals(5_000_000_000L, downloader.getAvailableStorage())
    }

    @Test
    fun `fileExists returns false initially`() = runTest {
        val downloader = FakeModelDownloader()

        assertFalse(downloader.fileExists("/some/file.gguf"))
    }

    @Test
    fun `fileExists returns true for initial existing files`() = runTest {
        val downloader = FakeModelDownloader(
            initialExistingFiles = setOf("/models/model.gguf")
        )

        assertTrue(downloader.fileExists("/models/model.gguf"))
    }

    @Test
    fun `download tracks call information`() = runTest {
        val downloader = FakeModelDownloader()

        downloader.download("https://example.com/model.gguf", "/dest/model.gguf") { _, _ -> }

        assertTrue(downloader.downloadCalled)
        assertEquals("https://example.com/model.gguf", downloader.lastDownloadUrl)
        assertEquals("/dest/model.gguf", downloader.lastDownloadDestination)
    }

    @Test
    fun `download returns success by default`() = runTest {
        val downloader = FakeModelDownloader()

        val result = downloader.download("url", "/dest/file.gguf") { _, _ -> }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `download returns failure when configured`() = runTest {
        val downloader = FakeModelDownloader()
        downloader.shouldDownloadFail = true
        downloader.downloadError = RuntimeException("Network error")

        val result = downloader.download("url", "/dest/file.gguf") { _, _ -> }

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `download marks file as existing after success`() = runTest {
        val downloader = FakeModelDownloader()

        downloader.download("url", "/dest/model.gguf") { _, _ -> }

        assertTrue(downloader.fileExists("/dest/model.gguf"))
    }

    @Test
    fun `download reports progress`() = runTest {
        val downloader = FakeModelDownloader()
        val progressValues = mutableListOf<Pair<Long, Long>>()

        downloader.download("url", "/dest/file.gguf") { bytes, total ->
            progressValues.add(bytes to total)
        }

        assertEquals(10, progressValues.size)
        assertEquals(10_000L, progressValues.first().first)
        assertEquals(100_000L, progressValues.last().first)
    }

    @Test
    fun `deleteFile removes file and tracks call`() = runTest {
        val downloader = FakeModelDownloader(
            initialExistingFiles = setOf("/models/to-delete.gguf")
        )

        val result = downloader.deleteFile("/models/to-delete.gguf")

        assertTrue(result)
        assertTrue(downloader.deleteFileCalled)
        assertEquals("/models/to-delete.gguf", downloader.lastDeletedFile)
        assertFalse(downloader.fileExists("/models/to-delete.gguf"))
    }

    @Test
    fun `verifyChecksum returns true for existing files`() = runTest {
        val downloader = FakeModelDownloader(
            initialExistingFiles = setOf("/models/model.gguf")
        )

        val result = downloader.verifyChecksum(
            "/models/model.gguf",
            "abc123"
        )

        assertTrue(result)
    }

    @Test
    fun `verifyChecksum returns true for blank checksum`() = runTest {
        val downloader = FakeModelDownloader()

        val result = downloader.verifyChecksum("/any/path", "")

        assertTrue(result)
    }

    @Test
    fun `verifyChecksum returns false for non-existing files`() = runTest {
        val downloader = FakeModelDownloader()

        val result = downloader.verifyChecksum(
            "/non/existing.gguf",
            "checksum"
        )

        assertFalse(result)
    }

    @Test
    fun `addExistingFile makes file exist`() = runTest {
        val downloader = FakeModelDownloader()

        downloader.addExistingFile("/added/file.gguf")

        assertTrue(downloader.fileExists("/added/file.gguf"))
    }

    @Test
    fun `removeExistingFile makes file not exist`() = runTest {
        val downloader = FakeModelDownloader(
            initialExistingFiles = setOf("/models/file.gguf")
        )

        downloader.removeExistingFile("/models/file.gguf")

        assertFalse(downloader.fileExists("/models/file.gguf"))
    }

    @Test
    fun `getExistingFiles returns copy of files set`() = runTest {
        val downloader = FakeModelDownloader(
            initialExistingFiles = setOf("/a.gguf", "/b.gguf")
        )

        val files = downloader.getExistingFiles()

        assertEquals(2, files.size)
        assertTrue("/a.gguf" in files)
        assertTrue("/b.gguf" in files)
    }

    @Test
    fun `reset clears all state`() = runTest {
        val downloader = FakeModelDownloader()
        downloader.download("url", "/dest/file") { _, _ -> }
        downloader.deleteFile("/some/file")
        downloader.shouldDownloadFail = true
        downloader.downloadError = Exception("error")

        downloader.reset()

        assertFalse(downloader.downloadCalled)
        assertNull(downloader.lastDownloadUrl)
        assertNull(downloader.lastDownloadDestination)
        assertFalse(downloader.deleteFileCalled)
        assertNull(downloader.lastDeletedFile)
        assertFalse(downloader.shouldDownloadFail)
    }
}
