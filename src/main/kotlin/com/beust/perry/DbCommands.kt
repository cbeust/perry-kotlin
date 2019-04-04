package com.beust.perry

import com.google.inject.Guice
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun convert(number: Int) {
    var jpgBytes: ByteArray?
    transaction {
        CoversTable.select {
            CoversTable.number eq number
        }.forEach {
            val bytes = it[CoversTable.image]
            val inputImage = ImageIO.read(ByteArrayInputStream(bytes))
            if (inputImage != null) {
                val baos = ByteArrayOutputStream(100000)
                ImageIO.write(inputImage, "jpg", baos)
                jpgBytes = baos.toByteArray()
                if (jpgBytes != null) {
                    println("Converting $number, size before: ${bytes.size}, after: ${jpgBytes!!.size}")
                    transaction {
                        CoversTable.update({ CoversTable.number eq number }) { row ->
                            row[CoversTable.size] = jpgBytes!!.size
                            row[CoversTable.image] = jpgBytes!!
                        }
                    }
                }
            } else {
                println("Bogus cover: $number")
            }
        }
    }
}

fun main(args: Array<String>) {
    val inj = Guice.createInjector(PerryModule(), DatabaseModule(DbProviderLocalToProduction()))
    val coversDao = inj.getInstance(CoversDao::class.java)
    val logic = inj.getInstance(PresentationLogic::class.java)

//    var date = LocalDate.of(1961, 9, 7)
//
//    (1..3008).forEach {
//        if (it % 100 == 0 || it == 1) {
//            println("$it: " + publicationDate(it))
//        }
//        date = date.plusWeeks(1)
//    }

    println("")
    fun exists(number: Int): Boolean {
        var found = false
        transaction {
            CoversTable.slice(CoversTable.number).select { CoversTable.number.eq(number)}.forEach { _ ->
                found = true
            }
        }
        return found
    }

    (922..3000).forEach { n ->
        if (! exists(n)) {
            logic.findCoverBytes(n)
            println("============ Fetched cover $n")
        }
    }
//    transaction {
//        CoversTable.selectAll().forEach {
//            convert(it[CoversTable.number])
//        }
//    }
}
