package mandarin.card.supporter.holder.card

import common.CommonStatic
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class CardModeHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "modify" -> {
                connectTo(event, CardSelectHolder(authorMessage, userID, channelID, message))
            }
            "create" -> {
                connectTo(event, CardFileHolder(authorMessage, userID, channelID, message, null))
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed card manager")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {

    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResults(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResults(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResults(event: IMessageEditCallback) {
        event.deferEdit()
            .setComponents(getComponents())
            .setContent(getContents())
            .setFiles()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setFiles()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
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