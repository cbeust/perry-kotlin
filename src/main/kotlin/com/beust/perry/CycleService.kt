package com.beust.perry

import com.google.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/cycles")
class CycleService @Inject constructor(private val dao: CyclesDao) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun allCycles() = dao.allCycles()
}
