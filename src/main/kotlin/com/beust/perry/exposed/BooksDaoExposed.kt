package com.beust.perry.exposed

import com.beust.perry.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class BooksDaoExposed: BooksDao {
    private val log = LoggerFactory.getLogger(BooksDaoExposed::class.java)

    override fun saveBook(book: BookFromDao) {
        fun bookToRow(it: UpdateBuilder<Int>, book: BookFromDao) {
            it[Hefte.number] = book.number
            it[Hefte.author] = book.author
            it[Hefte.title] = book.germanTitle
            it[Hefte.germanFile] = book.germanFile
            it[Hefte.published] = book.published
        }

        val found = findBook(book.number)
        transaction {
            if (found == null) {
                Hefte.insert {
                    log.info("Inserting new book $book")
                    bookToRow(it, book)
                }
            } else {
                Hefte.update({ Hefte.number eq book.number}) {
                    log.info("Updating existing book $book")
                    bookToRow(it, book)
                }
            }
        }
    }

    private fun createBookFromRow(row: ResultRow, englishTitle: String?) =
            BookFromDao(row[Hefte.number], row[Hefte.title], englishTitle, row[Hefte.author],
                    row[Hefte.published], row[Hefte.germanFile])

    override fun count() = transaction { Hefte.selectAll().count() }

    private fun fetchBooks(closure: () -> List<BookFromDao>) : List<BookFromDao> {
        val books = transaction {
            closure()
        }
        return books
    }

    override fun findBook(number: Int): BookFromDao? {
        val book =
            transaction {
                val row = Hefte.select {
                    Hefte.number.eq(number)
                }.firstOrNull()
                if (row != null) {
                    createBookFromRow(row, null)
                } else {
                    null
                }
            }
        return book
    }

    override fun findBooks(start: Int, end: Int): List<BookFromDao> {
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

    override fun findBooksForCycle(cycle: Int): List<BookFromDao> {
        return fetchBooks {
            arrayListOf<BookFromDao>().let { result ->
                val row = Cycles.select { Cycles.number.eq(cycle) }.firstOrNull()
                if (row != null) {
                    val start = row[Cycles.start]
                    val end = row[Cycles.end]
                    result.addAll(findBooks(start, end))
                }
                return@fetchBooks result
            }
        }
    }
}