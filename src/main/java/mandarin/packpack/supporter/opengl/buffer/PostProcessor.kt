package mandarin.packpack.supporter.opengl.buffer

import mandarin.packpack.supporter.opengl.RenderSession
import mandarin.packpack.supporter.opengl.Texture
import mandarin.packpack.supporter.opengl.model.SpriteSheet
import org.lwjgl.opengl.GL33
import java.nio.ByteBuffer

class PostProcessor private constructor(val fboID: Int, val width: Int, val height: Int) {
    var bound = false
        private set

    private val fboTextureID = GL33.glGenTextures()

    private val vbo = VBO.build(VBO.Type.BUFFER)

    companion object {
        fun build(window: RenderSession) : PostProcessor {
            val fboID = GL33.glGenFramebuffers()

            return PostProcessor(fboID, window.width, window.height)
        }
    }

    init {
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, fboID)
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, fboTextureID)

        vbo.bind()

        GL33.glBindTexture(GL33.GL_TEXTURE_2D, fboTextureID)
        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, width, height, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, null as ByteBuffer?)

        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST)
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST)

        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_CLAMP_TO_EDGE)
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_CLAMP_TO_EDGE)

        GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_TEXTURE_2D, fboTextureID, 0)

        val vertexMap = floatArrayOf(
            -1f, 1f , 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f , -1f, 1f, 0f,
            -1f, 1f , 0f, 1f,
            1f , -1f, 1f, 0f,
            1f , 1f , 1f, 1f
        )

        vbo.renewBuffer(vertexMap, VBO.Method.STATIC, VBO.Purpose.DRAW)

        val checkCode = GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)

        if (checkCode != GL33.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Failed to generate frame buffer : Error code = $checkCode")
        }
    }

    fun bind() {
        vbo.bind()

        GL33.glBindTexture(GL33.GL_TEXTURE_2D, fboTextureID)
        Texture.findBoundTexture()?.bound = false

        VAO.vao.pointerVBO(vbo, VAO.Attribute.VERTEX, VAO.ValueType.FLOAT, SpriteSheet.DIMENSION, stride = SpriteSheet.VERTEX_SIZE)
        VAO.vao.pointerVBO(vbo, VAO.Attribute.UV, VAO.ValueType.FLOAT, SpriteSheet.DIMENSION, stride = SpriteSheet.VERTEX_SIZE, offsetPointer = SpriteSheet.DIMENSION.toLong() * Float.SIZE_BYTES)

        VAO.vao.setVertexAttributeArray(VAO.Attribute.VERTEX, true)
        VAO.vao.setVertexAttributeArray(VAO.Attribute.UV, true)
    }

    fun registerMultiSampler(multiSampler: MultiSampler) {
        GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER, multiSampler.fboID)
        GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER, fboID)

        GL33.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL33.GL_COLOR_BUFFER_BIT, GL33.GL_NEAREST)
    }

    fun release() {
        GL33.glDeleteTextures(fboTextureID)
        GL33.glDeleteFramebuffers(fboID)

        vbo.release()
    }
}