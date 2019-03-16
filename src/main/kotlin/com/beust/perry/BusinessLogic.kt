package com.beust.perry

import com.google.inject.Inject
import javax.ws.rs.WebApplicationException

class BusinessLogic @Inject constructor(private val cyclesDao: CyclesDao, private val summariesDao: SummariesDao) {
    private fun createCycle(it: CycleFromDao, summaryCount: Int)
        = Cycle(it.number, it.germanTitle, it.englishTitle, it.shortTitle, it.start, it.end,
                    summaryCount)

    fun findCycle(number: Int): Cycle? {
        val cycle = cyclesDao.findCycle(number)
        if (cycle != null) {
            return createCycle(cycle, summariesDao.findEnglishSummaries(cycle.start, cycle.end).size)
        } else {
            throw WebApplicationException("Couldn't find cycle $number")
        }
    }

    fun findAllCycles(): List<Cycle> {
        val result = arrayListOf<Cycle>()
        val cyclesDao = cyclesDao.allCycles()
        cyclesDao.forEach {
            val summaryCount = summariesDao.findEnglishSummaries(it.start, it.end).size
            result.add(createCycle(it, summaryCount))
        }
        return result
    }
}