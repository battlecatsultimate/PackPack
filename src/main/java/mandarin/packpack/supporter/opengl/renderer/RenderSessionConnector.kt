package mandarin.packpack.supporter.opengl.renderer

import mandarin.packpack.supporter.lwjgl.GLGraphics
import mandarin.packpack.supporter.opengl.RenderSession

class RenderSessionConnector(private val renderSession: RenderSession) {
    fun queue(onRender: (GLGraphics) -> Unit) {
        renderSession.queue(onRender)
    }
}