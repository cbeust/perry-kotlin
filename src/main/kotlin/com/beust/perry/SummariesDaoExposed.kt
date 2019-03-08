package com.beust.perry

import com.google.inject.Inject
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class SummariesDaoExposed @Inject constructor(private val booksDao: BooksDao) : SummariesDao {
    private fun createSummaryFromRow(row: ResultRow)
            = Summary(
            row[Summaries.number], row[Summaries.englishTitle],
            row[Summaries.authorName],
            row[Summaries.authorEmail], row[Summaries.date], row[Summaries.summary], row[Summaries.time])

    override fun findEnglishSummaries(start: Int, end: Int): SummariesDao.SummariesResponse {
        // Get summaries
        val smallSummaries = hashMapOf<Int, Summary>()
        transaction {
            Summaries.select {
                Summaries.number.greaterEq(start) and Summaries.number.lessEq(end)
            }.forEach { row ->
                smallSummaries[row[Summaries.number]] = createSummaryFromRow(row)
            }
        }

        // Get German titles
        val germanTitles = booksDao.findBooks(start, end).books

        // Merge them to create full summaries
        val result = arrayListOf<FullSummary>()

        germanTitles.withIndex().forEach { iv ->
            val book = iv.value
            val index = iv.index
            val summary = smallSummaries[iv.value.number]
            if (summary != null) {
                result.add(FullSummary(book.number, book.title, summary.englishTitle, book.author,
                        summary.authorName, summary.authorEmail, summary.date, summary.summary, summary.time))
            } else {
                throw IllegalArgumentException("Couldn't find a summary for ${iv.value.number}")
            }
        }
        return SummariesDao.SummariesResponse(result)
    }

}