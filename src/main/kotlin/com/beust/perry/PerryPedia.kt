package com.beust.perry

import org.jsoup.Jsoup

object PerryPedia {
    const val HOST = "https://www.perrypedia.de"

    private fun coverUrl(n: Int): String {
        val number = String.format("%04d", n)
        return  "$HOST/wiki/Datei:PR$number.jpg"
    }

    fun findCoverUrl(n: Int): String? {
        val number = String.format("%04d", n)
        val url = coverUrl(n)
        val line = Misc.findLine(url, ".*(/mediawiki.*/PR$number.jpg)\"")
        if (line != null) {
            val result = HOST + line
            return result
        } else {
            return null
        }
    }

    fun heftUrl(number: Int) = "$HOST/wiki/Quelle:PR$number"

    fun findSummary(n: Int): String? {
        val url = heftUrl(n)
        val doc = Jsoup.connect(url).get();
        val lines = arrayListOf<String>()
        doc.select("div.mw-parser-output > p").forEach {
            val text = it.text()
            if (! text.isNullOrBlank()) lines.add(text)
        }
        return if (lines.isNotEmpty()) lines.joinToString("\n<p>\n") else null
    }
}