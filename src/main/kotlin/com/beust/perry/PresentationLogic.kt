package com.beust.perry

import com.github.mustachejava.DefaultMustacheFactory
import com.google.inject.Guice
import com.google.inject.Inject
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.*
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


@Suppress("unused")
data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int, val summaryCount: Int) {
    val percentage: Int get() = if (summaryCount == 0) 0 else summaryCount * 100 / (end - start + 1)
    val href: String get() = Urls.cycles(number)
    val hrefBack: String get() = "/"
}

/** A text with both English and German titles */
@Suppress("unused")
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
        private val typedProperties: TypedProperties, private val perryContext: PerryContext,
        private val covers: Covers, private val coversDao: CoversDao, private val usersDao: UsersDao)
{
    private val log = LoggerFactory.getLogger(PresentationLogic::class.java)

    private fun createCycle(it: CycleFromDao, summaryCount: Int)
        = Cycle(it.number, it.germanTitle, it.englishTitle, it.shortTitle, it.start, it.end,
                    summaryCount)

    fun findSummary(number: Int, username: String?): Summary? {
        val cycleNumber = cyclesDao.cycleForBook(number)
        val result =
            if (cycleNumber != null) {
                val cycle = cyclesDao.findCycle(cycleNumber)
                val book = booksDao.findBook(number)
                if (book != null) {
                    val s = summariesDao.findEnglishSummary(number)
                    if (s != null) {
                        Summary(s.number, cycleNumber, book.germanTitle, s.englishTitle, book.author,
                                s.authorName, s.authorEmail, s.date, s.text, s.time, username, cycle.germanTitle)
                    } else {
                        // No summary found, provide the minimum amount of information we can from the book
                        // and cycle.
                        Summary(book.number, cycle.number, book.germanTitle, null, book.author, null, null,
                            Dates.formatDate(LocalDate.now()), "No summary found", null, username, cycle.germanTitle)
                    }
                } else {
                    null
                }
            } else {
                null
            }
        return result
    }

    fun findPending(id: Int): PendingSummaryFromDao? = pendingDao.findPending(id)

    fun findSummaries(start: Int, end: Int, username: String?): List<Summary>
        = (start..end).mapNotNull { findSummary(it, username) }

    fun findCycle(number: Int): Cycle {
        val cycle = cyclesDao.findCycle(number)
        return createCycle(cyclesDao.findCycle(number), summariesDao.findEnglishSummaries(cycle.start, cycle.end).size)
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

    fun saveSummary(summary: SummaryFromDao, germanTitle: String?, bookAuthor: String?): Boolean {
        //
        // See if we need to create a book first
        //
        val book = booksDao.findBook(summary.number)
            ?: BookFromDao(summary.number, germanTitle, summary.englishTitle, bookAuthor, null, null).apply {
                booksDao.saveBook(this)
            }

        val result = summariesDao.saveSummary(summary)

        //
        // Update the book, if needed
        //
        if ((germanTitle != null && book.germanTitle != germanTitle) ||
                (bookAuthor != null && book.author != bookAuthor)) {
            val newBook = BookFromDao(book.number, germanTitle ?: book.germanTitle, book.englishTitle,
                    bookAuthor ?: book.author, book.published, book.germanFile)
            booksDao.saveBook(newBook)
        }

        return result
    }


    private fun emailNewPendingSummary(pending: PendingSummaryFromDao, id: Int) {
        @Suppress("unused")
        class Model(val pending: PendingSummaryFromDao, val id: Int, val oldText: String?, val host: String)
        val mf = DefaultMustacheFactory()
        val resource = EmailService::class.java.getResource("email-newPending.mustache")
        val mustache = mf.compile(InputStreamReader(resource.openStream()), "name")
        val content = StringWriter(10000)

        val oldSummary = summariesDao.findEnglishSummary(pending.number)
        mustache.execute(content, Model(pending, id, oldSummary?.text,
                typedProperties.getRequired(LocalProperty.HOST))).flush()
        val from = pending.authorName
        val number = pending.number
        emailService.notifyAdmin("New summary waiting for approval from $from: $number", content.toString())
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

    fun createSummary(number: Int, context: PerryContext): Any {
        val summary = findSummary(number, perryContext.user?.fullName)
        if (summary != null) {
            return Response.seeOther(URI(Urls.SUMMARIES + "/$number/edit")).build()
        } else {
            val book = booksDao.findBook(number)
            val (germanTitle, bookAuthor) =
                    if (book != null) {
                        Pair(book.germanTitle, book.author)
                    } else {
                        Pair(null, null)
                    }
            val user = context.user
            val cycleNumber = cyclesDao.cycleForBook(number)
            if (cycleNumber != null) {
                val cycle = cyclesDao.findCycle(cycleNumber)
                val newSummary = Summary(number, cycleNumber, germanTitle, null, bookAuthor, null, null,
                        Dates.formatDate(LocalDate.now()), null, Dates.formatTime(LocalDateTime.now()), user?.fullName,
                        cycle.germanTitle)
                return EditSummaryView(newSummary, user?.fullName)
            } else {
                return EditSummaryView(null, user?.fullName)
            }
        }
    }

    fun findCoverBytes(number: Int): ByteArray? {
        var result = coversDao.findCover(number)
        if (result == null) {
            val coverUrl = covers.findCoverFor(number)
            log.info("Fetching new cover for $number: $coverUrl")
            if (coverUrl != null) {
                result = fetchUrl(coverUrl)
                coversDao.save(number, result)
                log.info("Saved new cover for $number in cache, size: " + result.size)
            }
        } else {
            log.info("Found cover in cache: $number, size: " + result.size)
        }

        return result
    }

    /**
     * Fetch the given URL and return its JPG encoded image.
     */
    private fun fetchUrl(url: String): ByteArray {
        URL(url).openStream().use { ins ->
            ByteArrayOutputStream().use { out ->
                val buf = ByteArray(1024 * 20)
                var n = ins.read(buf)
                while (n != -1) {
                    out.write(buf, 0, n)
                    n = ins.read(buf)
                }
                val image = ImageIO.read(ByteArrayInputStream(out.toByteArray()))
                ByteArrayOutputStream(100000).use { baos ->
                    ImageIO.write(image, "jpg", baos)
                    return baos.toByteArray()
                }
            }
        }
    }

    fun login(username: String, response: HttpServletResponse): Response.ResponseBuilder {
        val user = usersDao.findUser(username)
        val result =
            if (user != null) {
                val authToken = UUID.randomUUID().toString()
                usersDao.updateAuthToken(username, authToken)
                val cookie = Cookie("authToken", authToken, "/", null, 1)
                val newCookie = NewCookie(cookie, null, 60, false)
                Response.status(Response.Status.OK).type(MediaType.TEXT_HTML).cookie(newCookie)
            } else {
                Response.status(Response.Status.UNAUTHORIZED)
            }
        return result
    }
}

fun main(args: Array<String>) {
    val inj = Guice.createInjector(PerryModule())
    val c = inj.getInstance(Covers::class.java)

    if (false) {
        val url = c.findCoverFor(2000)
        println(url)
        URL(url).openStream().use { ins ->
            ByteArrayOutputStream().use { out ->
                val buf = ByteArray(1024)
                var n = ins.read(buf)
                while (n != -1) {
                    out.write(buf, 0, n)
                    n = ins.read(buf)
                }
                val response = out.toByteArray()
                transaction {
                    CoversTable.insert {
                        it[CoversTable.number] = 2000
                        it[CoversTable.image] = response
                    }
                }
            }
        }
    }

    transaction {
        CoversTable.select { CoversTable.number eq 2000 }.forEach {
            val bytes = it[CoversTable.image]
            FileOutputStream(File("a.jpg")).write(bytes)
            println("")
        }
    }
}
