package com.beust.perry

import com.beust.perry.Cycles.end
import com.beust.perry.Cycles.start
import com.google.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/")
class PerryService @Inject constructor(private val dao: CyclesDao, private val booksDao: BooksDao) {
    @GET
    @Path("/cycles/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findCycle(@PathParam("number") number: Int) = dao.findCycle(number)

    @GET
    @Path("/cycles")
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = dao.allCycles()

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    fun findBooks(@QueryParam("start") start: Int, @QueryParam("end") end: Int) = booksDao.findBooks(start, end)
}
