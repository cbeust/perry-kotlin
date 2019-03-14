package com.beust.perry

import com.google.inject.Inject
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Pattern

class Covers @Inject constructor(private val cyclesDao: CyclesDao) {

    fun findCoverFor2(number: Int): String? {
        val cycle = cyclesDao.cycleForBook(number)
        return "http://rhodan.stellarque.com/covers/pr_vo/$cycle/$number.jpg"
    }

    fun findCoverFor(number: Int): String? {
        val HOST = "https://perry-rhodan.net"
        val url1 = findLine("$HOST/shop/search?titel=$number", ".*\"(/shop.*perry-rhodan.*)\"")
        if (url1 != null) {
            return findLine(HOST + url1, ".*\"(http.*jpg)\".*")
        } else {
            return null
        }
    }

    private fun findLine(url: String, regexp: String): String? {
        val pattern = Pattern.compile(regexp)

        var foundUrl: String? = null
        InputStreamReader(URL(url).openConnection().getInputStream()).readLines().firstOrNull {
            val matcher = pattern.matcher(it)!!
            if (matcher.find()) {
                foundUrl = matcher.group(1)
                true
            } else {
                false
            }
        }

        return foundUrl
    }
}

fun main(args: Array<String>) {
    println(Covers(CyclesDaoExposed(BooksDaoExposed())).findCoverFor(412))
}
