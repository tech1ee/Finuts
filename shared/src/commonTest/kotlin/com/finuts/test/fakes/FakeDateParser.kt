package com.finuts.test.fakes

import com.finuts.data.import.utils.DateFormat
import com.finuts.data.import.utils.DateParserInterface
import kotlinx.datetime.LocalDate

/**
 * Fake implementation of DateParserInterface for testing.
 * Provides configurable responses and failure simulation.
 */
class FakeDateParser : DateParserInterface {

    private var parseResponses = mutableMapOf<String, LocalDate>()
    private var detectFormatResponses = mutableMapOf<String, DateFormat>()
    private var defaultFormat = DateFormat.ISO
    private var shouldFail = false
    private var failureException: Exception? = null

    override fun parse(text: String, format: DateFormat): LocalDate {
        if (shouldFail) {
            throw failureException ?: FakeDateParseException("Simulated parse failure")
        }

        return parseResponses[text.trim()]
            ?: parseResponses[text]
            ?: parseDefaultDate(text)
    }

    override fun parseOrNull(text: String, format: DateFormat): LocalDate? {
        return try {
            parse(text, format)
        } catch (_: Exception) {
            null
        }
    }

    override fun detectFormat(text: String): DateFormat {
        return detectFormatResponses[text.trim()] ?: defaultFormat
    }

    // Test helpers

    fun setParseResponse(input: String, result: LocalDate) {
        parseResponses[input] = result
    }

    fun setDetectFormatResponse(input: String, format: DateFormat) {
        detectFormatResponses[input] = format
    }

    fun setDefaultFormat(format: DateFormat) {
        defaultFormat = format
    }

    fun setFailure(exception: Exception? = null) {
        shouldFail = true
        failureException = exception
    }

    fun clearFailure() {
        shouldFail = false
        failureException = null
    }

    fun reset() {
        parseResponses.clear()
        detectFormatResponses.clear()
        defaultFormat = DateFormat.ISO
        shouldFail = false
        failureException = null
    }

    private fun parseDefaultDate(text: String): LocalDate {
        // Try simple ISO format
        val parts = text.trim().split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val day = parts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                return LocalDate(year, month, day)
            }
        }
        throw FakeDateParseException("Cannot parse date: $text")
    }
}

class FakeDateParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
