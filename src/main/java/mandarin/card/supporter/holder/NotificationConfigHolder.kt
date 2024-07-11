package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class NotificationConfigHolder(author: Message, channelID: String, message: Message) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var tested = false

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
            "test" -> {
                authorMessage.author.openPrivateChannel().queue { private ->
                    private.sendMessage("This is test message to check if bot can send DM or not").queue({ _ ->
                        event.deferReply()
                            .setContent("Successfully sent DM!")
                            .setEphemeral(true)
                            .queue()

                        tested = true

                        applyResult()
                    }) { _ ->
                        event.deferReply()
                            .setContent("Bot failed to reach your DM... Maybe you have blocked the bot or DM in this server?")
                            .setEphemeral(true)
                            .queue()
                    }
                }
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed notification config")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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

    private fun getComponents() : List<LayoutComponent> {
        val userID = authorMessage.author.idLong
        val notifyGroup = CardData.notifierGroup.computeIfAbsent(userID) { booleanArrayOf(false, false) }

        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("card", "Notify Available Card Pack").withEmoji(if (notifyGroup[0]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
            Button.secondary("slot", "Notify Available Slot Machine").withEmoji(if (notifyGroup[1]) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
        ))

        result.add(ActionRow.of(Button.secondary("test", "Test Sending DM").withEmoji(Emoji.fromUnicode("📢")).withDisabled(tested)))

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}