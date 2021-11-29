package com.beust.perry

object PerryPedia {
    private const val HOST = "https://www.perrypedia.de"

    private fun coverUrl(n: Int): String {
        val number = String.format("%04d", n)
        return  "$HOST/wiki/Datei:PR$number.jpg"
    }

    fun findCoverUrl(n: Int): String? {
        val number = String.format("%04d", n)
        val url = coverUrl(n)
        val line = Misc.findLine(url, ".*(/mediawiki.*/PR$number.jpg)\"")
        return if (line != null) {
            val result = HOST + line
            result
        } else {
            null
        }
    }

    private const val TRANSLATED_HOST = "https://www-perrypedia-de.translate.goog"
    fun heftUrl(number: Int)
            = "$TRANSLATED_HOST/wiki/Quelle:PR$number?_x_tr_sl=auto&_x_tr_tl=en&_x_tr_hl=en&_x_tr_pto=nui"
}