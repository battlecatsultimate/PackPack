package mandarin.packpack.supporter.lwjgl.opengl.renderer

import java.io.File

class AttachQueue(
    private val width: Int,
    private val height: Int,
    private val folder: File,
    private val onAttach: (RenderSessionConnector) -> Unit,
    private val onExport: ((Int) -> File)?,
    private val onFinish: () -> Unit) {

    fun performQueue(renderer: Renderer) {
        val renderSession = renderer.renderSessionManager.createRenderSession(width, height, resizable = false, show = false, title = "")

        renderSession.onExport(onExport)
        renderSession.onFinish(onFinish)
        renderSession.targetFolder = folder

        onAttach.invoke(RenderSessionConnector(renderSession))
    }
}