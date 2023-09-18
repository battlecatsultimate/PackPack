package mandarin.card.supporter.transaction

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import java.lang.UnsupportedOperationException

class Restoration(val message: Message) {
    enum class Type {
        ROLL,
        TRADE,
        MODIFY,
        SALVAGE,
        CRAFT
    }

    private enum class ModifyType {
        CARD,
        ROLE,
        NONE
    }

    private enum class ModifyAction {
        ADD,
        REMOVE,
        NONE
    }

    private enum class Source {
        USER,
        MOD,
        NONE
    }

    companion object {
        val restorationLog = ArrayList<String>()

        private const val ROLL_TITLE = "Card Rolled"
        private const val TRADE_TITLE = "Trade Session Closed"
        private const val CARD_MODIFY_ADD_TITLE = "Cards Added"
        private const val CARD_MODIFY_REMOVE_TITLE = "Cards Removed"
        private const val ROLE_MODIFY_ADD_TITLE = "Roles Added"
        private const val ROLE_MODIFY_REMOVE_TITLE = "Roles Removed"
        private const val SALVAGE_TITLE = "Cards Salvaged"
        private const val CRAFT_TITLE = "Tried to Craft"

        private const val NEED_FEEDING = "Check cards messages below"

        fun log(message: String) {
            restorationLog.add(message)
        }
    }

    val type: Type
    private val modifyType: ModifyType
    private val modifyAction: ModifyAction
    private val source: Source

    private val embed: MessageEmbed

    private var feedMessage: Message? = null

    init {
        if (message.embeds.isEmpty())
            throw IllegalStateException("This message doesn't have embed!")

        embed = message.embeds[0]

        val title = embed.title ?: throw UnsupportedOperationException("This embed doesn't have title")

        when(title) {
            ROLL_TITLE -> {
                type = Type.ROLL
                modifyType = ModifyType.NONE
                modifyAction = ModifyAction.NONE
                source = Source.USER
            }
            TRADE_TITLE -> {
                type = Type.TRADE
                modifyType = ModifyType.NONE
                modifyAction = ModifyAction.NONE
                source = Source.NONE
            }
            CARD_MODIFY_ADD_TITLE,
            CARD_MODIFY_REMOVE_TITLE -> {
                type = Type.MODIFY
                modifyType = ModifyType.CARD

                modifyAction = if (title == CARD_MODIFY_ADD_TITLE)
                    ModifyAction.ADD
                else
                    ModifyAction.REMOVE

                source = Source.MOD
            }
            ROLE_MODIFY_ADD_TITLE,
            ROLE_MODIFY_REMOVE_TITLE -> {
                type = Type.MODIFY
                modifyType = ModifyType.ROLE

                modifyAction = if (title == CARD_MODIFY_ADD_TITLE)
                    ModifyAction.ADD
                else
                    ModifyAction.REMOVE

                source = Source.MOD
            }
            SALVAGE_TITLE -> {
                type = Type.SALVAGE
                modifyType = ModifyType.NONE
                modifyAction = ModifyAction.NONE
                source = Source.USER
            }
            CRAFT_TITLE -> {
                type = Type.CRAFT
                modifyType = ModifyType.NONE
                modifyAction = ModifyAction.NONE
                source = Source.USER
            }
            else -> throw UnsupportedOperationException("Unrecognized title : $title")
        }
    }

    fun needFeeding() : Boolean {
        if (type != Type.SALVAGE && type != Type.CRAFT && type != Type.MODIFY)
            return false

        val cards = embed.fields.find { field -> field.name == "Cards" }

        if (cards == null) {
            log("W/ Failed to get Cards field from embed while type is $type")

            return false
        }

        return cards.value == NEED_FEEDING
    }

    fun feedMessage(additional: Message) {
        if (feedMessage != null)
            throw IllegalStateException("This restoration chunk already has fed message!")

        feedMessage = additional
    }

    fun getSourceUser(g: Guild) : RestAction<Member>? {
        when(type) {
            Type.ROLL -> {
                val author = embed.author

                if (author == null) {
                    log("W/ Failed to get author data from embed while type is $type")

                    return null
                }

                val iconUrl = author.iconUrl

                if (iconUrl == null) {
                    log("W/ Failed to find icon URL from author data")

                    return null
                }

                val id = iconUrl.split("/avatars/")[1].split("/")[0]

                return g.retrieveMember(UserSnowflake.fromId(id))
            }
            Type.TRADE -> {
                val traderOne = embed.fields.find { field -> field.name == "Trader 1" }

                if (traderOne == null) {
                    log("W/ Failed to get Trader 1 field from embed while type is $type")

                    return null
                }

                val value = traderOne.value

                if (value == null) {
                    log("W/ Failed to get trader 1's content")

                    return null
                }

                val id = value.split(">")[0].replace("<@", "")

                return g.retrieveMember(UserSnowflake.fromId(id))
            }
            Type.SALVAGE,
            Type.CRAFT -> {
                val desc = embed.description

                if (desc == null) {
                    log("W/ Failed to get description data from embed while type is $type")

                    return null
                }

                val id = desc.split(">")[0].split("<@")[1]

                return g.retrieveMember(UserSnowflake.fromId(id))
            }
            Type.MODIFY -> {
                val targetMember = embed.fields.find { field -> field.name == "Target Member" }

                if (targetMember == null) {
                    log("W/ Failed to get Target Member field from embed while type is $type")

                    return null
                }

                val value = targetMember.value

                if (value == null) {
                    log("W/ Failed to get target member's contents")

                    return null
                }

                val id = value.split("[")[1].replace("]", "")

                return g.retrieveMember(UserSnowflake.fromId(id))
            }
        }
    }

    fun getDestinationMember(g: Guild) : RestAction<Member>? {
        when (type) {
            Type.TRADE -> {
                val traderTwo = embed.fields.find { field -> field.name == "Trader 2" }

                if (traderTwo == null) {
                    log("W/ Failed to get Trader 2 field from embed while type is $type")

                    return null
                }

                val value = traderTwo.value

                if (value == null) {
                    log("W/ Failed to get trader 2's content")

                    return null
                }

                val id = value.split(">")[0].replace("<@", "")

                return g.retrieveMember(UserSnowflake.fromId(id))
            }
            else -> {
                return null
            }
        }
    }

    fun getAddedCardsFromSourceMember() : List<Card> {
        val result = ArrayList<Card>()

        when(type) {
            Type.ROLL -> {
                val rollResult = embed.fields.find { field -> field.name == "Result" }

                if (rollResult == null) {
                    log("W/ Failed to get roll result field from embed while type is $type")

                    return result
                }

                val value = rollResult.value

                if (value == null) {
                    log("W/ Failed to get roll result's content")

                    return result
                }

                val cardLines = value.split("\n")

                for (line in cardLines) {
                    val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                    val card = CardData.cards.find { card -> card.unitID == cardID }

                    if (card == null) {
                        log("W/ Failed to obtain card data : $cardID")

                        continue
                    }

                    result.add(card)
                }
            }
            Type.TRADE -> {
                val tradeResult = embed.fields.find { field -> field.name == "Trader 2's Suggestion" }

                if (tradeResult == null) {
                    log("W/ Failed to get trader 2's suggestion field from embed while type is $type")

                    return result
                }

                val value = tradeResult.value

                if (value == null) {
                    log("W/ Failed to get trader 2's suggestion content")

                    return result
                }

                val cardSection = value.split("\n\n")[1]

                if (cardSection == "- No Cards")
                    return result

                val cardLines = cardSection.split("\n")

                for (line in cardLines) {
                    val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                    val card = CardData.cards.find { card -> card.unitID == cardID }

                    if (card == null) {
                        log("W/ Failed to obtain card data : $cardID")

                        continue
                    }

                    result.add(card)
                }
            }
            Type.MODIFY -> {
                if (modifyType != ModifyType.CARD || modifyAction != ModifyAction.ADD)
                    return result

                val cards = embed.fields.find { field -> field.name == "Cards" }

                if (cards == null) {
                    log("W/ Failed to get Cards field from embed while type is $type")

                    return result
                }

                val value = cards.value

                if (value == null) {
                    log("W/ Failed to get cards' contents")

                    return result
                }

                if (value == NEED_FEEDING) {
                    val fed = feedMessage

                    if (fed == null) {
                        log("E/ This restoration chunk required additional message while there isn't fed additional message, type was $type")

                        return result
                    }

                    val cardLines = fed.contentRaw.split("\n")

                    for (line in cardLines) {
                        val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                        val card = CardData.cards.find { card -> card.unitID == cardID }

                        if (card == null) {
                            log("W/ Failed to obtain card data : $cardID")

                            continue
                        }

                        if (line.matches(Regex(".+x\\d+$"))) {
                            val segment = line.split("x")

                            val amount = segment[segment.size - 1].toInt()

                            repeat(amount) {
                                result.add(card)
                            }
                        } else {
                            result.add(card)
                        }
                    }
                } else {
                    val cardLines = value.split("\n")

                    for (line in cardLines) {
                        val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                        val card = CardData.cards.find { card -> card.unitID == cardID }

                        if (card == null) {
                            log("W/ Failed to obtain card data : $cardID")

                            continue
                        }

                        if (line.matches(Regex(".+x\\d+$"))) {
                            val segment = line.split("x")

                            val amount = segment[segment.size - 1].toInt()

                            repeat(amount) {
                                result.add(card)
                            }
                        } else {
                            result.add(card)
                        }
                    }
                }
            }
            Type.CRAFT -> {
                val receivedCard = embed.fields.find { field -> field.name == "Received Card" }

                if (receivedCard == null) {
                    log("W/ Failed to get Received Cards field from embed while type is $type")

                    return result
                }

                val value = receivedCard.value

                if (value == null) {
                    log("W/ Failed to get received card's contents")

                    return result
                }

                val cardID = value.split("No.")[1].split(" : ")[0].toInt()

                val card = CardData.cards.find { card -> card.unitID == cardID }

                if (card == null) {
                    log("W/ Failed to obtain card data : $cardID")

                    return result
                }

                result.add(card)
            }
            else -> {
                return result
            }
        }

        return result
    }

    fun getRemovedCardsFromSourceMember() : List<Card> {
        val result = ArrayList<Card>()

        when(type) {
            Type.TRADE -> {
                val tradeResult = embed.fields.find { field -> field.name == "Trader 1's Suggestion" }

                if (tradeResult == null) {
                    log("W/ Failed to get trader 1's suggestion field from embed while type is $type")

                    return result
                }

                val value = tradeResult.value

                if (value == null) {
                    log("W/ Failed to get trader 1's suggestion content")

                    return result
                }

                val cardSection = value.split("\n\n")[1]

                if (cardSection == "- No Cards")
                    return result

                val cardLines = cardSection.split("\n")

                for (line in cardLines) {
                    val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                    val card = CardData.cards.find { card -> card.unitID == cardID }

                    if (card == null) {
                        log("W/ Failed to obtain card data : $cardID")

                        continue
                    }

                    result.add(card)
                }
            }
            Type.MODIFY -> {
                if (modifyType != ModifyType.CARD || modifyAction != ModifyAction.REMOVE)
                    return result

                val cards = embed.fields.find { field -> field.name == "Cards" }

                if (cards == null) {
                    log("W/ Failed to get Cards field from embed while type is $type")

                    return result
                }

                val value = cards.value

                if (value == null) {
                    log("W/ Failed to get cards' contents")

                    return result
                }

                if (value == NEED_FEEDING) {
                    val fed = feedMessage

                    if (fed == null) {
                        log("E/ This restoration chunk required additional message while there isn't fed additional message, type was $type")

                        return result
                    }

                    val cardLines = fed.contentRaw.split("\n")

                    for (line in cardLines) {
                        val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                        val card = CardData.cards.find { card -> card.unitID == cardID }

                        if (card == null) {
                            log("W/ Failed to obtain card data : $cardID")

                            continue
                        }

                        if (line.matches(Regex(".+x\\d+$"))) {
                            val segment = line.split("x")

                            val amount = segment[segment.size - 1].toInt()

                            repeat(amount) {
                                result.add(card)
                            }
                        } else {
                            result.add(card)
                        }
                    }
                } else {
                    val cardLines = value.split("\n")

                    for (line in cardLines) {
                        val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                        val card = CardData.cards.find { card -> card.unitID == cardID }

                        if (card == null) {
                            log("W/ Failed to obtain card data : $cardID")

                            continue
                        }

                        if (line.matches(Regex(".+x\\d+$"))) {
                            val segment = line.split("x")

                            val amount = segment[segment.size - 1].toInt()

                            repeat(amount) {
                                result.add(card)
                            }
                        } else {
                            result.add(card)
                        }
                    }
                }
            }
            Type.SALVAGE,
            Type.CRAFT -> {
                val cards = embed.fields.find { field -> field.name == "Cards" }

                if (cards == null) {
                    log("W/ Failed to get Cards field from embed while type is $type")

                    return result
                }

                val value = cards.value

                if (value == null) {
                    log("W/ Failed to get cards' contents")

                    return result
                }

                if (value == NEED_FEEDING) {
                    val fed = feedMessage

                    if (fed == null) {
                        log("E/ This restoration chunk required additional message while there isn't fed additional message, type was $type")

                        return result
                    }

                    val cardLines = fed.contentRaw.split("\n")

                    for (line in cardLines) {
                        val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                        val card = CardData.cards.find { card -> card.unitID == cardID }

                        if (card == null) {
                            log("W/ Failed to obtain card data : $cardID")

                            continue
                        }

                        if (line.matches(Regex(".+x\\d+$"))) {
                            val segment = line.split("x")

                            val amount = segment[segment.size - 1].toInt()

                            repeat(amount) {
                                result.add(card)
                            }
                        } else {
                            result.add(card)
                        }
                    }
                } else {
                    val cardLines = value.split("\n")

                    for (line in cardLines) {
                        val cardID = line.trim().split("No.")[1].split(" : ")[0].toInt()

                        val card = CardData.cards.find { card -> card.unitID == cardID }

                        if (card == null) {
                            log("W/ Failed to obtain card data : $cardID")

                            continue
                        }

                        if (line.matches(Regex(".+x\\d+$"))) {
                            val segment = line.split("x")

                            val amount = segment[segment.size - 1].toInt()

                            repeat(amount) {
                                result.add(card)
                            }
                        } else {
                            result.add(card)
                        }
                    }
                }
            }
            else -> {
                return result
            }
        }

        return result
    }

    fun getAddedCardsFromDestinationMember() : List<Card> {
        val result = ArrayList<Card>()

        when(type) {
            Type.TRADE -> {
                val tradeResult = embed.fields.find { field -> field.name == "Trader 1's Suggestion" }

                if (tradeResult == null) {
                    log("W/ Failed to get trader 1's suggestion field from embed while type is $type")

                    return result
                }

                val value = tradeResult.value

                if (value == null) {
                    log("W/ Failed to get trader 1's suggestion content")

                    return result
                }

                val cardSection = value.split("\n\n")[1]

                if (cardSection == "- No Cards")
                    return result

                val cardLines = cardSection.split("\n")

                for (line in cardLines) {
                    val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                    val card = CardData.cards.find { card -> card.unitID == cardID }

                    if (card == null) {
                        log("W/ Failed to obtain card data : $cardID")

                        continue
                    }

                    result.add(card)
                }
            }
            else -> {
                return result
            }
        }

        return result
    }

    fun getRemovedCardsFromDestinationMember() : List<Card> {
        val result = ArrayList<Card>()

        when(type) {
            Type.TRADE -> {
                val tradeResult = embed.fields.find { field -> field.name == "Trader 2's Suggestion" }

                if (tradeResult == null) {
                    log("W/ Failed to get trader 2's suggestion field from embed while type is $type")

                    return result
                }

                val value = tradeResult.value

                if (value == null) {
                    log("W/ Failed to get trader 2's suggestion content")

                    return result
                }

                val cardSection = value.split("\n\n")[1]

                if (cardSection == "- No Cards")
                    return result

                val cardLines = cardSection.split("\n")

                for (line in cardLines) {
                    val cardID = line.split("No.")[1].split(" : ")[0].toInt()

                    val card = CardData.cards.find { card -> card.unitID == cardID }

                    if (card == null) {
                        log("W/ Failed to obtain card data : $cardID")

                        continue
                    }

                    result.add(card)
                }
            }
            else -> {
                return result
            }
        }

        return result
    }
}