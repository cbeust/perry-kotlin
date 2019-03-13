package com.beust.perry

import com.google.inject.Inject
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern

class Covers @Inject constructor(private val cyclesDao: CyclesDao) {
    private val HOST = "https://perry-rhodan.net"

    fun _findCoverFor2(number: Int): String? {
        val cycle = cyclesDao.cycleForBook(number)
        val url = "http://rhodan.stellarque.com/covers/pr_vo/$cycle/$number.jpg"
        return url
    }

    fun findCoverFor(number: Int): String? {
        val url1 = findLine("$HOST/shop/search?titel=$number", ".*\"(/shop.*perry-rhodan.*)\"")
        if (url1 != null) {
            val url2 = findLine(HOST + url1, ".*\"(http.*jpg)\".*")
            return url2
        } else {
            return null
        }
    }

    private fun findLine(url: String, regexp: String): String? {
        val pattern = Pattern.compile(regexp)

        var foundUrl: String? = null
        var matcher: Matcher? = null
        InputStreamReader(URL(url).openConnection().getInputStream()).readLines().firstOrNull {
            matcher = pattern.matcher(it)!!
            if (matcher!!.find()) {
                foundUrl = matcher!!.group(1)
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
