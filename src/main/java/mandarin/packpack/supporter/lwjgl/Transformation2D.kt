package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeTransform
import mandarin.packpack.supporter.opengl.Program
import kotlin.math.cos
import kotlin.math.sin

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

    private val matrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    var changed = false
        private set

    private var translation = false
    private var scale = false
    private var skew = false

    fun translate(x: Float, y: Float) {
        if (x == 0f && y == 0f)
            return

        changed = true

        if (translation) {
            matrix[3] += matrix[0] * x + matrix[1] * y
            matrix[7] += matrix[4] * x + matrix[5] * y
        } else {
            translation = true

            matrix[3] = matrix[0] * x + matrix[1] * y
            matrix[7] = matrix[4] * x + matrix[5] * y
        }
    }

    private fun rotate90() {
        var preM00 = matrix[0]

        matrix[0] = matrix[1]
        matrix[1] = -preM00

        preM00 = matrix[4]

        matrix[4] = matrix[5]
        matrix[5] = -preM00

        scale = matrix[0] != 1f || matrix[5] != 1f
        skew = matrix[1] != 0f || matrix[4] != 0f
    }

    private fun rotate180() {
        matrix[0] = -matrix[0]
        matrix[5] = -matrix[5]

        scale = true

        matrix[1] = -matrix[1]
        matrix[4] = -matrix[4]

        scale = matrix[0] != 1f || matrix[5] != 1f
        skew = matrix[1] != 0f || matrix[4] != 0f
    }

    private fun rotate270() {
        var preM00 = matrix[0]

        matrix[0] = -matrix[1]
        matrix[1] = preM00

        preM00 = matrix[4]

        matrix[4] = -matrix[5]
        matrix[5] = preM00

        scale = matrix[0] != 1f || matrix[5] != 1f
        skew = matrix[1] != 0f || matrix[4] != 0f
    }

    fun rotate(radian: Float) {
        if (radian == 0f)
            return

        changed = true

        val sin = sin(radian)

        if (sin == 1f) {
            rotate90()
        } else if (sin == -1f) {
            rotate270()
        } else {
            val cos = cos(radian)

            if (cos == -1f) {
                rotate180()
            } else {
                var preM00 = matrix[0]
                var preM01 = matrix[1]

                matrix[0] = cos * preM00 + sin * preM01
                matrix[1] = -sin * preM00 + cos * preM01

                preM00 = matrix[4]
                preM01 = matrix[5]

                matrix[4] = cos * preM00 + sin * preM01
                matrix[5] = -sin * preM00 + cos * preM01

                scale = matrix[0] != 1f || matrix[5] != 1f
                skew = matrix[1] != 0f || matrix[4] != 0f
            }
        }
    }

    fun scale(sx: Float, sy: Float) {
        if (sx == 1f && sy == 1f)
            return

        changed = true

        matrix[0] *= sx
        matrix[5] *= sy

        if (skew) {
            matrix[1] *= sy
            matrix[4] *= sx

            skew = matrix[1] != 0f || matrix[4] != 0f
        }

        scale = matrix[0] != 1f || matrix[5] != 1f
    }

    fun orthogonal(left: Float, right: Float, bottom: Float, top: Float) {
        reset()

        matrix[0] = 2f / (right - left)
        matrix[5] = 2f / (top - bottom)
        matrix[10] = -1f

        val tx = -(right + left) / (right - left)
        val ty = -(top + bottom) / (top - bottom)

        matrix[3] = tx
        matrix[7] = ty
    }

    fun reset() {
        changed = true

        matrix[0] = 1f
        matrix[1] = 0f
        matrix[3] = 0f

        matrix[4] = 0f
        matrix[5] = 1f
        matrix[7] = 0f
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

        program.setMatrix4(if (projection) "projection" else "matrix", true, matrix)

        changed = false
    }

    private fun injectMatrix(destination: Transformation2D) {
        for(i in destination.matrix.indices) {
            destination.matrix[i] = matrix[i]
        }
    }

    override fun getAT(): Any {
        return this
    }
}