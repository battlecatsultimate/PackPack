package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import kotlin.math.max
import kotlin.math.min

class ExcludeChannelHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Channel config expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val g = event.guild ?: return

        when (event.componentId) {
            "addChannel" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val channels = event.values.map { id -> id.id }.filter { id -> id !in CardData.excludedCatFoodChannel }.toSet()

                CardData.excludedCatFoodChannel.addAll(channels)

                event.deferReply()
                    .setContent("Successfully excluded selected channels!")
                    .setEphemeral(true)
                    .queue()

                applyResult(g)

                TransactionLogger.logChannelExcluded(authorMessage.author.id, channels.toList(), true)
            }
            "removeChannel" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val channels = event.values.filter { id -> id in CardData.excludedCatFoodChannel }

                CardData.excludedCatFoodChannel.removeAll(channels.toSet())

                event.deferReply()
                    .setContent("Successfully removed selected channels from exclusion list!")
                    .setEphemeral(true)
                    .queue()

                val totalPage = getTotalPage(CardData.excludedCatFoodChannel.size)

                page = min(max(0, page), totalPage - 1)

                applyResult(g)

                TransactionLogger.logChannelExcluded(authorMessage.author.id, channels.toList(), false)
            }
            "prev10" -> {
                page -= 10

                applyResult(event, g)
            }
            "prev" -> {
                page--

                applyResult(event, g)
            }
            "next" -> {
                page++

                applyResult(event, g)
            }
            "next10" -> {
                page += 10

                applyResult(event, g)
            }
            "confirm" -> {
                CardBot.saveCardData()

                event.deferEdit()
                    .setContent("Confirmed configure")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback,guild: Guild) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents(guild))
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(guild: Guild) {
        message.editMessage(getContents())
            .setComponents(getComponents(guild))
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents(guild: Guild) : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK) {
            var totalPage = CardData.excludedCatFoodChannel.size / SearchHolder.PAGE_CHUNK

            if (CardData.excludedCatFoodChannel.size % SearchHolder.PAGE_CHUNK != 0)
                totalPage++

            val pages = ArrayList<Button>()

            if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            pages.add(Button.of(ButtonStyle.SECONDARY,"prev", "Previous Page", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))
            pages.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(pages))
        }

        result.add(
            ActionRow.of(
                EntitySelectMenu.create("addChannel", EntitySelectMenu.SelectTarget.CHANNEL)
                    .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                    .setPlaceholder("Select channels to add them into exclusion list")
                    .setMaxValues(25)
                    .build()
            )
        )

        if (CardData.excludedCatFoodChannel.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            val size = min(CardData.excludedCatFoodChannel.size, (page + 1) * SearchHolder.PAGE_CHUNK)

            for (m in page * SearchHolder.PAGE_CHUNK until size) {
                val channel = guild.getGuildChannelById(CardData.excludedCatFoodChannel[m])

                if (channel != null) {
                    options.add(
                        SelectOption.of(channel.name, CardData.excludedCatFoodChannel[m]).withDescription(
                            CardData.excludedCatFoodChannel[m]))
                } else {
                    options.add(SelectOption.of("UNKNOWN", CardData.excludedCatFoodChannel[m]).withDescription(CardData.excludedCatFoodChannel[m]))
                }
            }

            result.add(
                ActionRow.of(
                    StringSelectMenu.create("removeChannel")
                        .addOptions(options)
                        .setPlaceholder("Select channel to remove from exclusion list")
                        .setMaxValues(25)
                        .build()
                )
            )
        }

        result.add(ActionRow.of(Button.primary("confirm", "Confirm").withEmoji(Emoji.fromUnicode("✅"))))

        return result
    }

    private fun getContents() : String {
        val builder = StringBuilder("List of excluded channel from giving cat food\n\n")

        if (CardData.excludedCatFoodChannel.isEmpty())
            return builder.append("- **No Channels**").toString()
        else {
            val size = min(CardData.excludedCatFoodChannel.size, (page + 1) * SearchHolder.PAGE_CHUNK)

            for (m in page * SearchHolder.PAGE_CHUNK until size) {
                builder.append(m + 1).append(". <#").append(CardData.excludedCatFoodChannel[m]).append(">\n")
            }

            if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK) {
                var totalPage = CardData.excludedCatFoodChannel.size / SearchHolder.PAGE_CHUNK

                if (CardData.excludedCatFoodChannel.size % SearchHolder.PAGE_CHUNK != 0)
                    totalPage++

                builder.append("\n").append("Page : ").append(1).append("/").append(totalPage)
            }

            return builder.toString()
        }
    }
}