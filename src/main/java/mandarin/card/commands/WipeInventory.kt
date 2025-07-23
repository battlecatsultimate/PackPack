package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.InventoryWipeHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.EntitySelectMenu

class WipeInventory : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(m.id, InventoryWipeHolder(loader.message, m.id, loader.channel.id, msg))
        }
    }

    private fun getContents() : String {
        return "Please select user to wipe their inventory"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("Select User To Wipe Inventory")
                .setRequiredRange(1, 1)
                .build()
        ))

        result.add(ActionRow.of(
            Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}