package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.CardCostPayHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.CardPayContainer
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload
import java.lang.StringBuilder
import java.util.HashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.any
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toSet
import kotlin.math.min
import kotlin.text.toInt

class PackPayHolder(
    author: Message,
    channelID: String,
    message: Message,
    private val member: Member,
    private val pack: CardPack,
    private val noImage: Boolean
) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val containers = Array(pack.cost.cardsCosts.size) {
        CardPayContainer(pack.cost.cardsCosts[it])
    }

    private val inventory = Inventory.getInventory(author.author.idLong)

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card pack manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "roll" -> {
                val cooldownMap = CardData.cooldown.computeIfAbsent(authorMessage.author.idLong) { HashMap() }

                if (cooldownMap.containsKey(pack.uuid)) {
                    val cooldown = cooldownMap[pack.uuid] ?: 0

                    if (cooldown - CardData.getUnixEpochTime() > 0L) {
                        event.deferReply().setContent("Failed to roll the pack due to cooldown!")
                            .setEphemeral(true)
                            .queue()

                        return
                    }
                }

                if (pack.cooldown > 0L) {
                    cooldownMap[pack.uuid] = CardData.getUnixEpochTime() + pack.cooldown
                }

                try {
                    val countdown = CountDownLatch(1)

                    event.deferEdit()
                        .setContent("Rolling...! \uD83C\uDFB2")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue({ _ ->
                            countdown.countDown()
                        }) { e -> {
                            StaticStore.logger.uploadErrorLog(e, "E/PackPayHolder::onEvent - Failed to edit message")

                            countdown.countDown()
                        }}

                    countdown.await()
                } catch (e: Exception) {
                    StaticStore.logger.uploadErrorLog(e, "E/PckPayHolder::onEvent - Failed to indicate rolling message")
                }

                inventory.catFoods -= pack.cost.catFoods
                inventory.platinumShard -= pack.cost.platinumShards

                containers.forEach { container -> container.pickedCards.forEach { card ->
                    inventory.cards[card] = (inventory.cards[card] ?: 0) - 1

                    if ((inventory.cards[card] ?: 0) < 0)
                        StaticStore.logger.uploadLog(
                            "W/PackPayHolder::onEvent - Negative card amount found\n" +
                                    "\n" +
                                    "User : ${authorMessage.author.effectiveName}\n" +
                                    "Card : ${card.simpleCardInfo()}\n" +
                                    "Amount : ${inventory.cards[card]}"
                        )
                } }

                val result = pack.roll()

                try {
                    displayRollResult(result)
                } catch (e: Exception) {
                    StaticStore.logger.uploadErrorLog(e, "E/PackPayHolder::onEvent - Failed to upload card roll result")
                }

                inventory.addCards(result)

                CardBot.saveCardData()

                TransactionLogger.logRoll(result, pack, member, false)
            }
            "cost" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()
                val container = containers[index]

                connectTo(event, CardCostPayHolder(authorMessage, channelID, message, container, containers))
            }
            "back" -> {
                if (containers.any { container -> container.pickedCards.isNotEmpty() }) {
                    StaticStore.removeHolder(authorMessage.author.id, this)

                    registerPopUp(event, "Are you sure you want to go back? All your selected cards will be cleared")

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        e.deferEdit().queue()

                        goBack()
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    event.deferEdit().queue()

                    goBack()
                }
            }
        }
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    override fun onConnected(event: IMessageEditCallback) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder()

        val cooldownMap = CardData.cooldown[member.idLong]
        val cooldown = if (cooldownMap == null) {
            0L
        } else {
            cooldownMap[pack.uuid] ?: 0L
        }

        if (cooldown - CardData.getUnixEpochTime() > 0) {
            builder.append("You can't roll this pack because you have cooldown left : ${CardData.convertMillisecondsToText(cooldown - CardData.getUnixEpochTime())}")
        } else {
            builder.append(pack.displayInfo())

            if (!pack.cost.affordable(inventory)) {
                builder.append("\n\nYou can't afford this pack. Check reason below :\n\n").append(pack.cost.getReason(
                    inventory
                ))
            } else if (containers.any { container -> !container.paid() }) {
                builder.append("\n\nTo roll this pack, you have to pay all card costs. You can check if you paid for each card cost or not by checking each list's descriptions\n" +
                        "\n" +
                        "Additionally, cat foods and platinum shards will be paid automatically")
            } else {
                builder.append("\n\nAll ready to go! Click `roll` button to pay cost and roll the pack!")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val cooldownMap = CardData.cooldown[member.idLong]
        val cooldown = if (cooldownMap == null) {
            0L
        } else {
            cooldownMap[pack.uuid] ?: 0L
        }

        if (cooldown - CardData.getUnixEpochTime() > 0) {
            result.add(ActionRow.of(Button.primary("back", "Go Back")))
        } else {
            if (containers.isNotEmpty()) {
                val options = ArrayList<SelectOption>()

                containers.forEachIndexed { index, container ->
                    options.add(
                        SelectOption.of(if (container.cost is SpecificCardCost) container.cost.simpleCostName() else container.cost.getCostName(), index.toString())
                            .withDescription(if (container.paid()) "Ready to pay" else "Need to be paid")
                    )
                }

                result.add(ActionRow.of(StringSelectMenu.create("cost").addOptions(options).setPlaceholder("Select cost to pay cards").setDisabled(!pack.cost.affordable(
                    inventory
                )).build()))
            }

            val buttons = ArrayList<Button>()

            buttons.add(
                Button.success("roll", "Roll")
                    .withDisabled(!pack.cost.affordable(inventory) || containers.any { container -> !container.paid() })
                    .withEmoji(Emoji.fromUnicode("\uD83C\uDFB2"))
            )

            buttons.add(Button.secondary("back", "Go Back"))

            result.add(ActionRow.of(buttons))
        }

        return result
    }

    private fun displayRollResult(result: List<Card>) {
        val builder = StringBuilder("### ${pack.packName} Result [${result.size} cards in total]\n\n")

        for (card in result) {
            builder.append("- ")

            if (card.tier == CardData.Tier.ULTRA) {
                builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
            } else if (card.tier == CardData.Tier.LEGEND) {
                builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
            }

            builder.append(card.cardInfo())

            if (!inventory.cards.containsKey(card)) {
                builder.append(" {**NEW**}")
            }

            if (card.tier == CardData.Tier.ULTRA) {
                builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
            } else if (card.tier == CardData.Tier.LEGEND) {
                builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
            }

            builder.append("\n")
        }

        val initialEmbed = EmbedBuilder()

        initialEmbed.setDescription(builder.toString().trim())
            .setColor(StaticStore.rainbow.random())

        if (noImage) {
            message.editMessage("")
                .setEmbeds(initialEmbed.build())

                .queue()

            return
        }

        val newCards = result.toSet().filter { c -> !inventory.cards.containsKey(c) && !inventory.favorites.containsKey(c) }.sortedWith(CardComparator()).reversed()

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
                    initialEmbed.setUrl("https://none.dummy").setImage(link)

                    embeds.add(initialEmbed.build())
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

        val availableSkins = result.toSet()
            .filter { c -> inventory.equippedSkins.containsKey(c) }
            .map { c -> inventory.equippedSkins[c] }
            .filterNotNull()

        if (availableSkins.isEmpty()) {
            message.editMessage("")
                .setEmbeds(initialEmbed.build())
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        availableSkins.forEach { s -> s.cache(authorMessage.jda, true) }

        val cachedLinks = availableSkins.subList(0, min(availableSkins.size, Message.MAX_EMBED_COUNT))
            .filter { skin -> skin.cacheLink.isNotEmpty() }
            .map { skin -> skin.cacheLink }

        val embeds = ArrayList<MessageEmbed>()

        cachedLinks.forEachIndexed { index, link ->
            if (index == 0) {
                initialEmbed.setUrl(link).setImage(link)

                embeds.add(initialEmbed.build())
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
}