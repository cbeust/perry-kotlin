package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.views.View
import org.slf4j.LoggerFactory
import java.net.URI
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path("/")
class PerryService @Inject constructor(private val logic: PresentationLogic,
        private val cyclesDao: CyclesDao, private val booksDao: BooksDao,
        private val summariesDao: SummariesDao,
        private val pendingDao: PendingDao,
        private val emailService: EmailService, private val urls: Urls,
        private val twitterService: TwitterService) {

    private val log = LoggerFactory.getLogger(PerryService::class.java)

    /////
    // HTML content
    //

    @GET
    fun root(@Context sc: SecurityContext): CyclesView {
        return CyclesView(logic.findAllCycles(), summariesDao.findRecentSummaries(), summariesDao.count(),
                booksDao.count(), (sc.userPrincipal as User?)?.fullName)
    }

    @GET
    @Path(Urls.SUMMARIES)
    fun summaryQueryParameter(@QueryParam("number") number: Int): Response = Response.seeOther(URI(Urls.SUMMARIES + "/$number")).build()

    @GET
    @Path(Urls.SUMMARIES + "/{number}")
    fun summary(@Suppress("UNUSED_PARAMETER") @PathParam("number") number: Int, @Context sc: SecurityContext) = SummaryView((sc.userPrincipal as User?)?.fullName)

    @PermitAll
    @GET
    @Path(Urls.SUMMARIES + "/{number}/edit")
    fun editSummary(@PathParam("number") number: Int, @Context sc: SecurityContext): View {
        val user = sc.userPrincipal as User?
        val fullName = user?.fullName
        val summary = logic.findSummary(number, fullName)
        if (summary != null) {
            val name = summary.authorName ?: fullName
            val email = summary.authorEmail ?: user?.email
            return EditSummaryView(summary, name, email)
        } else {
            throw WebApplicationException("Couldn't find a summary for $number")
        }
    }

    @GET
    @Path(Urls.SUMMARIES + "/{number}/create")
    fun createSummary(@PathParam("number") number: Int, @Context sc: SecurityContext) = logic.createSummary(number, sc.userPrincipal as User?)

    @GET
    @Path(Urls.CYCLES + "/{number}")
    fun cycle(@PathParam("number") number: Int, @Context sc: SecurityContext): Any {
        try {
            val cycle = cyclesDao.findCycle(number)
            val books = booksDao.findBooksForCycle(number)
            val summaries = summariesDao.findEnglishSummaries(cycle.start, cycle.end)
            return CycleView(logic.findCycle(number), books, summaries, (sc.userPrincipal as User?)?.fullName)
        } catch (ex: WebApplicationException) {
            return Response.seeOther(URI(Urls.CYCLES))
        }
    }

//    @GET
//    @Path("/login")
//    fun login() = LoginView()

    @GET
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun logout(@Context request: HttpServletRequest, @Context response: HttpServletResponse): Response? {
        return logic.logout(request.getHeader("Referer")).build()
    }

    //
    // HTML content
    /////

    /////
    // API content
    //

    @POST
    @Path("/api/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun apiLogin(@FormParam("username") username: String, @Context request: HttpServletRequest,
            @Context response: HttpServletResponse): Response {
        return logic.login(request.getHeader("Referer"), username).build()
    }

    @GET
    @Path("${Urls.API}${Urls.CYCLES}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findCycle(@PathParam("number") number: Int) = cyclesDao.findCycle(number)

    @GET
    @Path("${Urls.API}${Urls.CYCLES}")
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = cyclesDao.allCycles()

    @GET
    @Path("${Urls.API}${Urls.SUMMARIES}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummaries(@Context context: SecurityContext,
            @QueryParam("start") start: Int, @QueryParam("end") end: Int): List<Summary> {
        val user = context.userPrincipal as User?
        return logic.findSummaries(start, end, user?.fullName)
    }

    @POST
    @Path("${Urls.API}${Urls.SUMMARIES}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun putSummary(
            @Context context: SecurityContext,
            @FormParam("number") number: Int,
            @FormParam("germanTitle") germanTitle: String,
            @FormParam("englishTitle") englishTitle: String,
            @FormParam("summary") summary: String,
            @FormParam("bookAuthor") bookAuthor: String,
            @FormParam("authorEmail") authorEmail: String?,
            @FormParam("date") date: String,
            @FormParam("time") time: String,
            @FormParam("authorName") authorName: String): Response
    {
        val user = context.userPrincipal as User?
        logic.postSummary(user, number, germanTitle, englishTitle, summary, bookAuthor, authorEmail, date,
                time, authorName)
        return Response.seeOther(URI(urls.summaries(number))).build()
    }

    @Suppress("unused")
    class SummaryResponse(val found: Boolean, val number: Int, val summary: Summary?)

    @GET
    @Path(Urls.THANK_YOU_FOR_SUBMITTING)
    fun thankYouForSubmitting() = ThankYouForSubmittingView()

    @GET
    @Path("${Urls.API}${Urls.SUMMARIES}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummary(@Context context: SecurityContext, @PathParam("number") number: Int): SummaryResponse {
        val result = logic.findSummary(number, (context.userPrincipal as User?)?.fullName)
        if (result != null) return SummaryResponse(true, number, result)
        else return SummaryResponse(false, number, null)
    }

    @GET
    @Produces("image/png")
    @Path("/api/covers/{number}")
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
            twitterService.updateStatus(pending.number, pending.text, url)
            return Response.ok("Summary ${pending.number} posted").build()
        } else {
            throw WebApplicationException("Couldn't find pending id $id")
        }
    }

    @GET
    @Path("/rss")
    fun rss(): View = RssView(summariesDao, urls, booksDao)
}
