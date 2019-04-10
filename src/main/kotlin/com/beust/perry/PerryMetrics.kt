package com.beust.perry

import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import com.google.inject.Injector
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDate
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
        val days = Duration.between(LocalDate.now().atStartOfDay(), start.toLocalDate().atStartOfDay()).toDays()
        return "Hits/Misses: $hits/$misses, $days days ago"
    }

    fun addHit() = hits++
    fun addMiss() = misses++
}

class PerryMetrics @Inject constructor(private val registry: MetricRegistry){
    enum class Counter(val counterName: String) {
        SUMMARIES_PAGE_HTML("summariesPageHtml"),
        SUMMARIES_PAGE_API("summariesPageApi"),
        ROOT_PAGE("rootPage"),
        CYCLES_PAGE_HTML("cyclesPageHtml"),
        CYCLES_PAGE_API("cyclesPageApi")
    }

    fun registerMetrics(injector: Injector) {
        registry.apply {
            listOf("coverCount" to CoverCountMetric::class.java,
                    "coverSize" to CoverSizeMetric::class.java,
                    "coverCache" to CoverCacheMetric::class.java).forEach {
                register(it.first, injector.getInstance(it.second))
            }
            Counter.values().forEach {
                register(it.counterName, Counter())

            }
        }
    }

    private fun increment(counter: Counter) = registry.counter(counter.counterName).inc()

    fun incrementRootPage() = increment(Counter.ROOT_PAGE)
    fun incrementCyclesPageHtml() = increment(Counter.CYCLES_PAGE_HTML)
    fun incrementCyclesPageApi() = increment(Counter.CYCLES_PAGE_API)
    fun incrementSummariesPageHtml() = increment(Counter.SUMMARIES_PAGE_HTML)
    fun incrementSummariesPageApi() = increment(Counter.SUMMARIES_PAGE_API)
}