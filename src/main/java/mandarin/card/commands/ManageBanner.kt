package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.banner.BannerManageHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.Holder.getTotalPage
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class ManageBanner : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        if (loader.user.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(loader.member)) {
            return
        }

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(loader.user.id, BannerManageHolder(loader.message, loader.user.id, loader.channel.id, msg))
        }
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select the banner that you want to modify\nIf you want to create new one, click `Create New Banner`\n\n```md\n")

        if (CardData.banners.isEmpty()) {
            builder.append("No banner\n```")
        } else {
            for (i in 0 until min(CardData.banners.size, SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". ").append(CardData.banners[i]).append("\n")
            }

            builder.append("```")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        if (CardData.banners.isNotEmpty()) {
            for (i in 0 until min(CardData.banners.size, SearchHolder.PAGE_CHUNK)) {
                options.add(SelectOption.of(CardData.banners[i].name, i.toString()))
            }

            result.add(ActionRow.of(StringSelectMenu.create("banner").addOptions(options).setPlaceholder("Select banner to assign").build()))
        } else {
            options.add(SelectOption.of("a", "a"))

            result.add(ActionRow.of(StringSelectMenu.create("banner").addOptions(options).setPlaceholder("No banner").setDisabled(true).build()))
        }

        val totalPage = getTotalPage(CardData.banners.size)

        if (CardData.banners.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(
            Button.success("create", "Create New Banner").withEmoji(Emoji.fromUnicode("➕")),
            Button.secondary("close", "Close").withEmoji(EmojiStore.CROSS)
        ))


        return result
    }
}