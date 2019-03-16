package com.beust.perry.inmemory

import com.beust.perry.Book
import com.beust.perry.BooksDao

class BooksDaoInMemory: BooksDao {
    val BOOKS = listOf(
            Book(1, "Unternehmen Stardust", "Enterprise Stardust", "Clark Darlton", null, null)
    )

    override fun count() = BOOKS.size

    override fun findBooks(start: Int, end: Int) = BooksDao.BooksResponse(BOOKS)

    override fun findBooksForCycle(cycle: Int) = BooksDao.BooksResponse(BOOKS)
}
