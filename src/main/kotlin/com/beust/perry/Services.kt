package com.beust.perry

import com.google.inject.Inject
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
    @Path("/booksForCycle/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findBooksForCycle(@PathParam("number") number: Int): SummariesDao.SummariesResponse {
        val cycle = cyclesDao.findCycle(number)
        if (cycle != null) {
            return summariesDao.findEnglishSummaries(cycle.start, cycle.end)
        } else {
            throw IllegalArgumentException("Couldn't find cycle $number")
        }
    }
}
