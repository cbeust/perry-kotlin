package com.beust.perry.exposed

import com.beust.perry.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.URI
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

class SummariesDaoExposed: SummariesDao {

    override fun count() = transaction { Summaries.selectAll().count() }

    private val log = LoggerFactory.getLogger(SummariesDaoExposed::class.java)

    override fun findRecentSummaries(): List<ShortSummary> {
        val result = arrayListOf<ShortSummary>()
        transaction {
            Summaries
                .slice(Summaries.number, Summaries.englishTitle, Summaries.date)
                .select { Summaries.date.isNotNull() }
                .orderBy(Pair(Summaries.date, SortOrder.DESC)).limit(5).forEach { row ->
                    result.add(ShortSummary(row[Summaries.number], row[Summaries.englishTitle], row[Summaries.date]!!))
                }

        }
        return result
    }

    override fun findEnglishSummaries(start: Int, end: Int, user: User?): List<SummaryFromDao> {
        val result = arrayListOf<SummaryFromDao>()

        transaction {
            (Hefte crossJoin Summaries)
                .slice(Hefte.number, Hefte.title, Summaries.englishTitle, Hefte.author, Summaries.authorName,
                        Summaries.authorEmail, Summaries.date, Summaries.summary, Summaries.time)
                .select { Summaries.number eq Hefte.number and Summaries.number.greaterEq(start) and
                        Summaries.number.lessEq(end)}
                .forEach { row ->
                    val bookNumber = row[Hefte.number]
                    val result = result.add(SummaryFromDao(bookNumber,
                            row[Summaries.englishTitle],
                            row[Summaries.authorName],
                            row[Summaries.authorEmail],
                            row[Summaries.date],
                            row[Summaries.summary],
                            row[Summaries.time]))
                }
            }
        result.sortBy { it.number }
        return result
    }

    override fun saveSummary(summary: SummaryFromDao): Response {
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
            transaction {
                val foundSummary = findEnglishSummary(summary.number)
                if (foundSummary == null) {
                    Summaries.insert {
                        log.info("Inserting new summary ${summary.number}")
                        it[number] = summary.number
                        summaryToRow(it, summary)
                    }
                } else {
                    log.info("Updating existing summary ${summary.number}")
                    Summaries.update({ Summaries.number eq summary.number }) {
                        summaryToRow(it, summary)
                    }
                }
            }

            return Response.seeOther(URI(Urls.summaries(summary.number))).build()
        } catch(ex: Exception) {
            throw WebApplicationException("Couldn't update summary", ex)
        }
    }

}