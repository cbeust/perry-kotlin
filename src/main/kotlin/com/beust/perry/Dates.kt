package com.beust.perry

import org.joda.time.DateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Dates {
    fun formatDate(ld: LocalDate): String {
        return ld.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
    }

    fun formatDate(ld: LocalDateTime): String {
        return ld.format(DateTimeFormatter.ofPattern("YYYY-MM-dd hh:mm"))
    }

    fun formatDateWords(ld: LocalDate): String {
        return ld.format(DateTimeFormatter.ofPattern("MMMM dd, YYYY"))
    }

    fun formatTime(ld: LocalDateTime): String {
        return ld.format(DateTimeFormatter.ofPattern("hh:mm"))
    }

    fun parseDate(date: String?): DateTime? {
        return if (date != null) DateTime.parse(date) else null
    }
}