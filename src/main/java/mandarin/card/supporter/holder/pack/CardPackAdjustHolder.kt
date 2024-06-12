package mandarin.card.supporter.holder.pack

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardPackCooldownHolder
import mandarin.card.supporter.holder.modal.CardPackNameHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class CardPackAdjustHolder(
    author: Message,
    channelID: String,
    private val message: Message,
    private val pack: CardPack,
    private val new: Boolean
) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

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

                connectTo(CardPackNameHolder(authorMessage, channelID, message, false, pack))
            }
            "cost" -> {
                connectTo(event, CardPackCostHolder(authorMessage, channelID, message, pack))
            }
            "content" -> {
                connectTo(event, CardPackContentHolder(authorMessage, channelID, message, pack))
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

                connectTo(CardPackCooldownHolder(authorMessage, channelID, message.id, pack))
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
                        "Are you sure you want to cancel creating card pack? This cannot be undone",
                        LangID.EN
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        expired = true

                        e.deferEdit().queue()

                        goBack()
                    }, LangID.EN))
                } else {
                    CardBot.saveCardData()

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card pack? This cannot be undone",
                    LangID.EN
                )

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    CardData.cardPacks.remove(pack)

                    CardBot.saveCardData()

                    e.deferReply()
                        .setContent("Successfully deleted card pack! Check result above")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, LangID.EN))
            }
        }
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    private fun applyResult() {
        message.editMessage(pack.displayInfo())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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