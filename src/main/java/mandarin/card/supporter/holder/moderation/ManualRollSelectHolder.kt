package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.ceil
import kotlin.math.min

class ManualRollSelectHolder(author: Message, channelID: String, message: Message, private val member: Member, private val users: List<String>) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Manual roll expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val uuid = event.values[0]

                val pack = CardData.cardPacks.find { pack -> pack.uuid == uuid }

                if (pack == null) {
                    event.deferReply()
                        .setContent("Sorry, bot failed bring information of selected pack...")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                connectTo(event, ManualRollConfirmHolder(authorMessage, channelID, message, member, pack, users))
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed pack selection")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
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
                page += 10

                applyResult(event)
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent("Please select the pack that you want to roll")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage("Please select the pack that you want to roll")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val packOptions = ArrayList<SelectOption>()

        val size = min(CardData.cardPacks.size, SearchHolder.PAGE_CHUNK * (page + 1))

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            packOptions.add(SelectOption.of(CardData.cardPacks[i].packName, CardData.cardPacks[i].uuid))
        }

        val packs = StringSelectMenu.create("pack")
            .addOptions(packOptions)
            .setPlaceholder("Select pack here")
            .build()

        result.add(ActionRow.of(packs))

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

        result.add(ActionRow.of(Button.danger("close", "Close")))

        return result
    }
}