package mandarin.card.commands

import mandarin.card.supporter.Activator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.ActivatorHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.math.min

class Activate : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isMod(m))
            return

        val msg = getRepliedMessageSafely(ch, getText(), getMessage(event)) { a -> a.setComponents(getComponents()) }

        StaticStore.putHolder(m.id, ActivatorHolder(getMessage(event), ch.id, msg))
    }

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val activators = Activator.values()

        val dataSize = Activator.values().size

        for (i in 0 until min(dataSize, 3)) {
            rows.add(ActionRow.of(Button.secondary(i.toString(), activators[i].title).withEmoji(if (activators[i] in CardData.activatedBanners) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)))
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

        val activators = Activator.values()

        for (i in 0 until min(3, activators.size)) {
            builder.append("**")
                .append(activators[i].title)
                .append("** : ")

            if (activators[i] in CardData.activatedBanners) {
                builder.append("Activated")
            } else {
                builder.append("Deactivated")
            }

            builder.append("\n")
        }

        return builder.toString()
    }
}