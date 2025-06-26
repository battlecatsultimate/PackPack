@file:Suppress("SameParameterValue")

import glm_.asHexString


@Suppress("unused")
class TestUnit {
    class Data(private val a: Int) {
        fun getCode() : String {
            return this.hashCode().asHexString.substring(0, 5)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val a = Data(10)
            val b = Data(10)
            val c = Data(20)

            println(a == b)

            println(a.getCode() + ", " + b.getCode() + ", " + c.getCode())
        }
    }
}