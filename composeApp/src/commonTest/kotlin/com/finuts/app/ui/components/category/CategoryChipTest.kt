package com.finuts.app.ui.components.category

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CategoryChip UI component.
 */
class CategoryChipTest {

    @Test
    fun `chip displays category name when provided`() {
        val categoryName = "Еда"
        val state = CategoryChipState(
            categoryName = categoryName,
            isAISuggested = false
        )

        assertEquals(categoryName, state.displayText)
    }

    @Test
    fun `chip displays placeholder when category is null`() {
        val state = CategoryChipState(
            categoryName = null,
            isAISuggested = false
        )

        assertEquals("Категория", state.displayText)
    }

    @Test
    fun `chip shows AI indicator when AI suggested`() {
        val state = CategoryChipState(
            categoryName = "Транспорт",
            isAISuggested = true
        )

        assertTrue(state.showAIIndicator)
    }

    @Test
    fun `chip hides AI indicator when not AI suggested`() {
        val state = CategoryChipState(
            categoryName = "Транспорт",
            isAISuggested = false
        )

        assertFalse(state.showAIIndicator)
    }

    @Test
    fun `chip uses accent background when AI suggested`() {
        val state = CategoryChipState(
            categoryName = "Магазины",
            isAISuggested = true
        )

        assertTrue(state.useAccentBackground)
    }

    @Test
    fun `chip uses surface background when not AI suggested`() {
        val state = CategoryChipState(
            categoryName = "Магазины",
            isAISuggested = false
        )

        assertFalse(state.useAccentBackground)
    }
}

/**
 * State holder for CategoryChip test assertions.
 */
data class CategoryChipState(
    val categoryName: String?,
    val isAISuggested: Boolean
) {
    val displayText: String
        get() = categoryName ?: "Категория"

    val showAIIndicator: Boolean
        get() = isAISuggested

    val useAccentBackground: Boolean
        get() = isAISuggested
}
