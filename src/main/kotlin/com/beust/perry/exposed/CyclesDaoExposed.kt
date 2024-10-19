package com.beust.perry.exposed

import com.beust.perry.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class CyclesDaoExposed: CyclesDao {
    private val log = LoggerFactory.getLogger(CyclesDaoExposed::class.java)

    override fun updateCycleName(cycleNumber: Int, englishCycleName: String) {
        transaction {
            Cycles.update({ Cycles.number eq cycleNumber }) {
                it[englishTitle] = englishCycleName
            }
        }
    }

    override fun addCycle(cycleNumber: Int, germanTitle: String, englishTitle: String, shortTitle: String,
            start: Int, end: Int) {
        transaction {
            Cycles.insert {
                it[number] = cycleNumber
                it[Cycles.germanTitle] = germanTitle
                it[Cycles.englishTitle] =  englishTitle
                it[Cycles.shortTitle] = shortTitle
                it[Cycles.start] = start
                it[Cycles.end] = end
            }
        }
    }

    override fun cycleForBook(bookNumber: Int): Int? {
        val row = transaction {
            Cycles.slice(Cycles.number)
                .select {
                    Cycles.start.lessEq(bookNumber) and Cycles.end.greaterEq(bookNumber)
                }.firstOrNull()
        }
        if (row != null) return row[Cycles.number]
        else return null
    }

    private fun createCycleFromRow(row: ResultRow): CycleFromDao {
        val result = CycleFromDao(
                row[Cycles.number], row[Cycles.germanTitle],
                row[Cycles.englishTitle], row[Cycles.shortTitle],
                row[Cycles.start], row[Cycles.end])
        return result
    }

    override fun allCycles(): List<CycleFromDao> {
        val result = arrayListOf<CycleFromDao>()
        transaction {
            Cycles.selectAll().forEach { row ->
                result.add(createCycleFromRow(row))
            }
        }
        return result
    }

    override fun findCycle(n: Int): DaoResult<CycleFromDao> {
        var result: CycleFromDao? = null
        transaction {
            Cycles.select{
                Cycles.number.eq(n)
            }.forEach { row ->
                result = createCycleFromRow(row)
            }
        }
        return DaoResult(true, result)
    }

}