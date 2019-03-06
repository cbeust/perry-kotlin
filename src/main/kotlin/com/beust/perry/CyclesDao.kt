package com.beust.perry

data class Cycle(val number: Int, val germanTitle: String, val englishTitle: String,
        val shortTitle: String, val start: Int, val end: Int)

interface CyclesDao {
    class CyclesResponse(val cycles: List<Cycle>)

    fun allCycles(): CyclesResponse
}

