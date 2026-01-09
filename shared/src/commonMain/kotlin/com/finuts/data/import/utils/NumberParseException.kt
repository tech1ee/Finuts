package com.finuts.data.import.utils

/**
 * Exception thrown when a number cannot be parsed from a string.
 */
class NumberParseException(
    message: String,
    val input: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause) {

    companion object {
        fun empty(): NumberParseException =
            NumberParseException("Cannot parse empty string", "")

        fun invalidFormat(input: String): NumberParseException =
            NumberParseException("Cannot parse '$input' as a number", input)

        fun noDigits(input: String): NumberParseException =
            NumberParseException("No digits found in '$input'", input)
    }
}
