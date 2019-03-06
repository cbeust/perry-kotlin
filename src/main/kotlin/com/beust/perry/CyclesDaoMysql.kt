package com.beust.perry

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class CyclesDaoMysql: CyclesDao {
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

}