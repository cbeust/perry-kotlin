package com.beust.perry

import com.google.inject.Inject
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class CyclesDaoExposed @Inject constructor(private val booksDao: BooksDao): CyclesDao {
    private val log = LoggerFactory.getLogger(CyclesDaoExposed::class.java)

    override fun cycleForBook(bookNumber: Int): Int {
        val row = transaction {
            Cycles.slice(Cycles.number)
                .select {
                    Cycles.start.lessEq(bookNumber) and Cycles.end.greaterEq(bookNumber)
                }.firstOrNull()
        }
        if (row != null) return row[Cycles.number]
        else throw IllegalArgumentException("Couldn't find cycle for book $bookNumber")
    }

    private fun createCycleFromRow(row: ResultRow, books: List<Book>)
        = Cycle(
            row[Cycles.number], row[Cycles.germanTitle],
            row[Cycles.englishTitle], row[Cycles.shortTitle],
            row[Cycles.start], row[Cycles.end], books)

    override fun allCycles(): CyclesDao.CyclesResponse {
        val result = arrayListOf<Cycle>()
        transaction {
            Cycles.selectAll().forEach { row ->
                val books = booksDao.findBooks(row[Cycles.start], row[Cycles.end]).books
                result.add(createCycleFromRow(row, books))
            }
        }
        return CyclesDao.CyclesResponse(result)
    }

    override fun findCycle(n: Int): Cycle? {
        var result: Cycle? = null
        transaction {
            Cycles.select{
                Cycles.number.eq(n)
            }.forEach { row ->
                val books = booksDao.findBooks(row[Cycles.start], row[Cycles.end]).books
                result = createCycleFromRow(row, books)
            }
        }
        return result
    }

}