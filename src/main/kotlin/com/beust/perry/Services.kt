package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.views.View
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.*

@Path("/")
class PerryService @Inject constructor(private val logic: PresentationLogic,
        private val cyclesDao: CyclesDao, private val booksDao: BooksDao,
        private val summariesDao: SummariesDao, private val covers: Covers,
        private val perryContext: PerryContext, private val pendingDao: PendingDao,
        private val emailService: EmailService, private val urls: Urls,
        private val twitterService: TwitterService)
{

    private val log = LoggerFactory.getLogger(PerryService::class.java)

    /////
    // HTML content
    //

    @GET
    fun root() = CyclesView(logic.findAllCycles(), summariesDao.findRecentSummaries(), summariesDao.count(),
            booksDao.count(), perryContext.user?.fullName)

    @GET
    @Path(Urls.SUMMARIES)
    fun summaryQueryParameter(@QueryParam("number") number: Int): Response
            = Response.seeOther(URI(Urls.SUMMARIES + "/$number")).build()

    @GET
    @Path(Urls.SUMMARIES + "/{number}")
    fun summary(@Suppress("UNUSED_PARAMETER") @PathParam("number") number: Int)
            = SummaryView(perryContext.user?.fullName)

    @PermitAll
    @GET
    @Path(Urls.SUMMARIES + "/{number}/edit")
    fun editSummary(@PathParam("number") number: Int, @Context context: PerryContext) : View {
        val summary = logic.findSummary(number, perryContext.user?.fullName)
        if (summary != null) {
            return EditSummaryView(summary, context.user?.fullName)
        } else {
            throw WebApplicationException("Couldn't find a summary for $number")
        }
    }

    @GET
    @Path(Urls.SUMMARIES + "/{number}/create")
    fun createSummary(@PathParam("number") number: Int, @Context context: PerryContext)
            = logic.createSummary(number, context)

    @GET
    @Path(Urls.CYCLES + "/{number}")
    fun cycle(@PathParam("number") number: Int): View {
        val cycle = cyclesDao.findCycle(number)
        if (cycle != null) {
            val books = booksDao.findBooksForCycle(number)
            val summaries = summariesDao.findEnglishSummaries(cycle.start, cycle.end)
            return CycleView(logic.findCycle(number)!!, books, summaries, perryContext.user?.fullName)
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
    }

    //
    // HTML content
    /////

    /////
    // api content
    //

    @GET
    @Path("${Urls.API}${Urls.CYCLES}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findCycle(@PathParam("number") number: Int) = cyclesDao.findCycle(number)

    @GET
    @Path("${Urls.API}${Urls.CYCLES}")
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = cyclesDao.allCycles()

//    @GET
//    @Path("/api/books")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun findBooks(@QueryParam("start") start: Int, @QueryParam("end") end: Int) = booksDao.findBooks(start, end)

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
    @Produces(MediaType.APPLICATION_JSON)
    fun putSummary(
            @Context context: PerryContext,
            @FormParam("number") number: Int,
            @FormParam("germanTitle") germanTitle: String,
            @FormParam("englishTitle") englishTitle: String,
            @FormParam("summary") summary: String,
            @FormParam("bookAuthor") bookAuthor: String,
            @FormParam("authorEmail") authorEmail: String?,
            @FormParam("date") date: String,
            @FormParam("time") time: String,
            @FormParam("authorName") authorName: String): Any {
        val cycleForBook = cyclesDao.findCycle(cyclesDao.cycleForBook(number))
        if (cycleForBook != null) {
            val user = context.user
            if (user != null) {
                logic.saveSummary(SummaryFromDao(number, englishTitle,
                        authorName, authorEmail, date, summary, time), germanTitle, bookAuthor)
                val url = urls.summaries(number, fqdn = true)
                emailService.sendEmail("cedric@beust.com", "New summary posted: $number", url)
                twitterService.updateStatus(number, summary, url)
                return Response.seeOther(URI(Urls.CYCLES + "/${cycleForBook.number}")).build()
            } else {
                logic.saveSummaryInPending(PendingSummaryFromDao(number, germanTitle, bookAuthor, englishTitle,
                        authorName, authorEmail, summary, date))
                return Response.seeOther(URI(Urls.THANK_YOU_FOR_SUBMITTING)).build()
            }
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
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

    @PermitAll
    @GET
    @Path("/api/logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(): Response? {
        return Response.seeOther(URI("/")).build()
    }

    @PermitAll
    @GET
    @Path("/api/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun login(@Context request: HttpServletRequest) = Response.seeOther(URI(request.getHeader("Referer"))).build()

    @GET
    @Path("/api/covers/{number}")
    fun covers(@PathParam("number") number: Int): Response? {
        fun isValid(url: String) : Boolean {
            val u = URL(url)
            (u.openConnection() as HttpURLConnection).let { huc ->
                huc.requestMethod = "GET"  //OR  huc.setRequestMethod ("HEAD");
                huc.connect()
                val code = huc.responseCode
                return code == 200
            }
        }

        val cover2 = covers.findCoverFor2(number)
        val cover =
            if (cover2 != null && isValid(cover2)) {
                cover2
            } else {
                covers.findCoverFor(number)
            }
        if (cover != null) {
            val uri = UriBuilder.fromUri(cover).build()
            return Response.seeOther(uri).build()
        } else {
            return Response.ok().build()
        }
    }


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
        } catch(ex: Exception) {
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
            emailService.sendEmail("cedric@beust.com", "New summary posted after approval: ${pending.number}",
                    "URL: $url")
            twitterService.updateStatus(pending.number, pending.text, url)
            return Response.ok("Summary ${pending.number} posted").build()
        } else {
            throw WebApplicationException("Couldn't find pending id $id")
        }
    }

}
