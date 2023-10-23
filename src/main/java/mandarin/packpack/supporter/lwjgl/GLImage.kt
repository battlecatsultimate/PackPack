package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeGraphics
import common.system.fake.FakeImage
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lwjgl.opengl.model.SpriteSheet
import mandarin.packpack.supporter.lwjgl.opengl.model.TextureMesh
import org.lwjgl.opengl.GL33
import java.awt.Color
import java.lang.UnsupportedOperationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class GLImage : FakeImage {
    companion object {
        private val sharedRGBA = IntArray(4)
    }

    private val sprite: SpriteSheet
    private val textureMesh: TextureMesh

    private val fromBC: Boolean

    constructor(spriteSheet: SpriteSheet, fromBC: Boolean) {
        sprite = spriteSheet
        textureMesh = sprite.wholePart
        this.fromBC = fromBC
    }

    constructor(spriteSheet: SpriteSheet, child: TextureMesh, fromBC: Boolean) {
        sprite = spriteSheet
        textureMesh = child
        this.fromBC = fromBC
    }

    override fun bimg(): Any {
        return textureMesh
    }

    override fun getHeight(): Int {
        return textureMesh.height.toInt()
    }

    override fun getRGB(i: Int, j: Int): Int {


        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val rgb = AtomicReference<Int>(null)

            StaticStore.renderManager.queueGL {
                sprite.bind()

                GL33.glReadPixels(i, j, 1, 1, GL33.GL_FLOAT, GL33.GL_UNSIGNED_INT, sharedRGBA)

                rgb.set(Color(sharedRGBA[0], sharedRGBA[1], sharedRGBA[2]).rgb)

                waiter.countDown()
            }

            waiter.await()

            rgb.get()
        } else {
            sprite.bind()

            GL33.glReadPixels(i, j, 1, 1, GL33.GL_FLOAT, GL33.GL_UNSIGNED_INT, sharedRGBA)

            Color(sharedRGBA[0], sharedRGBA[1], sharedRGBA[2]).rgb
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
                image.set(GLImage(sprite, sprite.generatePart(i.toFloat(), j.toFloat(), k.toFloat(), l.toFloat()), fromBC))

                waiter.countDown()
            }

            waiter.await()

            image.get()
        } else {
            GLImage(sprite, sprite.generatePart(i.toFloat(), j.toFloat(), k.toFloat(), l.toFloat()), fromBC)
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

            sharedRGBA[0] = c.red
            sharedRGBA[1] = c.green
            sharedRGBA[2] = c.blue
            sharedRGBA[3] = c.alpha

            GL33.glTexSubImage2D(GL33.GL_TEXTURE_2D, 0, i, j, 1, 1, GL33.GL_RGBA, GL33.GL_UNSIGNED_INT, sharedRGBA)
        }
    }

    override fun unload() {
        if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            val waiter = CountDownLatch(1)

            StaticStore.renderManager.queueGL {
                sprite.release()

                waiter.countDown()
            }

            waiter.await()
        } else {
            sprite.release()
        }
    }

    override fun cloneImage(): FakeImage {
        return GLImage(sprite, textureMesh, fromBC)
    }

    override fun getGraphics(): FakeGraphics {
        throw UnsupportedOperationException("Texture can't have graphics!")
    }

    fun getBuffer() : IntArray {
        sprite.bind()

        val buffer = IntArray(textureMesh.width.toInt() * textureMesh.height.toInt() * 4)

        GL33.glReadPixels(textureMesh.offsetX.toInt(), textureMesh.offsetY.toInt(), textureMesh.width.toInt(), textureMesh.height.toInt(), GL33.GL_RGBA, GL33.GL_UNSIGNED_INT, buffer)

        return buffer
    }
}