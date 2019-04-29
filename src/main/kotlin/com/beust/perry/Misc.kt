package com.beust.perry

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.time.LocalDate
import java.util.regex.Pattern

object Misc {
    fun publicationDate(number: Int) : LocalDate {
        return LocalDate.of(1961, 9, 7).plusWeeks(number.toLong() - 1)
    }

    fun findLine(url: String, regexp: String): String? {
        return try {
            findLine(URL(url).openConnection().getInputStream(), regexp)
        } catch(ex: Exception) {
            null
        }
    }

    fun findLine(ins: InputStream, regexp: String): String? {
        val pattern = Pattern.compile(regexp)

        var foundUrl: String? = null
        try {
            InputStreamReader(ins).readLines().firstOrNull {
                val matcher = pattern.matcher(it)!!
                if (matcher.find()) {
                    foundUrl = matcher.group(1)
                    true
                } else {
                    false
                }
            }
            return foundUrl
        } catch(ex: IOException) {
            return null
        }
    }
}