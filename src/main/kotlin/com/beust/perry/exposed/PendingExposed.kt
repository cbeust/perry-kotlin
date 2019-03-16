package com.beust.perry.exposed

import com.beust.perry.PendingDao
import com.beust.perry.PendingSummaries
import com.beust.perry.PendingSummaryFromDao
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction

class PendingDaoExposed: PendingDao {
    fun summaryToRow(it: UpdateBuilder<Int>, summary: PendingSummaryFromDao) {
        it[PendingSummaries.number] = summary.number
        it[PendingSummaries.germanTitle] = summary.germanTitle
        it[PendingSummaries.englishTitle] = summary.englishTitle
        it[PendingSummaries.authorName] = summary.authorName
        it[PendingSummaries.authorEmail] = summary.authorEmail
        it[PendingSummaries.summary] = summary.summary
        it[PendingSummaries.dateSummary] = summary.dateSummary
    }

    override fun saveSummary(summary: PendingSummaryFromDao) {
        transaction {
            PendingSummaries.insert { summaryToRow(it, summary) }
        }
    }
}
