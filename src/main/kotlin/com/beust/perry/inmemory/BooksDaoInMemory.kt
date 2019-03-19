package com.beust.perry.inmemory

import com.beust.perry.BookFromDao
import com.beust.perry.BooksDao

class BooksDaoInMemory: BooksDao {
    override fun saveBook(book: BookFromDao) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val BOOKS = listOf(
            BookFromDao(1, "Unternehmen Stardust", "Enterprise Stardust", "Clark Darlton", null, null)
    )

    override fun count() = BOOKS.size

    override fun findBook(n: Int) = BOOKS[0]
    override fun findBooks(start: Int, end: Int) = BooksDao.BooksResponse(BOOKS)

    override fun findBooksForCycle(cycle: Int) = BooksDao.BooksResponse(BOOKS)
}
