package com.beust.perry

class CyclesDaoInMemory: CyclesDao {
    override fun cycleForBook(bookNumber: Int): Int {
        return 0
    }

    private val cycles = listOf(
            Cycle(1, "Die dritte machte", "The third power", "third-power", 0, 50, emptyList()),
            Cycle(2, "Atlan und Arkon", "Atlan and Arkonis", "atlan", 50, 99, emptyList())
    )

    override fun allCycles() = cycles

    override fun findCycle(n: Int) = if (n > 0 && n < cycles.size) cycles[n - 1] else null
}
