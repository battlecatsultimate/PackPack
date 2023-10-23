package mandarin.packpack.supporter.lwjgl.opengl.model

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.lwjgl.opengl.Texture
import mandarin.packpack.supporter.lwjgl.opengl.buffer.VAO
import mandarin.packpack.supporter.lwjgl.opengl.buffer.VBO
import org.lwjgl.opengl.GL33
import java.io.File
import java.io.InputStream

class SpriteSheet private constructor(private val parentTexture: Texture) : Model() {
    companion object {
        const val VERTEX_SIZE = 4 * Float.SIZE_BYTES
        const val INDEX_SIZE = 6
        const val DIMENSION = 2

        fun build(file: File) : SpriteSheet {
            Logger.addLog("Generating SpriteSheet from file : ${file.absolutePath}")

            return SpriteSheet(Texture.build(file))
        }

        fun build(stream: InputStream) : SpriteSheet {
            Logger.addLog("Generating SpriteSheet from stream")

            return SpriteSheet(Texture.build(stream))
        }
    }

    private val vertexVBO = VBO.build(VBO.Type.BUFFER)

    val width = parentTexture.width
    val height = parentTexture.height

    val wholePart: TextureMesh
    private val segments = ArrayList<TextureMesh>()

    internal var released = false

    // VERTEX ORDER
    // 1 --- 4
    // |  \  |
    // 2 --- 3
    init {
        val vertexMap = floatArrayOf(
            0f                 , 0f                  , 0f, 0f,
            0f                 , parentTexture.height, 0f, 1f,
            parentTexture.width, parentTexture.height, 1f, 1f,
            0f                 , 0f                  , 0f, 0f,
            parentTexture.width, parentTexture.height, 1f, 1f,
            parentTexture.width, 0f                  , 1f, 0f
        )

        vertexVBO.bind()

        vertexVBO.renewBuffer(vertexMap, VBO.Method.DYNAMIC, VBO.Purpose.DRAW)

        wholePart = TextureMesh(0L, this, 0f, 0f, parentTexture.width, parentTexture.height)
    }

    fun generatePart(x: Float, y: Float, w: Float, h: Float) : TextureMesh {
        if (released) {
            throw IllegalStateException("This sprite sheet is already released!\n" +
                    "Texture ID : ${parentTexture.textureID}\n" +
                    "VBO ID : ${vertexVBO.vboID}")
        }

        val xRatio = x / width
        val yRatio = y / height
        val wRatio = w / width
        val hRatio = h / height

        val vertexMap = floatArrayOf(
            0f, 0f, xRatio         , yRatio,
            0f, h , xRatio         , yRatio + hRatio,
            w , h , xRatio + wRatio, yRatio + hRatio,
            0f, 0f, xRatio         , yRatio,
            w , h , xRatio + wRatio, yRatio + hRatio,
            w , 0f, xRatio + wRatio, yRatio
        )

        val vertexOffset = (1L + segments.size) * VERTEX_SIZE * INDEX_SIZE

        vertexVBO.bind()
        vertexVBO.injectBuffer(vertexMap, vertexOffset)

        val mesh = TextureMesh(vertexOffset, this, x, y, w, h)

        segments.add(mesh)

        return mesh
    }

    fun drawMesh(mesh: TextureMesh) {
        if (released) {
            throw IllegalStateException("This sprite sheet is already released!\nTexture ID : ${parentTexture.textureID}\nVBO ID : ${vertexVBO.vboID}")
        }

        bind()

        if (!vertexVBO.bound) {
            throw IllegalStateException("You have to bind vertex VBO of this sprite sheet first!")
        }

        if (!parentTexture.bound) {
            throw IllegalStateException("You have to bind texture of this sprite sheet first!")
        }

        if (mesh.spriteSheet !== this) {
            throw IllegalStateException("Tried to draw mesh that isn't from this sprite sheet")
        }

        VAO.vao.pointerVBO(vertexVBO, VAO.Attribute.VERTEX, VAO.ValueType.FLOAT, DIMENSION, stride = VERTEX_SIZE, offsetPointer = mesh.vertexOffset)
        VAO.vao.pointerVBO(vertexVBO, VAO.Attribute.UV, VAO.ValueType.FLOAT, DIMENSION, stride = VERTEX_SIZE, offsetPointer = mesh.vertexOffset + DIMENSION * Float.SIZE_BYTES)

        VAO.vao.setVertexAttributeArray(VAO.Attribute.VERTEX, true)
        VAO.vao.setVertexAttributeArray(VAO.Attribute.UV, true)

        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, INDEX_SIZE)
    }

    fun clone(): SpriteSheet {
        bind()

        val buffer = IntArray(parentTexture.width.toInt() * parentTexture.height.toInt() * 4)

        GL33.glGetTexImage(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_INT, buffer)

        return SpriteSheet(Texture.build(buffer, parentTexture.width.toInt(), parentTexture.height.toInt()))
    }

    override fun doBind() {
        vertexVBO.bind()

        parentTexture.bind()
    }

    fun release() {
        if (released)
            return

        Logger.addLog("Releasing SpriteSheet")

        vertexVBO.release()
        parentTexture.release()

        released = true
    }
}