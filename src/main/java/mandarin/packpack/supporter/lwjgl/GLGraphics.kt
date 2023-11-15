package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeGraphics
import common.system.fake.FakeImage
import common.system.fake.FakeTransform
import mandarin.packpack.supporter.lwjgl.opengl.Program
import mandarin.packpack.supporter.lwjgl.opengl.RenderSession
import mandarin.packpack.supporter.lwjgl.opengl.model.FontModel
import mandarin.packpack.supporter.lwjgl.opengl.model.MeshModel
import mandarin.packpack.supporter.lwjgl.opengl.model.TextureMesh
import org.lwjgl.opengl.GL33
import kotlin.math.*

class GLGraphics(private val renderSession: RenderSession, private val program: Program) : FakeGraphics {
    enum class State {
        MESH,
        TEXTURE,
        TEXT,
        RENDER
    }

    enum class Blend {
        SOURCE,
        ADD,
        SUBTRACT,
        MULTIPLY,
        DARK,
        OVERRIDE
    }

    enum class HorizontalSnap {
        LEFT,
        MIDDLE,
        RIGHT
    }

    enum class VerticalSnap {
        TOP,
        MIDDLE,
        BOTTOM
    }

    enum class LineType {
        PLAIN,
        DASH
    }

    enum class LineEndMode {
        ROUND,
        VERTICAL
    }

    companion object {
        private val basicShape = MeshModel()
    }

    var fontModel: FontModel? = null
        set(value) {
            field = value

            if (value != null)
                usedFontModel.add(value)
        }

    private val transformation2D = Transformation2D()
    private val projection = Transformation2D()

    private val firstStep = floatArrayOf(0f, 0f)
    private val secondStep = floatArrayOf(0f, 0f)

    private val firstColor = floatArrayOf(1f, 1f, 1f, 1f)
    private val secondColor = floatArrayOf(1f, 1f, 1f, 1f)

    private var stroke = 1f
    private var endMode = LineEndMode.VERTICAL

    private val usedTexture = HashSet<GLImage>()
    private val usedFontModel = HashSet<FontModel>()

    private var fillMode = true
        set(value) {
            if (value == field)
                return

            field = value

            program.setBoolean("fillMode", value)
        }

    var state = State.MESH
        set(value) {
            program.setInt("state", value.ordinal)

            if (value != State.MESH) {
                if (blend == Blend.SOURCE) {
                    GL33.glBlendEquation(GL33.GL_FUNC_ADD)
                    GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE_MINUS_SRC_ALPHA)
                }

                program.setBoolean("dashMode", false)
            } else {
                if (blend == Blend.SOURCE) {
                    GL33.glBlendEquation(GL33.GL_FUNC_ADD)
                    GL33.glBlendFuncSeparate(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA, GL33.GL_ONE, GL33.GL_ONE_MINUS_SRC_ALPHA)
                }

                program.setBoolean("dashMode", dashMode == LineType.DASH)
            }

            field = value
        }

    var blend: Blend = Blend.SOURCE
        set(value) {
            program.setBoolean("addMode", value == Blend.ADD)

            when(value) {
                Blend.SOURCE -> {
                    GL33.glBlendEquation(GL33.GL_FUNC_ADD)
                    GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE_MINUS_SRC_ALPHA)
                }
                Blend.ADD -> {
                    GL33.glBlendEquation(GL33.GL_FUNC_ADD)
                    GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE)
                }
                Blend.MULTIPLY -> {
                    GL33.glBlendEquation(GL33.GL_MULT)
                    GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE)
                }
                Blend.SUBTRACT -> {
                    GL33.glBlendEquation(GL33.GL_FUNC_SUBTRACT)
                    GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE)
                }
                Blend.DARK -> {
                    GL33.glBlendEquation(GL33.GL_MIN)
                    GL33.glBlendFunc(GL33.GL_ONE, GL33.GL_ONE)
                }
                Blend.OVERRIDE -> {
                    GL33.glBlendEquation(GL33.GL_ADD)
                    GL33.glBlendFunc(GL33.GL_ZERO, GL33.GL_ONE)
                }
            }

            field = value
        }

    private var dashMode = LineType.PLAIN
        set(value) {
            field = value

            program.setBoolean("dashMode", value == LineType.DASH)
        }

    init {
        syncProjection()
        blend = Blend.SOURCE
        setColor(255f, 255f, 255f)
        setAlpha(255f)
    }

    override fun colRect(x: Float, y: Float, w: Float, h: Float, r: Int, g: Int, b: Int, a: Int) {
        setColor(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
        fillRect(x, y, w, h)
    }

    override fun drawImage(bimg: FakeImage, x: Float, y: Float) {
        if (bimg !is GLImage)
            return

        val texture = bimg.bimg()

        if (texture !is TextureMesh)
            return

        val save = transformation2D.save()

        translate(x, y)

        applyMatrix()

        state = State.TEXTURE

        texture.draw()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)

        applyMatrix()

        usedTexture.add(bimg)
    }

    override fun drawImage(bimg: FakeImage, x: Float, y: Float, w: Float, h: Float) {
        if (bimg !is GLImage)
            return

        val texture = bimg.bimg()

        if (texture !is TextureMesh)
            return

        val wr = w / texture.width
        val hr = h / texture.height

        val save = transformation2D.save()

        translate(x, y)
        scale(wr, hr)

        applyMatrix()

        state = State.TEXTURE

        texture.draw()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)

        usedTexture.add(bimg)
    }

    fun drawText(text: String, x: Float, y: Float, xSnap: HorizontalSnap, ySnap: VerticalSnap) {
        val font = fontModel ?: return

        val save = transformation2D.save()

        state = State.TEXT

        val dimension = font.measureDimension(text)

        val offsetX = when(xSnap) {
            HorizontalSnap.RIGHT -> -dimension[0]
            HorizontalSnap.MIDDLE -> - (dimension[0] + dimension[2] / 2f)
            HorizontalSnap.LEFT -> -(dimension[0] + dimension[2])
        }

        val offsetY = when(ySnap) {
            VerticalSnap.TOP -> -dimension[1]
            VerticalSnap.MIDDLE -> -(dimension[1] + dimension[3] / 2f)
            VerticalSnap.BOTTOM -> -(dimension[1] + dimension[3])
        }

        translate(x + offsetX, y + offsetY)

        applyMatrix()

        font.drawText(this, text)

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (x1 == x2 && y1 == y2)
            return

        applyMatrix()

        if (stroke == 1f) {
            state = State.MESH
            fillMode = false

            basicShape.bind()

            basicShape.drawLine(x1, y1, x2, y2)
        } else {
            val angle = if (x1 == x2) {
                if (y2 >= y1) {
                    (Math.PI / 2.0).toFloat()
                } else {
                    -(Math.PI / 2.0).toFloat()
                }
            } else {
                atan2(y2 - y1, x2 - x1)
            }

            val length = sqrt((x2 - x1).pow(2f) + (y2 - y1).pow(2f))

            val save = transformation2D.save()

            translate(x1, y1)
            rotate(angle)

            applyMatrix()

            if (endMode == LineEndMode.VERTICAL) {
                drawLineRect(0f, -stroke / 2f, length, stroke)
            } else {
                drawLineRect(0f, -stroke / 2f, length, stroke)

                lineOval(-stroke / 2f, -stroke / 2f, stroke / 2f, stroke / 2f)
                lineOval(length -stroke / 2f, -stroke / 2f, stroke / 2f, stroke / 2f)
            }

            transformation2D.restore(save)
            Transformation2D.giveBack(save)
        }
    }

    override fun drawOval(x: Float, y: Float, rx: Float, ry: Float) {
        val save = transformation2D.save()

        translate(x + rx, y + ry)
        scale(rx, ry)

        applyMatrix()

        state = State.MESH
        fillMode = false

        basicShape.bind()

        basicShape.lineCircle()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    override fun drawRect(x: Float, y: Float, w: Float, h: Float) {
        val save = transformation2D.save()

        translate(x, y)
        scale(w, h)

        applyMatrix()

        state = State.MESH
        fillMode = false

        basicShape.bind()

        basicShape.lineSquare()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    override fun fillOval(x: Float, y: Float, rx: Float, ry: Float) {
        val save = transformation2D.save()

        translate(x + rx, y + ry)
        scale(rx, ry)

        applyMatrix()

        state = State.MESH
        fillMode = true

        basicShape.bind()

        basicShape.fillCircle()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    private fun lineOval(x: Float, y: Float, rx: Float, ry: Float) {
        val save = transformation2D.save()

        translate(x + rx, y + ry)
        scale(rx, ry)

        applyMatrix()

        state = State.MESH
        fillMode = false

        basicShape.bind()

        basicShape.fillCircle()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    fun roundRect(x: Float, y: Float, w: Float, h: Float, dx: Float, dy: Float) {
        val rx = dx / 2f
        val ry = dy / 2f

        applyMatrix()

        val currentEndMode = endMode
        endMode = LineEndMode.ROUND

        for (i in 0 until 4) {
            val offsetX = when(i) {
                0 -> x + w - rx
                1 -> x + rx
                2 -> x + rx
                3 -> x + w - rx
                else -> 0f
            }

            val offsetY = when(i) {
                0 -> y + ry
                1 -> y + ry
                2 -> y + h - ry
                3 -> y + h - ry
                else -> 0f
            }

            when(i) {
                0 -> drawLine(x + rx, y, x + w - rx, y)
                1 -> drawLine(x, y + rx, x, y + h - rx)
                2 -> drawLine(x + rx, y + h, x + w - rx, y + h)
                3 -> drawLine(x + w, y + rx, x + w, y + h - rx)
            }

            for (j in 0 until 100) {

                val angle = Math.PI / 200 * j + Math.PI / 2 * i
                val nextAngle = Math.PI / 200 * (j + 1) + Math.PI / 2 * i

                drawLine(
                    offsetX + (rx * cos(angle).toFloat()),
                    offsetY - (ry * sin(angle)).toFloat(),
                    offsetX + (rx * cos(nextAngle).toFloat()),
                    offsetY - (ry * sin(nextAngle)).toFloat()
                )
            }
        }

        endMode = currentEndMode
    }

    fun drawVertices(coordinates: ArrayList<Float>) {
        applyMatrix()

        state = State.MESH
        fillMode = false

        for (i in 0 until coordinates.size / 2 - 1) {
            drawLine(coordinates[i * 2], coordinates[i * 2 + 1], coordinates[i * 2 + 2], coordinates[i * 2 + 3])
        }
    }

    override fun fillRect(x: Float, y: Float, w: Float, h: Float) {
        val save = transformation2D.save()

        translate(x, y)
        scale(w, h)

        applyMatrix()

        state = State.MESH
        fillMode = true

        basicShape.bind()

        basicShape.fillSquare()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    private fun drawLineRect(x: Float, y: Float, w: Float, h: Float) {
        val save = transformation2D.save()

        translate(x, y)
        scale(w, h)

        applyMatrix()

        state = State.MESH
        fillMode = false

        basicShape.bind()

        basicShape.fillSquare()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    fun fillRoundRect(x: Float, y: Float, w: Float, h: Float, dx: Float, dy: Float) {
        val rx = dx / 2f
        val ry = dy / 2f

        applyMatrix()

        val vertices = ArrayList<Float>()

        vertices.add(x + rx)
        vertices.add(y + ry)

        repeat(2) {
            vertices.add(x + w - rx)
            vertices.add(y + ry)

            vertices.add(x + rx)
            vertices.add(y + h - ry)
        }

        vertices.add(x + w - rx)
        vertices.add(y + h - ry)

        for (i in 0 until 4) {
            val offsetX = when(i) {
                0 -> x + w - rx
                1 -> x + rx
                2 -> x + rx
                3 -> x + w - rx
                else -> 0f
            }

            val offsetY = when(i) {
                0 -> y + ry
                1 -> y + ry
                2 -> y + h - ry
                3 -> y + h - ry
                else -> 0f
            }

            when(i) {
                0 -> {
                    vertices.add(offsetX)
                    vertices.add(offsetY)

                    repeat(2) {
                        vertices.add(offsetX + rx)
                        vertices.add(offsetY)

                        vertices.add(offsetX)
                        vertices.add(offsetY + h - 2 * ry)
                    }

                    vertices.add(offsetX + rx)
                    vertices.add(offsetY + h - 2 * ry)
                }
                1 -> {
                    vertices.add(offsetX)
                    vertices.add(offsetY - ry)

                    repeat(2) {
                        vertices.add(offsetX + w - 2 * rx)
                        vertices.add(offsetY - ry)

                        vertices.add(offsetX)
                        vertices.add(offsetY)
                    }

                    vertices.add(offsetX + w - 2 * rx)
                    vertices.add(offsetY)
                }
                2 -> {
                    vertices.add(offsetX - rx)
                    vertices.add(offsetY - h + 2 * ry)

                    repeat(2) {
                        vertices.add(offsetX)
                        vertices.add(offsetY - h + 2 * ry)

                        vertices.add(offsetX - rx)
                        vertices.add(offsetY)
                    }

                    vertices.add(offsetX)
                    vertices.add(offsetY)
                }
                3 -> {
                    vertices.add(offsetX - w + 2 * rx)
                    vertices.add(offsetY)

                    repeat(2) {
                        vertices.add(offsetX)
                        vertices.add(offsetY)

                        vertices.add(offsetX - w + 2 * rx)
                        vertices.add(offsetY + ry)
                    }

                    vertices.add(offsetX)
                    vertices.add(offsetY + ry)
                }
            }

            for (j in 0..100) {

                val angle = Math.PI / 200 * j + Math.PI / 2 * i
                val nextAngle = Math.PI / 200 * (j + 1) + Math.PI / 2 * i

                vertices.add(offsetX + (rx * cos(angle).toFloat()))
                vertices.add(offsetY - (ry * sin(angle)).toFloat())
                vertices.add(offsetX + (rx * cos(nextAngle).toFloat()))
                vertices.add(offsetY - (ry * sin(nextAngle)).toFloat())
                vertices.add(offsetX)
                vertices.add(offsetY)
            }
        }

        state = State.MESH
        fillMode = false

        basicShape.bind()

        basicShape.drawFill(basicShape.customMesh(vertices.toFloatArray(), 2))
    }

    override fun gradRect(x: Float, y: Float, w: Float, h: Float, x1: Float, y1: Float, c1: IntArray, x2: Float, y2: Float, c2: IntArray) {
        setGradient(x1, y1, c1[0].toFloat(), c1[1].toFloat(), c1[2].toFloat(), x2, y2, c2[0].toFloat(), c2[1].toFloat(), c2[2].toFloat())
        fillRect(x, y, w, h)
    }

    override fun gradRectAlpha(x: Float, y: Float, w: Float, h: Float, x1: Float, y1: Float, a1: Int, c1: IntArray, x2: Float, y2: Float, a2: Int, c2: IntArray) {
        setGradient(x1, y1, c1[0].toFloat(), c1[1].toFloat(), c1[2].toFloat(), a1.toFloat(), y1, y2, c2[0].toFloat(), c2[1].toFloat(), c2[2].toFloat(), a2.toFloat())
        fillRect(x, y, w, h)
    }
    override fun setComposite(mode: Int, p0: Int, p1: Int) {
        when(mode) {
            FakeGraphics.DEF -> {
               blend = Blend.SOURCE
               setAlpha(255f)
            }
            FakeGraphics.TRANS -> {
                blend = Blend.SOURCE
                setAlpha(p0.toFloat())
            }
            FakeGraphics.BLEND -> {
                when(p1) {
                    1 -> blend = Blend.ADD
                    2 -> blend = Blend.SUBTRACT
                    3 -> blend = Blend.MULTIPLY
                }

                setAlpha(p0.toFloat())
            }
        }
    }

    override fun setRenderingHint(key: Int, obj: Int) {

    }

    // ==================================================
    // |                     Color                      |
    // ==================================================

    override fun setColor(c: Int) {
        when(c) {
            FakeGraphics.RED -> setColor(255f, 0f, 0f)
            FakeGraphics.YELLOW -> setColor(255f, 255f, 0f)
            FakeGraphics.BLACK -> setColor(0f, 0f, 0f)
            FakeGraphics.MAGENTA -> setColor(255f, 0f, 255f)
            FakeGraphics.BLUE -> setColor(0f, 0f, 255f)
            FakeGraphics.CYAN -> setColor(0f, 255f, 255f)
            FakeGraphics.WHITE -> setColor(255f, 255f, 255f)
        }
    }

    override fun setColor(r: Int, g: Int, b: Int) {
        setColor(r.toFloat(), g.toFloat(), b.toFloat())
    }

    fun setColor(r: Float, g: Float, b: Float) {
        firstColor[0] = r / 255f
        secondColor[0] = r / 255f

        firstColor[1] = g / 255f
        secondColor[1] = g / 255f

        firstColor[2] = b / 255f
        secondColor[2] = b / 255f

        firstColor[3] = 1f
        secondColor[3] = 1f

        program.setVector4("color1", firstColor)
        program.setVector4("color2", secondColor)
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        firstColor[0] = r / 255f
        secondColor[0] = r / 255f

        firstColor[1] = g / 255f
        secondColor[1] = g / 255f

        firstColor[2] = b / 255f
        secondColor[2] = b / 255f

        firstColor[3] = a / 255f
        secondColor[3] = a / 255f

        program.setVector4("color1", firstColor)
        program.setVector4("color2", secondColor)
    }

    private fun setAlpha(a: Float) {
        program.setFloat("alpha", a / 255f)
    }

    private fun setGradient(x1: Float, y1: Float, r1: Float, g1: Float, b1: Float, x2: Float, y2: Float, r2: Float, g2: Float, b2: Float) {
        firstStep[0] = x1
        firstStep[1] = y1

        secondStep[0] = x2
        secondStep[1] = y2

        firstColor[0] = r1 / 255f
        secondColor[0] = r2 / 255f

        firstColor[1] = g1 / 255f
        secondColor[1] = g2 / 255f

        firstColor[2] = b1 / 255f
        secondColor[2] = b2 / 255f

        firstColor[3] = 1f
        secondColor[3] = 1f

        program.setVector2("step1", firstStep)
        program.setVector2("step2", secondStep)

        program.setVector4("color1", firstColor)
        program.setVector4("color2", secondColor)
    }

    private fun setGradient(x1: Float, y1: Float, r1: Float, g1: Float, b1: Float, a1: Float, x2: Float, y2: Float, r2: Float, g2: Float, b2: Float, a2: Float) {
        firstStep[0] = x1
        firstStep[1] = y1

        secondStep[0] = x2
        secondStep[1] = y2

        firstColor[0] = r1 / 255f
        secondColor[0] = r2 / 255f

        firstColor[1] = g1 / 255f
        secondColor[1] = g2 / 255f

        firstColor[2] = b1 / 255f
        secondColor[2] = b2 / 255f

        firstColor[3] = a1 / 255f
        secondColor[3] = a2 / 255f

        program.setVector2("step1", firstStep)
        program.setVector2("step2", secondStep)

        program.setVector4("color1", firstColor)
        program.setVector4("color2", secondColor)
    }

    fun setStroke(width: Float, endMode: LineEndMode) {
        stroke = width
        this.endMode = endMode
    }

    fun setStrokeType(type: LineType, factor: Float, pattern: Int) {
        dashMode = type

        if (dashMode == LineType.DASH) {
            program.setFloat("factor", factor)
            program.setUnsignedInt("pattern", pattern)
        }
    }

    // ==================================================
    // |                 Transformation                 |
    // ==================================================

    override fun getTransform(): FakeTransform {
        return transformation2D.save()
    }

    override fun setTransform(at: FakeTransform) {
        if (at is Transformation2D) {
            transformation2D.restore(at)
        }
    }

    override fun translate(x: Float, y: Float) {
        transformation2D.translate(x, y)
    }

    override fun rotate(radian: Float) {
        transformation2D.rotate(radian)
    }

    override fun scale(sx: Float, sy: Float) {
        transformation2D.scale(sx, sy)
    }

    override fun delete(at: FakeTransform) {
        if (at is Transformation2D)
            Transformation2D.giveBack(at)
    }

    fun reset() {
        transformation2D.reset()

        applyMatrix()
    }

    fun syncProjection() {
        projection.orthogonal(0f, renderSession.width.toFloat(), renderSession.height.toFloat(), 0f)

        projection.connectWithProgram(program, true)
    }

    fun applyMatrix() {
        transformation2D.connectWithProgram(program, false)
    }

    fun clearUpTexture() {
        usedTexture.forEach { mesh -> mesh.unload() }
        usedFontModel.forEach { font -> font.flush() }
    }
}