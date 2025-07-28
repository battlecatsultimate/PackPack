package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class PackSelectHolder(
    author: Message,
    userID: String,
    channelID: String,
    private val member: Member,
    message: Message,
    private val noImage: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val packList = CardData.cardPacks.filter { pack -> pack.activated && !pack.isInvalid() && (pack.cost.roles.isEmpty() || pack.cost.roles.any { id -> id in member.roles.map { role -> role.id } }) }

    private var page = 0

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card pack select expired")
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

                val pack = packList.find { pack -> pack.uuid == uuid }

                if (pack == null) {
                    event.deferReply()
                        .setContent("Sorry, bot failed bring information of selected pack...")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                connectTo(event, PackPayHolder(authorMessage, userID, channelID, message, member, pack, noImage))
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

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
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

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val packOptions = ArrayList<SelectOption>()

        val size = min(packList.size, ConfigHolder.SearchLayout.COMPACTED.chunkSize * (page + 1))

        for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until size) {
            val pack = packList[i]
            val cooldownMap = CardData.cooldown[authorMessage.author.idLong]

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

        if (packList.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()
            val totalPage = ceil(packList.size * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize)

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