package mandarin.packpack.supporter.lwjgl

import common.system.fake.FakeGraphics
import common.system.fake.FakeImage
import common.system.fake.FakeTransform
import mandarin.packpack.supporter.opengl.Program
import mandarin.packpack.supporter.opengl.RenderSession
import mandarin.packpack.supporter.opengl.model.FontModel
import mandarin.packpack.supporter.opengl.model.MeshModel
import mandarin.packpack.supporter.opengl.model.TextureMesh
import org.lwjgl.opengl.GL33

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

    companion object {
        private val basicShape = MeshModel()
    }

    var fontModel: FontModel? = null

    private val transformation2D = Transformation2D()
    private val projection = Transformation2D()

    private val firstStep = floatArrayOf(0f, 0f)
    private val secondStep = floatArrayOf(0f, 0f)

    private val firstColor = floatArrayOf(1f, 1f, 1f, 1f)
    private val secondColor = floatArrayOf(1f, 1f, 1f, 1f)

    var state = State.MESH
        set(value) {
            program.setInt("state", value.ordinal)

            field = value
        }

    var blend: Blend = Blend.SOURCE
        set(value) {
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
    }

    fun drawText(text: String, x: Float, y: Float) {
        val font = fontModel ?: return

        val save = transformation2D.save()

        state = State.TEXT

        font.loadText(text)

        translate(x, y)

        applyMatrix()

        font.drawText(this, text)

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    fun drawImage(texture: TextureMesh, x: Float, y: Float, w: Float, h: Float) {
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
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        applyMatrix()

        state = State.MESH

        basicShape.bind()

        basicShape.drawLine(x1, y1, x2, y2)
    }

    override fun drawOval(x: Float, y: Float, rx: Float, ry: Float) {
        val save = transformation2D.save()

        translate(x, y)
        scale(rx, ry)

        applyMatrix()

        state = State.MESH

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

        basicShape.bind()

        basicShape.lineSquare()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    override fun fillOval(x: Float, y: Float, rx: Float, ry: Float) {
        val save = transformation2D.save()

        translate(x, y)
        scale(rx, ry)

        applyMatrix()

        state = State.MESH

        basicShape.bind()

        basicShape.fillCircle()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    override fun fillRect(x: Float, y: Float, w: Float, h: Float) {
        val save = transformation2D.save()

        translate(x, y)
        scale(w, h)

        applyMatrix()

        state = State.MESH

        basicShape.bind()

        basicShape.fillSquare()

        transformation2D.restore(save)
        Transformation2D.giveBack(save)
    }

    override fun getTransform(): FakeTransform {
        return transformation2D.save()
    }

    override fun gradRect(x: Float, y: Float, w: Float, h: Float, x1: Float, y1: Float, c1: IntArray, x2: Float, y2: Float, c2: IntArray) {
        setGradient(x1, y1, c1[0].toFloat(), c1[1].toFloat(), c1[2].toFloat(), x2, y2, c2[0].toFloat(), c2[1].toFloat(), c2[2].toFloat())
        fillRect(x, y, w, h)
    }

    override fun gradRectAlpha(x: Float, y: Float, w: Float, h: Float, x1: Float, y1: Float, a1: Int, c1: IntArray, x2: Float, y2: Float, a2: Int, c2: IntArray) {
        setGradient(x1, y1, c1[0].toFloat(), c1[1].toFloat(), c1[2].toFloat(), y1, y2, c2[0].toFloat(), c2[1].toFloat(), c2[2].toFloat())
        fillRect(x, y, w, h)
    }

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

    override fun setTransform(at: FakeTransform) {
        if (at is Transformation2D) {
            transformation2D.restore(at)
        }
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

    fun setAlpha(a: Float) {
        program.setFloat("alpha", a / 255f)
    }

    fun setGradient(x1: Float, y1: Float, r1: Float, g1: Float, b1: Float, x2: Float, y2: Float, r2: Float, g2: Float, b2: Float) {
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
}