package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.CardPayContainer
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload

class PackPayHolder(
    author: Message,
    channelID: String,
    private val message: Message,
    private val member: Member,
    private val pack: CardPack,
    private val noImage: Boolean
) : ComponentHolder(author, channelID, message.id) {
    private val containers = Array(pack.cost.cardsCosts.size) {
        CardPayContainer(pack.cost.cardsCosts[it])
    }

    private val inventory = Inventory.getInventory(author.author.idLong)

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "roll" -> {
                event.deferEdit()
                    .setContent("Rolling...! \uD83C\uDFB2")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

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

                inventory.cards.entries.removeIf { (_, amount) -> amount <= 0 }

                val result = pack.roll()

                try {
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

                    if (noImage) {
                        message.delete().queue()

                        event.messageChannel
                            .sendMessage(builder.toString())
                            .setMessageReference(authorMessage)
                            .mentionRepliedUser(false)
                            .queue()
                    } else {
                        message.delete().queue()

                        event.messageChannel
                            .sendMessage(builder.toString())
                            .setMessageReference(authorMessage)
                            .mentionRepliedUser(false)
                            .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }
                                .toSet()
                                .map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                            .queue()
                    }
                } catch (e: Exception) {
                    StaticStore.logger.uploadErrorLog(e, "E/PackPayHolder::onEvent - Failed to upload card roll result")
                }

                result.forEach { card ->
                    inventory.cards[card] = (inventory.cards[card] ?: 0) + 1
                }

                if (pack.cooldown > 0L) {
                    val cooldownMap = CardData.cooldown.computeIfAbsent(authorMessage.author.id) { HashMap() }

                    cooldownMap[pack.uuid] = CardData.getUnixEpochTime() + pack.cooldown
                }

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

                    registerPopUp(event, "Are you sure you want to go back? All your selected cards will be cleared", LangID.EN)

                    StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                        e.deferEdit().queue()

                        goBack()

                        return@ConfirmPopUpHolder null
                    }, { e ->
                        StaticStore.putHolder(authorMessage.author.id, this)

                        applyResult(e)

                        return@ConfirmPopUpHolder null
                    }, LangID.EN))
                } else {
                    event.deferEdit().queue()

                    goBack()
                }
            }
        }
    }

    override fun onBack() {
        super.onBack()

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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

        val cooldownMap = CardData.cooldown[member.id]
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

        val cooldownMap = CardData.cooldown[member.id]
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
                        SelectOption.of(container.cost.getCostName(), index.toString())
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
}