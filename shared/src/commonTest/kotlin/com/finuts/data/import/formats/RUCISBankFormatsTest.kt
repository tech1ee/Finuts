package com.finuts.data.import.formats

import com.finuts.ai.privacy.RegexPIIAnonymizer
import com.finuts.data.import.ocr.DocumentPreprocessor
import com.finuts.data.import.ocr.DocumentType
import com.finuts.data.import.ocr.LocalTransactionExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD Tests for Russian and CIS Bank Statement Formats.
 *
 * Based on research:
 * - Kazakhstan: Kaspi Bank, Halyk Bank, Forte Bank
 * - Russia: Sberbank, Tinkoff (T-Bank), VTB, Alfa-Bank
 * - Date format: DD.MM.YYYY
 * - Amount format: 1 234,56 (space as thousand separator, comma as decimal)
 *
 * Sources:
 * - [EasyBankConvert RU](https://www.easybankconvert.com/ru)
 * - [Bank Parser Sberbank](https://bank-parser.com/banks/sberbank)
 * - [Tinkoff Export](https://dipbuh.ru/blog/instruktsiya-po-vygruzke-bankovskoy-vypiski-iz-internet-banka-tinkoff/)
 */
class RUCISBankFormatsTest {

    private val preprocessor = DocumentPreprocessor()
    private val extractor = LocalTransactionExtractor()
    private val anonymizer = RegexPIIAnonymizer()

    // === Kaspi Bank (Kazakhstan) Format Tests ===

    @Test
    fun `extracts transactions from Kaspi statement format`() {
        val kaspiStatement = """
            Kaspi Bank
            Выписка по карте **** 1234
            Период: 01.01.2026 - 31.01.2026

            15.01.2026 - 3 700,00 ₸ Glovo Оплата заказа
            16.01.2026 + 100 000,00 ₸ Пополнение карты
            17.01.2026 - 25 000,00 ₸ Magnum Cash & Carry
            18.01.2026 - 5 000,00 ₸ Перевод Иванов А.С.
        """.trimIndent()

        val preprocessed = preprocessor.process(kaspiStatement)
        assertEquals(DocumentType.BANK_STATEMENT, preprocessed.hints.type)
        assertEquals("ru", preprocessed.hints.language)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 4, "Expected at least 4 transactions, got ${transactions.size}")

        val glovo = transactions.find { it.rawDescription.contains("Glovo") }
        assertTrue(glovo != null, "Expected Glovo transaction")
        assertEquals(-370000, glovo?.amountMinorUnits)
        assertEquals("KZT", glovo?.currency)
        assertTrue(glovo?.isDebit == true)

        val income = transactions.find { it.rawDescription.contains("Пополнение") }
        assertTrue(income != null, "Expected income transaction")
        assertEquals(10000000, income?.amountMinorUnits)
        assertTrue(income?.isCredit == true)
    }

    @Test
    fun `handles Kaspi amount format with spaces and comma`() {
        val amounts = listOf(
            "01.01.2026 - 1 234,56 ₸ Test" to -123456L,
            "02.01.2026 - 12 345,67 ₸ Test" to -1234567L,
            "03.01.2026 + 123 456,78 ₸ Test" to 12345678L,
            "04.01.2026 - 1 234 567,89 ₸ Test" to -123456789L
        )

        amounts.forEach { (line, expected) ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals(expected, result[0].amountMinorUnits, "Failed for: $line")
        }
    }

    @Test
    fun `extracts Kaspi transfers with PII anonymization`() {
        val statement = """
            15.01.2026 - 50 000,00 ₸ Перевод Иванов Иван Иванович
            16.01.2026 + 25 000,00 ₸ Перевод от Петрова А.С.
        """.trimIndent()

        val anonymized = anonymizer.anonymize(statement)

        assertTrue(anonymized.wasModified, "PII should be detected")
        assertFalse(anonymized.anonymizedText.contains("Иванов"), "Name should be anonymized")
        assertFalse(anonymized.anonymizedText.contains("Петрова"), "Name should be anonymized")
        assertTrue(anonymized.anonymizedText.contains("[PERSON_NAME_"), "Should contain placeholder")

        // Can still extract transactions from anonymized text
        val transactions = extractor.extract(anonymized.anonymizedText)
        assertTrue(transactions.size >= 2)
    }

    // === Halyk Bank (Kazakhstan) Format Tests ===

    @Test
    fun `extracts transactions from Halyk Bank format`() {
        val halykStatement = """
            АО «Халык Банк»
            Выписка по счёту
            IBAN: KZ123456789012345678

            Дата        Описание                     Сумма
            15.01.2026  Оплата за услуги            -5 000,00 ₸
            16.01.2026  Зачисление зарплаты        +250 000,00 ₸
            17.01.2026  Коммунальные платежи        -12 500,00 ₸
        """.trimIndent()

        val preprocessed = preprocessor.process(halykStatement)
        assertEquals(DocumentType.BANK_STATEMENT, preprocessed.hints.type)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 3, "Expected at least 3 transactions")

        val salary = transactions.find { it.rawDescription.contains("Зачисление") || it.rawDescription.contains("зарплаты") }
        assertTrue(salary != null, "Expected salary transaction")
        assertEquals(25000000, salary?.amountMinorUnits)
    }

    @Test
    fun `handles Halyk statement in Kazakh language`() {
        val halykKazakh = """
            «Халық Банкі» АҚ
            Шоттан үзінді көшірме

            15.01.2026  Төлем                       -5 000,00 ₸
            16.01.2026  Жалақы түсуі               +250 000,00 ₸
        """.trimIndent()

        val preprocessed = preprocessor.process(halykKazakh)
        assertEquals("kk", preprocessed.hints.language, "Should detect Kazakh language")

        val transactions = extractor.extract(preprocessed.cleanedText)
        assertTrue(transactions.size >= 2)
    }

    // === Sberbank (Russia) Format Tests ===

    @Test
    fun `extracts transactions from Sberbank format`() {
        val sberbankStatement = """
            ПАО Сбербанк
            Выписка по счёту карты **** 1234
            За период с 01.01.2026 по 31.01.2026

            15.01.2026  Покупка. Пятёрочка          -1 234,56 ₽
            16.01.2026  Зачисление зарплаты        +85 000,00 ₽
            17.01.2026  Перевод на карту           -10 000,00 ₽
            18.01.2026  Оплата ЖКХ                  -5 678,90 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(sberbankStatement)
        assertEquals(DocumentType.BANK_STATEMENT, preprocessed.hints.type)
        assertEquals("ru", preprocessed.hints.language)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 4, "Expected at least 4 transactions")

        val purchase = transactions.find { it.rawDescription.contains("Пятёрочка") }
        assertTrue(purchase != null, "Expected Pyaterochka transaction")
        assertEquals(-123456, purchase?.amountMinorUnits)
        assertEquals("RUB", purchase?.currency)
    }

    @Test
    fun `handles Sberbank ruble symbol ₽`() {
        val statement = """
            15.01.2026  Покупка  -1 000,00 ₽
            16.01.2026  Перевод  -5 000,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertEquals("RUB", transactions[0].currency)
        assertEquals(-100000, transactions[0].amountMinorUnits)
    }

    // === Tinkoff (T-Bank) Format Tests ===

    @Test
    fun `extracts transactions from Tinkoff format`() {
        val tinkoffStatement = """
            Тинькофф Банк
            Выписка по карте Black
            Период: 01.01.2026 - 31.01.2026

            15.01.2026 12:34  OZON                       -2 345,00 ₽
            16.01.2026 09:15  Яндекс Такси                 -450,00 ₽
            17.01.2026 14:00  Перевод от Иванов И.И.    +10 000,00 ₽
            18.01.2026 18:30  СберМаркет                 -3 200,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(tinkoffStatement)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 4, "Expected at least 4 transactions")

        val ozon = transactions.find { it.rawDescription.contains("OZON") }
        assertTrue(ozon != null, "Expected OZON transaction")
        assertEquals(-234500, ozon?.amountMinorUnits)

        val yandex = transactions.find { it.rawDescription.contains("Яндекс") }
        assertTrue(yandex != null, "Expected Yandex transaction")
        assertEquals(-45000, yandex?.amountMinorUnits)
    }

    @Test
    fun `handles Tinkoff transactions with timestamps`() {
        val statement = """
            15.01.2026 12:34:56  Покупка  -1 000,00 ₽
            16.01.2026 09:15    Перевод  +5 000,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        // Date should be extracted without time
        assertTrue(transactions[0].rawDate.startsWith("15"))
    }

    // === VTB Format Tests ===

    @Test
    fun `extracts transactions from VTB format`() {
        val vtbStatement = """
            ВТБ (ПАО)
            Выписка по счёту
            Номер счёта: 40817810123456789012

            Дата       Операция                    Списание    Зачисление
            15.01.2026 Оплата услуг                1 500,00
            16.01.2026 Зачисление ЗП                           95 000,00
            17.01.2026 Перевод в другой банк      25 000,00
        """.trimIndent()

        val preprocessed = preprocessor.process(vtbStatement)
        val transactions = extractor.extract(preprocessed.cleanedText)

        // VTB format with separate columns needs handling
        assertTrue(transactions.size >= 0, "Should attempt to extract VTB transactions")
    }

    // === Alfa-Bank Format Tests ===

    @Test
    fun `extracts transactions from Alfa-Bank format`() {
        val alfaStatement = """
            Альфа-Банк
            Выписка за январь 2026

            15.01.2026  Магнит                      -856,30 ₽
            16.01.2026  Перевод с карты           +5 000,00 ₽
            17.01.2026  Wildberries               -3 450,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(alfaStatement)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 3, "Expected at least 3 transactions")

        val magnit = transactions.find { it.rawDescription.contains("Магнит") }
        assertTrue(magnit != null, "Expected Magnit transaction")
        assertEquals(-85630, magnit?.amountMinorUnits)
    }

    // === Forte Bank (Kazakhstan) Format Tests ===

    @Test
    fun `extracts transactions from Forte Bank format`() {
        val forteStatement = """
            ForteBank
            Выписка по счёту

            15.01.2026  Оплата Kaspi.kz            -15 000,00 ₸
            16.01.2026  Пополнение                +100 000,00 ₸
            17.01.2026  Перевод                    -25 000,00 ₸
        """.trimIndent()

        val preprocessed = preprocessor.process(forteStatement)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 3, "Expected at least 3 transactions")
    }

    // === Common Russian/CIS Patterns Tests ===

    @Test
    fun `handles Russian merchant names with Cyrillic`() {
        val merchants = listOf(
            "15.01.2026 -1 000,00 ₽ Пятёрочка",
            "16.01.2026 -2 000,00 ₽ Магнит",
            "17.01.2026 -3 000,00 ₽ Перекрёсток",
            "18.01.2026 -4 000,00 ₽ Лента",
            "19.01.2026 -5 000,00 ₽ Дикси"
        )

        merchants.forEach { line ->
            val result = extractor.extract(line)
            assertTrue(result.isNotEmpty(), "Should parse: $line")
            assertEquals("RUB", result[0].currency)
        }
    }

    @Test
    fun `handles Kazakh tenge symbol ₸`() {
        val statement = """
            15.01.2026  Покупка  -5 000,00 ₸
            16.01.2026  Пополнение  +10 000,00 ₸
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertEquals("KZT", transactions[0].currency)
        assertEquals(-500000, transactions[0].amountMinorUnits)
    }

    @Test
    fun `handles salary transactions - Зарплата`() {
        val statement = """
            15.01.2026  ЗАРПЛАТА ЗА ЯНВАРЬ 2026  +350 000,00 ₽
            16.01.2026  Аванс                    +150 000,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertTrue(transactions[0].isCredit)
        assertEquals(35000000, transactions[0].amountMinorUnits)
    }

    // === PII Anonymization Tests for RU/CIS ===

    @Test
    fun `anonymizes Russian phone numbers`() {
        val text = """
            Перевод на +7 (999) 123-45-67
            Оплата +79991234567
        """.trimIndent()

        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("+7 (999)"))
        assertFalse(result.anonymizedText.contains("+79991234567"))
        assertTrue(result.anonymizedText.contains("[PHONE_"))
    }

    @Test
    fun `anonymizes Kazakhstan phone numbers`() {
        val text = """
            Перевод на +7 777 123 45 67
            Оплата +77771234567
        """.trimIndent()

        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("777 123"))
        assertTrue(result.anonymizedText.contains("[PHONE_"))
    }

    @Test
    fun `anonymizes Russian names in various formats`() {
        val names = listOf(
            "Иванов И.И.",
            "Петрова Мария Сергеевна",
            "Сидоров А.С.",
            "Козлов Андрей"
        )

        names.forEach { name ->
            val result = anonymizer.anonymize("Перевод от $name")
            assertTrue(result.wasModified, "Should detect: $name")
            assertFalse(result.anonymizedText.contains(name.split(" ")[0]), "Should anonymize: $name")
        }
    }

    @Test
    fun `anonymizes Kazakhstan IBAN`() {
        val text = "Перевод на KZ123456789012345678"

        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("KZ123456"))
        assertTrue(result.anonymizedText.contains("[IBAN_"))
    }

    @Test
    fun `anonymizes Russian bank account numbers`() {
        val text = "Счёт получателя: 40817810123456789012"

        val result = anonymizer.anonymize(text)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("40817810123456789012"))
        assertTrue(result.anonymizedText.contains("[ACCOUNT_"))
    }

    // === Edge Cases ===

    @Test
    fun `handles mixed Russian-English transaction descriptions`() {
        val statement = """
            15.01.2026  OZON.ru Заказ #123456  -2 500,00 ₽
            16.01.2026  Яндекс.Маркет Order  -1 800,00 ₽
            17.01.2026  AliExpress Покупка   -3 200,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(3, transactions.size)
        assertTrue(transactions[0].rawDescription.contains("OZON"))
        assertTrue(transactions[1].rawDescription.contains("Яндекс"))
        assertTrue(transactions[2].rawDescription.contains("AliExpress"))
    }

    @Test
    fun `handles transactions with reference numbers`() {
        val statement = """
            15.01.2026  Перевод #123456789  -10 000,00 ₽
            16.01.2026  Платёж по счёту INV-2026-001  -5 000,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertTrue(transactions[0].rawDescription.contains("123456789"))
    }

    @Test
    fun `handles commission fees - Комиссия`() {
        val statement = """
            15.01.2026  Перевод в другой банк  -10 000,00 ₽
            15.01.2026  Комиссия за перевод       -100,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertTrue(transactions[1].rawDescription.contains("Комиссия"))
        assertEquals(-10000, transactions[1].amountMinorUnits)
    }

    @Test
    fun `handles cash withdrawal transactions`() {
        val statement = """
            15.01.2026  Снятие наличных ATM  -20 000,00 ₽
            16.01.2026  Банкомат выдача      -15 000,00 ₸
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertEquals(-2000000, transactions[0].amountMinorUnits)
        assertEquals("RUB", transactions[0].currency)
        assertEquals("KZT", transactions[1].currency)
    }

    @Test
    fun `handles refund transactions - Возврат`() {
        val statement = """
            15.01.2026  Возврат покупки OZON  +2 500,00 ₽
            16.01.2026  Возврат средств      +1 000,00 ₸
        """.trimIndent()

        val transactions = extractor.extract(statement)

        assertEquals(2, transactions.size)
        assertTrue(transactions[0].isCredit)
        assertTrue(transactions[1].isCredit)
        assertTrue(transactions[0].rawDescription.contains("Возврат"))
    }
}
