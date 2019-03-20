package com.beust.perry.exposed

import com.beust.perry.PendingDao
import com.beust.perry.PendingSummaries
import com.beust.perry.PendingSummaryFromDao
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction

class PendingDaoExposed: PendingDao {
    override fun deletePending(id: Int) {
        transaction {
            PendingSummaries.deleteWhere { PendingSummaries.id eq id}
        }
    }

    override fun findPending(id: Int): PendingSummaryFromDao? {
        var result: PendingSummaryFromDao? = null
        transaction {
            PendingSummaries.select { PendingSummaries.id.eq(id) }.forEach {
                result = PendingSummaryFromDao(it[PendingSummaries.number],
                        it[PendingSummaries.germanTitle], it[PendingSummaries.bookAuthor],
                        it[PendingSummaries.englishTitle], it[PendingSummaries.authorName],
                        it[PendingSummaries.authorEmail], it[PendingSummaries.summary],
                        it[PendingSummaries.dateSummary])
            }
        }
        return result
    }

    private fun summaryToRow(it: UpdateBuilder<Int>, summary: PendingSummaryFromDao) {
        it[PendingSummaries.number] = summary.number
        it[PendingSummaries.germanTitle] = summary.germanTitle
        it[PendingSummaries.bookAuthor] = summary.bookAuthor
        it[PendingSummaries.englishTitle] = summary.englishTitle
        it[PendingSummaries.authorName] = summary.authorName
        it[PendingSummaries.authorEmail] = summary.authorEmail
        it[PendingSummaries.summary] = summary.text
        it[PendingSummaries.dateSummary] = summary.dateSummary
    }

    override fun saveSummary(summary: PendingSummaryFromDao): Int {
        val result = transaction {
            PendingSummaries.insert { summaryToRow(it, summary) }
        }
        return result[PendingSummaries.id]!!
    }
}
