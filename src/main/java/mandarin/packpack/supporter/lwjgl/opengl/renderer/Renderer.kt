package mandarin.packpack.supporter.lwjgl.opengl.renderer

import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lwjgl.opengl.RenderSessionManager
import org.lwjgl.glfw.GLFW
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class Renderer {
    lateinit var renderThread: Thread
    lateinit var renderSessionManager: RenderSessionManager

    private val attachQueue = ArrayList<AttachQueue>()
    private val customQueue = Collections.synchronizedList(ArrayList<Runnable>())

    init {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (!this@Renderer::renderSessionManager.isInitialized) {
                    renderSessionManager = RenderSessionManager()
                }

                if (!this@Renderer::renderThread.isInitialized) {
                    renderThread = Thread.currentThread()
                }

                while(attachQueue.isNotEmpty()) {
                    try {
                        attachQueue.removeAt(0).performQueue(this@Renderer)
                    } catch (e: Exception) {
                        StaticStore.logger.uploadErrorLog(e, "E/Renderer::init - Failed to attach render session")
                    }
                }

                while(customQueue.isNotEmpty()) {
                    try {
                        customQueue.removeAt(0).run()
                    } catch (e: Exception) {
                        StaticStore.logger.uploadErrorLog(e, "E/Renderer::init - Failed to perform custom GL queue")
                    }
                }

                renderSessionManager.renderSessions.forEach { r ->
                    try {
                        renderSessionManager.switchRenderSession(r)
                        r.drawAndExport()
                    } catch (e: Exception) {
                        StaticStore.logger.uploadErrorLog(e, "E/Renderer::init - Failed to perform render session rendering")
                    }
                }

                if (renderSessionManager.renderSessions.isNotEmpty()) {
                    GLFW.glfwPollEvents()
                }

                renderSessionManager.renderSessions.removeIf {
                    val done = it.done()

                    if (done)
                        renderSessionManager.closeRenderSession(it)

                    done
                }
            }
        }, 0L, 1L)
    }

    fun createRenderer(width: Int, height: Int, folder: File, onAttach: (RenderSessionConnector) -> Unit, onExport: ((Int) -> File)?, onFinished: () -> Unit) {
        attachQueue.add(AttachQueue(width, height, folder, onAttach, onExport, onFinished))
    }

    fun queueGL(runnable: Runnable) {
        customQueue.add(runnable)
    }
}