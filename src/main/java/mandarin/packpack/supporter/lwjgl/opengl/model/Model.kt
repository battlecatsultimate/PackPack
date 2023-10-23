package mandarin.packpack.supporter.lwjgl.opengl.model

abstract class Model {
    companion object {
        private val modelList = ArrayList<Model>()

        fun findBoundModel() : Model? {
            return modelList.find { model -> model.bound }
        }
    }

    protected var bound = false

    fun bind() {
        if (bound)
            return

        val boundModel = findBoundModel()

        if (boundModel != null) {
            boundModel.bound = false
        }

        doBind()
    }

    internal abstract fun doBind()
}