package com.beust.perry

class CyclesDaoInMemory: CyclesDao {
    override fun cycleForBook(bookNumber: Int): Int {
        return 0
    }

    private val cycles = listOf(
            CycleFromDao(1, "Die dritte machte", "The third power", "third-power", 0, 50),
            CycleFromDao(2, "Atlan und Arkon", "Atlan and Arkonis", "atlan", 50, 9)
    )

    override fun allCycles() = cycles

    override fun findCycle(n: Int) = if (n > 0 && n < cycles.size) cycles[n - 1] else null
}
