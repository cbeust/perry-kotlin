package com.beust.perry

import com.google.inject.Inject
import javax.ws.rs.WebApplicationException

data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int, val summaryCount: Int) {
    val percentage: Int get() = if (summaryCount == 0) 0 else summaryCount * 100 / (end - start + 1)
    val href: String get() = Urls.cycles(number)
    val hrefBack: String get() = "/"
}

/** A text with both English and German titles */
data class Summary(val number: Int, val cycleNumber: Int, val germanTitle: String?, val englishTitle: String?,
        val bookAuthor: String?,
        val authorName: String?, val authorEmail: String?,
        val date: String?, var text: String?, val time: String?,
        val username: String? = null, val germanCycleTitle: String) {
    private fun h(number: Int) =  Urls.summaries(number)
    val href = h(number)
    val hrefPrevious = h(number - 1)
    val hrefNext = h(number + 1)
    val hrefBackToCycle = Urls.cycles(cycleNumber)
}

/**
 * @return fully fledged objects gathered from combining multiple DAO calls.
 */
class PresentationLogic @Inject constructor(private val cyclesDao: CyclesDao,
        private val summariesDao: SummariesDao, private val booksDao: BooksDao,
        private val pendingDao: PendingDao) {
    private fun createCycle(it: CycleFromDao, summaryCount: Int)
        = Cycle(it.number, it.germanTitle, it.englishTitle, it.shortTitle, it.start, it.end,
                    summaryCount)

    fun findSummary(number: Int, username: String?): Summary? {
        val s = summariesDao.findEnglishSummary(number)
        if (s != null) {
            val cycleNumber = cyclesDao.cycleForBook(number)
            val cycle = cyclesDao.findCycle(cycleNumber)!!
            val book = booksDao.findBook(number)!!
            val result = Summary(s.number, cycleNumber, book.germanTitle, s.englishTitle, book.author,
                    s.authorName, s.authorEmail, s.date, s.text, s.time, username, cycle.germanTitle)
            return result
        } else {
            return null
        }
    }

    fun findPending(number: Int, fullName: String?): PendingSummaryFromDao? {
        val s = pendingDao.findPending(number)
        if (s != null) {
            val cycleNumber = cyclesDao.cycleForBook(number)
            val book = booksDao.findBook(number)!!
            val result = PendingSummaryFromDao(s.number, book.germanTitle, s.englishTitle,
                    s.authorName, s.authorEmail, s.text, s.dateSummary)
            return result
        } else {
            return null
        }
    }

    fun findSummaries(start: Int, end: Int, username: String?): List<Summary> {
        val result = (start..end).map { findSummary(it, username) }.filterNotNull()
        return result
    }

    fun findSummariesForCycle(cycleNumber: Int, username: String?): List<Summary> {
        val cycle = cyclesDao.findCycle(cycleNumber)!!
        return findSummaries(cycle.start, cycle.end, username)
    }

    fun findCycle(number: Int): Cycle? {
        val cycle = cyclesDao.findCycle(number)
        if (cycle != null) {
            return createCycle(cycle, summariesDao.findEnglishSummaries(cycle.start, cycle.end).size)
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
    }

    fun findAllCycles(): List<Cycle> {
        val result = arrayListOf<Cycle>()
        val cyclesDao = cyclesDao.allCycles()
        cyclesDao.forEach {
            val summaryCount = summariesDao.findEnglishSummaries(it.start, it.end).size
            result.add(createCycle(it, summaryCount))
        }
        return result
    }

    fun saveSummary(summary: SummaryFromDao, germanTitle: String?) {
        //
        // See if we need to create a book first
        //
        val b = booksDao.findBook(summary.number)
        if (b == null) {
            booksDao.saveBook(BookFromDao(summary.number, null, summary.englishTitle, null,
                    Dates.parseDate(summary.date), null))
        }

        summariesDao.saveSummary(summary)
        //
        // Update the book, if needed
        //
        val book = booksDao.findBooks(summary.number, summary.number).books.firstOrNull()
        if (germanTitle != null && book?.germanTitle != germanTitle) {
            booksDao.updateTitle(summary.number, germanTitle)
        }
    }

    fun saveSummaryInPending(s: PendingSummaryFromDao, germanTitle: String) {
        pendingDao.saveSummary(s)
    }

}