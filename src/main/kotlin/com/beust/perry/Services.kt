package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.auth.Auth
import javax.annotation.security.PermitAll
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * All these URL's are under /api/.
 */
@Path("/")
class PerryService @Inject constructor(private val cyclesDao: CyclesDao, private val booksDao: BooksDao,
        private val summariesDao: SummariesDao) {
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
    fun findSummaries(@QueryParam("start") start: Int, @QueryParam("end") end: Int)
            = summariesDao.findEnglishSummaries(start, end)

    @GET
    @Path("/summaries/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findSummary(@PathParam("number") number: Int, @QueryParam("end") end: Int)
            = summariesDao.findEnglishSummary(number)

    @PermitAll
    @POST
    @Path("/editSummary")
    @Produces(MediaType.APPLICATION_JSON)
    fun editSummary(@Auth user: User) = "Test"
}
