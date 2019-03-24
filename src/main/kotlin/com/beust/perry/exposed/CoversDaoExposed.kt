package com.beust.perry.exposed

import com.beust.perry.CoversDao
import com.beust.perry.CoversTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

class CoversDaoExposed : CoversDao {
    private val log = LoggerFactory.getLogger(CoversDaoExposed::class.java)

    override val count: Int
        get() = transaction { CoversTable.selectAll().count() }

    override fun save(number: Int, coverImageBytes: ByteArray) {
        val found = findCover(number)
        if (found == null) {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            transaction {
                CoversTable.insert {
                    log.info("Inserting new cover for $number")
                    it[CoversTable.number] = number
                    it[CoversTable.image] = coverImageBytes
                    it[CoversTable.size] = coverImageBytes.size
                }
            }
        } else {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            transaction {
                CoversTable.update({ CoversTable.number eq CoversTable.number }) {
                    log.info("Updating existing cover $number")
                    it[CoversTable.number] = number
                    it[CoversTable.image] = coverImageBytes
                    it[CoversTable.size] = coverImageBytes.size
                }
            }
        }

    }

    override fun findCover(number: Int): ByteArray? {
        var result: ByteArray? = null
        transaction {
            CoversTable.select { CoversTable.number eq number }.forEach {
                val bytes = it[CoversTable.image]
                result = bytes
            }
        }
        return result
    }
}
