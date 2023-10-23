package mandarin.packpack.supporter.lwjgl.opengl

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lwjgl.GLGraphics
import mandarin.packpack.supporter.lwjgl.opengl.buffer.MultiSampler
import mandarin.packpack.supporter.lwjgl.opengl.buffer.PostProcessor
import mandarin.packpack.supporter.lwjgl.opengl.buffer.VAO
import mandarin.packpack.supporter.lwjgl.opengl.model.SpriteSheet
import okhttp3.internal.and
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class RenderSession {
    companion object {
        private var initialized = false

        fun build(manager: RenderSessionManager, width: Int, height: Int, resizable: Boolean, show: Boolean, title: String = "Title", programGenerator: () -> Program) : RenderSession {
            return RenderSession(manager, width, height, resizable, show, title, programGenerator)
        }

        fun build(manager: RenderSessionManager, width: Int, height: Int, resizable: Boolean, show: Boolean, title: String = "Title", parent: RenderSession) : RenderSession {
            return RenderSession(manager, width, height, resizable, show, title, parent)
        }

        private fun initialize() {
            GLFWErrorCallback.createPrint(System.err).set()

            if (!GLFW.glfwInit()) {
                throw IllegalStateException("Unable to initialize GLFW")
            }

            initialized = true
        }
    }

    val windowID: Long

    var width: Int
        private set
    var height: Int
        private set

    private val manager: RenderSessionManager

    private val multiSampler: MultiSampler
    private val postProcessor: PostProcessor

    internal val program: Program

    private val graphics: GLGraphics

    private val pngExecuting = ArrayList<Boolean>()

    private var renderQueues = ArrayList<(GLGraphics) -> Unit>()
    private var onFinish: (() -> Unit)? = null
    private var onExport: ((Int) -> File)? = null

    lateinit var targetFolder: File
    private var progress = 0

    private constructor(manager: RenderSessionManager, width: Int, height: Int, resizable: Boolean, show: Boolean, title: String, programGenerator: () -> Program) {
        this.width = width
        this.height = height

        this.manager = manager

        if (!initialized) {
            initialize()
        }

        GLFW.glfwDefaultWindowHints()

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, if (show) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, if (resizable) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 8)

        windowID = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)

        if (windowID == MemoryUtil.NULL) {
            throw RuntimeException("Failed to create the GLFW Window")
        }

        val pixelWidth = IntArray(1)
        val pixelHeight = IntArray(1)

        GLFW.glfwGetWindowSize(windowID, pixelWidth, pixelHeight)

        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()) ?:
        throw RuntimeException("Failed to get resolution of monitor")

        GLFW.glfwSetWindowPos(
            windowID,
            (videoMode.width() - pixelWidth[0]) / 2,
            (videoMode.height() - pixelHeight[0]) / 2
        )

        GLFW.glfwMakeContextCurrent(windowID)

        GL.createCapabilities()

        multiSampler = MultiSampler.build(this)

        postProcessor = PostProcessor.build(this)

        program = programGenerator.invoke()

        program.use()

        program.setVector2("screenSize", floatArrayOf(width.toFloat(), height.toFloat()))

        graphics = GLGraphics(this, program)

        GLFW.glfwSwapInterval(1)

        if (show) {
            GLFW.glfwShowWindow(windowID)
        }
    }

    private constructor(manager: RenderSessionManager, width: Int, height: Int, resizable: Boolean, show: Boolean, title: String, parent: RenderSession) {
        this.width = width
        this.height = height

        this.manager = manager

        if (!initialized) {
            initialize()
        }

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, if (show) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, if (resizable) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)

        windowID = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, parent.windowID)
        Logger.addLog("Generating new GLFW window : $windowID")

        if (windowID == MemoryUtil.NULL) {
            throw RuntimeException("Failed to create the GLFW Window")
        }

        val pixelWidth = IntArray(1)
        val pixelHeight = IntArray(1)

        GLFW.glfwGetWindowSize(windowID, pixelWidth, pixelHeight)

        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()) ?:
        throw RuntimeException("Failed to get resolution of monitor")

        GLFW.glfwSetWindowPos(
            windowID,
            (videoMode.width() - pixelWidth[0]) / 2,
            (videoMode.height() - pixelHeight[0]) / 2
        )

        GLFW.glfwMakeContextCurrent(windowID)

        VAO.switchVAO(this)

        multiSampler = MultiSampler.build(this)
        postProcessor = PostProcessor.build(this)

        program = parent.program

        program.use()

        program.setVector2("screenSize", floatArrayOf(postProcessor.width.toFloat(), postProcessor.height.toFloat()))

        graphics = GLGraphics(this, program)

        GLFW.glfwSwapInterval(1)

        if (show) {
            GLFW.glfwShowWindow(windowID)
        }
    }

    fun getGraphics() : GLGraphics {
        return graphics
    }

    fun close() {
        GLFW.glfwDestroyWindow(windowID)
    }

    fun queue(q: (GLGraphics) -> Unit) {
        renderQueues.add(q)
    }

    fun onFinish(finisher: (() -> Unit)?) {
        onFinish = finisher
    }

    fun onExport(exporter: ((Int) -> File)?) {
        onExport = exporter
    }

    fun drawAndExport() {
        val queue = if (renderQueues.isNotEmpty()) {
            renderQueues.removeFirst()
        } else {
            null
        } ?: return

        multiSampler.bind()

        GL33.glClearColor(0f, 0f, 0f, 0f)
        GL33.glClear(GL33.GL_COLOR_BUFFER_BIT or GL33.GL_DEPTH_BUFFER_BIT)

        graphics.reset()

        queue.invoke(graphics)

        //Post Process
        postProcessor.registerMultiSampler(multiSampler)
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0)

        val blend = graphics.blend

        graphics.state = GLGraphics.State.RENDER
        graphics.blend = GLGraphics.Blend.SOURCE

        postProcessor.bind()
        GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, SpriteSheet.INDEX_SIZE)

        graphics.blend = blend

        GLFW.glfwSwapBuffers(windowID)

        export()

        progress++
    }

    private fun export() {
        if (!this::targetFolder.isInitialized)
            return

        val buffer = ByteBuffer.allocateDirect(postProcessor.width * postProcessor.height * 4)

        GL33.glGetTexImage(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buffer)

        val array = IntArray(buffer.limit() / 4)

        for (x in 0 until postProcessor.width) {
            for (y in 0 until postProcessor.height) {
                val r = buffer[(y * postProcessor.width + x) * 4] and 0xFF
                val g = buffer[(y * postProcessor.width + x) * 4 + 1] and 0xFF
                val b = buffer[(y * postProcessor.width + x) * 4 + 2] and 0xFF
                val a = buffer[(y * postProcessor.width + x) * 4 + 3] and 0xFF

                array[((postProcessor.height - y - 1) * postProcessor.width + x)] = (a shl 24) + (r shl 16) + (g shl 8) + b
            }
        }

        val image = BufferedImage(postProcessor.width, postProcessor.height, BufferedImage.TYPE_INT_ARGB)

        image.setRGB(0, 0, postProcessor.width, postProcessor.height, array, 0, postProcessor.width)

        val file = onExport?.invoke(progress) ?: File(targetFolder, quad(progress) + ".png")

        if (!file.exists() && !file.createNewFile())
            return

        pngExecuting.add(true)

        StaticStore.executorHandler.post {
            try {
                ImageIO.write(image, "PNG", file)
            } catch (e: Exception) {
                StaticStore.logger.uploadErrorLog(e, "E/RenderSession::export - Failed to export png file")
            }

            pngExecuting.removeAt(0)
        }
    }

    fun done() : Boolean {
        val done = renderQueues.isEmpty() && pngExecuting.isEmpty()

        if (done) {
            onFinish?.invoke()

            graphics.clearUpTexture()
        }

        return done
    }

    fun release() {
        Logger.addLog("Releasing Window : $windowID")

        removeEvents()

        Callbacks.glfwFreeCallbacks(windowID)
        GLFW.glfwDestroyWindow(windowID)

        multiSampler.release()
        postProcessor.release()

        VAO.releaseVAO(this)
    }

    private fun removeEvents() {
        GLFW.glfwSetKeyCallback(windowID, null)
        GLFW.glfwSetWindowSizeCallback(windowID, null)
    }

    private fun quad(n: Int) : String {
        return if (n < 10) {
            "000$n"
        } else if (n < 100) {
            "00$n"
        } else if (n < 1000) {
            "0$n"
        } else {
            n.toString()
        }
    }
}