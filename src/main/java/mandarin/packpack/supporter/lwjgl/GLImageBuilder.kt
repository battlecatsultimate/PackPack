package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeImage
import common.system.fake.ImageBuilder
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.awt.FIBI
import mandarin.packpack.supporter.lwjgl.opengl.model.SpriteSheet
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import javax.imageio.ImageIO

class GLImageBuilder : ImageBuilder<SpriteSheet>() {
    override fun build(f: File): FakeImage {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val image = AtomicReference<GLImage>(null)

            StaticStore.renderManager.queueGL {
                image.set(GLImage(SpriteSheet.build(f), false))

                waiter.countDown()
            }

            waiter.await()

            image.get()
        } else {
            GLImage(SpriteSheet.build(f), false)
        }
    }

    override fun build(sup: Supplier<InputStream>): FakeImage {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val image = AtomicReference<GLImage>(null)

            StaticStore.renderManager.queueGL {
                image.set(GLImage(SpriteSheet.build(sup.get()), true))

                waiter.countDown()
            }

            waiter.await()

            image.get()
        } else {
            GLImage(SpriteSheet.build(sup.get()), true)
        }
    }

    override fun build(o: SpriteSheet): FakeImage {
        return GLImage(o, false)
    }

    override fun build(w: Int, h: Int): FakeImage {
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)

        return FIBI.build(image)
    }

    override fun write(o: FakeImage, fmt: String, out: Any): Boolean {
        val image = when (o) {
            is FIBI -> {
                val img = o.bimg()

                if (img !is BufferedImage)
                    return false

                img
            }
            is GLImage -> {
                val pixels = o.getBuffer()

                val img = BufferedImage(o.width, o.height, BufferedImage.TYPE_INT_ARGB)

                for (x in 0 until o.width) {
                    for (y in 0 until o.height) {
                        val pixelIndex = (y * o.width + x) * 4

                        val rgb = Color(pixels[pixelIndex], pixels[pixelIndex + 1], pixels[pixelIndex + 2]).rgb
                        val alpha = (pixels[pixelIndex + 3] shl 24) or 0x00FFFFFF

                        img.setRGB(x, y, rgb and alpha)
                    }
                }

                img
            }
            else -> {
                return false
            }
        }

        when (out) {
            is File -> {
                ImageIO.write(image, fmt, out)
            }
            is OutputStream -> {
                ImageIO.write(image, fmt, out)
            }
        }

        return false
    }
}