package com.beust.perry

import com.google.inject.Guice
import com.google.inject.Inject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class Covers @Inject constructor(private val cyclesDao: CyclesDao) {

    private fun isValid(url: String) : Boolean {
        val u = URL(url)
        (u.openConnection() as HttpURLConnection).let { huc ->
            huc.requestMethod = "GET"  //OR  huc.setRequestMethod ("HEAD");
            huc.connect()
            val code = huc.responseCode
            return code == 200
        }
    }

    private fun validate(url: String?) = if (url != null && isValid(url)) url else null

    fun findCoverFor2(number: Int): String? {
        val cycle = cyclesDao.cycleForBook(number)
        return validate("http://rhodan.stellarque.com/covers/pr_vo/$cycle/$number.jpg")
    }

    fun findCoverFor(number: Int): String? {
        val HOST = "https://perry-rhodan.net"
        val url1 = findLine("$HOST/shop/search?titel=$number", ".*\"(/shop.*perry-rhodan.*)\"")
        if (url1 != null) {
            return validate(findLine(HOST + url1, ".*\"(http.*jpg)\".*"))
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
    val inj = Guice.createInjector(PerryModule())
    val c = inj.getInstance(Covers::class.java)

    fun measure(closure: () -> String?) {
        val start = System.currentTimeMillis()
        val result = closure()
        println("$result Time: " + (System.currentTimeMillis() - start))
    }

    measure { c.findCoverFor(123) }
    measure { c.findCoverFor(2000) }
    measure { c.findCoverFor2(123) }
    measure { c.findCoverFor2(2000) }
}