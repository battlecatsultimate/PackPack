package mandarin.packpack.supporter.lwjgl.opengl.buffer

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.lwjgl.opengl.RenderSession
import org.checkerframework.common.value.qual.IntRange
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL33

@Suppress("unused")
class VAO private constructor(private val vaoID: Int) {
    enum class ValueType {
        FLOAT,
        UNSIGNED_INT
    }

    enum class Attribute {
        VERTEX,
        UV
    }

    companion object {
        private val vaoMap = HashMap<Long, VAO>()
        private lateinit var currentVAO: VAO

        val vao: VAO
            get() {
                if (!this::currentVAO.isInitialized) {
                    throw IllegalStateException("VAO hasn't been initialized")
                }

                return currentVAO
            }

        fun switchVAO(window: RenderSession) {
            if (window.windowID != GLFW.glfwGetCurrentContext()) {
                throw IllegalStateException("Current context hasn't set to this window yet")
            }

            val vao = vaoMap[window.windowID]

            currentVAO = if (vao == null) {
                val initVAO = VAO(GL33.glGenVertexArrays())

                vaoMap[window.windowID] = initVAO

                initVAO
            } else {
                vao
            }

            GL33.glBindVertexArray(currentVAO.vaoID)
        }

        fun releaseVAO(window: RenderSession) {
            val vao = vaoMap[window.windowID] ?: return

            Logger.addLog("Releasing VAO : ${vao.vaoID} from Window ID : ${window.windowID}")
            GL33.glDeleteVertexArrays(vao.vaoID)

            vaoMap.remove(window.windowID)
        }
    }

    private val enabled = booleanArrayOf(false, false, false)

    init {
        Logger.addLog("Generating VAO : $vaoID")
    }

    fun pointerVBO(vbo: VBO, attribute: Attribute, type: ValueType, size: @IntRange(from = 1, to = 4) Int, stride: Int = 0, offsetPointer: Long = 0L) {
        if (!vbo.bound) {
            throw IllegalStateException("You must bind this VBO before binding it!")
        }

        val index = when(attribute) {
            Attribute.VERTEX -> 0
            Attribute.UV -> 1
        }

        val value = if (type == ValueType.FLOAT) {
            GL33.GL_FLOAT
        } else {
            GL33.GL_UNSIGNED_INT
        }

        GL33.glVertexAttribPointer(index, size, value, false, stride, offsetPointer)
    }

    fun setVertexAttributeArray(attribute: Attribute, value: Boolean) {
        val index = when(attribute) {
            Attribute.VERTEX -> 0
            Attribute.UV -> 1
        }

        if (value == enabled[index]) {
            return
        }

        if (value) {
            GL33.glEnableVertexAttribArray(index)

            enabled[index] = true
        } else {
            GL33.glEnableVertexAttribArray(index)

            enabled[index] = false
        }
    }
}