package mandarin.card.supporter

import java.math.BigDecimal
import java.math.BigInteger

class PositiveMap<K, V> : HashMap<K, V>() where V : Number {
    override fun put(key: K, value: V): V? {
        val isZero = when (value) {
            is Byte -> value <= 0.toByte()
            is Short -> value <= 0.toShort()
            is Int -> value <= 0
            is Long -> value <= 0L
            is Float -> value <= 0f
            is Double -> value <= 0.0
            is BigInteger -> value <= BigInteger.ZERO
            is BigDecimal -> value <= BigDecimal.ZERO
            else -> false
        }

        if (!containsKey(key) && isZero) {
            return null
        }

        val old = super.put(key, value)

        if (isZero) {
            remove(key)
        }

        return old
    }
}