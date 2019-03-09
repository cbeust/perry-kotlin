package com.beust.perry

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class SummariesDaoExposed: SummariesDao {
    override fun findEnglishSummaries(start: Int, end: Int): SummariesDao.SummariesResponse {
        val result = arrayListOf<FullSummary>()


        transaction {
            (Hefte crossJoin Summaries)
                .slice(Hefte.number, Hefte.title, Summaries.englishTitle, Hefte.author, Summaries.authorName,
                        Summaries.authorEmail, Summaries.date, Summaries.summary, Summaries.time)
                .select { Summaries.number eq Hefte.number and Summaries.number.greaterEq(start) and
                        Summaries.number.lessEq(end)}
                .forEach { row ->
                    result.add(FullSummary(row[Hefte.number], row[Hefte.title], row[Summaries.englishTitle],
                            row[Hefte.author], row[Summaries.authorName], row[Summaries.authorEmail],
                            row[Summaries.date], row[Summaries.summary], row[Summaries.time]))
                }
        }

        return SummariesDao.SummariesResponse(result)
    }
}