package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeGraphics
import common.system.fake.FakeImage
import mandarin.packpack.supporter.Pauser
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.opengl.model.SpriteSheet
import mandarin.packpack.supporter.opengl.model.TextureMesh
import org.lwjgl.opengl.GL33
import java.awt.Color
import java.lang.UnsupportedOperationException
import java.util.concurrent.atomic.AtomicReference

class GLImage : FakeImage {
    companion object {
        private val sharedRGBA = IntArray(4)
    }

    private val sprite: SpriteSheet
    private val textureMesh: TextureMesh

    constructor(spriteSheet: SpriteSheet) {
        sprite = spriteSheet
        textureMesh = sprite.wholePart
    }

    constructor(spriteSheet: SpriteSheet, child: TextureMesh) {
        sprite = spriteSheet
        textureMesh = child
    }

    override fun bimg(): Any {
        return textureMesh
    }

    override fun getHeight(): Int {
        return textureMesh.height.toInt()
    }

    override fun getRGB(i: Int, j: Int): Int {
        val rgb = AtomicReference<Int>(null)
        val pauser = Pauser()

        StaticStore.renderManager.queueGL {
            sprite.bind()

            GL33.glReadPixels(i, j, 1, 1, GL33.GL_FLOAT, GL33.GL_UNSIGNED_INT, sharedRGBA)

            rgb.set(Color(sharedRGBA[0], sharedRGBA[1], sharedRGBA[2]).rgb)

            pauser.resume()
        }

        pauser.pause()

        return rgb.get()
    }

    override fun getSubimage(i: Int, j: Int, k: Int, l: Int): FakeImage {
        val image = AtomicReference<GLImage>(null)
        val pauser = Pauser()

        StaticStore.renderManager.queueGL {
            image.set(GLImage(sprite, sprite.generatePart(i.toFloat(), j.toFloat(), k.toFloat(), l.toFloat())))

            pauser.resume()
        }

        pauser.pause()

        return image.get()
    }

    override fun getWidth(): Int {
        return textureMesh.width.toInt()
    }

    override fun gl(): Any? {
        return null
    }

    override fun isValid(): Boolean {
        return true
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

    }

    override fun cloneImage(): FakeImage {
        return GLImage(sprite, textureMesh)
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