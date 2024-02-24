package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.views.View
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext



@Path("/")
class PerryService @Inject constructor(private val logic: PresentationLogic,
        private val cyclesDao: CyclesDao, private val booksDao: BooksDao,
        private val summariesDao: SummariesDao, private val perryMetrics: PerryMetrics,
        private val pendingDao: PendingDao, private val covers: Covers,
        private val emailService: EmailService, private val urls: Urls,
        private val twitterService: TwitterService,
        @Host private val host: String) {

    private val log = LoggerFactory.getLogger(PerryService::class.java)

    /////
    // HTML content
    //

    @GET
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    fun root(@Context sc: SecurityContext): CyclesView {
        perryMetrics.incrementRootPage()
        return CyclesView(logic.findAllCycles(), summariesDao.findRecentSummaries(), summariesDao.count,
                booksDao.count(), BannerInfo(sc.userPrincipal as User?))
    }

    @GET
    @Path(Urls.SUMMARIES)
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    fun summaryQueryParameter(@QueryParam("number") number: Int): Response = Response.seeOther(URI(Urls.SUMMARIES + "/$number")).build()

    @GET
    @Path(Urls.SUMMARIES + "/{number}")
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    fun summary(@Suppress("UNUSED_PARAMETER") @PathParam("number") number: Int, @Context sc: SecurityContext): Any {
        perryMetrics.incrementSummariesPageHtml()
        if (logic.isLegalSummaryNumber(number)) {
            return SummaryView(BannerInfo(sc.userPrincipal as User?))
        } else {
            return IllegalHeftNumberView(number)
        }
    }

    @PermitAll
    @GET
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    @Path(Urls.SUMMARIES + "/{number}/edit")
    fun editSummary(@PathParam("number") number: Int, @Context sc: SecurityContext): View = logic.editSummary(number, sc.userPrincipal as User?)

    @GET
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    @Path("${Urls.SUMMARIES}/{number}/create")
    fun createSummary(@PathParam("number") number: Int, @Context sc: SecurityContext) = logic.createSummary(number, sc.userPrincipal as User?)

    @GET
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    @Path(Urls.CYCLES + "/{number}")
    fun cycle(@Context sc: SecurityContext, @PathParam("number") number: Int): Any {
        perryMetrics.incrementCyclesPageHtml()
        try {
            // will throw if the cycle doesn't exist
            logic.findCycleOrThrow(number)
            return CycleView(BannerInfo(sc.userPrincipal as User?))
        } catch (ex: WebApplicationException) {
            return Response.seeOther(URI("/")).build()
        }
    }

    @GET
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun logout(@Context request: HttpServletRequest): Response = logic.logout(request.getHeader("referer")).build()

    /**
     * favicon
     */
    @GET
    @Path("/{fileName: .*ico}")
    @Produces("image/x-icon")
    @Throws(IOException::class)
    fun getPage(@PathParam("fileName") passedFileName: String): Response {
        var fileName = passedFileName
        fileName = if (fileName == "") "index.htm" else fileName
        val fn = fileName.substring(0, fileName.lastIndexOf('.')) + ".png"
        return serveImage(fn)
    }

    /**
     * png files
     */
    @GET
    @Path("/{fileName: .*png}")
    @Produces("image/x-icon")
    @Throws(IOException::class)
    fun getPng(@PathParam("fileName") fn: String) = serveImage(fn)

    private fun serveImage(fn: String): Response {
        val urlToResource = javaClass.getResource("/$fn")
        if (urlToResource != null) {
            val conn = urlToResource.openConnection()
            conn.getInputStream().use {
                val size = conn.contentLength
                val imageData = ByteArray(size)

                it.read(imageData, 0, size)
                return Response.ok(ByteArrayInputStream(imageData)).build()
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @GET
    @Path("/php/displaySummary.php")
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    fun phpSummary(@QueryParam("number") number: Int): Response = Response.seeOther(URI(Urls.SUMMARIES + "/$number")).build()

    //
    // HTML content
    /////

    /////
    // API content
    //

//    @POST
//    @Path("${Urls.API}/createPassword")
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    fun createPassword(@FormParam("password1") password1: String, @FormParam("password2") password2: String,
//            @Context request: HttpServletRequest): Response {
//        return logic.createPassword(password1, password2)
//    }

    @POST
    @Path("${Urls.API}${Urls.LOGIN}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun apiLogin(@FormParam("username") username: String, @FormParam("password") password: String,
            @Context request: HttpServletRequest): Response
        = logic.login(request.getHeader("referer") ?: Urls.HOST, username, password).build()

    @Suppress("unused", "CanBeParameter", "MemberVisibilityCanBePrivate")
    class SmallBook(val number: Int, val germanTitle: String?, val englishTitle: String?, val bookAuthor: String?,
            val href: String, val cycleStart: Int) {
        val numberString: String = if (number == cycleStart) "heft $number" else number.toString()
    }

    @Suppress("unused")
    class CycleResponse(val cycle: Cycle, val books: ArrayList<SmallBook>, val hideLeft: Boolean) {
        val hrefBack = "/"
    }

    @GET
    @Path("${Urls.API}${Urls.CYCLES}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun apiCycles(@PathParam("number") number: Int): CycleResponse? {
        perryMetrics.incrementCyclesPageApi()
        try {
            val cycle = logic.findCycleOrThrow(number)
            val englishTitles = summariesDao.findEnglishTitles(cycle.start, cycle.end)
            val passedBooks = booksDao.findBooksForCycle(number)
            val books = arrayListOf<SmallBook>()
            passedBooks.forEach { book ->
                books.add(
                    SmallBook(
                        book.number, book.germanTitle, englishTitles[book.number], book.author,
                        Urls.SUMMARIES + "/${book.number}", cycle.start
                    )
                )
            }

            return CycleResponse(cycle, books, number == 1)
        } catch (ex: WebApplicationException) {
            return null
        }
    }

    @GET
    @Path("${Urls.API}${Urls.CYCLES}")
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = cyclesDao.allCycles()

//    @GET
//    @Path("${Urls.API}${Urls.SUMMARIES}")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun findSummaries(@Context context: SecurityContext,
//            @QueryParam("start") start: Int, @QueryParam("end") end: Int): List<Summary> {
//        val user = context.userPrincipal as User?
//        return logic.findSummaries(start, end, user)
//    }

    @POST
    @Path("${Urls.API}${Urls.SUMMARIES}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun putSummary(
            @Context context: SecurityContext,
            @FormParam("englishCycleName") englishCycleName: String,
            @FormParam("number") number: Int,
            @FormParam("germanTitle") germanTitle: String,
            @FormParam("englishTitle") englishTitle: String,
            @FormParam("summary") summary: String,
            @FormParam("bookAuthor") bookAuthor: String,
            @FormParam("authorEmail") authorEmail: String?,
            @FormParam("date") date: String,
            @FormParam("time") time: String?,
            @FormParam("authorName") authorName: String): Response {
        val user = context.userPrincipal as User?
        logic.maybeUpdateCycle(number, englishCycleName)
        val result = logic.postSummary(user, number, germanTitle, englishTitle, summary, bookAuthor, authorEmail, date,
                time, authorName)
        return result
    }

    @GET
    @Path(Urls.THANK_YOU_FOR_SUBMITTING)
    fun thankYouForSubmitting() = ThankYouForSubmittingView()

    @Suppress("unused")
    class SummaryResponse(val found: Boolean, val number: Int, val summary: Summary?, val cycle: Cycle,
            val coverUrl: String?) {
        val hideLeft = number == 1
        val hrefBack = Urls.cycles(cycle.number)
        val hrefEdit = Urls.editSummary(number)
        val perryPedia = PerryPedia.heftUrl(number)
        val emailMailingList = "${Urls.API}/sendEmail?number=$number"
    }

    @GET
    @Path("${Urls.API}${Urls.SUMMARIES}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun apiSummaries(@Context context: SecurityContext, @PathParam("number") number: Int?): Any {
        return if (number == null) {
            Response.seeOther(URI(host)).build()
        } else {
            perryMetrics.incrementSummariesPageApi()
            val result = logic.findSummary(number, context.userPrincipal as User?)
            val cycleNumber = cyclesDao.cycleForBook(number)
            if (cycleNumber != null) {
                val cycle = logic.findCycleOrThrow(cycleNumber)
                if (result != null) SummaryResponse(true, number, result, cycle, covers.findCoverFor(number))
                else SummaryResponse(false, number, null, cycle, covers.findCoverFor(number))
            } else {
                throw WebApplicationException("No cycle found for book $number")
            }
        }
    }

    @GET
    @Produces("image/png")
    @Path("${Urls.API}${Urls.COVERS}/{number}")
    fun covers(@PathParam("number") number: Int): ByteArray? = logic.findCoverBytes(number)

    @Suppress("unused")
    class PendingResponse(val found: Boolean, val number: Int, val summary: PendingSummaryFromDao?)

    @GET
    @Path("${Urls.API}${Urls.PENDING}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findPending(@PathParam("number") number: Int): PendingResponse {
        val result = logic.findPending(number)
        if (result != null) return PendingResponse(true, number, result)
        else return PendingResponse(false, number, null)
    }

    @GET
    @Path("${Urls.API}${Urls.PENDING}/{id}/delete")
    @Produces(MediaType.APPLICATION_JSON)
    fun deletePending(@PathParam("id") id: Int): Response {
        try {
            pendingDao.deletePending(id)
            return Response.ok().build()
        } catch (ex: Exception) {
            throw WebApplicationException(ex.message, ex)
        }
    }

    @GET
    @Path("${Urls.API}${Urls.PENDING}/{id}/approve")
    @Produces(MediaType.APPLICATION_JSON)
    fun approvePending(@PathParam("id") id: Int): Response {
        val pending = logic.findPending(id)
        if (pending != null) {
            logic.saveSummaryFromPending(pending)
            log.info("Saved summary ${pending.number}: ${pending.englishTitle}")
            pendingDao.deletePending(id)
            log.info("Deleted pending summary $id")
            val url = urls.summaries(pending.number, fqdn = true)
            emailService.notifyAdmin("New summary posted after approval: ${pending.number}", "URL: $url")
            twitterService.updateStatus(pending.number, pending.englishTitle, url)
            return Response.ok("Summary ${pending.number} posted").build()
        } else {
            throw WebApplicationException("Couldn't find pending id $id")
        }
    }

    @GET
    @Path(Urls.RSS)
    fun rss(): View = RssView(summariesDao, urls, booksDao)

//    @POST
//    @Path("${Urls.API}/requestAccount")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun requestAccount(@FormParam("fullName") fullName: String,
//        @FormParam("email") emailAddress: String,
//        @FormParam("reason") reason: String): Response {
//        return logic.requestAccountCreation(fullName, emailAddress, reason)
//    }

    @GET
    @Path("${Urls.API}/sendEmail")
    @Produces(MediaType.APPLICATION_JSON)
    fun sendMailingListEmail(@QueryParam("number") number: Int) = logic.sendMailingListEmail(number)

    @POST
    @Path("${Urls.API}/createAccount")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun createAccount(
            @FormParam("username") username: String,
            @FormParam("password1") password1: String,
            @FormParam("password2") password2: String,
            @FormParam("fullName") fullName: String,
            @FormParam("email") email: String): Response {
        return logic.createUser(username, fullName, email, password1, password2)
    }

    @GET
    @Path("${Urls.API}${Urls.VERIFY}/{tempLink}")
    fun verifyUser(@PathParam("tempLink") tempLink: String): Response {
        val daoResult = logic.verifyUser(tempLink)
        return if (daoResult.success) Response.seeOther(URI(host)).build()
        else Response.serverError().entity(daoResult.message).build()
    }

    @GET
    @Path("${Urls.TEST}")
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    fun test(@Context sc: SecurityContext): Any {
        val user = sc.userPrincipal as User?
        return if (user != null) {
            TestView()
        } else {
            Response.seeOther(URI("/")).build()
        }
    }

    private fun verifyAuth(sc: SecurityContext, block: () -> Response): Response =
        if (sc.userPrincipal is User) block()
        else Response.seeOther(URI("/")).build()

    @GET
    @Path("${Urls.API}${Urls.TEST}/authTwitter")
    @Produces(MediaType.APPLICATION_JSON)
    fun authTwitter(@Context sc: SecurityContext) = verifyAuth(sc) {
        logic.authTwitter()
    }

    @GET
    @Path("${Urls.API}${Urls.TEST}/authGmail")
    @Produces(MediaType.APPLICATION_JSON)
    fun authGmail(@Context sc: SecurityContext) = verifyAuth(sc) {
        logic.authGmail()
    }

    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_HTML + "; " + MediaType.CHARSET_PARAMETER + "=UTF-8")
    fun error() {
        throw RuntimeException("Error test")
    }
}
