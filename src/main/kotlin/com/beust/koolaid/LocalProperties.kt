package com.beust.koolaid

import com.google.inject.Singleton
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Encapsulate read access to local.properties.
 */
@Singleton
class LocalProperties {
    private val localProperties: Properties by lazy {
        Properties().apply {
            Paths.get("local.properties").let { path ->
                if (path.toFile().exists()) {
                    Files.newInputStream(path).use {
                        load(it)
                    }
                }
            }
        }
    }

    fun get(p: LocalProperty) : String? {
        val result = localProperties.getProperty(p.name)
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
