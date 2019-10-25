package com.beust.perry

import com.github.mustachejava.DefaultMustacheFactory
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.io.StringWriter
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Suppress("unused")
data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int, val summaryCount: Int, val cycleCount: Int) {
    val percentage: Int get() = if (summaryCount == 0) 0 else summaryCount * 100 / (end - start + 1)
    val href: String get() = Urls.cycles(number)
    val hrefBack: String get() = "/"
    val numberString = (if (number == cycleCount) "cycle " else "") + number.toString()
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

    fun cloneWith(authorName: String?, authorEmail: String?, date: String?)
        = Summary(number, cycleNumber, germanTitle, englishTitle, bookAuthor,
            authorName, authorEmail, date,
            text, time, username, germanCycleTitle)
}

/**
 * @return fully fledged objects gathered from combining multiple DAO calls.
 */
class PresentationLogic @Inject constructor(private val cyclesDao: CyclesDao,
        private val summariesDao: SummariesDao, private val booksDao: BooksDao, private val pendingDao: PendingDao,
        private val emailService: EmailService, private val urls: Urls, private val twitterService: TwitterService,
        private val covers: Covers, private val coversDao: CoversDao, private val usersDao: UsersDao,
        private val cacheMetric: CoverCacheMetric) {
    private val log = LoggerFactory.getLogger(PresentationLogic::class.java)

    private fun createCycle(it: CycleFromDao, summaryCount: Int)
        = Cycle(it.number, it.germanTitle, it.englishTitle, it.shortTitle, it.start, it.end,
                    summaryCount, cyclesDao.allCycles().size)

    fun isLegalSummaryNumber(number: Int) = booksDao.findBook(number) != null

    fun findSummary(number: Int, user: User?): Summary? {
        val cycleNumber = cyclesDao.cycleForBook(number)
        val result =
            if (cycleNumber != null) {
                val cycle = cyclesDao.findCycle(cycleNumber)
                val book = booksDao.findBook(number)
                if (book != null) {
                    val s = summariesDao.findEnglishSummary(number)
                    if (s != null) {
                        Summary(s.number, cycleNumber, book.germanTitle, s.englishTitle, book.author,
                                s.authorName, s.authorEmail, s.date, s.text, s.time, user?.fullName, cycle.germanTitle)
                    } else {
                        // No summary found, provide the minimum amount of information we can from the book
                        // and cycle.
                        Summary(book.number, cycle.number, book.germanTitle, null, book.author, null, null,
                            Dates.formatDate(LocalDateTime.now()), "No summary found", null, user?.fullName,
                                cycle.germanTitle)
                    }
                } else {
                    Summary(number, cycleNumber, null, null, null,
                            user?.fullName, user?.email, Dates.formatDate(LocalDateTime.now()), null, null,
                            user?.fullName, cycle.germanTitle)
                }
            } else {
                null
            }
        return result
    }

    fun findPending(id: Int): PendingSummaryFromDao? = pendingDao.findPending(id)

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
        result.sortByDescending { it.number }
        return result
    }

    private fun saveSummary(summary: SummaryFromDao, germanTitle: String?, bookAuthor: String?): Boolean {
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
        mustache.execute(content, Model(pending, id, oldSummary?.text, urls.host)).flush()
        val from = pending.authorName
        val number = pending.number
        emailService.notifyAdmin("New summary waiting for approval from $from: $number", content.toString())
    }

    private fun saveSummaryInPending(s: PendingSummaryFromDao) {
        val id = pendingDao.saveSummary(s)
        emailNewPendingSummary(s, id)
    }

    fun saveSummaryFromPending(pending: PendingSummaryFromDao) {
        with(pending) {
            booksDao.saveBook(BookFromDao(number, germanTitle, englishTitle, bookAuthor, null, null))
            summariesDao.saveSummary(
                    SummaryFromDao(number, englishTitle, authorName, authorEmail, dateSummary, text, null))
        }
    }

    fun createSummary(number: Int, user: User?): Any {
        val summary = findSummary(number, user)
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
            val cycleNumber = cyclesDao.cycleForBook(number)
            val bannerInfo = BannerInfo(user)
            if (cycleNumber != null) {
                val cycle = cyclesDao.findCycle(cycleNumber)
                val newSummary = Summary(number, cycleNumber, germanTitle, null, bookAuthor,
                        user?.fullName, user?.email, Dates.formatDate(LocalDate.now()), null,
                        Dates.formatTime(LocalDateTime.now()), user?.fullName, cycle.germanTitle)
                return EditSummaryView(bannerInfo, newSummary, covers.findCoverFor(number),
                        urls.summaries(number), book, cycle)
            } else {
                return EditSummaryView(bannerInfo, null, null, urls.summaries(number), null, null)
            }
        }
    }

    fun findCoverBytes(number: Int): ByteArray? {
        var result = coversDao.findCover(number)
        if (result == null) {
            cacheMetric.addMiss()
            val coverUrl = covers.findCoverFor(number)
            log.info("Fetching new cover for $number: $coverUrl")
            if (coverUrl != null) {
                result = Images.shrinkBelowSize(number, fetchUrl(coverUrl), 80000)
                coversDao.save(number, result)
                log.info("Saved new cover for $number in cache, size: " + result.size)
            }
        } else {
            cacheMetric.addHit()
            log.info("Found cover in cache: $number, size: " + result.size)
        }

        return result
    }

    /**
     * Fetch the given URL and return its JPG encoded image.
     */
    private fun fetchUrl(url: String): ByteArray = Images.fromInputStream(URL(url).openStream())

    fun login(referer: String, username: String, password: String?): Response.ResponseBuilder {
        val result = try {
            val user = usersDao.findUser(username)
            val userSalt = user.salt
            val userPassword = user.password

            val ok1 = password.isNullOrBlank() && userSalt == null && userPassword == null
            val ok2 = password != null && userSalt != null && userPassword != null
                    && Passwords.verifyPassword(password, userSalt, userPassword)
            if (ok1 || ok2) {
                val authToken = UUID.randomUUID().toString()
                usersDao.updateAuthToken(username, authToken)
                val cookie = Cookies.createAuthCookie(authToken)
                emailService.notifyAdmin("Successfully authorized ${user.fullName}", "")
                Response.seeOther(URI(referer)).cookie(cookie)
            } else {
                emailService.onUnauthorized("ok1: $ok1, ok2: $ok2",
                        "User name: $username, Referer: $referer")
                Response.status(Response.Status.UNAUTHORIZED)
            }
        } catch(ex: UserNotFoundException) {
            emailService.onUnauthorized("User is null",
                    "User name: $username, Referer: $referer")
            Response.status(Response.Status.UNAUTHORIZED)
        }
        return result
    }

    fun logout(referer: String): Response.ResponseBuilder {
        val cookie = Cookies.clearAuthCookie()
        return Response.seeOther(URI(referer)).type(MediaType.TEXT_HTML).cookie(cookie)
    }

    fun maybeUpdateCycle(bookNumber: Int, cycleName: String) {
        val cycleNumber = cyclesDao.cycleForBook(bookNumber)
        if (cycleNumber != null) {
            val cycle = cyclesDao.findCycle(cycleNumber)
            if (cycle.germanTitle != cycleName) {
                cyclesDao.updateCycleName(cycleNumber, cycleName)
            }
        }
    }

    fun postSummary(user: User?, number: Int, germanTitle: String, englishTitle: String, summary: String,
            bookAuthor: String, authorEmail: String?, date: String, time: String?,
            authorName: String): Response {
        val cycleNumber = cyclesDao.cycleForBook(number)
        if (cycleNumber != null) {
            val cycleForBook = cyclesDao.findCycle(cycleNumber)
            if (user != null) {
                val oldSummary = summariesDao.findEnglishSummary(number)
                val newSummary = SummaryFromDao(number, englishTitle, authorName, authorEmail, date, summary, time)
                val isNew = saveSummary(newSummary, germanTitle, bookAuthor)
                val url = urls.summaries(number, fqdn = true)
                val body = StringBuilder().apply {
                    append("""
                            | NEW SUMMARY: $url
                            | ===========
                            | ${newSummary.number}
                            | ${newSummary.englishTitle}
                            | ${newSummary.text}
                            | ${newSummary.authorName}
                            | ${newSummary.authorEmail}
                            """.trimIndent())
                    if (oldSummary != null) {
                        append("""
                            |
                            | OLD SUMMARY
                            | ===========
                            | ${oldSummary.number}
                            | ${oldSummary.englishTitle}
                            | ${oldSummary.text}
                            | ${oldSummary.authorName}
                            | ${oldSummary.authorEmail}
                            """.trimIndent())
                    }
                }
                val text = if (isNew) "New summary posted" else "Summary updated"
                emailService.notifyAdmin("$text: $number", body.toString())
                if (isNew) {
                    twitterService.updateStatus(number, englishTitle, url)
                }
                return Response.seeOther(URI(Urls.CYCLES + "/${cycleForBook.number}")).build()
            } else {
                saveSummaryInPending(PendingSummaryFromDao(number, germanTitle, bookAuthor, englishTitle,
                        authorName, authorEmail, summary, date))
                return Response.seeOther(URI(Urls.THANK_YOU_FOR_SUBMITTING)).build()
            }
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
    }

    fun createPassword(password1: String, password2: String): Response {
        if (password1 != password2) {
            return Response.serverError().entity("The two passwords don't match").build()
        } else {
            val user = "jerry_s"
            usersDao.setPassword(user, password1)
            emailService.notifyAdmin("New password set for user $user", "")
            return logout("/").build()
        }

    }

    fun sendMailingListEmail(number: Int): Response {
        val heft = booksDao.findBook(number)
        val summary = summariesDao.findEnglishSummary(number)
        val coverUrl = covers.findCoverFor(number)
        if (heft != null && summary != null) {
            val content = """
                <img src="$coverUrl" />
                <p>
                $number: ${summary.englishTitle}
                <br>
                <i>${heft.germanTitle}</i>
                <br>
                <i>${heft.author}</i>
                <br<
                ${Urls.HOST + Urls.summaries(number)}
                <p>
                ${summary.text}
            """
            emailService.sendEmail("cedric@beust.com", "$number: ${summary.englishTitle}", content)
            return Response.ok().build()
        } else {
            return Response.serverError().build()
        }
    }
}
