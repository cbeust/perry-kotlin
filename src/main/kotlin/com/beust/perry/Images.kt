package com.beust.perry

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO



object Images {
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

    fun scale(img: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {

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