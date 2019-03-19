package com.beust.perry

import com.github.mustachejava.DefaultMustacheFactory
import com.google.inject.Inject
import java.io.InputStreamReader
import java.io.StringWriter
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
        private val pendingDao: PendingDao, private val emailService: EmailService,
        private val vars: Vars) {
    private fun createCycle(it: CycleFromDao, summaryCount: Int)
        = Cycle(it.number, it.germanTitle, it.englishTitle, it.shortTitle, it.start, it.end,
                    summaryCount)

    fun findSummary(number: Int, username: String?): Summary? {
        val s = summariesDao.findEnglishSummary(number)
        if (s != null) {
            val cycleNumber = cyclesDao.cycleForBook(number)
            val cycle = cyclesDao.findCycle(cycleNumber)!!
            val book = booksDao.findBook(number)
            val result = Summary(s.number, cycleNumber, book?.germanTitle, s.englishTitle, book?.author,
                    s.authorName, s.authorEmail, s.date, s.text, s.time, username, cycle.germanTitle)
            return result
        } else {
            return null
        }
    }

    fun findPending(id: Int, fullName: String?): PendingSummaryFromDao? = pendingDao.findPending(id)

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

    fun saveSummary(summary: SummaryFromDao, germanTitle: String?, bookAuthor: String?) {
        //
        // See if we need to create a book first
        //
        val book = booksDao.findBook(summary.number)
            ?: BookFromDao(summary.number, germanTitle, summary.englishTitle, bookAuthor, null, null).apply {
                booksDao.saveBook(this)
            }

        summariesDao.saveSummary(summary)

        //
        // Update the book, if needed
        //
        if ((germanTitle != null && book.germanTitle != germanTitle) ||
                (bookAuthor != null && book.author != bookAuthor)) {
            booksDao.saveBook(book)
        }
    }


    private fun emailNewPendingSummary(pending: PendingSummaryFromDao, id: Int) {
        class Model(val pending: PendingSummaryFromDao, val id: Int, val oldText: String?, val host: String)
        val mf = DefaultMustacheFactory()
        val resource = EmailService::class.java.getResource("email-newPending.mustache")
        val mustache = mf.compile(InputStreamReader(resource.openStream()), "name")
        val content = StringWriter(10000)

        val oldSummary = summariesDao.findEnglishSummary(pending.number)
        mustache.execute(content, Model(pending, id, oldSummary?.text, vars.map[Vars.HOST]!!)).flush()
        val from = pending.authorName
        val number = pending.number
        emailService.sendEmail("cedric@beust.com", "New summary waiting for approval from $from: $number",
                content.toString())
    }

    fun saveSummaryInPending(s: PendingSummaryFromDao) {
        val id = pendingDao.saveSummary(s)
        emailNewPendingSummary(s, id)
    }

    fun saveSummaryFromPending(pending: PendingSummaryFromDao) {
        val bookDao = BookFromDao(pending.number, pending.germanTitle, pending.englishTitle, pending.bookAuthor,
                null, null)
        booksDao.saveBook(bookDao)
        val summary = SummaryFromDao(pending.number, pending.englishTitle, pending.authorName, pending.authorEmail,
                pending.dateSummary, pending.text, null)
        summariesDao.saveSummary(summary)
    }
}