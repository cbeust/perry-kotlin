package com.beust.perry

import com.google.inject.Guice
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


fun readJpg(number: Int) {
    transaction {
        CoversTable.select {
            CoversTable.number eq number
        }.forEach {
            val bytes = it[CoversTable.image]
            val image = ImageIO.read(ByteArrayInputStream(bytes))
        }
    }
}

fun convert(number: Int) {
    var jpgBytes: ByteArray? = null
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
    val covers = arrayListOf<Pair<Int, Int>>()

    transaction {
        CoversTable.selectAll().forEach {
            convert(it[CoversTable.number])
        }
    }

//    val number = 2004
//    var jpgBytes: ByteArray? = null
//    transaction {
//        CoversTable.select {
//            CoversTable.number eq number
//        }.forEach {
//            val bytes = it[CoversTable.image]
//            val inputImage = ImageIO.read(ByteArrayInputStream(bytes))
//            ImageIO.write(inputImage, "png", File("$number-a.png"))
//            val baos = ByteArrayOutputStream(100000)
//            ImageIO.write(inputImage, "png", baos)
//            jpgBytes = baos.toByteArray()
//        }
//    if (jpgBytes != null) {
//        transaction {
//            CoversTable.update({ CoversTable.number eq number }) {
//                it[CoversTable.size] = jpgBytes!!.size
//                it[CoversTable.image] = jpgBytes!!
//            }
//        }
//    }


//            ImageIO.write(inputImage, "jpg", baos)
//            val bais = ByteArrayInputStream(baos.toByteArray())
//            val imageFromStream = ImageIO.read(bais)
//
//            // creates output image
//            val scaledWidth = 300
//            val scaledHeight = 200
//            val outputImage = java.awt.image.BufferedImage(scaledWidth, scaledHeight,inputImage.getType())
//
//            // scales the input image to the output image
//            val g2d = outputImage.createGraphics()
//            g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null)
//            g2d.dispose()
//            val outputBytes = (outputImage.raster as ByteInterleavedRaster).dataStorage
//
////            // extracts extension of output file
////            String formatName = outputImagePath.substring(outputImagePath
////                    .lastIndexOf(".") + 1);
////
////            // writes to output file
////            ImageIO.write(outputImage, formatName, new File(outputImagePath));
////        }
////
////            val newImage = image.getScaledInstance(300, 200, java.awt.Image.SCALE_DEFAULT)
////            val bi = java.awt.image.BufferedImage().
////
////            val baos = ByteArrayOutputStream()
//            ImageIO.write(outputImage, "jpg", File("$number-b.jpg"))
////            val imageInByte = baos.toByteArray()
//
//            println("Image: " + bytes.size + " ")
//        }
//    }
}
