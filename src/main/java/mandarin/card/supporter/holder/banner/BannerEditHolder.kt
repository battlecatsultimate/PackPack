package mandarin.card.supporter.holder.banner

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.holder.modal.BannerNameHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.SelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.TimeUnit

class BannerEditHolder(author: Message, userID: String, channelID: String, message: Message, private val banner: Banner, private val createMode: Boolean) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "name" -> {
                val input = TextInput.create("name", TextInputStyle.SHORT).setPlaceholder("Type banner name here").build()

                val modal = Modal.create("bannerName", "Banner Name")
                    .addComponents(Label.of("Name", input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(BannerNameHolder(authorMessage, userID, channelID, message, banner))
            }
            "category" -> {
                banner.category = !banner.category

                applyResult(event)
            }
            "legend" -> {
                banner.legendCollector = !banner.legendCollector

                applyResult(event)
            }
            "activate" -> {
                if (banner in CardData.activatedBanners)
                    CardData.activatedBanners.remove(banner)
                else
                    CardData.activatedBanners.add(banner)

                val member = event.member

                if (member != null)
                    TransactionLogger.logBannerActivate(banner, member, banner in CardData.activatedBanners)

                applyResult(event)
            }
            "create" -> {
                CardData.banners.add(banner)

                CardBot.saveCardData()

                event.deferReply().setContent("Successfully added the banner!")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this banner? All the cards which had this banner will be affected too, and this cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    CardData.banners.remove(banner)

                    CardData.cards.filter { c -> banner in c.banner }.forEach { c -> c.banner.isEmpty() }

                    CardBot.saveCardData()

                    e.deferReply().setContent("Successfully deleted the banner ${banner.name}!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
            }
            "back" -> {
                goBack(event)
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Banner manager expired")
            .setComponents()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "## " + banner.name + "\nIncluded in the filter : " + if (banner.category) "Yes" else "No"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            Button.secondary("name", "Change Name").withEmoji(Emoji.fromUnicode("🏷️"))
        ))

        val category = if (banner.category)
            EmojiStore.SWITCHON
        else
            EmojiStore.SWITCHOFF

        val legend = if (banner.legendCollector)
            EmojiStore.SWITCHON
        else
            EmojiStore.SWITCHOFF

        val activate = if (banner in CardData.activatedBanners)
            EmojiStore.SWITCHON
        else
            EmojiStore.SWITCHOFF

        result.add(ActionRow.of(
            Button.secondary("category", "Include into Category").withEmoji(category).withDisabled(!banner.category && CardData.banners.count { b -> b.category } >= SelectMenu.OPTIONS_MAX_AMOUNT - 1),
            Button.secondary("legend", "Required by Legend Collector").withEmoji(legend),
            Button.secondary("activate", "Activate the banner").withEmoji(activate)
        ))

        if (createMode) {
            result.add(ActionRow.of(
                Button.success("create", "Create Banner").withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Back").withEmoji(EmojiStore.BACK),
                Button.danger("delete", "Delete").withEmoji(Emoji.fromUnicode("🗑️"))
            ))
        }

        return result
    }
}