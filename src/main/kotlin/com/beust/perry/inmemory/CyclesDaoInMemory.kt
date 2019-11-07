package com.beust.perry.inmemory

import com.beust.perry.CycleFromDao
import com.beust.perry.CyclesDao
import com.beust.perry.DaoResult

class CyclesDaoInMemory: CyclesDao {
    override fun updateCycleName(cycleNumber: Int, cycleName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cycleForBook(bookNumber: Int): Int {
        return 0
    }

    private val cycles = listOf(
            CycleFromDao(1, "Die dritte machte", "The third power", "third-power", 0, 50),
            CycleFromDao(2, "Atlan und Arkon", "Atlan and Arkonis", "atlan", 50, 9)
    )

    override fun allCycles() = cycles

    override fun findCycle(n: Int) = DaoResult(true, cycles[n - 1])
}
