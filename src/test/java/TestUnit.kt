@file:Suppress("SameParameterValue")

import mandarin.card.supporter.PositiveMap
import mandarin.packpack.supporter.calculation.Equation
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.min

@Suppress("unused")
class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println(getOdds(15, 7, 1, 5))
        }

        private fun factorial(n: BigInteger) : BigInteger {
            if (n <= BigInteger.ONE)
                return BigInteger.ONE

            return n * factorial(n - BigInteger.ONE)
        }

        private fun nCr(n: BigInteger, r: BigInteger) : BigInteger {
            if (r > n)
                return BigInteger.ZERO

            return factorial(n) / (factorial(r) * factorial(n - r))
        }

        private fun getStacks(slotSize: Int, sameEmoji: Int, sequence: Int) : List<IntArray> {
            val result = ArrayList<IntArray>()

            if (sequence == slotSize) {
                result.add(intArrayOf(sequence))

                return result
            }

            if (sequence == sameEmoji) {
                result.add(intArrayOf(sequence))

                return result
            }

            if (sequence > slotSize || sequence > sameEmoji) {
                return result
            }

            val stack = sequence + 1

            var possibleSTack = min(sequence, sameEmoji - sequence)

            while (possibleSTack >= 1) {
                val subStacks = getSubStacks(slotSize - stack, sameEmoji - sequence, possibleSTack)

                for (subStack in subStacks) {
                    val subList = ArrayList<Int>()

                    subList.add(sequence)

                    for (stackElement in subStack) {
                        subList.add(stackElement)
                    }

                    if (subList.sum() == sameEmoji) {
                        result.add(subList.toIntArray())
                    }
                }

                possibleSTack -= 1
            }

            return result
        }

        private fun getSubStacks(slotSize: Int, sameEmoji: Int, sequence: Int) : List<IntArray> {
            val result = ArrayList<IntArray>()

            if (sequence > slotSize || sequence > sameEmoji)
                return result

            val stack = sequence + 1
            var possibleStack = min(sequence, sameEmoji - sequence)

            if (possibleStack == 0) {
                result.add(intArrayOf(sequence))
            } else {
                while (possibleStack >= 1) {
                    val subStacks = getSubStacks(slotSize - stack, sameEmoji - sequence, possibleStack)

                    if (subStacks.isEmpty()) {
                        result.add(intArrayOf(sequence))
                    } else {
                        for (subStack in subStacks) {
                            val subList = ArrayList<Int>()

                            subList.add(sequence)

                            for (stackElement in subStack) {
                                subList.add(stackElement)
                            }

                            result.add(subList.toIntArray())
                        }
                    }

                    possibleStack -= 1
                }
            }

            return result
        }

        private fun calculateOccasion(slotSize: Int, sameEmoji: Int, sequence: Int) : BigInteger {
            if (slotSize < sameEmoji) {
                throw IllegalStateException("E/SlotMachine::calculateOccasion - Out of bounds : slotSize < sameEmoji => %d < %d".format(slotSize, sameEmoji))
            }

            if (slotSize < sequence) {
                throw IllegalStateException("E/SlotMachine::calculateOccasion - Out of bounds : slotSize < sequence => %d < %d".format(slotSize, sequence))
            }

            if (sameEmoji < sequence) {
                throw IllegalStateException("E/SlotMachine::calculateOccasion - Out of bounds : sameEmoji > sequence => %d < %d".format(sameEmoji, sequence))
            }

            if (slotSize == sameEmoji && slotSize == sequence)
                return BigInteger.ONE

            if (sameEmoji < 2 * sequence) {
                val edge = BigInteger.TWO * nCr(BigInteger.valueOf((slotSize - sequence - 1).toLong()), BigInteger.valueOf((sameEmoji - sequence).toLong()))
                val middle = BigInteger.valueOf((slotSize - 2 - sequence + 1).toLong()) * nCr(BigInteger.valueOf((slotSize - sequence - 2).toLong()), BigInteger.valueOf((sameEmoji - sequence).toLong()))

                return edge + middle
            } else if (sameEmoji == 2 * sequence) {
                val edge = BigInteger.TWO * nCr(BigInteger.valueOf((slotSize - sequence - 1).toLong()), BigInteger.valueOf((sameEmoji - sequence).toLong()))
                val middle = BigInteger.valueOf((slotSize - 2 - sequence + 1).toLong()) * nCr(BigInteger.valueOf((slotSize - sequence - 2).toLong()), BigInteger.valueOf((sameEmoji - sequence).toLong()))

                val exception = nCr(BigInteger.valueOf((slotSize - sameEmoji + 1).toLong()), BigInteger.TWO)

                return edge + middle - exception
            } else {
                var occasions = BigInteger.ZERO
                val stacks = getStacks(slotSize, sameEmoji, sequence)

                for (stack in stacks) {
                    val stackMap = HashMap<Int, Int>()
                    var sum = 0

                    for (element in stack) {
                        stackMap[element] = (stackMap[element] ?: 0) + 1
                        sum++
                    }

                    var possiblePosition = BigInteger.ONE
                    var tempSum = sum.toLong()

                    stackMap.keys.forEach { k ->
                        val v = stackMap[k]?.toLong() ?: return@forEach

                        possiblePosition *= nCr(BigInteger.valueOf(tempSum), BigInteger.valueOf(v))

                        tempSum -= v
                    }

                    occasions += possiblePosition * nCr(BigInteger.valueOf((slotSize - sameEmoji + 1).toLong()), BigInteger.valueOf((slotSize - sameEmoji - sum + 1).toLong()))
                }

                return occasions
            }
        }

        private fun calculateOdd(slotSize: Int, emojiSize: Int, minSequence: Int, maxSequence: Int) : BigDecimal {
            if (emojiSize == 0)
                return BigDecimal.ZERO

            var odd = BigDecimal.ZERO

            for (s in minSequence..maxSequence) {
                for (sameEmoji in s..slotSize) {
                    val occasion = calculateOccasion(slotSize, sameEmoji, s)

                    odd += BigDecimal.valueOf(emojiSize - 1L).pow(slotSize - sameEmoji) * occasion.toBigDecimal()
                }
            }

            return odd.divide(BigDecimal.valueOf(emojiSize.toLong()).pow(slotSize), Equation.context) * BigDecimal.valueOf(100L)
        }

        private fun getOdds(slotSize: Int, emojiSize: Int, minSequence: Int, maxSequence: Int) : BigDecimal {
            return calculateOdd(slotSize, emojiSize, minSequence, maxSequence)
        }
    }
}