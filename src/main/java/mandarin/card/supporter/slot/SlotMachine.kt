package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.calculation.Equation
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.FileUpload
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
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

        const val PAGE_CHUNK = 10
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

    fun asText(page: Int) : String {
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

            for (index in page * PAGE_CHUNK until min(content.size, (page + 1) * PAGE_CHUNK)) {
                val content = content[index]

                builder.append(index + 1).append(". ").append(content.emoji?.formatted ?: EmojiStore.UNKNOWN.formatted)

                if (content !is SlotPlaceHolderContent) {
                    builder.append("x").append(content.slot)
                }

                when (content) {
                    is SlotCardContent -> {
                        builder.append(" [Card] : ").append(content.name.ifBlank { "None" })

                        if (odds.isNotEmpty()) {
                            builder.append(" { Chance = ").append(odds[index].toPlainString()).append("% }")
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
                                    builder.append(" { Chance = ").append(odds[index].toPlainString()).append("% }")
                                }
                            }
                            SlotCurrencyContent.Mode.PERCENTAGE -> {
                                builder.append(" [Percentage] : ").append(content.amount).append("% of Entry Fee")

                                if (odds.isNotEmpty()) {
                                    builder.append(" { Chance = ").append(odds[index].toPlainString()).append("% }")
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

        if (content.size > PAGE_CHUNK) {
            builder.append("\n- **These rewards aren't all! Please click page button below to check more rewards**\n")
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

        val cooldownMap = CardData.slotCooldown.computeIfAbsent(user.toString()) { _ -> HashMap() }

        cooldownMap[uuid] = CardData.getUnixEpochTime() + cooldown

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

        val pickedContents = pickReward(sequenceStacks)

        val cardsResult = ArrayList<Card>()

        // Pre-roll cards in advance to display the result with synchronized inventory status
        pickedContents.filterIsInstance<SlotCardContent>().forEach { c ->
            val cards = c.roll()

            cardsResult.addAll(cards)
        }

        try {
            displayResult(message, inventory, input, pickedContents, emojiSequence, totalSequenceStacks, cardsResult)
        } catch (e: Exception) {
            StaticStore.logger.uploadErrorLog(e, "E/SlotMachine::roll - Failed to display slot machine roll result")
        }

        if (pickedContents.isNotEmpty()) {
            if (!pickedContents.any { c -> c.emoji == null }) {
                var currencySum = 0L
                pickedContents.filterIsInstance<SlotCurrencyContent>().forEach { c ->
                    val reward = when(c.mode) {
                        SlotCurrencyContent.Mode.FLAT -> c.amount
                        SlotCurrencyContent.Mode.PERCENTAGE -> round(input * c.amount / 100.0).toLong()
                    }

                    currencySum += reward
                }

                when(entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += currencySum
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += currencySum
                }

                inventory.addCards(cardsResult)

                TransactionLogger.logSlotMachineWin(user, input, this, pickedContents, currencySum, cardsResult)
            }
        } else {
            val percentage = if (slotSize == 2)
                0.0
            else
                min(1.0, totalSequenceStacks * 1.0 / (slotSize - 2))

            val compensation = round(input * percentage).toLong()

            when (entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += compensation
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += compensation
            }

            TransactionLogger.logSlotMachineRollFail(user, input, this, totalSequenceStacks, compensation)
        }

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

        if (emojiSize == 0)
            return BigDecimal.ZERO

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
                sameEmojiContents.minOf { co -> co.slot } - 1
            }

            val odd = calculateOdd(c.slot, maxSequence).round(MathContext(5, RoundingMode.HALF_EVEN))

            result.add(odd)
        }

        return result
    }

    private fun displayResult(message: Message, inventory: Inventory, input: Long, pickedContents: List<SlotContent>, emojiSequence: List<Emoji>, totalSequenceStacks: Int, cardsResult: List<Card>) {
        val emojis = StringBuilder()

        emojiSequence.forEach { e -> emojis.append(e.formatted) }

        if (pickedContents.isNotEmpty()) {
            if (pickedContents.any { c -> c.emoji == null }) {
                message.editMessage("$emojis\n\nBot failed to find reward with emoji above... Please contact card managers!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            } else {
                val pickedRewards = StringBuilder()

                val feeEmoji = when(entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                }

                var currencySum = 0L

                pickedContents.forEach { c ->
                    pickedRewards.append("- ").append(c.emoji?.formatted).append("x").append(c.slot).append(" ")

                    when(c) {
                        is SlotCurrencyContent -> {
                            val reward = when(c.mode) {
                                SlotCurrencyContent.Mode.FLAT -> c.amount
                                SlotCurrencyContent.Mode.PERCENTAGE -> round(input * c.amount / 100.0).toLong()
                            }

                            currencySum += reward

                            when(c.mode) {
                                SlotCurrencyContent.Mode.FLAT -> {
                                    pickedRewards.append("[Flat] : ").append(feeEmoji).append(" ").append(c.amount)
                                }
                                SlotCurrencyContent.Mode.PERCENTAGE -> {
                                    pickedRewards.append("[Percentage] : ").append(c.amount).append("% of Entry Fee")
                                }
                            }

                            pickedRewards.append("\n")
                        }
                        is SlotCardContent -> {
                            pickedRewards.append("[Card] : ").append(c.name).append("\n")
                        }
                    }
                }

                val builder = EmbedBuilder()

                if (currencySum <= input && cardsResult.isEmpty()) {
                    builder.setDescription("### 😔 You lost the slot machine... 😔")
                        .setColor(StaticStore.rainbow[0])
                } else {
                    builder.setDescription("## 🎰 You won the slot machine!!! 🎰")
                        .setColor(StaticStore.rainbow[3])
                }

                builder.addField("Result", emojis.toString(), false)

                builder.addField("Picked Reward", pickedRewards.toString(), false)

                if (currencySum != 0L) {
                    val feeName = when(entryFee.entryType) {
                        SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
                        SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
                    }

                    builder.addField(feeName, "$feeEmoji $currencySum", false)
                }

                if (cardsResult.isNotEmpty()) {
                    val cards = StringBuilder()

                    cardsResult.forEach { c ->
                        val cardEmoji = if (c.tier == CardData.Tier.ULTRA) {
                            Emoji.fromUnicode("✨").formatted
                        } else if (c.tier == CardData.Tier.LEGEND) {
                            EmojiStore.ABILITY["LEGEND"]?.formatted
                        } else {
                            ""
                        }

                        cards.append("- ").append(cardEmoji)

                        if (c.tier == CardData.Tier.ULTRA || c.tier == CardData.Tier.LEGEND) {
                            cards.append(" ")
                        }

                        cards.append(c.simpleCardInfo())

                        if (!inventory.cards.containsKey(c) && !inventory.favorites.containsKey(c)) {
                            cards.append(" {**NEW**}")
                        }

                        if (c.tier == CardData.Tier.ULTRA || c.tier == CardData.Tier.LEGEND) {
                            cards.append(" ")
                        }

                        cards.append(cardEmoji).append("\n")
                    }

                    builder.addField("Cards", cards.toString(), false)
                }

                if (cardsResult.isEmpty()) {
                    message.editMessage("")
                        .setEmbeds(builder.build())
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                val newCards = cardsResult.toSet().filter { c -> !inventory.cards.containsKey(c) && !inventory.favorites.containsKey(c) }.sortedWith(CardComparator()).reversed()

                if (newCards.isNotEmpty()) {
                    val links = ArrayList<String>()
                    val files = ArrayList<FileUpload>()

                    newCards.forEachIndexed { index, card ->
                        val skin = inventory.equippedSkins[card]

                        if (skin == null) {
                            files.add(FileUpload.fromData(card.cardImage, "card$index.png"))
                            links.add("attachment://card$index.png")
                        } else {
                            skin.cache(message.jda, true)

                            links.add(skin.cacheLink)
                        }
                    }

                    val embeds = ArrayList<MessageEmbed>()

                    links.forEachIndexed { index, link ->
                        if (index == 0) {
                            builder.setUrl("https://none.dummy").setImage(link)

                            embeds.add(builder.build())
                        } else {
                            embeds.add(EmbedBuilder().setUrl("https://none.dummy").setImage(link).build())
                        }
                    }

                    message.editMessage("")
                        .setEmbeds(embeds)
                        .setComponents()
                        .setFiles(files)
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                val availableSkins = cardsResult.toSet()
                    .filter { c -> inventory.equippedSkins.containsKey(c) }
                    .map { c -> inventory.equippedSkins[c] }
                    .filterNotNull()

                if (availableSkins.isEmpty()) {
                    message.editMessage("")
                        .setEmbeds(builder.build())
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                availableSkins.forEach { s -> s.cache(message.jda, true) }

                val cachedLinks = availableSkins.subList(0, min(availableSkins.size, Message.MAX_EMBED_COUNT))
                    .filter { skin -> skin.cacheLink.isNotEmpty() }
                    .map { skin -> skin.cacheLink }

                val embeds = ArrayList<MessageEmbed>()

                cachedLinks.forEachIndexed { index, link ->
                    if (index == 0) {
                        builder.setUrl(link).setImage(link)

                        embeds.add(builder.build())
                    } else {
                        embeds.add(EmbedBuilder().setUrl(cachedLinks[0]).setImage(link).build())
                    }
                }

                message.editMessage("")
                    .setEmbeds(embeds)
                    .setComponents()
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

            val builder = EmbedBuilder()
                .setDescription("### 😔 You lost the slot machine... 😔")
                .setColor(StaticStore.rainbow[0])

            val point = if (totalSequenceStacks <= 1)
                "Point"
            else
                "Points"

            builder.addField("Result", emojis.toString(), false)
            builder.addField("Sequence Stack Score", "$totalSequenceStacks $point", false)
            builder.addField("Compensation", "${CardData.df.format(percentage * 100.0)}% of Entry Fee -> $entryEmoji $compensation", false)

            message.editMessage("")
                .setEmbeds(builder.build())
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()
        }
    }
}