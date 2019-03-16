package com.beust.perry.exposed

import com.beust.perry.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class BooksDaoExposed: BooksDao {
    private fun createBookFromRow(row: ResultRow, englishTitle: String?) =
            Book(row[Hefte.number], row[Hefte.title], englishTitle, row[Hefte.author],
                    row[Hefte.published], row[Hefte.germanFile])

    override fun count() = transaction { Hefte.selectAll().count() }

    private fun fetchBooks(closure: () -> List<Book>) : BooksDao.BooksResponse {
        val books = transaction {
            closure()
        }
        return BooksDao.BooksResponse(books)
    }

    override fun findBooks(start: Int, end: Int): BooksDao.BooksResponse {
        val englishTitles = hashMapOf<Int, String>()
        transaction {
            Summaries
                .slice(Summaries.number, Summaries.englishTitle)
                .select {
                    Summaries.number.greaterEq(start) and Summaries.number.lessEq(end)
                }.forEach { row ->
                    englishTitles[row[Summaries.number]] = row[Summaries.englishTitle]
                }
        }
        return fetchBooks {
            arrayListOf<Book>().let { result ->
                Hefte.select {
                    Hefte.number.greaterEq(start) and Hefte.number.lessEq(end)
                }.forEach { row ->
                    result.add(createBookFromRow(row, englishTitles[row[Hefte.number]]))
                }
                return@fetchBooks result
            }
        }
    }

    override fun findBooksForCycle(cycle: Int): BooksDao.BooksResponse {
        return fetchBooks {
            arrayListOf<Book>().let { result ->
                val row = Cycles.select { Cycles.number.eq(cycle) }.firstOrNull()
                if (row != null) {
                    val start = row[Cycles.start]
                    val end = row[Cycles.end]
                    result.addAll(findBooks(start, end).books)
                }
                return@fetchBooks result
            }
        }
    }
}