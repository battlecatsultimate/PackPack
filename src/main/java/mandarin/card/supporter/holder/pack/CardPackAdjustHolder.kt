package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardPackCooldownHolder
import mandarin.card.supporter.holder.modal.CardPackNameHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit

class CardPackAdjustHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val pack: CardPack,
    private val new: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "name" -> {
                val input = TextInput.create("name", "Name", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Decide name of card pack")
                    .build()

                val modal = Modal.create("name", "Card Pack Name")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardPackNameHolder(authorMessage, userID, channelID, message, false, pack))
            }
            "cost" -> {
                connectTo(event, CardPackCostHolder(authorMessage, userID, channelID, message, pack))
            }
            "content" -> {
                connectTo(event, CardPackContentHolder(authorMessage, userID, channelID, message, pack))
            }
            "cooldown" -> {
                val input = TextInput.create("cooldown", "Cooldown", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Example : 1d3h4m30s => 1 day, 3 hours, 4 minutes, 30 seconds")
                    .build()

                val modal = Modal.create("cooldown", "Cooldown of Card Pack")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardPackCooldownHolder(authorMessage, userID, channelID, message, pack))
            }
            "activate" -> {
                if (!pack.activated && pack.isInvalid()) {
                    event.deferReply()
                        .setContent("You can't activate this pack because this pack is invalid! Check this pack's content")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                pack.activated = !pack.activated

                CardBot.saveCardData()

                applyResult(event)
            }
            "create" -> {
                CardData.cardPacks.add(pack)

                CardBot.saveCardData()

                event.deferReply()
                    .setContent("Successfully created card pack! Check result above")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card pack? This cannot be undone"
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        e.deferEdit().queue()

                        goBack()
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    CardBot.saveCardData()

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card pack? This cannot be undone"
                )

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    CardData.cardPacks.remove(pack)

                    CardBot.saveCardData()

                    e.deferReply()
                        .setContent("Successfully deleted card pack! Check result above")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult() {
        message.editMessage(pack.displayInfo())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(pack.displayInfo())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("name", "Change Name"),
            Button.secondary("cost", "Adjust Pack Cost"),
            Button.secondary("content", "Adjust Pack Content"),
            Button.secondary("cooldown", "Adjust Pack Cooldown")
        ))

        if (new) {
            result.add(ActionRow.of(
                Button.danger("back", "Go Back"),
                Button.success("create", "Create Pack")
            ))
        } else {
            val emoji = if (pack.activated)
                EmojiStore.SWITCHON
            else
                EmojiStore.SWITCHOFF

            result.add(ActionRow.of(
                Button.secondary("activate", "Activate Card Pack").withEmoji(emoji)
            ))

            result.add(ActionRow.of(
                Button.secondary("back", "Go Back"),
                Button.danger("delete", "Delete Pack")
            ))
        }

        return result
    }
}