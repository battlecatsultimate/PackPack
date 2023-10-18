package mandarin.packpack.supporter.opengl

import okhttp3.internal.and
import org.lwjgl.opengl.GL33
import org.lwjgl.stb.STBImage
import org.lwjgl.util.freetype.FT_Bitmap
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class Texture private constructor(val textureID: Int, val width: Float, val height: Float) {
    companion object {
        private val registeredTexture = ArrayList<Texture>()

        fun build(file: File) : Texture {
            val w = IntArray(1)
            val h = IntArray(1)

            val channels = IntArray(1)

            val buffer = STBImage.stbi_load(file.absolutePath, w, h, channels, 4) ?: throw IOException("Failed to load texture : ${file.absolutePath}")

            val width = w[0]
            val height = h[0]

            for (c in 0 until width * height) {
                val r = buffer.get(c * 4) and 0xFF
                val g = buffer.get(c * 4 + 1) and 0xFF
                val b = buffer.get(c * 4 + 2) and 0xFF
                val a = buffer.get(c * 4 + 3) and 0xFF

                buffer.put(c * 4, (r * a / 255).toByte())
                buffer.put(c * 4 + 1, (g * a / 255).toByte())
                buffer.put(c * 4 + 2, (b * a / 255).toByte())
            }

            val id = GL33.glGenTextures()

            GL33.glBindTexture(GL33.GL_TEXTURE_2D, id)

            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_BORDER)
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_BORDER)

            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST_MIPMAP_LINEAR)
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_LINEAR)

            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, width, height, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buffer)

            GL33.glGenerateMipmap(GL33.GL_TEXTURE_2D)

            STBImage.stbi_image_free(buffer)

            val texture = Texture(id, width.toFloat(), height.toFloat())

            registeredTexture.add(texture)

            return texture
        }

        fun build(stream: InputStream) : Texture {
            val image = ImageIO.read(stream)

            val pixels = IntArray(image.width * image.height)

            image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)

            val buffer = ByteBuffer.allocateDirect(image.width * image.height * 4)

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val color = pixels[y * image.width + x]

                    val alpha = (color shr 24) and 0xFF

                    val red = ((color shr 16) and 0xFF) * alpha / 255f
                    val green = ((color shr 8) and 0xFF) * alpha / 255f
                    val blue = (color and 0xFF) * alpha / 255f

                    buffer.put(red.toInt().toByte())
                    buffer.put(green.toInt().toByte())
                    buffer.put(blue.toInt().toByte())
                    buffer.put(alpha.toByte())
                }
            }

            buffer.flip()

            val id = GL33.glGenTextures()

            GL33.glBindTexture(GL33.GL_TEXTURE_2D, id)

            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_BORDER)
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_BORDER)

            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_LINEAR)
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_LINEAR)

            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, image.width, image.height, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buffer)

            val texture = Texture(id, image.width.toFloat(), image.height.toFloat())

            registeredTexture.add(texture)

            return texture
        }

        fun build(bitmap: FT_Bitmap) : Texture {
            GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, 1)

            val id = GL33.glGenTextures()

            GL33.glBindTexture(GL33.GL_TEXTURE_2D, id)

            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_BORDER)
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_BORDER)

            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST_MIPMAP_LINEAR)
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_LINEAR)

            GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RED, bitmap.width(), bitmap.rows(), 0, GL33.GL_RED, GL33.GL_UNSIGNED_BYTE, bitmap.buffer(1))

            GL33.glGenerateMipmap(GL33.GL_TEXTURE_2D)

            val texture = Texture(id, bitmap.width().toFloat(), bitmap.rows().toFloat())

            registeredTexture.add(texture)

            return texture
        }

        fun findBoundTexture() : Texture? {
            return registeredTexture.find { t -> t.bound }
        }
    }

    var bound = false

    fun bind() {
        if (bound)
            return

        findBoundTexture()?.bound = false

        GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureID)

        bound = true
    }

    fun release() {
        GL33.glDeleteTextures(textureID)

        registeredTexture.remove(this)
    }
}