package com.beust.perry

import java.time.LocalDate

object Misc {
    fun publicationDate(number: Int) : LocalDate {
        return LocalDate.of(1961, 9, 7).plusWeeks(number.toLong() - 1)
    }
}