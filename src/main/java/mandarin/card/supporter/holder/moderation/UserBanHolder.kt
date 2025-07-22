package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import kotlin.math.ceil
import kotlin.math.min

class UserBanHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun onExpire() {
        message.editMessage("User ban manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "user" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val reasons = ArrayList<String>()

                event.mentions.users.forEach { u ->
                    if (u.isBot) {
                        reasons.add("- <@${u.idLong}> : You can't ban bot")
                    } else if (u.idLong == authorMessage.author.idLong) {
                        reasons.add("- <@${u.idLong}> : You can't ban yourself")
                    } else {
                        if (u.idLong !in CardData.bannedUser) {
                            CardData.bannedUser.add(u.idLong)
                        } else {
                            CardData.bannedUser.remove(u.idLong)
                        }
                    }
                }

                if (reasons.isNotEmpty()) {
                    val builder = StringBuilder("Bot failed to ban some users due to reason below :\n\n")

                    for (reason in reasons) {
                        builder.append(reason).append("\n")
                    }

                    event.deferReply()
                        .setContent(builder.toString().trim())
                        .setAllowedMentions(ArrayList())
                        .setEphemeral(true)
                        .queue()

                    applyResult()
                } else {
                    applyResult(event)
                }
            }
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
            "confirm" -> {
                event.deferEdit()
                    .setContent("Confirmed the moderation config")
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
        val builder = StringBuilder()

        builder.append("Select user to ban from using any commands of this bot. Select banned user again to unban them\n")
            .append("### List of Banned Users\n")

        if (CardData.bannedUser.isEmpty()) {
            builder.append("- No Banned Users")
        } else {
            for (i in (page * SearchHolder.PAGE_CHUNK) until min(CardData.bannedUser.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". <@").append(CardData.bannedUser[i]).append("> [").append(CardData.bannedUser[i]).append("]\n")
            }
        }

        return builder.toString().trim()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("Select User To Ban/Unban")
                .setRequiredRange(1, EntitySelectMenu.OPTIONS_MAX_AMOUNT)
                .build()
        ))

        if (CardData.bannedUser.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = ceil(CardData.bannedUser.size * 1.0 / SearchHolder.PAGE_CHUNK).toInt()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(
            Button.primary("confirm", "Confirm").withEmoji(EmojiStore.CHECK)
        ))

        return result
    }
}