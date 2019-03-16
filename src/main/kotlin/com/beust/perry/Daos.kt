package com.beust.perry

import org.joda.time.DateTime
import javax.ws.rs.core.Response

data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int) {
    val href: String get() = Urls.cycles(number)
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
    val href: String? get() = if (englishTitle != null) Urls.summaries(number) else null

}

interface BooksDao {
    class BooksResponse(val books: List<Book>)

    fun findBooks(start: Int, end: Int): BooksResponse
    fun findBook(number: Int) = findBooks(number, number).books.firstOrNull()
    fun findBooksForCycle(cycle: Int): BooksResponse
}

data class Summary(val number: Int, val englishTitle: String, val authorName: String, val authorEmail: String,
        val date: String, val summary: String, val time: String?)

/** A text with both English and German titles */
data class FullSummary(val number: Int, val cycleNumber: Int, val germanTitle: String, val englishTitle: String,
        val bookAuthor: String,
        val authorName: String, val authorEmail: String?,
        val date: String?, var text: String, val time: String?,
        val username: String? = null, val germanCycleTitle: String) {
    private fun h(number: Int) =  Urls.summaries(number)
    val href = h(number)
    val hrefPrevious = h(number - 1)
    val hrefNext = h(number + 1)
    val hrefBackToCycle = Urls.cycles(cycleNumber)
}

data class ShortSummary(val number: Int, val englishTitle: String, val date: String)

interface SummariesDao {
    fun findEnglishSummaries(start: Int, end: Int, user: User? = null): List<FullSummary>
    fun findEnglishSummary(number: Int, user: User? = null)
            = findEnglishSummaries(number, number, user).firstOrNull()
    fun findRecentSummaries(): List<ShortSummary>
    fun saveSummary(summary: FullSummary): Response
}

interface UsersDao {
    fun findUser(loginName: String): User?
}
