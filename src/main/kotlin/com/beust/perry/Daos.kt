package com.beust.perry

import org.joda.time.DateTime

data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int, val books: List<Book>)

interface CyclesDao {
    class CyclesResponse(val cycles: List<Cycle>)

    fun allCycles(): CyclesResponse
    fun findCycle(n: Int): Cycle?
}

data class Book(val number: Int, val germanTitle: String, val englishTitle: String?, val author: String,
        val published: DateTime?, val germanFile: String?)

interface BooksDao {
    class BooksResponse(val books: List<Book>)

    fun findBooks(start: Int, end: Int): BooksResponse
    fun findBooksForCycle(cycle: Int): BooksResponse
}

data class Summary(val number: Int, val englishTitle: String, val authorName: String, val authorEmail: String,
        val date: String, val summary: String, val time: String?)

/** A summary with both English and German titles */
data class FullSummary(val number: Int, val germanTitle: String, val englishTitle: String,
        val bookAuthor: String,
        val authorName: String, val authorEmail: String,
        val date: String, val summary: String, val time: String?)

interface SummariesDao {
    class SummariesResponse(val summaries: List<FullSummary>)

    fun findEnglishSummaries(start: Int, end: Int): SummariesResponse
}
