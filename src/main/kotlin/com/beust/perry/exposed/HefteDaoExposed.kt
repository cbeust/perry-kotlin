package com.beust.perry.exposed

import com.beust.perry.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class BooksDaoExposed: BooksDao {
    override fun updateTitle(number: Int, newTitle: String) {
        transaction {
            Hefte.update({ Hefte.number eq number }) {
                it[title] = newTitle
            }
        }
    }

    private fun createBookFromRow(row: ResultRow, englishTitle: String?) =
            BookFromDao(row[Hefte.number], row[Hefte.title], englishTitle, row[Hefte.author],
                    row[Hefte.published], row[Hefte.germanFile])

    override fun count() = transaction { Hefte.selectAll().count() }

    private fun fetchBooks(closure: () -> List<BookFromDao>) : BooksDao.BooksResponse {
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
            arrayListOf<BookFromDao>().let { result ->
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
            arrayListOf<BookFromDao>().let { result ->
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