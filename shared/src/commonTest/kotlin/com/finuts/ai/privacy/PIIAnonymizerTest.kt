package com.finuts.ai.privacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PIIAnonymizerTest {

    private val anonymizer = RegexPIIAnonymizer()

    // === IBAN Detection ===

    @Test
    fun `anonymizes Kazakhstan IBAN`() {
        val text = "Transfer to KZABC1234567890123456"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("KZABC1234567890123456"))
        assertTrue(result.anonymizedText.contains("[IBAN_"))
        assertEquals(1, result.detectedPII.count { it.type == PIIType.IBAN })
    }

    @Test
    fun `anonymizes Russian IBAN`() {
        val text = "Account: RU12345678901234567890123456789012"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertTrue(result.anonymizedText.contains("[IBAN_"))
    }

    // === Phone Detection ===

    @Test
    fun `anonymizes Kazakhstan phone number`() {
        val text = "Call me at +7 777 123 45 67"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("777"))
        assertTrue(result.anonymizedText.contains("[PHONE_"))
        assertEquals(1, result.detectedPII.count { it.type == PIIType.PHONE })
    }

    @Test
    fun `anonymizes Russian phone number`() {
        val text = "Phone: 8(495)123-45-67"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertTrue(result.anonymizedText.contains("[PHONE_"))
    }

    @Test
    fun `anonymizes simple KZ mobile format`() {
        val text = "Transfer to 87071234567"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("87071234567"))
    }

    // === Email Detection ===

    @Test
    fun `anonymizes email address`() {
        val text = "Contact: user.name@example.com for details"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("user.name@example.com"))
        assertTrue(result.anonymizedText.contains("[EMAIL_"))
        assertEquals(1, result.detectedPII.count { it.type == PIIType.EMAIL })
    }

    // === Card Number Detection ===

    @Test
    fun `anonymizes card number with spaces`() {
        val text = "Card: 4111 1111 1111 1111"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("4111"))
        assertTrue(result.anonymizedText.contains("[CARD_NUMBER_"))
    }

    @Test
    fun `anonymizes card number with dashes`() {
        val text = "Payment from 4111-1111-1111-1111"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertTrue(result.anonymizedText.contains("[CARD_NUMBER_"))
    }

    // === Person Name Detection (Cyrillic) ===

    @Test
    fun `anonymizes Russian name with initials`() {
        val text = "Transfer to Иванов А.С."
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("Иванов"))
        assertTrue(result.anonymizedText.contains("[PERSON_NAME_"))
    }

    @Test
    fun `anonymizes full Russian name with patronymic`() {
        val text = "From Петров Иван Сергеевич"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("Петров"))
    }

    // === Person Name Detection (Latin) ===

    @Test
    fun `anonymizes Latin name First Last`() {
        val text = "Transfer to John Smith"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("John Smith"))
        assertTrue(result.anonymizedText.contains("[PERSON_NAME_"))
        assertEquals(1, result.detectedPII.count { it.type == PIIType.PERSON_NAME })
    }

    @Test
    fun `anonymizes Latin name with initial J Smith`() {
        val text = "Payment from J. Smith"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("J. Smith"))
        assertTrue(result.anonymizedText.contains("[PERSON_NAME_"))
    }

    @Test
    fun `anonymizes Latin name Last comma First`() {
        val text = "Recipient: Smith, John"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("Smith, John"))
    }

    // === IIN Detection ===

    @Test
    fun `anonymizes Kazakhstan IIN`() {
        val text = "IIN: 900101123456"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("900101123456"))
        assertTrue(result.anonymizedText.contains("[IIN_"))
    }

    // === Preservation Tests ===

    @Test
    fun `preserves dates`() {
        val text = "Transaction on 15.01.2026"
        val result = anonymizer.anonymize(text)

        assertFalse(result.wasModified)
        assertTrue(result.anonymizedText.contains("15.01.2026"))
    }

    @Test
    fun `preserves amounts`() {
        val text = "Amount: 12500.50 KZT"
        val result = anonymizer.anonymize(text)

        assertTrue(result.anonymizedText.contains("12500.50"))
    }

    @Test
    fun `preserves merchant names`() {
        val text = "Payment to Kaspi Bank"
        val result = anonymizer.anonymize(text)

        assertTrue(result.anonymizedText.contains("Kaspi Bank"))
    }

    // === De-anonymization Tests ===

    @Test
    fun `deanonymizes correctly`() {
        val original = "Transfer to Иванов А.С. phone +7 777 123 45 67"
        val anonymized = anonymizer.anonymize(original)

        val restored = anonymizer.deanonymize(anonymized.anonymizedText, anonymized.mapping)

        assertEquals(original, restored)
    }

    @Test
    fun `deanonymizes multiple PIIs`() {
        val original = "From Петров И.С. to Сидоров А.Б., card 4111 1111 1111 1111"
        val anonymized = anonymizer.anonymize(original)

        assertTrue(anonymized.piiCount >= 2)
        val restored = anonymizer.deanonymize(anonymized.anonymizedText, anonymized.mapping)

        // All PIIs should be restored
        assertTrue(restored.contains("Петров") || restored.contains("Сидоров"))
    }

    // === Edge Cases ===

    @Test
    fun `handles empty text`() {
        val result = anonymizer.anonymize("")

        assertFalse(result.wasModified)
        assertEquals("", result.anonymizedText)
        assertEquals(0, result.piiCount)
    }

    @Test
    fun `handles text without PII`() {
        val text = "Grocery shopping at Magnum supermarket"
        val result = anonymizer.anonymize(text)

        assertFalse(result.wasModified)
        assertEquals(text, result.anonymizedText)
    }

    @Test
    fun `detectPII returns same as anonymize`() {
        val text = "Email: test@example.com, Phone: +7 777 123 45 67"
        val anonymized = anonymizer.anonymize(text)
        val detected = anonymizer.detectPII(text)

        assertEquals(anonymized.piiCount, detected.size)
    }

    // === Real Bank Statement Lines ===

    @Test
    fun `anonymizes Kaspi bank transfer line`() {
        val text = "07.01.26 - 3 700,00 ₸ Перевод Shukhrat S."
        val result = anonymizer.anonymize(text)

        // Date and amount should be preserved
        assertTrue(result.anonymizedText.contains("07.01.26"))
        assertTrue(result.anonymizedText.contains("3 700,00"))
        assertTrue(result.anonymizedText.contains("₸"))
        assertTrue(result.anonymizedText.contains("Перевод"))
    }

    @Test
    fun `anonymizes line with IBAN and name`() {
        val text = "Transfer to KZ123456789012345678 Иванов А.С. 50000 KZT"
        val result = anonymizer.anonymize(text)

        // PII should be anonymized
        assertFalse(result.anonymizedText.contains("KZ123456789012345678"))
        assertFalse(result.anonymizedText.contains("Иванов"))

        // Amount should be preserved
        assertTrue(result.anonymizedText.contains("50000"))
    }

    // === Data Class Tests ===

    @Test
    fun `AnonymizationResult piiCount returns correct count`() {
        val detectedPII = listOf(
            DetectedPII(PIIType.EMAIL, "test@test.com", "[EMAIL_1]", 0, 12),
            DetectedPII(PIIType.PHONE, "+71234567890", "[PHONE_1]", 20, 32)
        )
        val result = AnonymizationResult(
            anonymizedText = "Contact [EMAIL_1] or [PHONE_1]",
            mapping = mapOf("[EMAIL_1]" to "test@test.com", "[PHONE_1]" to "+71234567890"),
            detectedPII = detectedPII,
            wasModified = true
        )

        assertEquals(2, result.piiCount)
    }

    @Test
    fun `AnonymizationResult with empty mapping`() {
        val result = AnonymizationResult(
            anonymizedText = "No PII here",
            mapping = emptyMap(),
            detectedPII = emptyList(),
            wasModified = false
        )

        assertEquals(0, result.piiCount)
        assertFalse(result.wasModified)
    }

    @Test
    fun `DetectedPII stores all fields correctly`() {
        val pii = DetectedPII(
            type = PIIType.CARD_NUMBER,
            original = "4111111111111111",
            placeholder = "[CARD_NUMBER_1]",
            startIndex = 5,
            endIndex = 20
        )

        assertEquals(PIIType.CARD_NUMBER, pii.type)
        assertEquals("4111111111111111", pii.original)
        assertEquals("[CARD_NUMBER_1]", pii.placeholder)
        assertEquals(5, pii.startIndex)
        assertEquals(20, pii.endIndex)
    }

    @Test
    fun `PIIType enum has all expected values`() {
        val allTypes = PIIType.entries

        assertTrue(allTypes.contains(PIIType.PERSON_NAME))
        assertTrue(allTypes.contains(PIIType.IBAN))
        assertTrue(allTypes.contains(PIIType.ACCOUNT_NUMBER))
        assertTrue(allTypes.contains(PIIType.CARD_NUMBER))
        assertTrue(allTypes.contains(PIIType.PHONE))
        assertTrue(allTypes.contains(PIIType.EMAIL))
        assertTrue(allTypes.contains(PIIType.ADDRESS))
        assertTrue(allTypes.contains(PIIType.SSN))
        assertTrue(allTypes.contains(PIIType.PASSPORT))
        assertTrue(allTypes.contains(PIIType.IIN))
        assertEquals(10, allTypes.size)
    }

    // === More Edge Cases ===

    @Test
    fun `anonymizes account number in isolation`() {
        val text = "Account: 12345678901234567890"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertTrue(result.anonymizedText.contains("[ACCOUNT_NUMBER_") ||
            result.anonymizedText.contains("[IIN_"))
    }

    @Test
    fun `handles multiple same type PIIs`() {
        val text = "Contact: user1@example.com and user2@example.com"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertTrue(result.piiCount >= 2)
        assertTrue(result.anonymizedText.contains("[EMAIL_1]"))
        assertTrue(result.anonymizedText.contains("[EMAIL_2]"))
    }

    @Test
    fun `deanonymize with empty mapping returns original text`() {
        val text = "No placeholders here"
        val result = anonymizer.deanonymize(text, emptyMap())

        assertEquals(text, result)
    }

    @Test
    fun `preserves text without any patterns`() {
        val text = "Simple transaction at store ABC"
        val result = anonymizer.anonymize(text)

        assertFalse(result.wasModified)
        assertEquals(text, result.anonymizedText)
        assertTrue(result.mapping.isEmpty())
    }

    @Test
    fun `anonymizes feminine patronymic correctly`() {
        val text = "Payment from Иванова Мария Петровна"
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("Иванова"))
        assertTrue(result.anonymizedText.contains("[PERSON_NAME_"))
    }

    @Test
    fun `handles mixed Latin and Cyrillic text`() {
        val text = "Transfer from John Smith to Иванов А.С."
        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("John Smith"))
        assertFalse(result.anonymizedText.contains("Иванов"))
    }
}
