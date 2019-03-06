package com.beust.perry

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class CyclesDaoPostgres : CyclesDao {
    private val cycles = listOf(
            Cycle(1, "Die dritte machte", "The third power", "third-power", 0, 50),
            Cycle(2, "Atlan und Arkon", "Atlan and Arkonis", "atlan", 50, 99)
    )

    override fun allCycles(): CyclesDao.CyclesResponse {
        val result = arrayListOf<Cycle>()
        transaction {
            Cycles.selectAll().forEach { row ->
                val cycle = Cycle(
                        row[Cycles.number], row[Cycles.germanTitle],
                        row[Cycles.englishTitle], row[Cycles.shortTitle],
                        row[Cycles.start], row[Cycles.end])
                result.add(cycle)
            }
        }
        return CyclesDao.CyclesResponse(result)
    }

    override fun findCycle(n: Int) = if (n > 0 && n < cycles.size) cycles[n - 1] else null
}
