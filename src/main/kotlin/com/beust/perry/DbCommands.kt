package com.beust.perry

import com.google.inject.Guice
import com.google.inject.Inject
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

class DbCommand @Inject constructor(private val usersDao: UsersDao) {
    fun createPassword(login: String, password: String) {
        val hp = Passwords.hashPassword(password)
        // throw if the user is not found
        usersDao.findUser(login)
        transaction {
            Users.update({ Users.login eq login}) {
                it[Users.salt] = hp.salt
                it[Users.password] = hp.hashedPassword
            }
        }
    }
}

fun main(args: Array<String>) {
    val inj = Guice.createInjector(PerryModule(), DatabaseModule(DbProviderLocal()))
    val coversDao = inj.getInstance(CoversDao::class.java)

    transaction {
        CoversTable.select {CoversTable.size greater 80000 }.forEach {
            val number = it[CoversTable.number]
            println("Shrinking cover $number")
            val byteArray = coversDao.findCover(number)
            if (byteArray != null) {
                val byteArray2 = Images.shrinkBelowSize(number, byteArray, 80000)
                coversDao.save(number, byteArray2)
            } else {
                println("Couldn't find bytes for cover $number")
            }
        }
    }

//    val dc = inj.getInstance(DbCommand::class.java)
//    dc.createPassword("", "")

//    var date = LocalDate.of(1961, 9, 7)
//
//    (1..3008).forEach {
//        if (it % 100 == 0 || it == 1) {
//            println("$it: " + publicationDate(it))
//        }
//        date = date.plusWeeks(1)
//    }

//    println("")
//    fun exists(number: Int): Boolean {
//        var found = false
//        transaction {
//            CoversTable.slice(CoversTable.number).select { CoversTable.number.eq(number)}.forEach { _ ->
//                found = true
//            }
//        }
//        return found
//    }
//
//    (922..3000).forEach { n ->
//        if (! exists(n)) {
//            logic.findCoverBytes(n)
//            println("============ Fetched cover $n")
//        }
//    }
//    transaction {
//        CoversTable.selectAll().forEach {
//            convert(it[CoversTable.number])
//        }
//    }
}
