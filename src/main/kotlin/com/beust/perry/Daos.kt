package com.beust.perry

import org.joda.time.DateTime
import javax.ws.rs.WebApplicationException

data class CycleFromDao(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int)

interface CyclesDao {
    fun allCycles(): List<CycleFromDao>
    @Throws(WebApplicationException::class)
    fun findCycle(n: Int): CycleFromDao

    /**
     * @return the cycle this book belongs to.
     */
    fun cycleForBook(bookNumber: Int): Int?

    fun updateCycleName(cycleNumber: Int, cycleName: String)
}

data class BookFromDao(val number: Int, val germanTitle: String?, val englishTitle: String?, val author: String?,
        val published: DateTime?, val germanFile: String?) {
    val href: String? get() = if (englishTitle != null) Urls.summaries(number) else null

}

interface BooksDao {
    class BooksResponse(val books: List<BookFromDao>)

    fun findBooks(start: Int, end: Int): List<BookFromDao>
    fun findBook(number: Int): BookFromDao?
    fun findBooksForCycle(cycle: Int): List<BookFromDao>
    fun count(): Int
    fun saveBook(book: BookFromDao)
}

data class SummaryFromDao(val number: Int, val englishTitle: String, val authorName: String, val authorEmail: String?,
        val date: String?, val text: String, val time: String?)

data class ShortSummary(val number: Int, val englishTitle: String, val date: String)

interface SummariesDao {
    fun findEnglishSummaries(start: Int, end: Int, user: User? = null): List<SummaryFromDao>
    fun findEnglishSummary(number: Int, user: User? = null)
            = findEnglishSummaries(number, number, user).firstOrNull()
    fun findRecentSummaries(count: Int = 5): List<ShortSummary>
    fun count(): Int
    /** @return true if this summary is new */
    fun saveSummary(summary: SummaryFromDao): Boolean
}

interface UsersDao {
    fun findUser(login: String): User?
    fun updateAuthToken(login: String, authToken: String)
    fun findByAuthToken(authToken: String): User?
}

class PendingSummaryFromDao(val number: Int, val germanTitle: String?, val bookAuthor: String?,
        val englishTitle: String, val authorName: String, val authorEmail: String?,
        val text: String, val dateSummary: String)

interface PendingDao {
    fun findPending(id: Int): PendingSummaryFromDao?

    /** @return the id of the newly created row */
    fun saveSummary(summary: PendingSummaryFromDao): Int

    fun deletePending(id: Int)
}

interface CoversDao {
    val count: Int

    fun findCover(number: Int): ByteArray?
    fun save(number: Int, coverImageBytes: ByteArray)
}