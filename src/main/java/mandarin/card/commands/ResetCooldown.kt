package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.ResetCooldownHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.data.ConfigHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import kotlin.math.ceil
import kotlin.math.min

class ResetCooldown : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        replyToMessageSafely(loader.channel, "Select pack to reset cooldown of it", loader.message, { a -> a.addComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(m.id, ResetCooldownHolder(loader.message, m.id, loader.channel.id, msg))
        }
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        for (i in 0 until min(CardData.cardPacks.size, ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
            val packName = if (CardData.cardPacks[i].packName.length >= 100) {
                CardData.cardPacks[i].packName.substring(0, 50) + "..."
            } else {
                CardData.cardPacks[i].packName
            }

            options.add(SelectOption.of(packName, i.toString()))
        }

        result.add(ActionRow.of(StringSelectMenu.create("pack").addOptions(options).build()))

        if (CardData.cardPacks.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(CardData.cardPacks.size * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize)

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

        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}