package com.beust.perry

enum class Database(val value: String) {
    POSTGRESQL("postgresql"), IN_MEMORY("inMemory"), MY_SQL("mysql")
}
