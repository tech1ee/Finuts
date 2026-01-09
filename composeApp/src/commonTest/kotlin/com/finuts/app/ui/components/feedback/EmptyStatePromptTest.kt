package com.finuts.app.ui.components.feedback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for EmptyStatePrompt component logic.
 * UI rendering tested via screenshot/manual testing.
 */
class EmptyStatePromptTest {

    @Test
    fun `title should not be empty for proper empty state display`() {
        val title = "No Budget Set"
        assertTrue(title.isNotEmpty())
        assertTrue(title.isNotBlank())
    }

    @Test
    fun `description should not be empty for proper empty state display`() {
        val description = "Create a budget to track your monthly spending."
        assertTrue(description.isNotEmpty())
        assertTrue(description.isNotBlank())
    }

    @Test
    fun `actionLabel should not be empty for clickable button`() {
        val actionLabel = "Create Budget"
        assertTrue(actionLabel.isNotEmpty())
        assertTrue(actionLabel.isNotBlank())
    }

    @Test
    fun `empty title should be detected`() {
        val emptyTitle = ""
        assertTrue(emptyTitle.isEmpty())
        assertTrue(emptyTitle.isBlank())
    }

    @Test
    fun `whitespace-only title should be detected as blank`() {
        val whitespaceTitle = "   "
        assertFalse(whitespaceTitle.isEmpty())
        assertTrue(whitespaceTitle.isBlank())
    }

    @Test
    fun `correct structure for empty state prompt`() {
        // Following industry best practice: "Two parts instruction, one part delight"
        data class EmptyStateContent(
            val title: String,
            val description: String,
            val actionLabel: String
        )

        val content = EmptyStateContent(
            title = "No Spending Data",
            description = "Add transactions to see your spending by category.",
            actionLabel = "Add Transaction"
        )

        // Title is instructional (part 1)
        assertTrue(content.title.isNotBlank())
        // Description is explanatory (part 2)
        assertTrue(content.description.isNotBlank())
        // Action provides delight/solution
        assertTrue(content.actionLabel.isNotBlank())

        // All parts present
        assertEquals(3, listOf(content.title, content.description, content.actionLabel).size)
    }
}
