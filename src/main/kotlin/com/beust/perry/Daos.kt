package com.beust.perry

import org.joda.time.DateTime
import javax.ws.rs.core.Response

data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int, val books: List<Book>) {
    val href: String get() = "/cycles/$number"
    val hrefBack: String get() = "/"
}

interface CyclesDao {
    fun allCycles(): List<Cycle>
    fun findCycle(n: Int): Cycle?

    /**
     * @return the cycle this book belongs to.
     */
    fun cycleForBook(bookNumber: Int): Int
}

data class Book(val number: Int, val germanTitle: String, val englishTitle: String?, val author: String,
        val published: DateTime?, val germanFile: String?) {
    val href: String? get() = if (englishTitle != null) "/summaries/$number" else null

}

interface BooksDao {
    class BooksResponse(val books: List<Book>)

    fun findBooks(start: Int, end: Int): BooksResponse
    fun findBooksForCycle(cycle: Int): BooksResponse
}

data class Summary(val number: Int, val englishTitle: String, val authorName: String, val authorEmail: String,
        val date: String, val summary: String, val time: String?)

/** A summary with both English and German titles */
data class FullSummary(val number: Int, val cycleNumber: Int, val germanTitle: String, val englishTitle: String,
        val bookAuthor: String,
        val authorName: String, val authorEmail: String?,
        val date: String?, val summary: String, val time: String?,
        val username: String? = null, val germanCycleTitle: String) {
    private fun href(number: Int) =  "/summaries/$number"
    val hrefPrevious = href(number - 1)
    val hrefNext = href(number + 1)
    val hrefBackToCycle = href(cycleNumber)
}

interface SummariesDao {
    class SummariesResponse(val summaries: List<FullSummary>)

    fun findEnglishSummaries(start: Int, end: Int, user: User? = null): SummariesResponse
    fun findEnglishSummary(number: Int, user: User? = null)
            = findEnglishSummaries(number, number, user).summaries.firstOrNull()
    fun saveSummary(summary: FullSummary): Response
}
