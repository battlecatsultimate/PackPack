package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.ActivatorHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
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

        for (i in 0 until min(dataSize, 3)) {
            rows.add(ActionRow.of(Button.secondary(i.toString(), banners[i].name).withEmoji(if (banners[i] in CardData.activatedBanners) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)))
        }

        var totPage = dataSize / 3

        if (dataSize % 3 != 0)
            totPage++

        if (dataSize > 3) {
            val buttons = ArrayList<Button>()

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totPage > 10) {
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

        for (i in 0 until min(3, banners.size)) {
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