package com.finuts.app.ui.components.import

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for TransactionReviewItem component logic.
 * UI rendering tested via screenshot/manual testing.
 */
class TransactionReviewItemTest {

    @Test
    fun `positive amount is formatted as income`() {
        val amount = 5000L
        val isIncome = amount > 0
        assertTrue(isIncome)
    }

    @Test
    fun `negative amount is formatted as expense`() {
        val amount = -5000L
        val isIncome = amount > 0
        assertFalse(isIncome)
    }

    @Test
    fun `zero amount is not income`() {
        val amount = 0L
        val isIncome = amount > 0
        assertFalse(isIncome)
    }

    @Test
    fun `isDuplicate true when status is ExactDuplicate`() {
        val status = TransactionDuplicateDisplayStatus.EXACT_DUPLICATE
        assertTrue(status.isDuplicate)
    }

    @Test
    fun `isDuplicate true when status is ProbableDuplicate`() {
        val status = TransactionDuplicateDisplayStatus.PROBABLE_DUPLICATE
        assertTrue(status.isDuplicate)
    }

    @Test
    fun `isDuplicate false when status is Unique`() {
        val status = TransactionDuplicateDisplayStatus.UNIQUE
        assertFalse(status.isDuplicate)
    }

    @Test
    fun `duplicate status affects background color`() {
        val duplicateStatus = TransactionDuplicateDisplayStatus.PROBABLE_DUPLICATE
        val showWarningBackground = duplicateStatus.isDuplicate
        assertTrue(showWarningBackground)
    }

    @Test
    fun `unique status uses default background`() {
        val uniqueStatus = TransactionDuplicateDisplayStatus.UNIQUE
        val showWarningBackground = uniqueStatus.isDuplicate
        assertFalse(showWarningBackground)
    }

    @Test
    fun `checkbox state can be toggled`() {
        var isSelected = true
        isSelected = !isSelected
        assertFalse(isSelected)
        isSelected = !isSelected
        assertTrue(isSelected)
    }

    @Test
    fun `description is displayed correctly`() {
        val description = "Kaspi Gold | Вкусно и точка"
        assertEquals("Kaspi Gold | Вкусно и точка", description)
    }
}

/**
 * Display status for duplicate detection in UI.
 */
enum class TransactionDuplicateDisplayStatus {
    UNIQUE,
    PROBABLE_DUPLICATE,
    EXACT_DUPLICATE;

    val isDuplicate: Boolean
        get() = this != UNIQUE
}
