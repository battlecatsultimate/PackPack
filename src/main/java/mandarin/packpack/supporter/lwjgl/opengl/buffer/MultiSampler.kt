package mandarin.packpack.supporter.lwjgl.opengl.buffer

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.lwjgl.opengl.RenderSession
import org.apache.commons.logging.Log
import org.lwjgl.opengl.GL33

class MultiSampler private constructor(val fboID: Int, val width: Int, val height: Int) {
    private val rbo: Int
    private val fboTextureID: Int

    companion object {
        fun build(window: RenderSession) : MultiSampler {
            Logger.addLog("Generating MultiSampler")

            val fboID = GL33.glGenFramebuffers()
            Logger.addLog("Generating FBO from MultiSampler $fboID")

            return MultiSampler(fboID, window.width, window.height)
        }
    }

    init {
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, fboID)

        fboTextureID = GL33.glGenTextures()
        Logger.addLog("Generating FBO texture from MultiSampler : $fboTextureID")

        GL33.glBindTexture(GL33.GL_TEXTURE_2D_MULTISAMPLE, fboTextureID)

        GL33.glTexImage2DMultisample(GL33.GL_TEXTURE_2D_MULTISAMPLE, 8, GL33.GL_RGBA, width, height, true)
        GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_TEXTURE_2D_MULTISAMPLE, fboTextureID, 0)

        rbo = GL33.glGenRenderbuffers()
        Logger.addLog("Generating RBO from MultiSampler $rbo")

        GL33.glRenderbufferStorageMultisample(GL33.GL_RENDERBUFFER, 8, GL33.GL_DEPTH24_STENCIL8, width, height)
        GL33.glFramebufferRenderbuffer(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_STENCIL_ATTACHMENT, GL33.GL_RENDERBUFFER, rbo)

        val checkCode = GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)

        if (checkCode != GL33.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Failed to generate frame buffer : Error code = $checkCode")
        }

        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, fboID)

        GL33.glBindTexture(GL33.GL_TEXTURE_2D_MULTISAMPLE, fboTextureID)
    }

    fun release() {
        Logger.addLog("Releasing MultiSampler")

        Logger.addLog("Releasing FBO Texture : $fboTextureID")
        GL33.glDeleteTextures(fboTextureID)

        Logger.addLog("Releasing FBO : $fboID")
        GL33.glDeleteFramebuffers(fboID)

        Logger.addLog("Releasing RBO : $rbo")
        GL33.glDeleteRenderbuffers(rbo)
    }
}