package com.beust.perry

class BooksDaoInMemory: BooksDao {
    val BOOKS = listOf(
            Book(1, "Unternehmen Stardust", "Enterprise Stardust", "Clark Darlton", null, null)
    )

    override fun findBooks(start: Int, end: Int) = BooksDao.BooksResponse(BOOKS)

    override fun findBooksForCycle(cycle: Int) = BooksDao.BooksResponse(BOOKS)
}
