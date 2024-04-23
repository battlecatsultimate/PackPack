package mandarin.card.supporter.holder.modal

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.pack.CardPackAdjustHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.PackCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class CardPackNameHolder(author: Message, channelID: String, private val message: Message, private val new: Boolean, private val pack: CardPack?) : ModalHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "name")
            return

        val name = getValueFromMap(event.values, "name")

        if (new) {
            val pack = CardPack(name, PackCost(0L, 0L, ArrayList(), ArrayList()), ArrayList(), 0L)

            event.deferEdit()
                .setContent(pack.displayInfo())
                .setComponents(getComponents())
                .mentionRepliedUser(false)
                .queue()

            parent?.connectTo(CardPackAdjustHolder(authorMessage, channelID, message, pack, new))
        } else if (pack != null) {
            pack.packName = name

            if (pack in CardData.cardPacks) {
                CardBot.saveCardData()
            }

            event.deferReply()
                .setContent("Changed pack name as $name! Check result above")
                .setEphemeral(true)
                .queue()

            goBack()
        }
    }

    fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("name", "Change Name"),
            Button.secondary("cost", "Adjust Pack Cost"),
            Button.secondary("content", "Adjust Pack Content"),
            Button.secondary("cooldown", "Adjust Pack Cooldown")
        ))

        if (new) {
            result.add(ActionRow.of(
                Button.danger("back", "Go Back"),
                Button.success("create", "Create Pack")
            ))
        } else {
            val emoji = if (pack?.activated == true)
                EmojiStore.SWITCHON
            else
                EmojiStore.SWITCHOFF

            result.add(ActionRow.of(
                Button.secondary("activate", "Activate Card Pack").withEmoji(emoji)
            ))

            result.add(ActionRow.of(
                Button.secondary("back", "Go Back"),
                Button.danger("delete", "Delete Pack")
            ))
        }

        return result
    }
}