package mandarin.card.supporter.holder.skin

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.modal.SkinNameHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import java.nio.file.Files

class SkinModifyHolder(
    author: Message,
    channelID: String,
    private var message: Message,
    private val skin: Skin,
    private val new: Boolean
) : ComponentHolder(author, channelID, message) {
    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "creator" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val users = event.mentions.users

                if (users.isEmpty()) {
                    skin.creator = -1L
                } else {
                    skin.creator = users.first().idLong
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
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(SkinNameHolder(authorMessage, channelID, message, skin))
            }
            "public" -> {
                skin.public = !skin.public

                applyResult(event)
            }
            "cost" -> {
                connectTo(event, SkinCostManageHolder(authorMessage, channelID, message, skin))
            }
            "create" -> {
                CardData.skins.add(skin)

                CardBot.saveCardData()

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

                    registerPopUp(event, "Are you sure you want to cancel creation of the skin? This cannot be undone", LangID.EN)

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        if (!Files.deleteIfExists(skin.file.toPath())) {
                            StaticStore.logger.uploadLog("W/SkinModifyHolder::onEvent - Failed to delete new skin file : ${skin.file.absolutePath}")
                        }

                        goBackTo(e, SkinSelectHolder::class.java)
                    }, LangID.EN))
                } else {
                    goBack(event)
                }
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this skin? This cannot be undone", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    if (skin.file.exists() && !Files.deleteIfExists(skin.file.toPath())) {
                        StaticStore.logger.uploadLog("W/SkinModifyHolder::onEvent - Failed to delete existing skin file : ${skin.file.absolutePath}")
                    }

                    CardData.skins.remove(skin)

                    CardData.inventories.values.forEach { inventory ->
                        inventory.skins.removeIf { s ->
                            return@removeIf s.skinID == skin.skinID
                        }
                    }

                    CardBot.saveCardData()

                    e.deferReply()
                        .setContent("Successfully deleted skin : ${skin.name}!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, LangID.EN))
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onConnected() {
        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder?) {
        applyResult(event)
    }

    override fun onBack(child: Holder?) {
        applyResult()
    }

    private fun applyResult() {
        message = updateMessageStatus(message)

        var builder =  message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isEmpty()) {
            builder = builder.setFiles(FileUpload.fromData(skin.file))
        }

        builder.queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        var builder = event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isEmpty()) {
            builder = builder.setFiles(FileUpload.fromData(skin.file))
        }

        builder.queue()

        message = updateMessageStatus(message)
    }

    private fun getContents() : String {
        val builder = StringBuilder("## Skin Manager\n")
            .append(skin.displayInfo(true))

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        var creatorBuilder = EntitySelectMenu.create("creator", EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("Select User To Change Creator")
            .setRequiredRange(0, 1)

        if (skin.creator != -1L) {
            creatorBuilder = creatorBuilder.setDefaultValues(EntitySelectMenu.DefaultValue.user(skin.creator))
        }

        result.add(ActionRow.of(creatorBuilder.build()))

        result.add(ActionRow.of(Button.secondary("name", "Change Name").withEmoji(Emoji.fromUnicode("🏷️"))))

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