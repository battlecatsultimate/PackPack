package mandarin.card.commands

import mandarin.card.supporter.ServerData
import mandarin.card.supporter.holder.moderation.TransferInventorySourceHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu

class TransferInventory : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && m.id != ServerData.get("gid"))
            return

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(m.id, TransferInventorySourceHolder(loader.message, loader.channel.id, msg))
        }
    }

    private fun getContents() : String {
        val cf = EmojiStore.ABILITY["CF"]

        return "## Inventory Transfer\n" +
                "This command will allow you to transfer specific user's inventory to other user. First, you have to pick which user's inventory you want to transfer\n" +
                "### Transfer Mode : Inject\n" +
                "Inject mode will keep target user's contents, and add up contents to it." +
                " For example, if you want to transfer inventory of user A to user B, and if we assume that user A had $cf 5000 and user B had $cf 10000, after transfer is done, user B will have $cf 15000\n" +
                "### Reset User's Inventory? : No\n" +
                "After transfer is done, picked user's inventory won't get reset, and keep the contents"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("Pick user here")
                .setRequiredRange(1, 1)
                .build()
        ))

        result.add(ActionRow.of(
            Button.secondary("mode", "Mode : Inject").withEmoji(Emoji.fromUnicode("🎛️")),
            Button.secondary("reset", "Reset User's Inventory").withEmoji(EmojiStore.SWITCHOFF)
        ))

        result.add(ActionRow.of(
            Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}