package com.beust.perry

import com.codahale.metrics.Gauge
import com.google.inject.Inject
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class CoverCountMetric @Inject constructor(private val coversDao: CoversDao): Gauge<Int> {
    override fun getValue() = coversDao.count
}

class CoverSizeMetric: Gauge<String> {
    override fun getValue(): String {
        var count = 0.0
        transaction {
            CoversTable.slice(CoversTable.image).selectAll().forEach {
                count += it[CoversTable.image].size
            }
        }
        return (count.toFloat() / 1_000_000).toString() + " MB"
    }
}

