package mandarin.card.supporter.holder

import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.ceil
import kotlin.math.min

class ResetCooldownHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message) {
    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        println(event.componentId)

        when(event.componentId) {
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                val pack = CardData.cardPacks[index]

                registerPopUp(event, "Are you sure you want to reset cooldown of this pack [`${pack.packName}`] **for all users?\n\n__This cannot be undone__**", LangID.EN)

                StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                    expired = true

                    for (cooldown in CardData.cooldown.values) {
                        cooldown[pack.uuid] = 0
                    }

                    TransactionLogger.logCooldownReset(pack, authorMessage.author.id)

                    e.deferEdit()
                        .setContent("Successfully reset cooldown of ${pack.packName}")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return@ConfirmPopUpHolder null
                }, { e ->
                    StaticStore.putHolder(authorMessage.author.id, this)

                    applyResult(e)

                    return@ConfirmPopUpHolder null
                }, LangID.EN))
            }
            "prev" -> {
                page--

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "next10" -> {
                page -= 10

                applyResult(event)
            }
            "cancel" -> {
                expired = true

                event.deferEdit()
                    .setContent("Canceled resetting cooldown")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        }
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent("Select pack to reset cooldown of it")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.cardPacks.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
            val uuid = CardData.cardPacks[i].uuid

            options.add(SelectOption.of(CardData.cardPacks[i].packName, uuid.substring(0, min(uuid.length, SelectOption.VALUE_MAX_LENGTH))))
        }

        result.add(ActionRow.of(StringSelectMenu.create("pack").addOptions(options).build()))

        if (CardData.cardPacks.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(CardData.cardPacks.size * 1.0 / SearchHolder.PAGE_CHUNK)

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}