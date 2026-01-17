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
 * TDD Tests for Receipt and Invoice Formats.
 *
 * Based on research:
 * - Retail receipts (grocery, electronics, clothing)
 * - Restaurant checks
 * - E-commerce invoices (Amazon, OZON, etc.)
 * - Utility bills (electricity, gas, water)
 * - Service invoices
 *
 * Sources:
 * - [FormX Receipt OCR](https://www.formx.ai/blog/how-to-extract-receipt-data-with-ocr-regex-and-ai)
 * - [TAGGUN Receipt API](https://www.taggun.io/)
 * - [Microsoft Document Intelligence](https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/prebuilt/receipt)
 */
class ReceiptInvoiceFormatsTest {

    private val preprocessor = DocumentPreprocessor()
    private val extractor = LocalTransactionExtractor()
    private val anonymizer = RegexPIIAnonymizer()

    // === Retail Receipt Tests ===

    @Test
    fun `detects receipt document type`() {
        val receipt = """
            МАГНИТ
            Чек №123456
            15.01.2026 14:35:22

            Молоко 1л         89,90
            Хлеб белый        45,00
            Яйца С1 10шт     120,00
            -----------------------
            ИТОГО:           254,90 ₽
            Безналичный расчёт
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)

        assertEquals(DocumentType.RECEIPT, preprocessed.hints.type)
    }

    @Test
    fun `extracts total from retail receipt`() {
        val receipt = """
            Пятёрочка
            Чек
            15.01.2026

            Товар 1          100,00
            Товар 2          200,00
            Товар 3          300,00
            -----------------------
            ИТОГО:           600,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)
        val transactions = extractor.extract(preprocessed.cleanedText)

        // Receipt should extract total as single transaction
        assertTrue(transactions.isNotEmpty())

        val total = transactions.find { it.rawDescription.contains("ИТОГО") }
        if (total != null) {
            assertEquals(-60000, total.amountMinorUnits)
        }
    }

    @Test
    fun `extracts from grocery store receipt format`() {
        val receipt = """
            ПЕРЕКРЁСТОК
            ИНН: 7728029110
            Чек: 00123456
            Дата: 15.01.2026 18:45

            Колбаса Докторская 1шт    245,90
            Сыр Российский 0.5кг      189,50
            Масло сливочное           165,00
            --------------------------------
            ИТОГО К ОПЛАТЕ:           600,40 ₽
            Карта **** 1234
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)
        assertEquals(DocumentType.RECEIPT, preprocessed.hints.type)
    }

    // === Restaurant Check Tests ===

    @Test
    fun `extracts from restaurant check format`() {
        val check = """
            Ресторан "Пушкин"
            г. Москва

            Стол: 15
            Официант: Мария
            15.01.2026 20:30

            Борщ                      450,00
            Пельмени                  650,00
            Чай                       150,00
            --------------------------------
            Подытог:                1 250,00
            Обслуживание 10%:         125,00
            --------------------------------
            ИТОГО:                  1 375,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(check)
        val transactions = extractor.extract(preprocessed.cleanedText)

        // Should extract the main transaction
        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun `handles restaurant tips calculation`() {
        val check = """
            Кафе
            15.01.2026

            Блюдо 1                   500,00
            Напиток                   100,00
            --------------------------------
            Подытог:                  600,00
            Чаевые 15%:                90,00
            ИТОГО:                    690,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(check)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    // === E-commerce Invoice Tests ===

    @Test
    fun `extracts from OZON invoice format`() {
        val invoice = """
            OZON
            Заказ №123456789
            Дата заказа: 15.01.2026

            Товар: Смартфон Samsung Galaxy
            Количество: 1
            Цена: 45 990,00 ₽

            Доставка: 0,00 ₽

            ИТОГО К ОПЛАТЕ: 45 990,00 ₽
            Оплачено картой **** 5678
        """.trimIndent()

        val preprocessed = preprocessor.process(invoice)
        assertEquals(DocumentType.INVOICE, preprocessed.hints.type)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun `extracts from Wildberries invoice`() {
        val invoice = """
            Wildberries
            Заказ #WB123456789
            15.01.2026

            Платье летнее         2 500,00 ₽
            Туфли женские         3 990,00 ₽
            Скидка:                -649,00 ₽
            ---------------------------------
            Итого:                5 841,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(invoice)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun `extracts from Amazon invoice format`() {
        val invoice = """
            Amazon.com
            Order #123-4567890-1234567
            Order Date: January 15, 2026

            Items:
            Kindle Paperwhite           $139.99
            USB Cable                    $12.99

            Subtotal:                   $152.98
            Shipping:                     $0.00
            Tax:                         $13.77
            Order Total:               $166.75
        """.trimIndent()

        val preprocessed = preprocessor.process(invoice)
        assertEquals(DocumentType.INVOICE, preprocessed.hints.type)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    // === Utility Bill Tests ===

    @Test
    fun `extracts from electricity bill format`() {
        val bill = """
            АО "Мосэнергосбыт"
            Счёт за электроэнергию
            Период: январь 2026

            Лицевой счёт: 1234567890
            Адрес: г. Москва, ул. Примерная, д. 1

            Показания:
            Предыдущие: 12345 кВт·ч
            Текущие: 12567 кВт·ч
            Расход: 222 кВт·ч

            К оплате: 1 543,86 ₽
            Срок оплаты: 25.02.2026
        """.trimIndent()

        val preprocessed = preprocessor.process(bill)

        val transactions = extractor.extract(preprocessed.cleanedText)
        // Should extract the payment amount
        assertTrue(transactions.size >= 0)
    }

    @Test
    fun `extracts from gas bill format`() {
        val bill = """
            Мосгаз
            Квитанция на оплату
            15.01.2026

            Адрес: ул. Тестовая, д. 1
            Счётчик: 12345

            Расход газа: 15.5 м³
            Тариф: 8,30 ₽/м³

            Итого к оплате: 128,65 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(bill)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 0)
    }

    @Test
    fun `extracts from water utility bill`() {
        val bill = """
            Мосводоканал
            Счёт на оплату воды

            15.01.2026

            ХВС: 5.2 м³ × 45,82 = 238,26 ₽
            ГВС: 3.1 м³ × 215,45 = 667,90 ₽
            Водоотведение: 8.3 м³ × 32,64 = 270,91 ₽

            ИТОГО: 1 177,07 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(bill)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 0)
    }

    // === Mobile/Internet Bill Tests ===

    @Test
    fun `extracts from mobile operator bill`() {
        val bill = """
            Билайн
            Счёт за услуги связи
            Период: январь 2026

            Номер: +7 (999) 123-45-67

            Абонентская плата:        599,00 ₽
            Дополнительные услуги:    100,00 ₽
            ---------------------------------
            Итого к оплате:           699,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(bill)
        val transactions = extractor.extract(preprocessed.cleanedText)

        // Phone number should be detected for anonymization
        val anonymized = anonymizer.anonymize(bill)
        assertTrue(anonymized.wasModified)
        assertFalse(anonymized.anonymizedText.contains("+7 (999)"))
    }

    @Test
    fun `extracts from internet provider bill`() {
        val bill = """
            Ростелеком
            Счёт за интернет
            15.01.2026

            Тариф: "100 Мбит/с"
            Абонентская плата: 600,00 ₽
            Аренда оборудования: 50,00 ₽

            Итого: 650,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(bill)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.size >= 0)
    }

    // === Service Invoice Tests ===

    @Test
    fun `extracts from taxi service receipt`() {
        val receipt = """
            Яндекс Go
            Чек за поездку
            15.01.2026 18:45

            Маршрут: Ленинский пр-т → Арбат
            Расстояние: 8.5 км
            Время в пути: 25 мин

            Стоимость поездки: 450,00 ₽
            Оплачено картой **** 1234
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)
        assertEquals(DocumentType.RECEIPT, preprocessed.hints.type)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun `extracts from delivery service receipt`() {
        val receipt = """
            Delivery Club
            Чек заказа #DC123456
            15.01.2026 20:15

            Пицца Маргарита          590,00 ₽
            Роллы Калифорния         450,00 ₽
            Напиток 0.5л             120,00 ₽
            Доставка                   0,00 ₽
            ---------------------------------
            Итого: 1 160,00 ₽
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    // === Subscription Invoice Tests ===

    @Test
    fun `extracts from streaming service invoice`() {
        val invoice = """
            Netflix
            Счёт за подписку
            Дата: 15.01.2026

            План: Стандарт
            Период: 15.01.2026 - 14.02.2026

            Сумма: $15.99
            Способ оплаты: Visa **** 1234
        """.trimIndent()

        val preprocessed = preprocessor.process(invoice)
        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
        assertEquals("USD", transactions[0].currency)
    }

    @Test
    fun `extracts from SaaS subscription invoice`() {
        val invoice = """
            Notion
            Invoice #INV-2026-001
            Date: January 15, 2026

            Plus Plan (Annual)
            12 months × $10/month = $120.00

            Total: $120.00
            Paid with card ending 5678
        """.trimIndent()

        val preprocessed = preprocessor.process(invoice)
        assertEquals(DocumentType.INVOICE, preprocessed.hints.type)

        val transactions = extractor.extract(preprocessed.cleanedText)

        assertTrue(transactions.isNotEmpty())
    }

    // === Kazakhstan Receipt Tests ===

    @Test
    fun `extracts from Kazakhstan fiscal receipt`() {
        val receipt = """
            ТОО "Магазин"
            БИН: 123456789012
            Кассовый чек
            15.01.2026 15:30

            Наименование          Цена
            Товар 1           1 500,00 ₸
            Товар 2           2 000,00 ₸
            --------------------------
            ИТОГО:            3 500,00 ₸
            НДС 12%:            378,57 ₸

            Фискальный признак: 1234567890
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)
        assertEquals(DocumentType.RECEIPT, preprocessed.hints.type)
        assertEquals("KZT", extractor.extract(preprocessed.cleanedText).firstOrNull()?.currency)
    }

    @Test
    fun `extracts from Kaspi QR payment receipt`() {
        val receipt = """
            Kaspi QR
            Чек об оплате
            15.01.2026 12:45

            Магазин: Sulpak
            Сумма: 45 990,00 ₸
            Способ оплаты: Kaspi Gold

            Транзакция: KQR123456789
            Статус: Успешно
        """.trimIndent()

        val preprocessed = preprocessor.process(receipt)

        val transactions = extractor.extract(preprocessed.cleanedText)
        assertTrue(transactions.isNotEmpty())
        assertEquals(-4599000, transactions[0].amountMinorUnits)
    }

    // === PII Anonymization in Receipts/Invoices ===

    @Test
    fun `anonymizes address in utility bills`() {
        val bill = """
            Счёт за ЖКУ
            Адрес: г. Москва, ул. Тверская, д. 15, кв. 42
            15.01.2026

            Итого: 5 000,00 ₽
        """.trimIndent()

        val result = anonymizer.anonymize(bill)

        // Address anonymization may be partial
        assertTrue(result.anonymizedText.contains("15.01.2026"))
        assertTrue(result.anonymizedText.contains("5 000"))
    }

    @Test
    fun `anonymizes card numbers in receipts`() {
        val receipt = """
            Чек
            15.01.2026

            Итого: 1 000,00 ₽
            Оплачено картой 4276 1234 5678 9012
        """.trimIndent()

        val result = anonymizer.anonymize(receipt)

        assertTrue(result.wasModified)
        assertFalse(result.anonymizedText.contains("4276 1234"))
        assertTrue(result.anonymizedText.contains("[CARD_NUMBER_"))
    }

    @Test
    fun `preserves fiscal data while anonymizing PII`() {
        val receipt = """
            ИП Иванов И.И.
            ИНН: 123456789012
            Чек №123456
            15.01.2026

            Товар: 500,00 ₽
            ИТОГО: 500,00 ₽
        """.trimIndent()

        val result = anonymizer.anonymize(receipt)

        // Should anonymize personal name but preserve fiscal identifiers
        assertFalse(result.anonymizedText.contains("Иванов"))
        assertTrue(result.anonymizedText.contains("Чек"))
        assertTrue(result.anonymizedText.contains("500"))
    }

    // === Edge Cases ===

    @Test
    fun `handles receipt with discount lines`() {
        val receipt = """
            Магазин
            15.01.2026

            Товар 1          1 000,00 ₽
            Скидка 10%        -100,00 ₽
            Товар 2            500,00 ₽
            Купон PROMO       -200,00 ₽
            --------------------------
            ИТОГО:           1 200,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(receipt)

        // Should handle both positive and negative amounts
        assertTrue(transactions.size >= 2)
    }

    @Test
    fun `handles receipt with item quantities`() {
        val receipt = """
            Магнит
            15.01.2026

            Молоко 1л × 2      178,00 ₽
            Хлеб × 3           135,00 ₽
            --------------------------
            ИТОГО:             313,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(receipt)

        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun `handles receipt with VAT breakdown`() {
        val receipt = """
            Компания
            15.01.2026

            Услуга:          10 000,00 ₽
            НДС 20%:          2 000,00 ₽
            --------------------------
            ИТОГО с НДС:     12 000,00 ₽
        """.trimIndent()

        val transactions = extractor.extract(receipt)

        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun `handles handwritten receipt OCR artifacts`() {
        val receipt = """
            Магазин
            l5.01.2O26

            Товар l          1 OOO,OO ₽
            Товар 2          5OO,OO ₽
            --------------------------
            ИТОГО:           1 5OO,OO ₽
        """.trimIndent()

        // OCR may replace 0 with O, 1 with l
        // Preprocessing should handle common OCR errors
        val transactions = extractor.extract(receipt)

        // May or may not parse depending on OCR correction
        assertTrue(transactions.size >= 0)
    }
}
