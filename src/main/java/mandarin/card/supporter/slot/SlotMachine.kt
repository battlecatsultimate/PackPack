package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.calculation.Equation
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.FileUpload
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class SlotMachine {
    companion object {
        fun fromJson(obj: JsonObject) : SlotMachine {
            if (!StaticStore.hasAllTag(obj, "name", "uuid", "activate", "cooldown", "slotSize", "entryFee", "content")) {
                throw IllegalStateException("E/SlotMachine::fromJson - Invalid json data found")
            }

            val name = obj.get("name").asString
            val uuid = obj.get("uuid").asString

            val slotMachine = SlotMachine(name, uuid)

            slotMachine.activate = obj.get("activate").asBoolean

            slotMachine.cooldown = obj.get("cooldown").asLong
            slotMachine.slotSize = obj.get("slotSize").asInt
            slotMachine.entryFee = SlotEntryFee.fromJson(obj.getAsJsonObject("entryFee"))

            val arr = obj.getAsJsonArray("content")

            arr.forEach { e ->
                slotMachine.content.add(SlotContent.fromJson(e.asJsonObject))
            }

            if (obj.has("roles")) {
                obj.getAsJsonArray("roles").forEach { e ->
                    slotMachine.roles.add(e.asLong)
                }
            }

            return slotMachine
        }
    }

    var name: String

    var activate = false

    var cooldown = 0L
    var slotSize = 3
    var entryFee = SlotEntryFee()
        private set

    val content = ArrayList<SlotContent>()

    val roles = ArrayList<Long>()

    val valid: Boolean
        get() {
            if (entryFee.invalid)
                return false

            if (content.isEmpty())
                return false

            content.forEach { c ->
                if (c !is SlotCardContent)
                    return@forEach

                if (c.cardChancePairLists.isEmpty())
                    return false

                if (c.slot == 0)
                    return false

                c.cardChancePairLists.forEach { l ->
                    if (l.amount == 0)
                        return false

                    if (l.pairs.sumOf { p -> p.chance } != 100.0)
                        return false

                    l.pairs.forEach { p ->
                        if (p.cardGroup.extra.isEmpty() && p.cardGroup.types.isEmpty())
                            return false
                    }
                }
            }

            return true
        }

    val uuid: String

    constructor(name: String) {
        this.name = name

        uuid = "$name|${CardData.getUnixEpochTime()}"
    }

    private constructor(name: String, uuid: String) {
        this.name = name
        this.uuid = uuid
    }

    fun asText() : String {
        val builder = StringBuilder("## ")
            .append(name)
            .append("\nThis slot machine has ")
            .append(slotSize)
            .append(" slot")

        if (slotSize >= 2)
            builder.append("s")

        builder.append("\n### Entry Fee\n")
            .append(entryFee.asText())
            .append("\n### Contents\n")

        if (content.isEmpty()) {
            builder.append("- No Contents\n")
        } else {
            val emoji = when(entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            val odds = getOdds()

            content.forEachIndexed { index, content ->
                builder.append(index + 1).append(". ").append(content.emoji?.formatted ?: EmojiStore.UNKNOWN.formatted)

                if (content !is SlotPlaceHolderContent) {
                    builder.append("x").append(content.slot)
                }

                when (content) {
                    is SlotCardContent -> {
                        builder.append(" [Card] : ").append(content.name.ifBlank { "None" })

                        if (odds.isNotEmpty()) {
                            builder.append(" { Chance = ").append("%5f".format(odds[index].toDouble())).append("% }")
                        }

                        builder.append("\n")

                        content.cardChancePairLists.forEachIndexed { ind, list ->
                            builder.append("  - ").append(list.amount).append(" ")

                            if (list.amount >= 2) {
                                builder.append("Cards\n")
                            } else {
                                builder.append("Card\n")
                            }

                            list.pairs.forEachIndexed { i, pair ->
                                builder.append("    - ").append(CardData.df.format(pair.chance)).append("% : ").append(pair.cardGroup.getName())

                                if (i < list.pairs.size - 1)
                                    builder.append("\n")
                            }

                            if (ind < content.cardChancePairLists.size - 1)
                                builder.append("\n")
                        }
                    }
                    is SlotCurrencyContent -> {
                        when(content.mode) {
                            SlotCurrencyContent.Mode.FLAT -> {
                                builder.append(" [Flat] : ").append(emoji).append(" ").append(content.amount)

                                if (odds.isNotEmpty()) {
                                    builder.append(" { Chance = ").append("%5f".format(odds[index].toDouble())).append("% }")
                                }
                            }
                            SlotCurrencyContent.Mode.PERCENTAGE -> {
                                builder.append(" [Percentage] : ").append(content.amount).append("% of Entry Fee")

                                if (odds.isNotEmpty()) {
                                    builder.append(" { Chance = ").append("%5f".format(odds[index].toDouble())).append("% }")
                                }
                            }
                        }
                    }
                    is SlotPlaceHolderContent -> {
                        builder.append(" [Place Holder]")
                    }
                }

                builder.append("\n")
            }
        }

        builder.append("### Cooldown\n")

        if (cooldown <= 0L) {
            builder.append("`No Cooldown`")
        } else {
            builder.append("`").append(CardData.convertMillisecondsToText(cooldown)).append("`")
        }

        if (roles.isNotEmpty()) {
            builder.append("\n\nThis slot machine requires any of roles below!\n")

            roles.forEachIndexed { i, r ->
                builder.append("- <@&").append(r).append(">")

                if (i < roles.size - 1)
                    builder.append("\n")
            }
        }

        return builder.toString()
    }

    fun roll(message: Message, user: Long, inventory: Inventory, input: Long, skip: Boolean) {
        content.filter { c -> c.emoji == null }.forEach { c -> c.load() }

        if (content.any { c -> c.emoji == null }) {
            message.editMessage("Failed to roll slot machine due to invalid emoji data... Contact card managers!")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        when(entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods -= input
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard -= input
        }

        var previousEmoji: CustomEmoji? = null

        val emojiSequence = ArrayList<CustomEmoji>()
        val sequenceStacks = HashMap<CustomEmoji, Int>()
        var totalSequenceStacks = 0

        var temporalSequenceStack = 0

        val downArrow = Emoji.fromUnicode("🔽").formatted
        val upArrow = Emoji.fromUnicode("🔼").formatted

        val emojis = content.mapNotNull { c -> c.emoji }.toSet()

        if (emojis.isEmpty()) {
            message.editMessage("Failed to roll slot machine due to empty emoji data... Contact card managers!")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        if (skip) {
            repeat(slotSize) { index ->
                val emoji = emojis.random()

                sequenceStacks.computeIfAbsent(emoji) { 0 }

                if (index > 0) {
                    if (previousEmoji === emoji) {
                        temporalSequenceStack++
                        totalSequenceStacks++
                    } else {
                        val e = previousEmoji

                        if (e != null) {
                            sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
                        }

                        temporalSequenceStack = 0
                    }
                }

                previousEmoji = emoji

                emojiSequence.add(emoji)
            }

            val e = previousEmoji

            if (e != null) {
                sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
            }
        } else {
            repeat(slotSize) { index ->
                val builder = StringBuilder()

                val emoji = emojis.random()

                sequenceStacks.computeIfAbsent(emoji) { 0 }

                if (index > 0) {
                    builder.append("**").append(" ").append(EmojiStore.AIR?.formatted?.repeat(index)).append("**").append(downArrow)
                } else {
                    builder.append("** **").append(downArrow)
                }

                builder.append("\n ")

                emojiSequence.forEach { e -> builder.append(e.formatted) }

                builder.append(emoji.formatted).append("\n ")

                builder.append(EmojiStore.AIR?.formatted?.repeat(index)).append(upArrow)

                message.editMessage(builder.toString())
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                if (index > 0) {
                    if (previousEmoji === emoji) {
                        temporalSequenceStack++
                        totalSequenceStacks++
                    } else {
                        val e = previousEmoji

                        if (e != null) {
                            sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
                        }

                        temporalSequenceStack = 0
                    }
                }

                previousEmoji = emoji

                emojiSequence.add(emoji)

                Thread.sleep(1000)
            }

            val e = previousEmoji

            if (e != null) {
                sequenceStacks[e] = max(sequenceStacks[e] ?: 0, temporalSequenceStack)
            }
        }

        val result = StringBuilder()

        emojiSequence.forEach { e -> result.append(e.formatted) }

        val pickedContents = pickReward(sequenceStacks)

        if (pickedContents.isNotEmpty()) {
            if (pickedContents.any { c -> c.emoji == null }) {
                result.append("\n\nBot failed to find reward with emoji above... Please contact card managers!")

                message.editMessage(result.toString())
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            } else {
                val feeEmoji = when(entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                }

                var currencySum = 0L
                val cardsSum = ArrayList<Card>()

                result.append("\n\n🎰 You won the slot machine!!! 🎰\n\nPicked Reward : \n")

                pickedContents.forEach { c ->
                    result.append("- ").append(c.emoji?.formatted).append("x").append(c.slot).append(" ")

                    when(c) {
                        is SlotCurrencyContent -> {
                            val reward = when(c.mode) {
                                SlotCurrencyContent.Mode.FLAT -> c.amount
                                SlotCurrencyContent.Mode.PERCENTAGE -> round(input * c.amount / 100.0).toLong()
                            }

                            currencySum += reward

                            when(c.mode) {
                                SlotCurrencyContent.Mode.FLAT -> {
                                    result.append("[Flat] : ").append(feeEmoji).append(" ").append(c.amount)
                                }
                                SlotCurrencyContent.Mode.PERCENTAGE -> {
                                    result.append("[Percentage] : ").append(c.amount).append("% of Entry Fee")
                                }
                            }

                            result.append("\n")
                        }
                        is SlotCardContent -> {
                            val cards = c.roll()

                            cardsSum.addAll(cards)

                            result.append("[Card] : ").append(c.name).append("\n")
                        }
                    }
                }

                result.append("\nReward : \n")

                if (currencySum != 0L) {
                    val feeName = when(entryFee.entryType) {
                        SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
                        SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
                    }

                    result.append("### ").append(feeName).append("\n").append(feeEmoji).append(" ").append(currencySum).append("\n")
                }

                if (cardsSum.isNotEmpty()) {
                    result.append("### Cards\n")

                    cardsSum.forEach { c ->
                        result.append("- ").append(c.simpleCardInfo()).append("\n")
                    }
                }

                val files = ArrayList<FileUpload>()

                cardsSum.toSet().filter { c -> !inventory.cards.containsKey(c) }.forEach { c ->
                    files.add(FileUpload.fromData(c.cardImage))
                }

                when(entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += currencySum
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += currencySum
                }

                cardsSum.forEach { c ->
                    inventory.cards[c] = (inventory.cards[c] ?: 0) + 1
                }

                CardBot.saveCardData()

                TransactionLogger.logSlotMachineWin(user, input, this, pickedContents, currencySum, cardsSum)

                message.editMessage(result.toString())
                    .setComponents()
                    .setFiles(files)
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        } else {
            val percentage = if (slotSize == 2)
                0.0
            else
                min(1.0, totalSequenceStacks * 1.0 / (slotSize - 2))

            val entryEmoji = when(entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            val compensation = round(input * percentage).toLong()

            result.append("\n\n😔 You lost the slot machine... 😔\n\n")
                .append("Sequence Stack Score : ").append(totalSequenceStacks).append(" Point(s)\n")
                .append("Compensation : ").append(CardData.df.format(percentage * 100.0)).append("% of Entry Fee -> $entryEmoji $compensation")

            when (entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += compensation
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += compensation
            }

            TransactionLogger.logSlotMachineRollFail(user, input, this, totalSequenceStacks, compensation)

            message.editMessage(result)
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()
        }

        val cooldownMap = CardData.slotCooldown.computeIfAbsent(user.toString()) { _ -> HashMap() }

        cooldownMap[uuid] = CardData.getUnixEpochTime() + cooldown

        CardBot.saveCardData()
    }

    fun getOdd(c: SlotContent) : BigDecimal {
        val e = c.emoji ?: return BigDecimal.ZERO

        val sameEmojiContent = content.filter { co -> c.emoji == e && co.slot > c.slot }

        val maxSequence = if (sameEmojiContent.isEmpty())
            slotSize
        else
            sameEmojiContent.minOf { co -> co.slot }

        return calculateOdd(c.slot, maxSequence)
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("name", name)
        obj.addProperty("uuid", uuid)

        obj.addProperty("activate", activate)

        obj.addProperty("cooldown", cooldown)
        obj.addProperty("slotSize", slotSize)
        obj.add("entryFee", entryFee.asJson())

        val arr = JsonArray()

        content.forEach { content ->
            arr.add(content.asJson())
        }

        obj.add("content", arr)

        val roleArr = JsonArray()

        roles.forEach { r -> roleArr.add(r) }

        obj.add("roles", roleArr)

        return obj
    }

    private fun pickReward(sequenceStacks: Map<CustomEmoji, Int>) : List<SlotContent> {
        val result = HashMap<CustomEmoji, HashSet<SlotContent>>()

        content.filter { c -> c !is SlotPlaceHolderContent }.forEach { c ->
            val e = c.emoji ?: return@forEach

            val stack = sequenceStacks[e] ?: return@forEach

            if (c.slot <= stack + 1) {
                val contentSet = result.computeIfAbsent(e) { HashSet() }

                contentSet.add(c)
            }
        }

        val finalResult = ArrayList<SlotContent>()

        result.forEach { (_, contents) ->
            val maxStack = contents.maxOf { c -> c.slot }

            finalResult.addAll(contents.filter { c -> c.slot == maxStack })
        }

        println(finalResult)

        return finalResult
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

    private fun getStacks(sameEmoji: Int, sequence: Int) : List<IntArray> {
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

    private fun calculateOccasion(sameEmoji: Int, sequence: Int) : BigInteger {
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
            val stacks = getStacks(sameEmoji, sequence)

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

    private fun calculateOdd(minSequence: Int, maxSequence: Int) : BigDecimal {
        val emojiSize = content.mapNotNull { c -> c.emoji }.toSet().size

        var odd = BigDecimal.ZERO

        for (s in minSequence..maxSequence) {
            for (sameEmoji in s..slotSize) {
                val occasion = calculateOccasion(sameEmoji, s)

                odd += BigDecimal.valueOf(emojiSize - 1L).pow(slotSize - sameEmoji) * occasion.toBigDecimal()
            }
        }

        return odd.divide(BigDecimal.valueOf(emojiSize.toLong()).pow(slotSize), Equation.context) * BigDecimal.valueOf(100L)
    }

    private fun getOdds() : List<BigDecimal> {
        val result = ArrayList<BigDecimal>()

        if (content.any { c -> c.emoji == null }) {
            return result
        }

        content.forEach { c ->
            if (c is SlotPlaceHolderContent) {
                result.add(BigDecimal.ZERO)

                return@forEach
            }

            val sameEmojiContents = content.filter { co -> co.emoji == c.emoji && co.slot > c.slot }

            val maxSequence = if (sameEmojiContents.isEmpty()) {
                slotSize
            } else {
                sameEmojiContents.minOf { co -> co.slot }
            }

            val odd = calculateOdd(c.slot, maxSequence)

            result.add(odd)
        }

        return result
    }
}