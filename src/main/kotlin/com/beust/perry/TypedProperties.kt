package com.beust.perry

import com.google.inject.Singleton

/**
 * Encapsulate read access to a  map of properties with optional type constraints on keys
 * and values.
 */
@Singleton
class TypedProperties(private val map: Map<String, String?>) {
    fun get(p: LocalProperty) : String? {
        val result = map[p.name]
        if (result != null && p.allowed.any() && ! p.allowed.contains(result)) {
            throw IllegalArgumentException("Illegal value for \"$p\": \"$result\", allowed values: ${p.allowed}")
        }
        return result
    }

    fun getRequired(p: LocalProperty): String
        = get(p) ?: throw IllegalArgumentException("Couldn't find required property ${p.name}")

    val database: Database?
        get() = when(get(LocalProperty.DATABASE)) {
            Database.POSTGRESQL.value -> Database.POSTGRESQL
            Database.MY_SQL.value -> Database.MY_SQL
            Database.IN_MEMORY.value -> Database.IN_MEMORY
            null -> null
            else -> throw IllegalArgumentException("Unknown datatabase type: " + get(LocalProperty.DATABASE))
        }
}

enum class Database(val value: String) {
    POSTGRESQL("postgresql"), IN_MEMORY("inMemory"), MY_SQL("mysql")
}

enum class LocalProperty(val allowed: Set<String> = hashSetOf()) {
    DATABASE(setOf(Database.POSTGRESQL.value, Database.IN_MEMORY.value, Database.MY_SQL.value)),
    DATABASE_USER,
    DATABASE_PASSWORD,
    DATABASE_URL
    ;
}
