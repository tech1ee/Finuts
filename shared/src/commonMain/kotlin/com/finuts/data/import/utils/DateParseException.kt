package com.finuts.data.import.utils

/**
 * Exception thrown when a date cannot be parsed from a string.
 */
class DateParseException(
    message: String,
    val input: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause) {

    companion object {
        fun empty(): DateParseException =
            DateParseException("Cannot parse empty string as date", "")

        fun invalidFormat(input: String): DateParseException =
            DateParseException("Cannot parse '$input' as a date", input)

        fun invalidDate(input: String, day: Int, month: Int, year: Int): DateParseException =
            DateParseException(
                "Invalid date: day=$day, month=$month, year=$year from '$input'",
                input
            )
    }
}
