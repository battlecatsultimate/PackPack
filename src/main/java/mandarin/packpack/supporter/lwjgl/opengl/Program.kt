package mandarin.packpack.supporter.lwjgl.opengl

import org.lwjgl.opengl.GL33

@Suppress("unused")
class Program {
    private val uniformFields = HashSet<String>()

    private val programID: Int = GL33.glCreateProgram()

    private val shaders = ArrayList<ShaderScript>()
    private val uniformFieldPointers = HashMap<String, Int>()

    fun addShader(shaderScript: ShaderScript) {
        shaderScript.attachShader(programID)
        uniformFields.addAll(shaderScript.uniformField)

        shaders.add(shaderScript)
    }

    fun initialize() {
        uniformFieldPointers.clear()

        GL33.glLinkProgram(programID)
        GL33.glValidateProgram(programID)

        shaders.forEach { script -> script.detachShader(programID) }
    }

    fun use() {
        GL33.glUseProgram(programID)

        GL33.glEnable(GL33.GL_BLEND)
        GL33.glEnable(GL33.GL_MULTISAMPLE)
        GL33.glEnable(GL33.GL_TEXTURE_2D)
    }

    fun release() {
        GL33.glDeleteProgram(programID)
    }

    fun setInt(fieldName: String, value: Int) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Int) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Int")

        GL33.glUniform1i(pointer, value)
    }

    fun setUnsignedInt(fieldName: String, value: Int) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (U_Int) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "U_Int")

        GL33.glUniform1ui(pointer, value)
    }

    fun setFloat(fieldName: String, value: Float) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Float) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Float")

        GL33.glUniform1f(pointer, value)
    }

    fun setFloatArray(fieldName: String, value: FloatArray) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Float[]) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Float[]")

        GL33.glUniform1fv(pointer, value)
    }

    fun setVector2(fieldName: String, value: FloatArray) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        if (value.size != 2) {
            throw IllegalStateException("Vector2 must have 2 values, not ${value.size}")
        }

        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Vec2) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Vec2")

        GL33.glUniform2fv(pointer, value)
    }

    fun setVector3(fieldName: String, value: FloatArray) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        if (value.size != 3) {
            throw IllegalStateException("Vector3 must have 3 values, not ${value.size}")
        }

        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Vec3) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Vec3")

        GL33.glUniform3fv(pointer, value)
    }

    fun setVector4(fieldName: String, value: FloatArray) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Vec4) $fieldName in this program [$programID]!")
        }

        if (value.size != 4) {
            throw IllegalStateException("Vector3 must have 4 values, not ${value.size}")
        }

        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Vec4) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Vec4")

        GL33.glUniform4fv(pointer, value)
    }

    fun setMatrix4(fieldName: String, transpose: Boolean, value: FloatArray) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Mat4) $fieldName in this program [$programID]!")
        }

        if (value.size != 16) {
            throw IllegalStateException("Matrix4 must have 16 values, not ${value.size}")
        }

        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Mat4) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Mat4")

        GL33.glUniformMatrix4fv(pointer, transpose, value)
    }

    fun setBoolean(fieldName: String, value: Boolean) {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Boolean")

        GL33.glUniform1i(pointer, if (value) 1 else 0)
    }

    fun getFloat(fieldName: String) : Float {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Float")

        val array = FloatArray(1)

        GL33.glGetUniformfv(programID, pointer, array)

        return array[0]
    }

    fun getBoolean(fieldName: String) : Boolean {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Float")

        val array = IntArray(1)

        GL33.glGetUniformiv(programID, pointer, array)

        return array[0] == 1
    }

    fun getInt(fieldName: String) : Int {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Float")

        val array = IntArray(1)

        GL33.glGetUniformiv(programID, pointer, array)

        return array[0]
    }

    fun getMat4(fieldName: String) : FloatArray {
        if (!uniformFields.contains(fieldName)) {
            throw IllegalStateException("There's no field name called (Boolean) $fieldName in this program [$programID]!")
        }

        val pointer = getPointerAddress(programID, fieldName, "Float")

        val array = FloatArray(16)

        GL33.glGetUniformfv(programID, pointer, array)

        return array
    }

    private fun getPointerAddress(programID: Int, fieldName: String, typeName: String) : Int {
        if (fieldName !in uniformFields) {
            throw IllegalStateException("There's no such $fieldName field in this program!")
        }

        val pointer = uniformFieldPointers[fieldName]

        return if (pointer != null) {
            pointer
        } else {
            val address = GL33.glGetUniformLocation(programID, fieldName)

            if (address == -1) {
                throw RuntimeException("Failed to get uniform field ($typeName) $fieldName from program ID of $programID")
            }

            uniformFieldPointers[fieldName] = address

            address
        }
    }
}