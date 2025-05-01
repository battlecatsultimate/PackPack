package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.ChannelAllowHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import kotlin.math.min

class AllowChannel : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        replyToMessageSafely(ch, getContents(), loader.message, { a -> a.setComponents(getComponents()) }) { message ->
            StaticStore.putHolder(m.id, ChannelAllowHolder(loader.message, m.id, ch.id, message))
        }
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select channel to allow commands. Select assigned channel again to disallow\n\n")

        if (CardData.allowedChannel.isEmpty()) {
            builder.append("- There's no allowed channel")
        } else {
            for (i in 0 until min(SearchHolder.PAGE_CHUNK, CardData.allowedChannel.size)) {
                builder.append(i + 1).append(". ").append("<#").append(CardData.allowedChannel[i]).append("> [").append(CardData.allowedChannel[i]).append("]\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS, ChannelType.FORUM, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD)
                .setMaxValues(EntitySelectMenu.OPTIONS_MAX_AMOUNT)
                .setPlaceholder("Select channel to allow/disallow")
                .build()
        ))

        val totalPage = SearchHolder.getTotalPage(CardData.allowedChannel.size)

        if (CardData.allowedChannel.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(
            Button.primary("confirm", "Confirm")
        ))

        return result
    }
}