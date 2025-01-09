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

class Notice : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user
        val ch = loader.channel

        replyToMessageSafely(ch, getContents(u.idLong), loader.message, { a -> a.setComponents(getComponents(u.idLong))}) { msg ->
            StaticStore.putHolder(u.id, NotificationConfigHolder(loader.message, u.id, ch.id, msg))
        }
    }

    private fun getContents(userID: Long) : String {
        val notifyGroup = CardData.notifierGroup.computeIfAbsent(userID) { booleanArrayOf(false, false) }

        val builder = StringBuilder(
            "## Notification Config\n" +
            "You can make bot notify you whenever available card pack or slot machine is found\n" +
            "\n" +
            "**Keep in mind that notification will be sent in <#${CardData.notification}>. Your status and actions will get displayed in there to everyone**\n\n"
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

        if (CardData.skins.any { s -> s.creator == userID }) {
            builder.append("\n- **Notify Skin Purchase** : ")

            if (userID in CardData.purchaseNotifier) {
                builder.append(EmojiStore.SWITCHON.formatted).append(" On")
            } else {
                builder.append(EmojiStore.SWITCHOFF.formatted).append(" Off")
            }
        }

        return builder.toString()
    }

    private fun getComponents(userID: Long) : List<LayoutComponent> {
        val notifyGroup = CardData.notifierGroup.computeIfAbsent(userID) { booleanArrayOf(false, false) }

        val result = ArrayList<LayoutComponent>()

        if (CardData.skins.any { s -> s.creator == userID }) {
            result.add(ActionRow.of(
                Button.secondary("card", "Notify Available Card Pack").withEmoji(if (notifyGroup[0]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
                Button.secondary("slot", "Notify Available Slot Machine").withEmoji(if (notifyGroup[1]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
                Button.secondary("skin", "Notify Skin Purchase").withEmoji(if (userID in CardData.purchaseNotifier) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("card", "Notify Available Card Pack").withEmoji(if (notifyGroup[0]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
                Button.secondary("slot", "Notify Available Slot Machine").withEmoji(if (notifyGroup[1]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
            ))
        }

        result.add(ActionRow.of(Button.secondary("test", "Test Sending Notification").withEmoji(Emoji.fromUnicode("📢"))))

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}