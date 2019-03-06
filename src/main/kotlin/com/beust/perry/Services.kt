package com.beust.perry

import com.google.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/cycles")
class AllCyclesService @Inject constructor(private val dao: CyclesDao) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = dao.allCycles()
}

@Path("/cycle")
class CycleService @Inject constructor(private val dao: CyclesDao) {
    @GET
    @Path("/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findCycle(@PathParam("number") number: Int) = dao.findCycle(number)
}
