package mandarin.card.supporter.holder.pack

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
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.util.ArrayList
import kotlin.collections.any
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.map
import kotlin.math.ceil
import kotlin.math.min
import kotlin.ranges.until
import kotlin.text.ifEmpty

class PackSelectHolder(
    author: Message,
    channelID: String,
    private val member: Member,
    message: Message,
    private val noImage: Boolean
) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val packList = CardData.cardPacks.filter { pack -> pack.activated && !pack.isInvalid() && (pack.cost.roles.isEmpty() || pack.cost.roles.any { id -> id in member.roles.map { role -> role.id } }) }

    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val uuid = event.values[0]

                val pack = packList.find { pack -> pack.uuid == uuid }

                if (pack == null) {
                    event.deferReply()
                        .setContent("Sorry, bot failed bring information of selected pack...")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                connectTo(event, PackPayHolder(authorMessage, channelID, message, member, pack, noImage))
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed pack selection")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true

                expire(authorMessage.author.id)
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

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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

        val size = min(packList.size, SearchHolder.PAGE_CHUNK * (page + 1))

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val pack = packList[i]
            val cooldownMap = CardData.cooldown[authorMessage.author.id]

            val desc = if (cooldownMap == null) {
                ""
            } else {
                val cooldown = cooldownMap[pack.uuid]

                if (cooldown != null && cooldown - CardData.getUnixEpochTime() > 0) {
                    "Cooldown Left : ${CardData.convertMillisecondsToText(cooldown - CardData.getUnixEpochTime())}"
                } else {
                    ""
                }
            }

            packOptions.add(
                SelectOption.of(pack.packName, pack.uuid).withDescription(desc.ifEmpty { null })
            )
        }

        val packs = StringSelectMenu.create("pack")
            .addOptions(packOptions)
            .setPlaceholder("Select pack here")
            .build()

        result.add(ActionRow.of(packs))

        if (packList.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = ceil(packList.size * 1.0 / SearchHolder.PAGE_CHUNK)

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