package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.card.CardModeHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class ManageCard : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        if (loader.member.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(loader.member)) {
            return
        }

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(loader.user.id, CardModeHolder(loader.message, loader.user.id, loader.channel.id, msg))
        }
    }

    private fun getContents() : String {
        return "Please select the action that you want to do"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val cardEmoji = EmojiStore.CARDS.values.random().random()

        result.add(ActionRow.of(Button.secondary("modify", "Modify Existing One").withEmoji(cardEmoji)))
        result.add(ActionRow.of(Button.success("create", "Create New Card").withEmoji(Emoji.fromUnicode("➕"))))
        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}