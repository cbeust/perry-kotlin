package com.beust.perry

import org.slf4j.LoggerFactory
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO



object Images {
    private val log = LoggerFactory.getLogger(Images::class.java)

    fun fromInputStream(stream: InputStream): ByteArray {
        stream.use { ins ->
            ByteArrayOutputStream().use { out ->
                val buf = ByteArray(1024 * 20)
                var n = ins.read(buf)
                while (n != -1) {
                    out.write(buf, 0, n)
                    n = ins.read(buf)
                }
                val image = ImageIO.read(ByteArrayInputStream(out.toByteArray()))
                ByteArrayOutputStream(100000).use { baos ->
                    ImageIO.write(image, "jpg", baos)
                    return baos.toByteArray()
                }

            }
        }
    }

    fun shrinkBelowSize(number: Int, array: ByteArray, targetSize: Int): ByteArray {
        var done = array.size < targetSize

        if (done) return array

        val bais = ByteArrayInputStream(array)
        val image = ImageIO.read(bais)

        var outputImage = image
        var bos = ByteArrayOutputStream()
        while (!done) {
            val targetWidth = (outputImage.width / 1.2).toInt()
            val targetHeight = (outputImage.height / 1.2).toInt()
            outputImage = scale(outputImage, targetWidth, targetHeight)
            bos = ByteArrayOutputStream()
            ImageIO.write(outputImage, "jpg", bos)
            done = bos.size() < targetSize
        }
        fun print(image: BufferedImage, size: Int) = "${image.width}x${image.height}: $size"
        log.info("Shrunk cover for $number ${print(image, array.size)} to ${print(outputImage, bos.size())}")
        return bos.toByteArray()
    }

    private fun scale(img: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {

        val type = if (img.transparency == Transparency.OPAQUE) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        var ret = img
        var scratchImage: BufferedImage? = null
        var g2: Graphics2D? = null

        var w = img.width
        var h = img.height

        var prevW = w
        var prevH = h

        do {
            if (w > targetWidth) {
                w /= 2
                w = if (w < targetWidth) targetWidth else w
            }

            if (h > targetHeight) {
                h /= 2
                h = if (h < targetHeight) targetHeight else h
            }

            if (scratchImage == null) {
                scratchImage = BufferedImage(w, h, type)
                g2 = scratchImage.createGraphics()
            }

            g2!!.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null)

            prevW = w
            prevH = h
            ret = scratchImage
        } while (w != targetWidth || h != targetHeight)

        g2?.dispose()

        if (targetWidth != ret.width || targetHeight != ret.height) {
            scratchImage = BufferedImage(targetWidth, targetHeight, type)
            g2 = scratchImage.createGraphics()
            g2!!.drawImage(ret, 0, 0, null)
            g2.dispose()
            ret = scratchImage
        }

        return ret

    }
}