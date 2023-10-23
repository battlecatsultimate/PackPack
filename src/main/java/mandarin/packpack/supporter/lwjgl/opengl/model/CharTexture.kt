package mandarin.packpack.supporter.lwjgl.opengl.model

import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.lwjgl.opengl.Texture

class CharTexture(
    val texture: Texture,
    val vertexOffset: Long,
    val width: Float,
    val height: Float,
    val bearingX: Float,
    val bearingY: Float,
    val advance: Float
) {

    fun release() {
        Logger.addLog("Releasing CharTexture")

        texture.release()
    }
}