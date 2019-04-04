package com.beust.perry

import com.codahale.metrics.Gauge
import com.google.inject.Inject
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class CoverCountMetric @Inject constructor(private val coversDao: CoversDao): Gauge<Int> {
    override fun getValue() = coversDao.count
}

class CoverSizeMetric: Gauge<String> {
    override fun getValue(): String {
        val count =
            transaction {
                CoversTable.slice(CoversTable.size).selectAll().sumBy {
                    it[CoversTable.size]
                }
            }
        return String.format("%.2f", count.toFloat() / 1_000_000) + " MB"
    }
}

class CoverCacheMetric(val start: LocalDateTime): Gauge<String> {
    private var hits = 0
    private var misses = 0

    override fun getValue(): String {
        return "Hits/Misses: $hits/$misses, since ${Dates.formatDate(start)}"
    }

    fun addHit() = hits++
    fun addMiss() = misses++
}