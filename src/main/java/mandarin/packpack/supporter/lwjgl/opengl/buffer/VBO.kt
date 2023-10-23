package mandarin.packpack.supporter.lwjgl.opengl.buffer

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.StaticStore
import org.lwjgl.opengl.GL33

class VBO private constructor(val vboID: Int, type: Type) {
    enum class Type {
        ELEMENT,
        BUFFER
    }

    enum class Method {
        STATIC,
        DYNAMIC,
        STREAM,
        NONE
    }

    enum class Purpose {
        DRAW,
        READ,
        COPY,
        NONE
    }

    companion object {
        private val allocatedVBO = ArrayList<VBO>()

        fun build(type: Type) : VBO {
            val vbo = VBO(GL33.glGenBuffers(), type)

            allocatedVBO.add(vbo)

            return vbo
        }

        fun findBoundVBO(glTpye: Int) : VBO? {
            return allocatedVBO.find { vbo -> vbo.bound && vbo.glType == glTpye }
        }
    }

    private val glType: Int = if (type == Type.BUFFER)
        GL33.GL_ARRAY_BUFFER
    else
        GL33.GL_ELEMENT_ARRAY_BUFFER

    var bound = false
        private set

    private var currentMethod = Method.NONE
    private var currentPurpose = Purpose.NONE

    /**
     * Create new empty buffer with specified size
     *
     * @param size - Size of this VBO buffer
     * @param method - Method of hint
     * @param purpose - Purpose of this VBO
     */
    fun renewBuffer(size: Long, method: Method, purpose: Purpose) {
        if (!bound) {
            throw IllegalStateException("Tried to get buffer size while VBO hasn't bound")
        }

        currentMethod = method
        currentPurpose = purpose

        val hint = getHint()

        var initSize = 2L

        while(initSize < size) {
            initSize *= 2
        }

        GL33.glBufferData(glType, initSize, hint)
    }

    /**
     * Create new empty buffer, and inject buffer data into this VBO
     *
     * @param buffer - Buffer data that will be injected into this VBO
     * @param method - Method of hint
     * @param purpose - Purpose of this VBO
     */
    fun renewBuffer(buffer: FloatArray, method: Method, purpose: Purpose) {
        if (!bound) {
            throw IllegalStateException("Tried to get buffer size while VBO hasn't bound")
        }

        currentMethod = method
        currentPurpose = purpose

        val hint = getHint()

        val targetSize = buffer.size * Float.SIZE_BYTES

        var initSize = 2L

        while (initSize < targetSize) {
            initSize *= 2L
        }

        GL33.glBufferData(glType, initSize, hint)
        GL33.glBufferSubData(glType, 0L, buffer)
    }

    /**
     * Create new empty buffer, and inject buffer data into this VBO
     *
     * @param buffer - Buffer data that will be injected into this VBO
     * @param method - Method of hint
     * @param purpose - Purpose of this VBO
     */
    fun renewBuffer(buffer: IntArray, method: Method, purpose: Purpose) {
        if (!bound) {
            throw IllegalStateException("Tried to get buffer size while VBO hasn't bound")
        }

        currentMethod = method
        currentPurpose = purpose

        val hint = getHint()

        val targetSize = buffer.size * Int.SIZE_BYTES

        var initSize = 2L

        while (initSize < targetSize) {
            initSize *= 2L
        }

        GL33.glBufferData(glType, initSize, hint)
        GL33.glBufferSubData(glType, 0L, buffer)
    }

    /**
     * Inject buffer to VBO
     *
     * @param buffer - Buffer data that will be injected into this VBO
     * @param offset - Offset in the buffer
     */
    fun injectBuffer(buffer: IntArray, offset: Long) {
        if (!bound) {
            throw IllegalStateException("Tried to get buffer size while VBO hasn't bound")
        }

        if (buffer.size * Int.SIZE_BYTES + offset > Int.MAX_VALUE) {
            throw IllegalStateException("This VBO can't hold more buffer data!")
        }

        val requiredSize = buffer.size * Int.SIZE_BYTES + offset
        val currentSize = getBufferSize().toLong()

        var newSize = 2L

        while(newSize < requiredSize) {
            newSize *= 2L
        }

        if(requiredSize > currentSize) {
            growBuffer(newSize)

            GL33.glBufferSubData(glType, offset, buffer)
        } else {
            GL33.glBufferSubData(glType, offset, buffer)
        }
    }

    /**
     * Inject buffer to VBO
     *
     * @param buffer - Buffer data that will be injected into this VBO
     * @param offset - Offset in the buffer
     */
    fun injectBuffer(buffer: FloatArray, offset: Long) {
        if (!bound) {
            throw IllegalStateException("Tried to get buffer size while VBO hasn't bound")
        }

        if (buffer.size * Float.SIZE_BYTES + offset > Int.MAX_VALUE) {
            throw IllegalStateException("This VBO can't hold more buffer data!")
        }

        val requiredSize = buffer.size * Float.SIZE_BYTES + offset
        val currentSize = getBufferSize().toLong()

        var newSize = 2L

        while(newSize < requiredSize) {
            newSize *= 2L
        }

        if(requiredSize > currentSize) {
            growBuffer(newSize)

            GL33.glBufferSubData(glType, offset, buffer)
        } else {
            GL33.glBufferSubData(glType, offset, buffer)
        }
    }

    /**
     * Grow buffer if there's no space to allocate buffer data.
     * It also allocates already-allocated buffer data into newly grown buffer
     *
     * @param newSize - New size of buffer in this VBO
     */
    private fun growBuffer(newSize: Long) {
        val hint = getHint()

        val copyVBO = GL33.glGenBuffers()

        GL33.glBindBuffer(GL33.GL_COPY_READ_BUFFER, vboID)
        GL33.glBindBuffer(GL33.GL_COPY_WRITE_BUFFER, copyVBO)

        val target = intArrayOf(0)

        GL33.glGetBufferParameteriv(GL33.GL_COPY_READ_BUFFER, GL33.GL_BUFFER_SIZE, target)

        GL33.glBufferData(GL33.GL_COPY_WRITE_BUFFER, target[0].toLong(), hint)
        GL33.glCopyBufferSubData(GL33.GL_COPY_READ_BUFFER, GL33.GL_COPY_WRITE_BUFFER, 0L, 0L, target[0].toLong())

        GL33.glBufferData(GL33.GL_COPY_READ_BUFFER, newSize, hint)
        GL33.glCopyBufferSubData(GL33.GL_COPY_WRITE_BUFFER, GL33.GL_COPY_READ_BUFFER, 0L, 0L, target[0].toLong())

        GL33.glDeleteBuffers(copyVBO)

        GL33.glBindBuffer(GL33.GL_COPY_READ_BUFFER, 0)
        GL33.glBindBuffer(GL33.GL_COPY_WRITE_BUFFER, 0)
    }

    /**
     * Get hint value of this VBO
     *
     * This method must be called after VBO has been initialized via [renewBuffer]
     *
     * @throws
     */
    private fun getHint() : Int {
        return when (currentMethod) {
            Method.STATIC -> when (currentPurpose) {
                Purpose.DRAW -> GL33.GL_STATIC_DRAW
                Purpose.READ -> GL33.GL_STATIC_READ
                Purpose.COPY -> GL33.GL_STATIC_COPY
                Purpose.NONE -> throw IllegalStateException("Invalid purpose $currentPurpose")
            }
            Method.DYNAMIC -> when (currentPurpose) {
                Purpose.DRAW -> GL33.GL_DYNAMIC_DRAW
                Purpose.READ -> GL33.GL_DYNAMIC_READ
                Purpose.COPY -> GL33.GL_DYNAMIC_COPY
                Purpose.NONE -> throw IllegalStateException("Invalid purpose $currentPurpose")
            }
            Method.STREAM -> when (currentPurpose) {
                Purpose.DRAW -> GL33.GL_STREAM_DRAW
                Purpose.READ -> GL33.GL_STREAM_READ
                Purpose.COPY -> GL33.GL_STREAM_COPY
                Purpose.NONE -> throw IllegalStateException("Invalid purpose $currentPurpose")
            }
            Method.NONE -> throw IllegalStateException("Invalid method $currentMethod")
        }
    }

    /**
     * Get size of VBO buffer.
     *
     * @return Size of buffer of this VBO.
     *
     */
    private fun getBufferSize() : Int {
        if (!bound) {
            throw IllegalStateException("Tried to get buffer size while VBO hasn't bound")
        }

        return GL33.glGetBufferParameteri(glType, GL33.GL_BUFFER_SIZE)
    }

    /**
     * Bind current VBO
     */
    fun bind() {
        if (bound)
            return

        val boundVBO = findBoundVBO(glType)

        if (boundVBO != null) {
            boundVBO.bound = false
        }

        GL33.glBindBuffer(glType, vboID)

        bound = true
    }

    fun release() {
        Logger.addLog("Releasing VBO : $vboID")
        GL33.glDeleteBuffers(vboID)

        allocatedVBO.remove(this)
    }
}