package com.finuts.data.repository

import com.finuts.test.fakes.FakeModelDownloader
import com.finuts.test.fakes.FakePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ModelRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope
    private lateinit var fakeDownloader: FakeModelDownloader
    private lateinit var fakePreferences: FakePreferencesRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        fakeDownloader = FakeModelDownloader(modelsDirectory = "/test/models")
        fakePreferences = FakePreferencesRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== BUG #2 TESTS: File Existence Check ====================

    @Test
    fun `refreshInstalledModels returns empty when no model files exist`() = runTest {
        // Given: No files exist on disk
        val repository = createRepository()
        advanceUntilIdle()

        // When: Get installed models
        val installed = repository.installedModels.first()

        // Then: No models reported as installed
        assertTrue(installed.isEmpty(), "Expected no installed models when files don't exist")
    }

    @Test
    fun `refreshInstalledModels returns only models with existing files`() = runTest {
        // Given: Only compact model file exists
        fakeDownloader.addExistingFile("/test/models/compact.gguf")

        val repository = createRepository()
        advanceUntilIdle()

        // When: Get installed models
        val installed = repository.installedModels.first()

        // Then: Only compact model is reported as installed
        assertEquals(1, installed.size, "Expected exactly 1 installed model")
        assertEquals("compact", installed.first().config.id)
    }

    @Test
    fun `refreshInstalledModels returns all models when all files exist`() = runTest {
        // Given: All model files exist
        fakeDownloader.addExistingFile("/test/models/compact.gguf")
        fakeDownloader.addExistingFile("/test/models/standard.gguf")
        fakeDownloader.addExistingFile("/test/models/pro.gguf")

        val repository = createRepository()
        advanceUntilIdle()

        // When: Get installed models
        val installed = repository.installedModels.first()

        // Then: All 3 models are reported as installed
        assertEquals(3, installed.size, "Expected all 3 models to be installed")
    }

    // ==================== BUG #3 TESTS: selectModel Race Condition ====================

    @Test
    fun `selectModel updates currentModel immediately`() = runTest {
        // Given: Model is installed
        fakeDownloader.addExistingFile("/test/models/compact.gguf")
        val repository = createRepository()
        advanceUntilIdle()

        // When: Select the model
        val result = repository.selectModel("compact")
        advanceUntilIdle()

        // Then: Result is success and currentModel is updated
        assertTrue(result.isSuccess, "selectModel should succeed")
        val current = repository.currentModel.first()
        assertNotNull(current, "currentModel should not be null")
        assertEquals("compact", current.config.id)
        assertTrue(current.isSelected, "Model should be marked as selected")
    }

    @Test
    fun `selectModel updates preferences`() = runTest {
        // Given: Model is installed
        fakeDownloader.addExistingFile("/test/models/compact.gguf")
        val repository = createRepository()
        advanceUntilIdle()

        // When: Select the model
        repository.selectModel("compact")
        advanceUntilIdle()

        // Then: Preferences contain selected model ID
        val prefs = fakePreferences.currentValue()
        assertEquals("compact", prefs.selectedModelId)
    }

    @Test
    fun `selectModel updates installedModels list atomically`() = runTest {
        // Given: Multiple models installed
        fakeDownloader.addExistingFile("/test/models/compact.gguf")
        fakeDownloader.addExistingFile("/test/models/standard.gguf")
        val repository = createRepository()
        advanceUntilIdle()

        // When: Select one model
        repository.selectModel("standard")
        advanceUntilIdle()

        // Then: Only selected model is marked as selected
        val installed = repository.installedModels.first()
        val selectedModels = installed.filter { it.isSelected }
        assertEquals(1, selectedModels.size, "Only one model should be selected")
        assertEquals("standard", selectedModels.first().config.id)
    }

    @Test
    fun `selectModel fails for non-installed model`() = runTest {
        // Given: No models installed
        val repository = createRepository()
        advanceUntilIdle()

        // When: Try to select non-existent model
        val result = repository.selectModel("pro")

        // Then: Result is failure
        assertTrue(result.isFailure, "selectModel should fail for non-installed model")
    }

    // ==================== Download Tests ====================

    @Test
    fun `downloadModel creates file and succeeds`() = runTest {
        // Given: Repository with enough storage
        val repository = createRepository()
        advanceUntilIdle()

        // When: Download a model
        val result = repository.downloadModel("compact")
        advanceUntilIdle()

        // Then: Download succeeds
        assertTrue(result.isSuccess, "Download should succeed")
        assertTrue(fakeDownloader.downloadCalled, "Downloader should be called")
    }

    @Test
    fun `downloaded model appears in installedModels after refresh`() = runTest {
        // Given: Repository
        val repository = createRepository()
        advanceUntilIdle()

        // When: Download completes (adds file to fake)
        repository.downloadModel("compact")
        advanceUntilIdle()

        // Then: Model appears in installed list
        val installed = repository.installedModels.first()
        assertTrue(
            installed.any { it.config.id == "compact" },
            "Downloaded model should appear in installed list"
        )
    }

    // ==================== Model Selection Persistence Tests ====================

    @Test
    fun `currentModel reflects selected model from preferences on init`() = runTest {
        // Given: Preferences have selected model and file exists
        fakePreferences.setSelectedModelId("standard")
        fakeDownloader.addExistingFile("/test/models/standard.gguf")

        // When: Create repository (init triggers refresh)
        val repository = createRepository()
        advanceUntilIdle()

        // Then: currentModel is set from preferences
        val current = repository.currentModel.first()
        assertNotNull(current, "currentModel should be set from preferences")
        assertEquals("standard", current.config.id)
        assertTrue(current.isSelected)
    }

    @Test
    fun `currentModel is null when selected model file is missing`() = runTest {
        // Given: Preferences have selected model but file doesn't exist
        fakePreferences.setSelectedModelId("pro")
        // Note: Not adding pro.gguf file

        // When: Create repository
        val repository = createRepository()
        advanceUntilIdle()

        // Then: currentModel is null (file missing)
        val current = repository.currentModel.first()
        assertNull(current, "currentModel should be null when file is missing")
    }

    // ==================== Delete Tests ====================

    @Test
    fun `deleteModel removes model from installedModels`() = runTest {
        // Given: Model is installed
        fakeDownloader.addExistingFile("/test/models/compact.gguf")
        val repository = createRepository()
        advanceUntilIdle()

        // When: Delete the model
        repository.deleteModel("compact")
        advanceUntilIdle()

        // Then: Model is removed (file no longer exists in fake)
        assertTrue(fakeDownloader.deleteFileCalled)
        assertEquals("/test/models/compact.gguf", fakeDownloader.lastDeletedFile)
    }

    @Test
    fun `deleteModel clears currentModel if deleting selected model`() = runTest {
        // Given: Model is installed and selected
        fakeDownloader.addExistingFile("/test/models/compact.gguf")
        val repository = createRepository()
        advanceUntilIdle()
        repository.selectModel("compact")
        advanceUntilIdle()

        // When: Delete the selected model
        repository.deleteModel("compact")
        advanceUntilIdle()

        // Then: currentModel is cleared
        val current = repository.currentModel.first()
        assertNull(current, "currentModel should be null after deleting selected model")
    }

    // ==================== Helper Methods ====================

    private fun createRepository(): ModelRepositoryImpl {
        return ModelRepositoryImpl(
            downloader = fakeDownloader,
            preferencesRepository = fakePreferences,
            scope = testScope
        )
    }
}
