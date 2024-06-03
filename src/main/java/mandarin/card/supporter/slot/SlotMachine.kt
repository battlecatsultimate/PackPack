package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
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

            content.forEachIndexed { index, content ->
                builder.append(index + 1).append(". ").append(content.emoji?.formatted ?: EmojiStore.UNKNOWN.formatted)

                when (content) {
                    is SlotCardContent -> {
                        builder.append(" [Card] : ").append(content.name.ifBlank { "None" }).append("\n")

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
                            SlotCurrencyContent.Mode.FLAT -> builder.append(" [Flat] : ").append(emoji).append(" ").append(content.amount)
                            SlotCurrencyContent.Mode.PERCENTAGE -> builder.append(" [Percentage] : ").append(content.amount).append("% of Entry Fee")
                        }
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
        var sequenceStack = 0

        val downArrow = Emoji.fromUnicode("🔽").formatted
        val upArrow = Emoji.fromUnicode("🔼").formatted

        if (skip) {
            repeat(slotSize) { index ->
                val c = content.random()

                if (index > 0 && previousEmoji === c.emoji) {
                    sequenceStack++
                }

                previousEmoji = c.emoji

                emojiSequence.add(c.emoji!!)
            }
        } else {
            repeat(slotSize) { index ->
                val builder = StringBuilder()

                val c = content.random()

                if (index > 0) {
                    builder.append("**").append(" ").append(EmojiStore.AIR?.formatted?.repeat(index)).append("**").append(downArrow)
                } else {
                    builder.append("** **").append(downArrow)
                }

                builder.append("\n ")

                emojiSequence.forEach { e -> builder.append(e.formatted) }

                builder.append(c.emoji?.formatted).append("\n ")

                builder.append(EmojiStore.AIR?.formatted?.repeat(index)).append(upArrow)

                message.editMessage(builder.toString())
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                if (index > 0 && previousEmoji === c.emoji) {
                    sequenceStack++
                }

                previousEmoji = c.emoji

                emojiSequence.add(c.emoji!!)

                Thread.sleep(1000)
            }
        }

        val result = StringBuilder()

        emojiSequence.forEach { e -> result.append(e.formatted) }

        if (sequenceStack == slotSize - 1) {
            val pickedContent = content.find { c -> c.emoji === previousEmoji }

            if (pickedContent == null) {
                result.append("\n\nBot failed to find reward with emoji above... Please contact card managers!")

                message.editMessage(result.toString())
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            } else {
                when (pickedContent) {
                    is SlotCurrencyContent -> {
                        val reward = when (pickedContent.mode) {
                            SlotCurrencyContent.Mode.FLAT -> pickedContent.amount
                            SlotCurrencyContent.Mode.PERCENTAGE -> round(input * pickedContent.amount / 100.0).toLong()
                        }

                        when (entryFee.entryType) {
                            SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += reward
                            SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += reward
                        }

                        result.append("\n\n🎰 You won the slot machine!!! 🎰\n\nPicked Reward : ")
                            .append(pickedContent.emoji?.formatted).append(" ")

                        val rewardEmoji = when(entryFee.entryType) {
                            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                        }

                        when (pickedContent.mode) {
                            SlotCurrencyContent.Mode.FLAT -> result.append("[Flat] : ").append(rewardEmoji).append(" ").append(reward)
                            SlotCurrencyContent.Mode.PERCENTAGE -> result.append("[Percentage] : ").append(CardData.df.format(pickedContent.amount)).append("% of Entry Fee")
                        }

                        result.append("\nReward : ").append(rewardEmoji).append(" ").append(reward)

                        message.editMessage(result)
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()
                    }
                    is SlotCardContent -> {
                        val cards = pickedContent.roll()

                        result.append("\n\n🎰 You won the slot machine!!! 🎰\n\nPicked Reward : ")
                            .append(pickedContent.emoji?.formatted)
                            .append(" [Card] : ")
                            .append(pickedContent.name)
                            .append("\nReward : \n\n")

                        cards.forEachIndexed { i, c ->
                            result.append(i + 1).append(". ")

                            if (c.tier == CardData.Tier.ULTRA) {
                                result.append("✨")
                            } else {
                                result.append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                            }

                            result.append(c.simpleCardInfo())

                            if (inventory.cards.containsKey(c)) {
                                result.append(" {**NEW**}")
                            }

                            if (c.tier == CardData.Tier.ULTRA) {
                                result.append("✨")
                            } else {
                                result.append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                            }

                            result.append("\n")
                        }

                        cards.forEach { c ->
                            inventory.cards[c] = (inventory.cards[c] ?: 0) + 1
                        }

                        message.editMessage(result)
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()
                    }
                }
            }
        } else {
            val percentage = if (slotSize == 2)
                0.0
            else
                sequenceStack * 1.0 / (slotSize - 2)

            val entryEmoji = when(entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            val compensation = round(input * percentage).toLong()

            result.append("\n\n😔 You lost the slot machine... 😔\n\n")
                .append("Sequence Stack Score : ").append(sequenceStack).append(" Point(s)\n")
                .append("Compensation : ").append(CardData.df.format(percentage * 100.0)).append("% of Entry Fee -> $entryEmoji $compensation")

            when (entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> inventory.catFoods += compensation
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard += compensation
            }

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

        return obj
    }
}