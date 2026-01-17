package com.finuts.test.fakes

import com.finuts.data.import.utils.NumberLocale
import com.finuts.data.import.utils.NumberParserInterface

/**
 * Fake implementation of NumberParserInterface for testing.
 * Provides configurable responses and failure simulation.
 */
class FakeNumberParser : NumberParserInterface {

    private var parseResponses = mutableMapOf<String, Long>()
    private var detectLocaleResponses = mutableMapOf<String, NumberLocale>()
    private var defaultLocale = NumberLocale.US
    private var shouldFail = false
    private var failureException: Exception? = null

    override fun parse(text: String, locale: NumberLocale): Long {
        if (shouldFail) {
            throw failureException ?: FakeNumberParseException("Simulated parse failure")
        }

        return parseResponses[text.trim()]
            ?: parseResponses[text]
            ?: parseDefaultNumber(text)
    }

    override fun detectLocale(text: String): NumberLocale {
        return detectLocaleResponses[text.trim()] ?: defaultLocale
    }

    // Test helpers

    fun setParseResponse(input: String, result: Long) {
        parseResponses[input] = result
    }

    fun setDetectLocaleResponse(input: String, locale: NumberLocale) {
        detectLocaleResponses[input] = locale
    }

    fun setDefaultLocale(locale: NumberLocale) {
        defaultLocale = locale
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
        detectLocaleResponses.clear()
        defaultLocale = NumberLocale.US
        shouldFail = false
        failureException = null
    }

    private fun parseDefaultNumber(text: String): Long {
        // Simple number parsing for testing
        val cleaned = text.trim()
            .replace(",", "")
            .replace(" ", "")

        // Check for decimal
        val dotIndex = cleaned.indexOf('.')
        return if (dotIndex >= 0) {
            val intPart = cleaned.substring(0, dotIndex).toLongOrNull() ?: 0L
            val decPart = cleaned.substring(dotIndex + 1).take(2).padEnd(2, '0')
            val decimal = decPart.toLongOrNull() ?: 0L
            intPart * 100 + decimal
        } else {
            (cleaned.toLongOrNull() ?: throw FakeNumberParseException("Cannot parse: $text")) * 100
        }
    }
}

class FakeNumberParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
