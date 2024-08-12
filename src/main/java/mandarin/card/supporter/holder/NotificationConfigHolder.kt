package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.Notification
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class NotificationConfigHolder(author: Message, channelID: String, message: Message) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var tested = false

    override fun onExpire() {
        message.editMessage("Notification setting expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val userID = authorMessage.author.idLong

        val notifyGroup = CardData.notifierGroup.computeIfAbsent(userID) { booleanArrayOf(false, false) }

        when(event.componentId) {
            "card" -> {
                notifyGroup[0] = !notifyGroup[0]

                applyResult(event)
            }
            "slot" -> {
                notifyGroup[1] = !notifyGroup[1]

                applyResult(event)
            }
            "skin" -> {
                if (userID in CardData.purchaseNotifier) {
                    CardData.purchaseNotifier.remove(userID)
                } else {
                    CardData.purchaseNotifier.add(userID)
                }

                applyResult(event)
            }
            "test" -> {
                Notification.handleNotificationTest(authorMessage.author.idLong)

                tested = true

                applyResult()
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed notification config")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun clean() {

    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val userID = authorMessage.author.idLong
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

    private fun getComponents() : List<LayoutComponent> {
        val userID = authorMessage.author.idLong
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

        result.add(ActionRow.of(Button.secondary("test", "Test Sending Notification").withEmoji(Emoji.fromUnicode("📢")).withDisabled(tested)))

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}