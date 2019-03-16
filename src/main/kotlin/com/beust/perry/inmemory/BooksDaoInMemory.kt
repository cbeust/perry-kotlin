package com.beust.perry.inmemory

import com.beust.perry.BookFromDao
import com.beust.perry.BooksDao

class BooksDaoInMemory: BooksDao {
    val BOOKS = listOf(
            BookFromDao(1, "Unternehmen Stardust", "Enterprise Stardust", "Clark Darlton", null, null)
    )

    override fun updateTitle(number: Int, newTitle: String) {
    }

    override fun count() = BOOKS.size

    override fun findBooks(start: Int, end: Int) = BooksDao.BooksResponse(BOOKS)

    override fun findBooksForCycle(cycle: Int) = BooksDao.BooksResponse(BOOKS)
}
