package com.beust.perry

import com.beust.perry.Misc.findLine
//import com.google.cloud.translate.Translate
//import com.google.cloud.translate.TranslateOptions
import com.google.inject.Inject
import java.net.HttpURLConnection
import java.net.URL

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

    fun findCoverFor(number: Int) = PerryPedia.findCoverUrl(number)

    private fun findCoverForFast(number: Int): String? {
        val cycle = cyclesDao.cycleForBook(number)
        return validate("https://rhodan.stellarque.com/covers/pr_vo/$cycle/$number.jpg")
    }

    private fun findCoverSlow(number: Int): String? {
        val HOST = "https://perry-rhodan.net"
        val url1 = findLine("$HOST/shop/search?titel=$number", ".*\"(/shop.*perry-rhodan.*)\"")
        if (url1 != null) {
            return validate(findLine(HOST + url1, ".*\"(http.*jpg)\".*"))
        } else {
            return null
        }
    }

}

//fun main(args: Array<String>) {
//    val inj = Guice.createInjector(PerryModule(), DatabaseModule())
//    val covers = inj.getInstance(Covers::class.java)
//    val text = PerryPedia.findSummary(3008)
//    val translate = TranslateOptions.getDefaultInstance().service
//    val translated = translate.translate(text,
//            Translate.TranslateOption.sourceLanguage("ge"), Translate.TranslateOption.targetLanguage("en"))
//    println(translated)
//    coversDao.findSummary(3008)
//    val covers = arrayListOf<Pair<Int, Int>>()
//    transaction {
//        CoversTable.selectAll().forEach {
//            covers.add(Pair(it[CoversTable.number], it[CoversTable.image].size))
//        }
//    }
//    covers.forEach { (number, size) ->
//        transaction {
//            CoversTable.update({CoversTable.number eq number}) {
//                it[CoversTable.size] = size
//            }
//        }
//    }

//    println(covers)
//    fun measure(closure: () -> String?) {
//        val start = System.currentTimeMillis()
//        val result = closure()
//        println("$result Time: " + (System.currentTimeMillis() - start))
//    }
//
//    listOf(10, 90, 120, 400, 800, 1000, 1200, 1350, 1900, 2000, 2200, 2700, 3005).forEach {
//        measure { c.findCoverFor(it) }
//    }
//    c.findCoverFor(1234)

//    measure { c.findCoverFor(123) }
//    measure { c.findCoverFor(2000) }
//    measure { c.findCoverFor2(123) }
//    measure { c.findCoverFor2(2000) }
//}