package com.beust.perry

import org.joda.time.DateTime

data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int)

interface CyclesDao {
    class CyclesResponse(val cycles: List<Cycle>)

    fun allCycles(): CyclesResponse
    fun findCycle(n: Int): Cycle?
}

data class Book(val number: Int, val title: String, val author: String, val published: DateTime,
        val germanFile: String?)

interface BooksDao {
    class BooksResponse(val books: List<Book>)

    fun findBooks(start: Int, end: Int): BooksResponse
}


