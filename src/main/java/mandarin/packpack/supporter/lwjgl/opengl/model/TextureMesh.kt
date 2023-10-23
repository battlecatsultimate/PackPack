package mandarin.packpack.supporter.lwjgl.opengl.model

class TextureMesh internal constructor(val vertexOffset: Long, val spriteSheet: SpriteSheet, val offsetX: Float, val offsetY: Float, val width: Float, val height: Float) {
    fun draw() {
        spriteSheet.drawMesh(this)
    }
}