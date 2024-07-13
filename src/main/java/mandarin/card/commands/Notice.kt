package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.NotificationConfigHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Notice : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        replyToMessageSafely(ch, getContents(m.idLong), loader.message, { a -> a.setComponents(getComponents(m.idLong))}) { msg ->
            StaticStore.putHolder(m.id, NotificationConfigHolder(loader.message, ch.id, msg))
        }
    }

    private fun getContents(userID: Long) : String {
        val notifyGroup = CardData.notifierGroup.computeIfAbsent(userID) { booleanArrayOf(false, false) }

        val builder = StringBuilder(
            "## Notification Config\n" +
            "You can make bot notify you whenever available card pack or slot machine is found\n" +
            "\n" +
            "**Keep in mind that you have to allow DM to make bot send you notification. Disallowing DM from bot will make it automatically turn off notifications**\n\n"
        )

        builder.append("- **Notify Card Pack** : ")

        if (notifyGroup[0]) {
            builder.append(EmojiStore.SWITCHON.formatted).append(" On")
        } else {
            builder.append(EmojiStore.SWITCHOFF.formatted).append(" Off")
        }

        builder.append("\n- **Notify Slot Machine** : ")

        if (notifyGroup[1]) {
            builder.append(EmojiStore.SWITCHON.formatted).append(" On")
        } else {
            builder.append(EmojiStore.SWITCHOFF.formatted).append(" Off")
        }

        return builder.toString()
    }

    private fun getComponents(userID: Long) : List<LayoutComponent> {
        val notifyGroup = CardData.notifierGroup.computeIfAbsent(userID) { booleanArrayOf(false, false) }

        val result = ArrayList<LayoutComponent>()

        if (CardData.skins.any { s -> s.creator == userID }) {
            result.add(ActionRow.of(
                Button.secondary("card", "Notify Available Card Pack").withEmoji(if (notifyGroup[0]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
                Button.secondary("slot", "Notify Available Slot Machine").withEmoji(if (notifyGroup[1]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("card", "Notify Available Card Pack").withEmoji(if (notifyGroup[0]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
                Button.secondary("slot", "Notify Available Slot Machine").withEmoji(if (notifyGroup[1]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
                Button.secondary("skin", "Notify Skin Purchase").withEmoji(if (userID in CardData.purchaseNotifier) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
            ))
        }

        result.add(ActionRow.of(Button.secondary("test", "Test Sending DM").withEmoji(Emoji.fromUnicode("📢"))))

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}