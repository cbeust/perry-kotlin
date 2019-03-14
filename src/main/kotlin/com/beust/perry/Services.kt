package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.views.View
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.*

class WrappedResponse<T>(private val name: String, val t: T, private val perryContext: PerryContext) {
    fun wrap(): HashMap<String, Any?> {
        return hashMapOf(
            name to t,
            "username" to perryContext.user?.name
        )
    }
}

class CyclesView(val cycles: List<Cycle>, val context: PerryContext) : View("cycles.mustache")

class CycleView(val cycle: Cycle, val books: List<FullSummary>) : View("cycle.mustache")

class SummaryView() : View("summary.mustache")

class EditSummaryView(val summary: FullSummary, val user: User?) : View("editSummary.mustache")

@Path("/")
class PerryService @Inject constructor(private val cyclesDao: CyclesDao, private val booksDao: BooksDao,
        private val summariesDao: SummariesDao, private val authenticator: PerryAuthenticator,
        private val covers: Covers, private val perryContext: PerryContext) {

    /////
    // HTML content
    //

    @GET
    fun root() = CyclesView(cyclesDao.allCycles(), perryContext)

    @GET
    @Path(Urls.SUMMARIES + "/{number}")
    fun summary(@PathParam("number") number: Int) = SummaryView()

    @PermitAll
    @GET
    @Path(Urls.SUMMARIES + "/{number}/edit")
    fun editSummary(@PathParam("number") number: Int, @Context context: PerryContext) : View {
        val summary = summariesDao.findEnglishSummary(number)
        if (summary != null) {
            return EditSummaryView(summary, context.user)
        } else {
            throw WebApplicationException("Couldn't find summary $number")
        }
    }

    @GET
    @Path(Urls.CYCLES + "/{number}")
    fun cycle(@PathParam("number") number: Int): View {
        val cycle = cyclesDao.findCycle(number)
        if (cycle != null) {
            val books = summariesDao.findEnglishSummaries(cycle.start, cycle.end)
            return CycleView(cycle, books)
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
    @Path("/api/cycles/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findCycle(@PathParam("number") number: Int) = cyclesDao.findCycle(number)

    @GET
    @Path("/api/cycles")
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = WrappedResponse("cycles", cyclesDao.allCycles(), perryContext).wrap()

    @GET
    @Path("/api/books")
    @Produces(MediaType.APPLICATION_JSON)
    fun findBooks(@QueryParam("start") start: Int, @QueryParam("end") end: Int) = booksDao.findBooks(start, end)

    @GET
    @Path("/api/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummaries(@Context context: SecurityContext,
            @QueryParam("start") start: Int, @QueryParam("end") end: Int): List<FullSummary> {
        val user = context.userPrincipal as User?
        return summariesDao.findEnglishSummaries(start, end, user)
    }

    @PermitAll
    @POST
    @Path("/api/summaries")
    @Produces(MediaType.APPLICATION_JSON)
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
            @FormParam("authorName") authorName: String): Response {
        val cycleForBook = cyclesDao.findCycle(cyclesDao.cycleForBook(number))
        val user = context.userPrincipal as User?
        if (cycleForBook != null) {
            return summariesDao.saveSummary(FullSummary(number, 10, germanTitle, englishTitle, bookAuthor,
                    authorName, authorEmail, date, summary, time, user?.name, cycleForBook.germanTitle))
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
    }

    @GET
    @Path("/api/summaries/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummary(@Context context: SecurityContext, @PathParam("number") number: Int, @QueryParam("end") end: Int)
            = summariesDao.findEnglishSummary(number, context.userPrincipal as User?)

    @PermitAll
    @GET
    @Path("/api/logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(@Context request: HttpServletRequest, @Context sec: SecurityContext): Response? {
        return Response.seeOther(URI("/")).build()
    }

    @PermitAll
    @GET
    @Path("/api/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun login(@Context request: HttpServletRequest) = Response.seeOther(URI("/")).build()

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

//    fun login(@FormParam("username") name: String, @Context context: HttpServletRequest) : String {
//        val user = authenticator.authenticate(BasicCredentials(name, ""))
//        return if (user.isPresent) {
//            "Success"
//        } else {
//            throw WebApplicationException("Illegal credentials: $name")
//        }
//    }
}
