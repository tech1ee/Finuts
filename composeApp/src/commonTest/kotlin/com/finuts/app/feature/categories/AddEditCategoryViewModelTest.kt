package com.finuts.app.feature.categories

import app.cash.turbine.test
import com.finuts.app.test.TestData
import com.finuts.app.test.fakes.FakeCategoryRepository
import com.finuts.domain.entity.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for AddEditCategoryViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditCategoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var categoryRepository: FakeCategoryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = FakeCategoryRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        categoryId: String? = null,
        defaultType: CategoryType = CategoryType.EXPENSE
    ) = AddEditCategoryViewModel(categoryRepository, categoryId, defaultType)

    // === Initial State (Create Mode) ===

    @Test
    fun `isEditMode is false when categoryId is null`() = runTest {
        val viewModel = createViewModel(categoryId = null)
        assertFalse(viewModel.isEditMode)
    }

    @Test
    fun `formState has default values in create mode`() = runTest {
        val viewModel = createViewModel(categoryId = null)

        viewModel.formState.test {
            val state = awaitItem()
            assertNull(state.id)
            assertEquals("", state.name)
            assertEquals("package", state.icon)
            assertEquals("#4CAF50", state.color)
            assertEquals(CategoryType.EXPENSE, state.type)
            assertFalse(state.isDefault)
            assertNull(state.nameError)
            assertFalse(state.isSaving)
        }
    }

    @Test
    fun `formState uses defaultType from constructor`() = runTest {
        val viewModel = createViewModel(categoryId = null, defaultType = CategoryType.INCOME)

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals(CategoryType.INCOME, state.type)
        }
    }

    // === Initial State (Edit Mode) ===

    @Test
    fun `isEditMode is true when categoryId is provided`() = runTest {
        val category = TestData.category(id = "cat-1", name = "Food")
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        assertTrue(viewModel.isEditMode)
    }

    @Test
    fun `formState loads category data in edit mode`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Food",
            icon = "🍔",
            color = "#22C55E",
            type = CategoryType.EXPENSE,
            isDefault = true
        )
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals("cat-1", state.id)
            assertEquals("Food", state.name)
            assertEquals("🍔", state.icon)
            assertEquals("#22C55E", state.color)
            assertEquals(CategoryType.EXPENSE, state.type)
            assertTrue(state.isDefault)
        }
    }

    // === Form Updates ===

    @Test
    fun `updateName updates formState name`() = runTest {
        val viewModel = createViewModel()

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.updateName("New Name")

            val state = awaitItem()
            assertEquals("New Name", state.name)
        }
    }

    @Test
    fun `updateName clears nameError`() = runTest {
        val viewModel = createViewModel()

        // First, trigger an error
        viewModel.save()
        advanceUntilIdle()

        viewModel.formState.test {
            var state = awaitItem()
            assertNotNull(state.nameError)

            viewModel.updateName("Valid Name")
            state = awaitItem()
            assertNull(state.nameError)
        }
    }

    @Test
    fun `updateIcon updates formState icon`() = runTest {
        val viewModel = createViewModel()

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.updateIcon("🎮")

            val state = awaitItem()
            assertEquals("🎮", state.icon)
        }
    }

    @Test
    fun `updateColor updates formState color`() = runTest {
        val viewModel = createViewModel()

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.updateColor("#3B82F6")

            val state = awaitItem()
            assertEquals("#3B82F6", state.color)
        }
    }

    @Test
    fun `updateType updates formState type in create mode`() = runTest {
        val viewModel = createViewModel(categoryId = null, defaultType = CategoryType.EXPENSE)

        viewModel.formState.test {
            var state = awaitItem()
            assertEquals(CategoryType.EXPENSE, state.type)

            viewModel.updateType(CategoryType.INCOME)
            state = awaitItem()
            assertEquals(CategoryType.INCOME, state.type)
        }
    }

    @Test
    fun `updateType is ignored in edit mode`() = runTest {
        val category = TestData.category(id = "cat-1", type = CategoryType.EXPENSE)
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.formState.test {
            val state = awaitItem()
            assertEquals(CategoryType.EXPENSE, state.type)

            viewModel.updateType(CategoryType.INCOME)
            // No new emission, type stays EXPENSE
            expectNoEvents()
        }
    }

    // === Validation ===

    @Test
    fun `save shows error for empty name`() = runTest {
        val viewModel = createViewModel()

        viewModel.formState.test {
            awaitItem() // Initial state

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Name is required", state.nameError)
        }
    }

    @Test
    fun `save shows error for name over 50 chars`() = runTest {
        val viewModel = createViewModel()
        val longName = "A".repeat(51)

        viewModel.updateName(longName)
        advanceUntilIdle()

        viewModel.formState.test {
            awaitItem() // Current state

            viewModel.save()
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Name must be 50 characters or less", state.nameError)
        }
    }

    // === Save Operations ===

    @Test
    fun `save creates new category in create mode`() = runTest {
        val viewModel = createViewModel(categoryId = null)

        viewModel.updateName("Gaming")
        viewModel.updateIcon("🎮")
        viewModel.updateColor("#8B5CF6")
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        // Check that category was created in repository
        val categories = categoryRepository.getAllCategories()
        categories.test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Gaming", list.first().name)
            assertEquals("🎮", list.first().icon)
            assertEquals("#8B5CF6", list.first().color)
        }
    }

    @Test
    fun `save updates existing category in edit mode`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Food",
            icon = "🍔"
        )
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.updateName("Fast Food")
        viewModel.updateIcon("🍟")
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        // Check that category was updated
        val categories = categoryRepository.getAllCategories()
        categories.test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Fast Food", list.first().name)
            assertEquals("🍟", list.first().icon)
        }
    }

    @Test
    fun `save emits SaveSuccess event on success`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateName("Test Category")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.save()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<CategoryFormEvent.SaveSuccess>(event)
        }
    }

    @Test
    fun `save sets isSaving to true then false`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateName("Test")
        advanceUntilIdle()

        viewModel.formState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isSaving)

            viewModel.save()

            // isSaving becomes true
            val savingState = awaitItem()
            assertTrue(savingState.isSaving)

            advanceUntilIdle()

            // isSaving becomes false after operation completes
            val finalState = awaitItem()
            assertFalse(finalState.isSaving)
        }
    }

    // === Delete Operations ===

    @Test
    fun `delete removes category from repository`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Gaming",
            isDefault = false
        )
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()

        val categories = categoryRepository.getAllCategories()
        categories.test {
            val list = awaitItem()
            assertTrue(list.isEmpty())
        }
    }

    @Test
    fun `delete emits DeleteSuccess event on success`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Gaming",
            isDefault = false
        )
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.delete()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<CategoryFormEvent.DeleteSuccess>(event)
        }
    }

    @Test
    fun `delete emits CannotDeleteDefault for default category`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Food",
            isDefault = true
        )
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.delete()
            advanceUntilIdle()

            val event = awaitItem()
            assertIs<CategoryFormEvent.CannotDeleteDefault>(event)
        }
    }

    @Test
    fun `delete does not remove default category`() = runTest {
        val category = TestData.category(
            id = "cat-1",
            name = "Food",
            isDefault = true
        )
        categoryRepository.setCategories(listOf(category))

        val viewModel = createViewModel(categoryId = "cat-1")
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()

        // Category should still exist
        val categories = categoryRepository.getAllCategories()
        categories.test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Food", list.first().name)
        }
    }
}
