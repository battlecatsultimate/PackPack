package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeGraphics
import common.system.fake.FakeImage
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lwjgl.opengl.model.SpriteSheet
import mandarin.packpack.supporter.lwjgl.opengl.model.TextureMesh
import org.lwjgl.opengl.GL33
import java.awt.Color
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class GLImage : FakeImage {
    private val cloneReferences = ArrayList<GLImage>()

    private var byteData: ByteArray? = null

    private var sprite: SpriteSheet
    private var textureMesh: TextureMesh
    private var reference: GLImage?

    constructor(spriteSheet: SpriteSheet) {
        sprite = spriteSheet
        textureMesh = sprite.wholePart
        reference = null
    }

    constructor(spriteSheet: SpriteSheet, child: TextureMesh) {
        sprite = spriteSheet
        textureMesh = child
        reference = null
    }

    private constructor(spriteSheet: SpriteSheet, child: TextureMesh, cloned: GLImage?) {
        sprite = spriteSheet
        textureMesh = child
        this.reference = cloned
    }

    override fun bimg(): Any {
        return textureMesh
    }

    override fun getHeight(): Int {
        return textureMesh.height.toInt()
    }

    override fun getRGB(i: Int, j: Int): Int {
        val data = byteData

        if (data != null) {
            val r = data[(i + j * width) * 4] and 0xFF
            val g = data[(i + j * width) * 4 + 1] and 0xFF
            val b = data[(i + j * width) * 4 + 2] and 0xFF
            val a = data[(i + j * width) * 4 + 3] and 0xFF

            return (a shl 24) + (r shl 16) + (g shl 8) + b
        }

        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val rgb = AtomicReference<Int>(null)

            StaticStore.renderManager.queueGL {
                sprite.bind()

                val buffer = ByteBuffer.allocateDirect((sprite.width * sprite.height * 4).toInt())

                GL33.glGetTexImage(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buffer)

                byteData = ByteArray((sprite.width * sprite.height * 4).toInt())

                buffer.put(byteData)

                val colorData = byteData

                if (colorData == null) {
                    rgb.set(0)

                    waiter.countDown()

                    return@queueGL
                }

                val r = colorData[(i + j * width) * 4] and 0xFF
                val g = colorData[(i + j * width) * 4 + 1] and 0xFF
                val b = colorData[(i + j * width) * 4 + 2] and 0xFF
                val a = colorData[(i + j * width) * 4 + 3] and 0xFF

                rgb.set((a shl 24) + (r shl 16) + (g shl 8) + b)

                waiter.countDown()
            }

            waiter.await()

            rgb.get()
        } else {
            sprite.bind()

            val buffer = ByteBuffer.allocateDirect((sprite.width * sprite.height * 4).toInt())

            GL33.glGetTexImage(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buffer)

            byteData = ByteArray((sprite.width * sprite.height * 4).toInt())

            buffer.put(byteData)

            val colorData = byteData ?: return 0

            val r = colorData[(i + j * width) * 4] and 0xFF
            val g = colorData[(i + j * width) * 4 + 1] and 0xFF
            val b = colorData[(i + j * width) * 4 + 2] and 0xFF
            val a = colorData[(i + j * width) * 4 + 3] and 0xFF

            (a shl 24) + (r shl 16) + (g shl 8) + b
        }
    }

    override fun getSubimage(i: Int, j: Int, k: Int, l: Int): FakeImage {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val image = AtomicReference<GLImage>(null)

            StaticStore.renderManager.queueGL {
                image.set(GLImage(sprite, sprite.generatePart(i.toFloat(), j.toFloat(), k.toFloat(), l.toFloat())))

                waiter.countDown()
            }

            waiter.await()

            image.get()
        } else {
            GLImage(sprite, sprite.generatePart(i.toFloat(), j.toFloat(), k.toFloat(), l.toFloat()))
        }

    }

    override fun getWidth(): Int {
        return textureMesh.width.toInt()
    }

    override fun gl(): Any? {
        return null
    }

    override fun isValid(): Boolean {
        return !sprite.released
    }

    override fun setRGB(i: Int, j: Int, p: Int) {
        StaticStore.renderManager.queueGL {
            sprite.bind()

            val c = Color(p)

            GL33.glTexSubImage2D(GL33.GL_TEXTURE_2D, 0, i, j, 1, 1, GL33.GL_RGBA, GL33.GL_UNSIGNED_INT, intArrayOf(c.red, c.green, c.blue, c.alpha))
        }
    }

    override fun unload() {
        if (reference != null)
            return

        if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            val waiter = CountDownLatch(1)

            StaticStore.renderManager.queueGL {
                if (cloneReferences.isNotEmpty()) {
                    cloneReferences.forEach { ref ->
                        ref.changeReferenceTo(hardClone())
                    }
                }

                sprite.release()

                waiter.countDown()
            }

            waiter.await()
        } else {
            if (cloneReferences.isNotEmpty()) {
                cloneReferences.forEach { ref ->
                    ref.changeReferenceTo(hardClone())
                }
            }

            sprite.release()
        }
    }

    override fun cloneImage(): FakeImage {
        val img = if (reference != null)
            GLImage(sprite, textureMesh, reference)
        else
            GLImage(sprite, textureMesh, this)

        if (reference != null)
            reference?.cloneReferences?.add(img)
        else
            cloneReferences.add(img)

        return img
    }

    private fun hardClone() : GLImage {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val img = AtomicReference<GLImage>(null)

            StaticStore.renderManager.queueGL {
                if (textureMesh === sprite.wholePart) {
                    img.set(GLImage(sprite.clone()))
                } else {
                    img.set(GLImage(sprite.clone()).getSubimage(textureMesh.offsetX.toInt(), textureMesh.offsetY.toInt(), textureMesh.width.toInt(), textureMesh.height.toInt()) as GLImage)
                }

                waiter.countDown()
            }

            waiter.await()

            img.get()
        } else {
            if (textureMesh === sprite.wholePart) {
                GLImage(sprite.clone())
            } else {
                GLImage(sprite.clone()).getSubimage(textureMesh.offsetX.toInt(), textureMesh.offsetY.toInt(), textureMesh.width.toInt(), textureMesh.height.toInt()) as GLImage
            }
        }
    }

    private fun changeReferenceTo(other: GLImage) {
        sprite = other.sprite
        textureMesh = other.textureMesh
        reference = null
    }

    override fun getGraphics(): FakeGraphics {
        throw UnsupportedOperationException("Texture can't have graphics!")
    }

    fun getBuffer() : IntArray {
        sprite.bind()

        val buffer = IntArray(textureMesh.width.toInt() * textureMesh.height.toInt() * 4)

        GL33.glGetTexImage(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_INT, buffer)

        return buffer
    }

    private infix fun Byte.and(mask: Int): Int = toInt() and mask
}