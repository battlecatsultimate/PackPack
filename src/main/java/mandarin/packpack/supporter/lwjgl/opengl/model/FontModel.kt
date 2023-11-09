package mandarin.packpack.supporter.lwjgl.opengl.model

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lwjgl.GLGraphics
import mandarin.packpack.supporter.lwjgl.opengl.Texture
import mandarin.packpack.supporter.lwjgl.opengl.buffer.VAO
import mandarin.packpack.supporter.lwjgl.opengl.buffer.VBO
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.freetype.FT_BitmapGlyph
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FreeType
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

class FontModel(private val size: Float, file: File, type: Type, stroke: Float = 0f) : Model() {
    enum class Type {
        STROKE,
        FILL
    }

    companion object {
        const val VERTEX_SIZE = 4 * Float.SIZE_BYTES
        const val INDEX_SIZE = 6
        const val DIMENSION = 2

        private var library = -1L

        fun initialize() {
            if (library != -1L)
                return

            val stack = MemoryStack.stackPush()
            val libraryPointer = stack.mallocPointer(1)

            val code = FreeType.FT_Init_FreeType(libraryPointer)

            if (code != FreeType.FT_Err_Ok) {
                throw RuntimeException("Failed to initialize FreeType : $code")
            }

            library = libraryPointer.get()
        }
    }

    var type = Type.FILL
        set(value) {
            if (field == value)
                return

            if (value == Type.STROKE && stroke == 0f)
                stroke = 1f

            field = value

            flush()
        }
    private var stroke = 0f
        set(value) {
            if (field == value)
                return

            field = value

            if (type == Type.STROKE)
                flush()
        }

    private val characters = HashMap<Char, CharTexture>()
    private val vertexVBO = VBO.build(VBO.Type.BUFFER)

    private val face: FT_Face

    var maxHeight = 0f
        private set

    init {
        Logger.addLog("Generating FontModel from file : ${file.absolutePath}")

        initialize()

        this.type = type
        this.stroke = stroke

        if (stroke < 0f) {
            throw IllegalStateException("Stroke can't be below 0!")
        }

        bind()

        vertexVBO.renewBuffer(0L, VBO.Method.DYNAMIC, VBO.Purpose.DRAW)

        val stack = MemoryStack.stackPush()

        val facePointer = stack.mallocPointer(1)

        var code = FreeType.FT_New_Face(library, file.absolutePath, 0L, facePointer)

        if (code != FreeType.FT_Err_Ok) {
            throw RuntimeException("Failed to initialize FreeType Face : $code")
        }

        val strokerPointer = stack.mallocPointer(1)

        code = FreeType.FT_Stroker_New(library, strokerPointer)

        if (code != FreeType.FT_Err_Ok) {
            throw RuntimeException("Failed to initialize Stroker : $code")
        }

        face = FT_Face.create(facePointer.get())

        stack.pop()
    }

    fun drawText(g: GLGraphics, text: String) {
        val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

        bind()

        if (!vertexVBO.bound) {
            throw IllegalStateException("You have to bind vertex VBO of this font model first!")
        }

        val t = g.transform

        for (letter in text) {
            val charTexture = characters[letter] ?: space

            charTexture.texture.bind()

            VAO.vao.pointerVBO(vertexVBO, VAO.Attribute.VERTEX, VAO.ValueType.FLOAT, DIMENSION, stride = VERTEX_SIZE, offsetPointer = charTexture.vertexOffset)
            VAO.vao.pointerVBO(vertexVBO, VAO.Attribute.UV, VAO.ValueType.FLOAT, DIMENSION, stride = VERTEX_SIZE, offsetPointer = charTexture.vertexOffset + DIMENSION * Float.SIZE_BYTES)

            VAO.vao.setVertexAttributeArray(VAO.Attribute.VERTEX, true)
            VAO.vao.setVertexAttributeArray(VAO.Attribute.UV, true)


            if (type == Type.STROKE) {
                g.translate(charTexture.bearingX - stroke, -charTexture.bearingY - stroke)
            } else {
                g.translate(charTexture.bearingX, -charTexture.bearingY)
            }

            g.applyMatrix()

            GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, INDEX_SIZE)

            if (type == Type.STROKE) {
                g.translate(charTexture.advance - charTexture.bearingX + stroke, charTexture.bearingY + stroke)
            } else {
                g.translate(charTexture.advance - charTexture.bearingX, charTexture.bearingY)
            }

            g.applyMatrix()
        }

        g.transform = t
    }

    private fun loadText(text: String) {
        if (!text.any { letter -> !characters.containsKey(letter) })
            return

        val stack = MemoryStack.stackPush()

        var code: Int

        FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE)
        FreeType.FT_Set_Char_Size(face, 0, (size * 64).toLong(), 0, 0)

        bind()

        val strokerPointer = if (type == Type.STROKE) {
            val stroker = stack.mallocPointer(1)

            code = FreeType.FT_Stroker_New(library, stroker)

            if (code != FreeType.FT_Err_Ok) {
                throw RuntimeException("Failed to generate Stroker : $code")
            }

            stroker.get()
        } else {
            -1L
        }

        if (strokerPointer != -1L) {
            FreeType.FT_Stroker_Set(strokerPointer, (stroke * 64).toLong(), FreeType.FT_STROKER_LINECAP_ROUND, FreeType.FT_STROKER_LINEJOIN_ROUND, 0)
        }

        for (letter in collectLetters(text)) {
            if (characters.containsKey(letter))
                continue

            val index = FreeType.FT_Get_Char_Index(face, letter.code.toLong())

            if (index == 0) {
                println("No character $letter")

                continue
            }

            code = FreeType.FT_Load_Glyph(face, index, FreeType.FT_LOAD_DEFAULT)

            if (code != FreeType.FT_Err_Ok) {
                throw RuntimeException("Failed to load glyph of letter $letter : $code")
            }

            val glyphSlot = face.glyph() ?: throw NullPointerException("Failed to get glyph slot")

            val glyphPointer = stack.mallocPointer(1)

            code = FreeType.FT_Get_Glyph(glyphSlot, glyphPointer)

            if (code != FreeType.FT_Err_Ok) {
                throw RuntimeException("Failed to get glyph of letter $letter : $code")
            }

            if (type == Type.STROKE && strokerPointer != -1L) {
                code = FreeType.FT_Glyph_Stroke(glyphPointer, strokerPointer, true)

                if (code != FreeType.FT_Err_Ok) {
                    throw RuntimeException("Failed to apply stroker to glyph of letter $letter : $code")
                }
            }

            code = FreeType.FT_Glyph_To_Bitmap(glyphPointer, FreeType.FT_RENDER_MODE_NORMAL, null, true)

            if (code != FreeType.FT_Err_Ok) {
                throw RuntimeException("Failed to convert glyph of letter $letter to bitmap : $code")
            }

            val glyphBitmap = FT_BitmapGlyph.create(glyphPointer.get())

            val bitmap = glyphBitmap.bitmap()

            val width = bitmap.width().toFloat()
            val height = bitmap.rows().toFloat()

            val vertexMap = floatArrayOf(
                0f   , 0f    ,     0f, 0f,
                0f   , height,     0f, 1f,
                width, height,     1f, 1f,
                0f   , 0f    ,     0f, 0f,
                width, height,     1f, 1f,
                width, 0f    ,     1f, 0f
            )

            val vertexOffset = characters.size.toLong() * VERTEX_SIZE * INDEX_SIZE

            vertexVBO.injectBuffer(vertexMap, vertexOffset)

            val charTexture = CharTexture(
                Texture.build(bitmap),
                vertexOffset,
                bitmap.width().toFloat(),
                bitmap.rows().toFloat(),
                glyphSlot.bitmap_left().toFloat(),
                glyphSlot.bitmap_top().toFloat(),
                glyphSlot.advance().x() / 64f)

            characters[letter] = charTexture
        }

        if (strokerPointer != -1L) {
            FreeType.FT_Stroker_Done(strokerPointer)
        }

        stack.pop()
    }

    fun measureDimension(text: String) : FloatArray {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val dimension = AtomicReference(FloatArray(4))

            StaticStore.renderManager.queueGL {
                loadText(text)

                val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

                var positiveY = 0f
                var negativeY = 0f

                text.forEachIndexed { index, letter ->
                    val texture = characters[letter] ?: space

                    maxHeight = max(maxHeight, texture.height)

                    if (index < text.length - 1) {
                        if (index == 0) {
                            dimension.get()[0] = texture.bearingX
                            dimension.get()[2] += texture.advance - texture.bearingX
                        } else {
                            dimension.get()[2] += texture.advance
                        }
                    } else {
                        dimension.get()[2] += texture.bearingX + texture.width
                    }

                    dimension.get()[1] = min(dimension.get()[1], -texture.bearingY)

                    positiveY = max(positiveY, texture.bearingY)
                    negativeY = max(negativeY, texture.height - texture.bearingY)
                }

                dimension.get()[3] = positiveY + negativeY

                waiter.countDown()
            }

            waiter.await()

            dimension.get()
        } else {
            val dimension = FloatArray(4)

            loadText(text)

            val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

            var positiveY = 0f
            var negativeY = 0f

            text.forEachIndexed { index, letter ->
                val texture = characters[letter] ?: space

                maxHeight = max(maxHeight, texture.height)

                if (index < text.length - 1) {
                    if (index == 0) {
                        dimension[0] = texture.bearingX
                        dimension[2] += texture.advance - texture.bearingX
                    } else {
                        dimension[2] += texture.advance
                    }
                } else {
                    dimension[2] += texture.bearingX + texture.width
                }

                dimension[1] = min(dimension[1], -texture.bearingY)

                positiveY = max(positiveY, texture.bearingY)
                negativeY = max(negativeY, texture.height - texture.bearingY)
            }

            dimension[3] = positiveY + negativeY

            dimension
        }
    }

    fun textWidth(text: String) : Float {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val dimension = AtomicReference(0f)

            StaticStore.renderManager.queueGL {
                loadText(text)

                val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

                text.forEachIndexed { index, letter ->
                    val texture = characters[letter] ?: space

                    maxHeight = max(maxHeight, texture.height)

                    dimension.set(dimension.get() + if (index < text.length - 1) {
                        if (index == 0) {
                            texture.advance - texture.bearingX
                        } else {
                            texture.advance
                        }
                    } else {
                        texture.bearingX + texture.width
                    })
                }

                waiter.countDown()
            }

            waiter.await()

            dimension.get()
        } else {
            loadText(text)

            val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

            var width = 0f

            text.forEachIndexed { index, letter ->
                val texture = characters[letter] ?: space

                maxHeight = max(maxHeight, texture.height)

                width += if (index < text.length - 1) {
                    if (index == 0) {
                        texture.advance - texture.bearingX
                    } else {
                        texture.advance
                    }
                } else {
                    texture.bearingX + texture.width
                }
            }

            width
        }
    }

    fun trueWidth(text: String) : Float {
        val waiter = if (!Thread.currentThread().equals(StaticStore.renderManager.renderThread)) {
            CountDownLatch(1)
        } else {
            null
        }

        return if (waiter != null) {
            val dimension = AtomicReference(0f)

            StaticStore.renderManager.queueGL {
                loadText(text)

                val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

                text.forEachIndexed { index, letter ->
                    val texture = characters[letter] ?: space

                    maxHeight = max(maxHeight, texture.height)

                    dimension.set(dimension.get() + if (index == 0) {
                        texture.advance - texture.bearingX
                    } else {
                        texture.advance
                    })
                }

                waiter.countDown()
            }

            waiter.await()

            dimension.get()
        } else {
            loadText(text)

            val space = characters[' '] ?: throw IllegalStateException("This font doesn't support space letter, invalid font\n\nPlease load font before drawing")

            var width = 0f

            text.forEachIndexed { index, letter ->
                val texture = characters[letter] ?: space

                maxHeight = max(maxHeight, texture.height)

                width += if (index == 0) {
                    texture.advance - texture.bearingX
                } else {
                    texture.advance
                }
            }

            width
        }
    }

    private fun collectLetters(text: String) : Set<Char> {
        val result = ArrayList<Char>()

        result.addAll(text.toList())
        result.add(' ')

        return HashSet(result)
    }

    override fun doBind() {
        vertexVBO.bind()
    }

    fun flush() {
        Logger.addLog("Flushing FontModel")

        characters.forEach { (_, texture) -> texture.release() }

        characters.clear()
    }

    fun release() {
        Logger.addLog("Releasing FontModel")

        flush()

        vertexVBO.release()
    }
}