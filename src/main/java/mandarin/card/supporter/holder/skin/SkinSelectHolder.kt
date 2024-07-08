package mandarin.card.supporter.holder.skin

import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.MessageUpdater
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class SkinSelectHolder(author: Message, channelID: String, private var message: Message, private val card: Card) : ComponentHolder(author, channelID, message), MessageUpdater {
    private val skins = CardData.skins.filter { s -> s.card == card }.toMutableList()

    private var page = 0

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "prev" -> {
                page--

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "next10" -> {
                page += 10

                applyResult(event)
            }
            "skin" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                connectTo(event, SkinModifyHolder(authorMessage, channelID, message, skins[index], false))
            }
            "add" -> {
                connectTo(event, SkinFileHolder(authorMessage, channelID, message, card))
            }
            "back" -> {
                goBack(event)
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Management closed")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true
            }
        }
    }

    override fun onMessageUpdated(message: Message) {
        this.message = message
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder?) {
        skins.clear()

        skins.addAll(CardData.skins.filter { s -> s.card == card })

        applyResult(event)
    }

    override fun onBack(child: Holder?) {
        skins.clear()

        skins.addAll(CardData.skins.filter { s -> s.card == card })

        applyResult()
    }

    private fun applyResult() {
        var builder = message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        var builder = event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select skin to edit/remove them, select `Create New Skin` to add new skin for this card\n")
            .append("### List Of Skins\n")

        if (skins.isEmpty()) {
            builder.append("- No Skins For This Card")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(skins.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". [").append(skins[i].skinID).append("] ").append(skins[i].name).append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        if (skins.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(skins.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                options.add(SelectOption.of("[${skins[i].skinID}] ${skins[i].name}", i.toString()).withDescription(skins[i].file.name))
            }
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("skin")
                .addOptions(options)
                .setDisabled(skins.isEmpty())
                .setPlaceholder("Select Skin To Modify")
                .build()
        ))

        result.add(ActionRow.of(Button.secondary("add", "Create New Skin").withEmoji(Emoji.fromUnicode("➕"))))

        result.add(ActionRow.of(
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}