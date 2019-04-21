package com.beust.perry

import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import com.google.inject.Singleton
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime

@Singleton
class CoverCountMetric @Inject constructor(private val coversDao: CoversDao): Gauge<Int> {
    override fun getValue() = coversDao.count
}

@Singleton
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

@Singleton
class CoverCacheMetric(val start: LocalDateTime): Gauge<String> {
    private var hits = 0
    private var misses = 0

    override fun getValue(): String {
        val duration = Duration.between(start, LocalDateTime.now())
        val days = duration.toDays()
        val hours = duration.toHours()
        val minutes = duration.toMinutes()
        val text = when {
            days > 0L -> "$days days"
            hours > 0L -> "$hours hours"
            else -> "$minutes minutes"
        }

        return "Hits/Misses: $hits/$misses, $text ago. Last restart: " + Dates.formatDate(start)
    }

    fun addHit() = hits++
    fun addMiss() = misses++
}

class PerryMetrics @Inject constructor(private val registry: MetricRegistry,
        private val coverCount: CoverCountMetric, private val coverSize: CoverSizeMetric,
        private val coverCache: CoverCacheMetric)
{
    enum class Counter(val counterName: String) {
        SUMMARIES_PAGE_HTML("summariesPageHtml"),
        SUMMARIES_PAGE_API("summariesPageApi"),
        ROOT_PAGE("rootPage"),
        CYCLES_PAGE_HTML("cyclesPageHtml"),
        CYCLES_PAGE_API("cyclesPageApi")
    }

    fun registerMetrics() {
        registry.apply {
            register("coverCount", coverCount)
            register("coverSize", coverSize)
            register("coverCache", coverCache)
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