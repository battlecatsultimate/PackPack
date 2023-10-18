package mandarin.packpack.supporter.opengl.model

import mandarin.packpack.supporter.opengl.buffer.VAO
import mandarin.packpack.supporter.opengl.buffer.VBO
import org.checkerframework.common.value.qual.IntRange
import org.lwjgl.opengl.GL33
import kotlin.math.cos
import kotlin.math.sin

class MeshModel : Model() {
    companion object {
        const val FILL_SQUARE = 0  // 6 * 2
        const val LINE_SQUARE = 1  // 4 * 2
        const val FILL_CIRCLE = 2  // 300 * 2
        const val LINE_CIRCLE = 3  // 100 * 2
        const val LINE = 4  // 2 * 2

        private const val CIRCLE_NODE = 100

        private const val LINE_VERTEX_OFFSET = 410L * 2L * Float.SIZE_BYTES
        private const val VERTEX_SIZE_LIMIT = (410L + 1000L) * 2L * Float.SIZE_BYTES
        private const val INDEX_SIZE_LIMIT = (410L + 1000L) * Int.SIZE_BYTES
    }

    private val vertexBufferVBO = VBO.build(VBO.Type.BUFFER)
    private val lineBuffer = floatArrayOf(0f, 0f, 1f, 1f)

    private val meshes = ArrayList<PolygonMesh>()

    init {
        vertexBufferVBO.bind()
        vertexBufferVBO.renewBuffer(VERTEX_SIZE_LIMIT, VBO.Method.DYNAMIC, VBO.Purpose.DRAW)

        //Square, Fill
        generateMesh(
            floatArrayOf(
                0f, 0f,
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 1f,
                1f, 0f
            ),
            2
        )

        //Square, Line
        generateMesh(
            floatArrayOf(
                0f, 0f,
                0f, 1f,
                1f, 1f,
                1f, 0f
            ),
            2
        )

        //Circle
        val fillVertices = ArrayList<Float>()
        val lineVertices = ArrayList<Float>()

        val angleSection = Math.PI * 2 / CIRCLE_NODE

        for (i in 0 until CIRCLE_NODE) {
            fillVertices.addAll(floatArrayOf(
                0f, 0f,
                cos(angleSection * i).toFloat(), sin(angleSection * i).toFloat(),
                cos(angleSection * (i + 1)).toFloat(), sin(angleSection * (i + 1)).toFloat()
            ).toList())

            lineVertices.addAll(floatArrayOf(
                cos(angleSection * i).toFloat(), sin(angleSection * i).toFloat()
            ).toList())
        }

        generateMesh(fillVertices.toFloatArray(), 2)

        generateMesh(lineVertices.toFloatArray(), 2)

        //Line
        generateMesh(
            floatArrayOf(
                0f, 0f,
                1f, 1f
            ),
            2
        )
    }

    fun generateMesh(vertices: FloatArray, dimension: @IntRange(from = 1, to = 4) Int) : PolygonMesh {
        if (vertices.size % dimension != 0) {
            throw IllegalStateException("Vertex data must be synced with dimension!\nVertex data : ${vertices.size / dimension}\nDimension : ${dimension}D")
        }

        vertexBufferVBO.bind()

        val vertexOffset = if (meshes.isEmpty()) {
            0L
        } else {
            meshes[meshes.size - 1].vertexOffset + meshes[meshes.size - 1].vertexSize
        }

        vertexBufferVBO.injectBuffer(vertices, vertexOffset)

        val indexSize = vertices.size / dimension

        val mesh = PolygonMesh(vertexOffset, vertices.size * Float.SIZE_BYTES, indexSize, dimension, this)

        meshes.add(mesh)

        return mesh
    }

    fun drawFill(mesh: PolygonMesh) {
        if (!vertexBufferVBO.bound) {
            throw IllegalStateException("You have to bind vertex VBO of this mesh model first!")
        }

        if (mesh.meshModel !== this) {
            throw IllegalStateException("Tried to draw mesh that wasn't registered into this mesh model")
        }

        VAO.vao.pointerVBO(vertexBufferVBO, VAO.Attribute.VERTEX, VAO.ValueType.FLOAT, mesh.dimension, offsetPointer = mesh.vertexOffset)

        VAO.vao.setVertexAttributeArray(VAO.Attribute.VERTEX, true)
        VAO.vao.setVertexAttributeArray(VAO.Attribute.UV, false)

        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, mesh.indexSize)
    }

    fun drawLine(mesh: PolygonMesh) {
        if (!vertexBufferVBO.bound) {
            throw IllegalStateException("You have to bind vertex VBO of this mesh model first!")
        }

        if (mesh.meshModel !== this) {
            throw IllegalStateException("Tried to draw mesh that wasn't registered into this mesh model")
        }

        VAO.vao.pointerVBO(vertexBufferVBO, VAO.Attribute.VERTEX, VAO.ValueType.FLOAT, mesh.dimension, offsetPointer = mesh.vertexOffset)

        VAO.vao.setVertexAttributeArray(VAO.Attribute.VERTEX, true)
        VAO.vao.setVertexAttributeArray(VAO.Attribute.UV, false)

        GL33.glDrawArrays(GL33.GL_LINE_LOOP, 0, mesh.indexSize)
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (injectLineBuffer(x1, y1, x2, y2)) {
            vertexBufferVBO.injectBuffer(lineBuffer, LINE_VERTEX_OFFSET)
        }

        drawLine(meshes[LINE])
    }

    fun meshSize() : Int {
        return meshes.size
    }

    fun getMesh(index: Int) : PolygonMesh {
        return meshes[index]
    }

    fun fillSquare() {
        drawFill(meshes[FILL_SQUARE])
    }

    fun lineSquare() {
        drawLine(meshes[LINE_SQUARE])
    }

    fun fillCircle() {
        drawFill(meshes[FILL_CIRCLE])
    }

    fun lineCircle() {
        drawLine(meshes[LINE_CIRCLE])
    }

    override fun doBind() {
        vertexBufferVBO.bind()
    }

    private fun injectLineBuffer(x1: Float, y1: Float, x2: Float, y2: Float) : Boolean {
        if (lineBuffer[0] == x1 && lineBuffer[2] == y1 && lineBuffer[3] == x2 && lineBuffer[4] == y2)
            return false

        lineBuffer[0] = x1
        lineBuffer[1] = y1
        lineBuffer[2] = x2
        lineBuffer[3] = y2

        return true
    }
}