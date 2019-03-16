package com.beust.perry

import org.joda.time.DateTime
import javax.ws.rs.core.Response

data class CycleFromDao(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int)

interface CyclesDao {
    fun allCycles(): List<CycleFromDao>
    fun findCycle(n: Int): CycleFromDao?

    /**
     * @return the cycle this book belongs to.
     */
    fun cycleForBook(bookNumber: Int): Int
}

data class BookFromDao(val number: Int, val germanTitle: String, val englishTitle: String?, val author: String,
        val published: DateTime?, val germanFile: String?) {
    val href: String? get() = if (englishTitle != null) Urls.summaries(number) else null

}

interface BooksDao {
    class BooksResponse(val books: List<BookFromDao>)

    fun findBooks(start: Int, end: Int): BooksResponse
    fun findBook(number: Int) = findBooks(number, number).books.firstOrNull()
    fun findBooksForCycle(cycle: Int): BooksResponse
    fun updateTitle(number: Int, newTitle: String)
    fun count(): Int
}

data class SummaryFromDao(val number: Int, val englishTitle: String, val authorName: String, val authorEmail: String?,
        val date: String?, val text: String, val time: String?)

data class ShortSummary(val number: Int, val englishTitle: String, val date: String)

interface SummariesDao {
    fun findEnglishSummaries(start: Int, end: Int, user: User? = null): List<SummaryFromDao>
    fun findEnglishSummary(number: Int, user: User? = null)
            = findEnglishSummaries(number, number, user).firstOrNull()
    fun findRecentSummaries(): List<ShortSummary>
    fun count(): Int
    fun saveSummary(summary: SummaryFromDao): Response
}

interface UsersDao {
    fun findUser(loginName: String): User?
}
