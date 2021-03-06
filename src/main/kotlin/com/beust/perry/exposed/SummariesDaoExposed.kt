package com.beust.perry.exposed

import com.beust.perry.*
import com.google.inject.Inject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.ws.rs.WebApplicationException

class SummariesDaoExposed @Inject constructor(private val covers: Covers): SummariesDao {
    private val log = LoggerFactory.getLogger(SummariesDaoExposed::class.java)

    override fun findEnglishTitles(start: Int, end: Int): Map<Int, String> {
        return transaction {
            Summaries
                    .slice(Summaries.number, Summaries.englishTitle)
                    .select { Summaries.number.greaterEq(start) and Summaries.number.lessEq(end)}
                    .map { row ->
                        row[Summaries.number] to row[Summaries.englishTitle]
                    }.toMap()
        }
    }

    override val count get() = transaction { Summaries.selectAll().count() }

    override fun findRecentSummaries(count: Int): List<ShortSummaryDao> {
        val result = arrayListOf<ShortSummaryDao>()
        transaction {
            Summaries
                    .slice(Summaries.number, Summaries.englishTitle, Summaries.date)
                    .select { Summaries.date.isNotNull() }
                    .orderBy(Pair(Summaries.date, SortOrder.DESC)).limit(count).forEach { row ->
                        val number = row[Summaries.number]
                        result.add(ShortSummaryDao(number, row[Summaries.englishTitle],
                                row[Summaries.date]!!, covers.findCoverFor(number)))
                    }

        }
        return result
    }

    override fun findEnglishSummaries(start: Int, end: Int, user: User?): List<SummaryFromDao> {
        val result = transaction {
            (Hefte crossJoin Summaries)
                    .slice(Hefte.number, Hefte.title, Summaries.englishTitle, Hefte.author, Summaries.authorName,
                            Summaries.authorEmail, Summaries.date, Summaries.summary, Summaries.time)
                    .select {
                        Summaries.number eq Hefte.number and Summaries.number.greaterEq(start) and
                                Summaries.number.lessEq(end)
                    }
                    .map { row ->
                        val bookNumber = row[Hefte.number]
                        SummaryFromDao(bookNumber,
                                row[Summaries.englishTitle],
                                row[Summaries.authorName],
                                row[Summaries.authorEmail],
                                row[Summaries.date],
                                row[Summaries.summary],
                                row[Summaries.time])
                    }
                    .sortedBy { it.number }
        }
        return result
    }

    override fun saveSummary(summary: SummaryFromDao): Boolean {
        fun summaryToRow(it: UpdateBuilder<Int>, summary: SummaryFromDao) {
            it[Summaries.englishTitle] = summary.englishTitle
            it[Summaries.authorName] = summary.authorName
            it[Summaries.authorEmail] = summary.authorEmail
            it[Summaries.date] = summary.date
            it[Summaries.summary] = summary.text
            it[Summaries.time] = summary.time
        }

        try {
            //
            // Update the summary
            //
            var isNew = false
            transaction {
                val foundSummary = findEnglishSummary(summary.number)
                if (foundSummary == null) {
                    log.info("Inserting new summary ${summary.number}")
                    @Suppress("IMPLICIT_CAST_TO_ANY")
                    Summaries.insert {
                        it[number] = summary.number
                        summaryToRow(it, summary)
                    }
                    isNew = true
                } else {
                    log.info("Updating existing summary ${summary.number}")
                    isNew = foundSummary.text.trim().isEmpty() && summary.text.trim().isNotEmpty()
                    @Suppress("IMPLICIT_CAST_TO_ANY")
                    Summaries.update({ Summaries.number eq summary.number }) {
                        summaryToRow(it, summary)
                    }
                }
            }

            return isNew
        } catch(ex: Exception) {
            throw WebApplicationException("Couldn't update summary", ex)
        }
    }

}
