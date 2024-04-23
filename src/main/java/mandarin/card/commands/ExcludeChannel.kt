package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.ExcludeChannelHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class ExcludeChannel : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL)
            return

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents(loader.guild)) }) { msg ->
            StaticStore.putHolder(m.id, ExcludeChannelHolder(loader.message, loader.channel.id, msg))
        }
    }

    private fun getComponents(guild: Guild) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK) {
            val pages = ArrayList<ActionComponent>()

            if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            pages.add(Button.of(ButtonStyle.SECONDARY,"prev", "Previous Page", EmojiStore.PREVIOUS).asDisabled())
            pages.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (CardData.excludedCatFoodChannel.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(pages))
        }

        result.add(
            ActionRow.of(
                EntitySelectMenu.create("addChannel", EntitySelectMenu.SelectTarget.CHANNEL)
                    .setChannelTypes(ChannelType.TEXT, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                    .setPlaceholder("Select channels to add them into exclusion list")
                    .setMaxValues(25)
                    .build()
            )
        )

        if (CardData.excludedCatFoodChannel.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            val size = min(CardData.excludedCatFoodChannel.size, SearchHolder.PAGE_CHUNK)

            for (m in 0 until size) {
                val channel = guild.getGuildChannelById(CardData.excludedCatFoodChannel[m])

                if (channel != null) {
                    options.add(SelectOption.of(channel.name, CardData.excludedCatFoodChannel[m]).withDescription(CardData.excludedCatFoodChannel[m]))
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
            val size = min(CardData.excludedCatFoodChannel.size, SearchHolder.PAGE_CHUNK)

            for (m in 0 until size) {
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