package mandarin.packpack.supporter.lwjgl.opengl.renderer

import mandarin.packpack.supporter.lwjgl.GLGraphics
import mandarin.packpack.supporter.lwjgl.opengl.RenderSession

class RenderSessionConnector(private val renderSession: RenderSession) {
    fun queue(onRender: (GLGraphics) -> Unit) {
        renderSession.queue(onRender)
    }
}