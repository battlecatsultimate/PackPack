import mandarin.card.supporter.PositiveMap
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val a = 0.00000000212345

            val b = BigDecimal.valueOf(a).round(MathContext(5, RoundingMode.HALF_EVEN))

            println(String.format("%5g", a))
            println(b.toPlainString())
        }
    }
}