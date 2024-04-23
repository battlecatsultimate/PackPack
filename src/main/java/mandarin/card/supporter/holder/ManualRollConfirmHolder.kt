package mandarin.card.supporter.holder

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload

class ManualRollConfirmHolder(author: Message, channelID: String, messageID: String, private val member: Member, private val pack: CardPack, private val users: List<String>) : ComponentHolder(author, channelID, messageID) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when (event.componentId) {
            "roll" -> {
                val g = event.guild ?: return
                val ch = event.messageChannel

                event.deferEdit()
                    .setContent("Rolling...! \uD83C\uDFB2")
                    .setComponents()
                    .queue()

                try {
                    if (users.size == 1) {
                        g.retrieveMember(UserSnowflake.fromId(users[0])).queue { targetMember ->
                            val result = pack.roll()

                            val inventory = Inventory.getInventory(targetMember.idLong)

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

                                ch.sendMessage(builder.toString())
                                    .setMessageReference(authorMessage)
                                    .mentionRepliedUser(false)
                                    .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }.toSet().map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                                    .queue()
                            } catch (_: Exception) {

                            }

                            inventory.addCards(result)

                            TransactionLogger.logRoll(result, pack, targetMember, true)
                        }
                    } else {
                        users.forEach {
                            g.retrieveMember(UserSnowflake.fromId(it)).queue { targetMember ->
                                val result = pack.roll()

                                val inventory = Inventory.getInventory(targetMember.idLong)

                                inventory.addCards(result)

                                TransactionLogger.logRoll(result, pack, targetMember, true)
                            }
                        }

                        Command.replyToMessageSafely(
                            ch,
                            "Rolled ${pack.packName} for ${users.size} people successfully",
                            authorMessage
                        ) { a -> a }

                        TransactionLogger.logMassRoll(member, users.size, pack)
                    }
                } catch (_: Exception) {
                    Command.replyToMessageSafely(
                        ch,
                        "Bot failed to find provided user in this server",
                        authorMessage
                    ) { a -> a }
                }
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
        }
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(pack.displayInfo() + "\n\nDo you want to manually roll this pack to users?")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val buttons = ArrayList<Button>()

        buttons.add(Button.success("roll", "Roll").withEmoji(Emoji.fromUnicode("\uD83C\uDFB2")))
        buttons.add(Button.secondary("back", "Go Back"))

        result.add(ActionRow.of(buttons))

        return result
    }
}