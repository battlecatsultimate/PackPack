package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class ChannelAllowHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0
        set(value) {
            field = value

            val totalPage = getTotalPage(CardData.allowedChannel.size)

            field = max(0, min(field, totalPage - 1))
        }

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1))
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
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
            "channel" -> {
                if (event !is EntitySelectInteractionEvent) {
                    return
                }

                val value = event.values.map { mentionable -> mentionable.idLong }

                val disallow = value.any { id -> id in CardData.allowedChannel }
                val allow = value.any { id -> id !in CardData.allowedChannel }

                value.forEach { id ->
                    if (id in CardData.allowedChannel) {
                        CardData.allowedChannel.remove(id)
                    } else {
                        CardData.allowedChannel.add(id)
                    }
                }

                if (disallow && allow) {
                    event.deferReply()
                        .setContent("Successfully allowed/disallowed channels!")
                        .setEphemeral(true)
                        .queue()
                } else if (disallow) {
                    event.deferReply()
                        .setContent("Successfully disallowed selected channels!")
                        .setEphemeral(true)
                        .queue()
                } else {
                    event.deferReply()
                        .setContent("Successfully allowed selected channels!")
                        .setEphemeral(true)
                        .queue()
                }

                applyResult()
            }
            "confirm" -> {
                event.deferEdit()
                    .setContent("Allowed channel manager confirmed")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Allowed channel manager expired")
            .setComponents()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select channel to allow commands. Select assigned channel again to disallow\n\n")

        if (CardData.allowedChannel.isEmpty()) {
            builder.append("- There's no allowed channel")
        } else {
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, CardData.allowedChannel.size)) {
                builder.append(i + 1).append(". ").append("<#").append(CardData.allowedChannel[i]).append("> [").append(CardData.allowedChannel[i]).append("]\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                .setPlaceholder("Select channel to allow/disallow")
                .build()
        ))

        val totalPage = getTotalPage(CardData.allowedChannel.size)

        if (CardData.allowedChannel.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

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

        result.add(ActionRow.of(
            Button.primary("confirm", "Confirm")
        ))

        return result
    }
}