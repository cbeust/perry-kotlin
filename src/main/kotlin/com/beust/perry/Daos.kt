package com.beust.perry

import org.joda.time.DateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DaoResult<T>(val success: Boolean, val result: T? = null, val message: String? = null)

data class CycleFromDao(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int)

interface CyclesDao {
    fun allCycles(): List<CycleFromDao>
    fun findCycle(n: Int): DaoResult<CycleFromDao>

    /**
     * @return the cycle this book belongs to.
     */
    fun cycleForBook(bookNumber: Int): Int?

    fun updateCycleName(cycleNumber: Int, englishCycleName: String)
    fun addCycle(number: Int, germanTitle: String, englishTitle: String, shortTitle: String, start: Int, end: Int)
}

data class BookFromDao(val number: Int, val germanTitle: String?, val englishTitle: String?, val author: String?,
        val published: DateTime?, val germanFile: String?) {
    val href: String? get() = if (englishTitle != null) Urls.summaries(number) else null

}

interface BooksDao {
    fun findBooks(start: Int, end: Int): List<BookFromDao>
    fun findBook(number: Int): BookFromDao?
    fun findBooksForCycle(cycle: Int): List<BookFromDao>
    fun count(): Int
    fun saveBook(book: BookFromDao)
}

data class SummaryFromDao(val number: Int, val englishTitle: String, val authorName: String, val authorEmail: String?,
        val date: String?, val text: String, val time: String?)

data class ShortSummaryDao(val number: Int, val englishTitle: String, val date: String, val coverUrl: String?) {
    private val FORMATTER = DateTimeFormatter.ofPattern("u-MM-d k:m")
    val prettyDate: String
        get() {
            return try {
                Dates.formatDateWords(LocalDate.parse(date, FORMATTER))
            } catch(ex: Exception) {
                date
            }
        }
}

interface SummariesDao {
    fun findEnglishSummaries(start: Int, end: Int, user: User? = null): List<SummaryFromDao>
    fun findEnglishTitles(start: Int, end: Int): Map<Int, String>
    fun findEnglishSummary(number: Int, user: User? = null)
            = findEnglishSummaries(number, number, user).firstOrNull()
    fun findRecentSummaries(count: Int = 5): List<ShortSummaryDao>
    val count: Int
    /** @return true if this summary is new */
    fun saveSummary(summary: SummaryFromDao): Boolean
}

interface UsersDao {
    fun findUser(login: String): DaoResult<User>
    fun createUser(user: User): Boolean
    fun updateAuthToken(login: String, authToken: String): DaoResult<Unit>
    fun findByAuthToken(authToken: String): User?
    fun setPassword(login: String, password: String): DaoResult<Unit>
    fun verifyAccount(tempLink: String): DaoResult<Unit>
    fun updateLastLogin(login: String)
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