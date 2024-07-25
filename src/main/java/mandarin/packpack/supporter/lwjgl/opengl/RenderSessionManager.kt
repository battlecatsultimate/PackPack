package mandarin.packpack.supporter.lwjgl.opengl

import mandarin.packpack.supporter.lwjgl.opengl.buffer.VAO
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL33

class RenderSessionManager {
    companion object {
        fun terminate() {
            GLFW.glfwTerminate()
            GLFW.glfwSetErrorCallback(null)?.free()
        }
    }

    private val mainRenderSession = RenderSession.build(this, 64, 64, false, show = false, title = "") {
        val program = Program()

        program.addShader(
            ShaderScript.build("fragment.frag", ShaderScript.Type.FRAGMENT,
            "state", "step1", "step2", "color1", "color2", "screenSize", "alpha", "dashMode", "fillMode", "factor", "pattern", "opposite", "addMode")
        )
        program.addShader(ShaderScript.build("vertex.vert", ShaderScript.Type.VERTEX, "state", "projection", "matrix"))

        program.initialize()

        program
    }

    val renderSessions = ArrayList<RenderSession>()

    fun createRenderSession(width: Int, height: Int, resizable: Boolean, show: Boolean, title: String = "Title") : RenderSession {
        val renderSession =
            RenderSession.build(this, width, height, resizable, show, title = title, parent = mainRenderSession)

        renderSessions.add(renderSession)

        return renderSession
    }

    fun switchRenderSession(renderSession: RenderSession) {
        GLFW.glfwMakeContextCurrent(renderSession.windowID)

        VAO.switchVAO(renderSession)

        mainRenderSession.program.setVector2("screenSize", floatArrayOf(renderSession.width.toFloat(), renderSession.height.toFloat()))

        GL33.glViewport(0, 0, renderSession.width, renderSession.height)

        renderSession.getGraphics().syncProjection()
    }

    fun closeRenderSession(renderSession: RenderSession) {
        if (renderSession !== mainRenderSession) {
            GLFW.glfwMakeContextCurrent(mainRenderSession.windowID)
            VAO.switchVAO(mainRenderSession)
        }

        renderSession.release()
    }

    fun closeAll() {
        renderSessions.forEach { window -> window.release() }
        renderSessions.clear()

        closeRenderSession(mainRenderSession)
    }
}