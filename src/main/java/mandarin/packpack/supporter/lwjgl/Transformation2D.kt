package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeTransform
import glm_.glm
import glm_.mat4x4.Mat4
import mandarin.packpack.supporter.lwjgl.opengl.Program

class Transformation2D : Cloneable, FakeTransform {
    companion object {
        private val unusedTransformation = ArrayDeque<Transformation2D>()

        fun borrow() : Transformation2D {
            return if (unusedTransformation.isNotEmpty()) {
                unusedTransformation.removeFirst()
            } else {
                Transformation2D()
            }
        }

        fun giveBack(transformation2D: Transformation2D) {
            unusedTransformation.addLast(transformation2D)
        }
    }

    private var matrix = Mat4()

    var changed = false
        private set

    fun translate(x: Float, y: Float) {
        if (x == 0f && y == 0f)
            return

        changed = true

        matrix = glm.translate(matrix, x, y, 0f)
    }

    fun rotate(radian: Float) {
        if (radian == 0f)
            return

        changed = true

        matrix = glm.rotateZ(matrix, radian)
    }

    fun scale(sx: Float, sy: Float) {
        if (sx == 1f && sy == 1f)
            return

        changed = true

        matrix = glm.scale(matrix, sx, sy, 1f)
    }

    fun orthogonal(left: Float, right: Float, bottom: Float, top: Float) {
        reset()

        matrix[0, 0] = 2f / (right - left)
        matrix[1, 1] = 2f / (top - bottom)
        matrix[2, 2] = -1f

        val tx = -(right + left) / (right - left)
        val ty = -(top + bottom) / (top - bottom)

        matrix[0, 3] = tx
        matrix[1, 3] = ty
    }

    fun reset() {
        changed = true

        matrix = Mat4(1)
    }

    fun save() : Transformation2D {
        val transformation = borrow()

        injectMatrix(transformation)

        return transformation
    }

    fun restore(transformation2D: Transformation2D) {
        transformation2D.injectMatrix(this)

        changed = true
    }

    fun connectWithProgram(program: Program, projection: Boolean) {
        if (!changed && !projection)
            return

        program.setMatrix4(if (projection) "projection" else "matrix", projection, matrix.toFloatArray())

        changed = false
    }

    private fun injectMatrix(destination: Transformation2D) {
        for (x in 0 until 4) {
            for (y in 0 until 4) {
                destination.matrix[x][y] = matrix[x][y]
            }
        }
    }

    override fun getAT(): Any {
        return this
    }
}