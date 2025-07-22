package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.modal.SkinNameHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class SkinModifyHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val skin: Skin,
    private val new: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Skin manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "file" -> {
                connectTo(event, SkinFileHolder(authorMessage, userID, channelID, message, skin.card, skin))
            }
            "creator" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val users = event.mentions.users

                if (users.isEmpty()) {
                    skin.creator = -1L
                } else {
                    skin.creator = users.first().idLong
                }

                val content = if (skin.creator == -1L) {
                    "${skin.skinID} - Official"
                } else {
                    "${skin.skinID} - <@${skin.creator}>"
                }

                val cachedMessage = skin.getCachedMessage(authorMessage.jda)

                if (cachedMessage != null) {
                    cachedMessage.editMessage(content).setAllowedMentions(ArrayList()).queue()
                }

                applyResult(event)
            }
            "name" -> {
                val input = TextInput.create("name", "Name", TextInputStyle.SHORT)
                    .setPlaceholder("Type Name of Skin Here")
                    .setRequiredRange(1, 100)
                    .setRequired(true)
                    .build()

                val modal = Modal.create("skinName", "Skin Name")
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(SkinNameHolder(authorMessage, userID, channelID, message, skin))
            }
            "public" -> {
                skin.public = !skin.public

                applyResult(event)
            }
            "cost" -> {
                connectTo(event, SkinCostManageHolder(authorMessage, userID, channelID, message, skin))
            }
            "create" -> {
                CardData.skins.add(skin)

                skin.cache(event.jda, true)

                CardBot.saveCardData()

                TransactionLogger.logSkinCreate(authorMessage.author.idLong, skin)

                if (skin.creator != -1L) {
                    val inventory = Inventory.getInventory(skin.creator)

                    inventory.skins.add(skin)

                    event.deferReply()
                        .setContent("Successfully added skin! Also skin is given to creator <@${skin.creator}>!")
                        .setEphemeral(true)
                        .queue()
                } else {
                    event.deferReply()
                        .setContent("Successfully added skin!")
                        .setEphemeral(true)
                        .queue()
                }

                goBackTo(SkinSelectHolder::class.java)
            }
            "back" -> {
                if (new) {
                    message.editMessageAttachments().queue()

                    registerPopUp(event, "Are you sure you want to cancel creation of the skin? This cannot be undone")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        if (!Files.deleteIfExists(skin.file.toPath())) {
                            StaticStore.logger.uploadLog("W/SkinModifyHolder::onEvent - Failed to delete new skin file : ${skin.file.absolutePath}")
                        }

                        goBackTo(e, SkinSelectHolder::class.java)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    goBack(event)
                }
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this skin? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    if (skin.file.exists() && !Files.deleteIfExists(skin.file.toPath())) {
                        StaticStore.logger.uploadLog("W/SkinModifyHolder::onEvent - Failed to delete existing skin file : ${skin.file.absolutePath}")
                    }

                    CardData.skins.remove(skin)

                    val cachedMessage = skin.getCachedMessage(authorMessage.jda)

                    if (cachedMessage != null) {
                        cachedMessage.delete().queue()
                    } else {
                        skin.cache(authorMessage.jda, false)

                        val retry = skin.getCachedMessage(authorMessage.jda)

                        if (retry != null) {
                            retry.delete().queue()
                        }
                    }

                    CardData.inventories.values.forEach { inventory ->
                        inventory.skins.removeIf { s ->
                            return@removeIf s.skinID == skin.skinID
                        }
                    }

                    CardBot.saveCardData()

                    TransactionLogger.logSkinRemove(authorMessage.author.idLong, skin)

                    e.deferReply()
                        .setContent("Successfully deleted skin : ${skin.name}!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun onConnected(parent: Holder) {
        applyResult()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult() {
        var builder = message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isEmpty()) {
            builder = builder.setFiles(FileUpload.fromData(skin.file))
        }

        builder.queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        if (event !is GenericComponentInteractionCreateEvent)
            return

        var builder = event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isEmpty()) {
            builder = builder.setFiles(FileUpload.fromData(skin.file))
        }

        builder.queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("## Skin Manager\n")
            .append(skin.displayInfo(authorMessage.jda, true, false))

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        var creatorBuilder = EntitySelectMenu.create("creator", EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("Select User To Change Creator")
            .setRequiredRange(0, 1)

        if (skin.creator != -1L) {
            creatorBuilder = creatorBuilder.setDefaultValues(EntitySelectMenu.DefaultValue.user(skin.creator))
        }

        result.add(ActionRow.of(creatorBuilder.build()))

        result.add(ActionRow.of(
            Button.secondary("name", "Change Name").withEmoji(Emoji.fromUnicode("🏷️")),
            Button.secondary("file", "Change File").withEmoji(EmojiStore.PNG)
        ))

        val publicSwitch = if (skin.public) {
            EmojiStore.SWITCHON
        } else {
            EmojiStore.SWITCHOFF
        }

        result.add(ActionRow.of(
            Button.secondary("public", "Is Public").withEmoji(publicSwitch),
            Button.secondary("cost", "Adjust Cost").withEmoji(Emoji.fromUnicode("💰"))
        ))

        if (new) {
            result.add(ActionRow.of(
                Button.success("create", "Create").withDisabled(!skin.public && skin.creator == -1L).withEmoji(EmojiStore.CHECK),
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withDisabled(!skin.public && skin.creator == -1L).withEmoji(EmojiStore.BACK),
                Button.danger("delete", "Delete Skin").withEmoji(EmojiStore.CROSS)
            ))
        }

        return result
    }
}