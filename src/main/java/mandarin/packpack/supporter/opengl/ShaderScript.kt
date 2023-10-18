package mandarin.packpack.supporter.opengl

import org.lwjgl.opengl.GL33
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

class ShaderScript private constructor(private val shaderID: Int, val uniformField: Array<out String>) {
    enum class Type {
        VERTEX,
        FRAGMENT
    }

    companion object {
        fun build(resourceName: String, type: Type, vararg uniformField: String) : ShaderScript {
            val content = loadShaderFile(resourceName)

            val id = GL33.glCreateShader(if (type == Type.VERTEX) GL33.GL_VERTEX_SHADER else GL33.GL_FRAGMENT_SHADER)

            GL33.glShaderSource(id, content)
            GL33.glCompileShader(id)

            if (GL33.glGetShaderi(id, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
                System.err.println(GL33.glGetShaderInfoLog(id))

                throw RuntimeException("Failed to compile shader file : ${resourceName}, type : $type")
            }

            return ShaderScript(id, uniformField)
        }

        private fun loadShaderFile(resourceName: String) : String {
            val loader = Thread.currentThread().contextClassLoader
            val stream = loader.getResourceAsStream(resourceName) ?: return ""

            BufferedReader(InputStreamReader(stream)).use { reader ->
                val builder = StringBuilder()

                var line = ""

                while (reader.readLine()?.also { l -> line = l } != null) {
                    builder.append(line).append("\n")
                }

                return builder.toString()
            }
        }
    }

    fun attachShader(programID: Int) {
        GL33.glAttachShader(programID, shaderID)
    }

    fun detachShader(programID: Int) {
        GL33.glDetachShader(programID, shaderID)
    }
}