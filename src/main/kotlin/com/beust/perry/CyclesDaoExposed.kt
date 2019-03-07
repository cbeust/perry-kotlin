package com.beust.perry

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class CyclesDaoExposed: CyclesDao {
    private val log = LoggerFactory.getLogger(CyclesDaoExposed::class.java)

    private fun createCycleFromRow(row: ResultRow)
        = Cycle(
            row[Cycles.number], row[Cycles.germanTitle],
            row[Cycles.englishTitle], row[Cycles.shortTitle],
            row[Cycles.start], row[Cycles.end])

    override fun allCycles(): CyclesDao.CyclesResponse {
        val result = arrayListOf<Cycle>()
        transaction {
            Cycles.selectAll().forEach { row ->
                result.add(createCycleFromRow(row))
            }
        }
        println("RETURNING ${result.size} CYCLES")
        log.info("Using log info")
        return CyclesDao.CyclesResponse(result)
    }

    override fun findCycle(n: Int): Cycle? {
        var result: Cycle? = null
        transaction {
            Cycles.select{
                Cycles.number.eq(n)
            }.forEach { row ->
                result = createCycleFromRow(row)
            }
        }
        return result
    }

}