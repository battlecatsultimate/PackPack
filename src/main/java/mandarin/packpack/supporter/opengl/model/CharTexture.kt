package mandarin.packpack.supporter.opengl.model

import mandarin.packpack.supporter.opengl.Texture

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
        texture.release()
    }
}