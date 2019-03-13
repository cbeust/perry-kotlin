package com.beust.perry

import com.google.inject.Inject
import java.net.URL
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.*



/**
 * All these URL's are under /api/.
 */
@Path("/")
class PerryService @Inject constructor(private val cyclesDao: CyclesDao, private val booksDao: BooksDao,
        private val summariesDao: SummariesDao, private val authenticator: PerryAuthenticator,
        private val covers: Covers) {
    @GET
    @Path("/cycles/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findCycle(@PathParam("number") number: Int) = cyclesDao.findCycle(number)

    @GET
    @Path("/cycles")
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = cyclesDao.allCycles()

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    fun findBooks(@QueryParam("start") start: Int, @QueryParam("end") end: Int) = booksDao.findBooks(start, end)

    @GET
    @Path("/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummaries(@Context context: SecurityContext,
            @QueryParam("start") start: Int, @QueryParam("end") end: Int) {
        val user = context.userPrincipal as User?
        summariesDao.findEnglishSummaries(start, end, user)
    }

//    @PermitAll
    @PUT
    @Path("/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    fun putSummary(
            @Context context: SecurityContext,
            @FormParam("number") number: Int,
            @FormParam("germanTitle") germanTitle: String,
            @FormParam("englishTitle") englishTitle: String,
            @FormParam("summary") summary: String,
            @FormParam("bookAuthor") bookAuthor: String,
            @FormParam("authorEmail") authorEmail: String,
            @FormParam("date") date: String,
            @FormParam("time") time: String,
            @FormParam("authorName") authorName: String) {
        val cycleForBook = cyclesDao.findCycle(cyclesDao.cycleForBook(number))
        val user = context.userPrincipal as User?
        if (cycleForBook != null) {
            summariesDao.saveSummary(FullSummary(number, 10, germanTitle, englishTitle, bookAuthor,
                    authorName, authorEmail, date, summary, time, user?.name, cycleForBook.germanTitle))
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
    }

    @PermitAll
    @GET
    @Path("/summaries/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummary(@Context context: SecurityContext, @PathParam("number") number: Int, @QueryParam("end") end: Int)
            = summariesDao.findEnglishSummary(number, context.userPrincipal as User?)

    @GET
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(@Context request: HttpServletRequest): Response? {
        request.session.invalidate()
        return Response.ok().build()
    }

    @GET
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun login() = "Success"

    @GET
    @Path("/covers/{number}")
    fun covers(@PathParam("number") number: Int): Response? {
        val cover2 = covers._findCoverFor2(number)
        val cover =
            if (cover2 != null && URL(cover2).openConnection() != null) {
                cover2
            } else {
                covers.findCoverFor(number)
            }
        if (cover != null) {
            val uri = UriBuilder.fromUri(cover2).build()
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
