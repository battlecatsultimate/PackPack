package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.ActivatorHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class Activate : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        replyToMessageSafely(ch, getText(), loader.message, { a -> a.setComponents(getComponents()) }, { msg ->
            StaticStore.putHolder(m.id, ActivatorHolder(loader.message, m.id, ch.id, msg))
        })
    }

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val banners = CardData.banners

        val dataSize = banners.size

        val bannerOptions = ArrayList<SelectOption>()

        for (i in 0 until min(dataSize, SearchHolder.PAGE_CHUNK)) {
            bannerOptions.add(SelectOption.of(banners[i].name, i.toString()).withEmoji(if (banners[i] in CardData.activatedBanners) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF))
        }

        rows.add(ActionRow.of(
            StringSelectMenu.create("banner").addOptions(bannerOptions).setPlaceholder("Select banner to activate/deactivate").build()
        ))

        val totalPage = SearchHolder.getTotalPage(dataSize)

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("confirm", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder("Select banners to activate/deactivate\n\n")

        val banners = CardData.banners

        for (i in 0 until min(SearchHolder.PAGE_CHUNK, CardData.banners.size)) {
            builder.append("**")
                .append(banners[i].name)
                .append("** : ")

            if (banners[i] in CardData.activatedBanners) {
                builder.append("Activated")
            } else {
                builder.append("Deactivated")
            }

            builder.append("\n")
        }

        return builder.toString()
    }
}