package mandarin.card.supporter.holder.moderation.modify

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class SkinModifyHolder(author: Message, channelID: String, message: Message, private val isAdd: Boolean, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val skins = ArrayList<Skin>(
        if (isAdd) {
            CardData.skins.filter { r -> r !in inventory.skins }
        } else {
            inventory.skins
        }
    )

    private val selectedSkins = ArrayList<Skin>()

    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "prev" -> {
                page--

                filterSkins()

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                filterSkins()

                applyResult(event)
            }
            "next" -> {
                page++

                filterSkins()

                applyResult(event)
            }
            "next10" -> {
                page += 10

                filterSkins()

                applyResult(event)
            }
            "skin" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val index = event.values.first().toInt()
                val skin = skins[index]

                selectedSkins.add(skin)

                filterSkins()

                applyResult(event)
            }
            "confirm" -> {
                if (isAdd) {
                    inventory.skins.addAll(selectedSkins)
                } else {
                    inventory.skins.removeAll(selectedSkins.toSet())
                    selectedSkins.toSet().forEach { skin ->
                        val s = inventory.equippedSkins[skin.card]

                        if (s == skin)
                            inventory.equippedSkins.remove(skin.card)
                    }
                }

                val m = event.member ?: return

                TransactionLogger.logSkinsModify(selectedSkins, m, targetMember, isAdd, !isAdd && inventory.vanityRoles.isNotEmpty() && selectedSkins.size == inventory.vanityRoles.size)

                selectedSkins.clear()

                event.deferReply()
                    .setContent("Successfully ${if (isAdd) "added" else "removed"} selected skins!")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "remove" -> {
                selectedSkins.clear()

                selectedSkins.addAll(inventory.skins)

                event.deferReply()
                    .setContent("Selected all skins that this user has. This will remove all skins from user. Please confirm this action")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "clear" -> {
                selectedSkins.clear()

                event.deferReply()
                    .setContent("Successfully cleared selected skins!")
                    .setEphemeral(true)
                    .queue()

                filterSkins()

                applyResult()
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Modify closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true

                expire()
            }
        }
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        filterSkins()

        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun filterSkins() {
        skins.clear()

        skins.addAll(
            if (isAdd) {
                CardData.skins.filter { s -> s !in inventory.skins }.filter { s -> s !in selectedSkins }
            } else {
                inventory.skins.filter { s -> s !in selectedSkins }
            }
        )

        if (page * SearchHolder.PAGE_CHUNK > skins.size) {
            page = getTotalPage(skins.size) - 1
        }
    }

    private fun assignComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val roleOptions = ArrayList<SelectOption>()

        if (skins.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min(skins.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                roleOptions.add(SelectOption.of(skins[i].name, i.toString()).withDescription(skins[i].skinID.toString()))
            }
        } else {
            roleOptions.add(SelectOption.of("a", "a"))
        }

        val roleMenu = StringSelectMenu.create("skin")
            .addOptions(roleOptions)
            .setPlaceholder(
                if (skins.isEmpty()) {
                    "No skins to select"
                } else {
                    "Select skin to be ${if (isAdd) "added" else "removed"}"
                }
            )
            .setDisabled(skins.isEmpty())
            .build()

        result.add(ActionRow.of(roleMenu))

        if (skins.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = getTotalPage(skins.size)

            if (totalPage > 10) {
                buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.secondary("prev", "Previous Pages").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.success("confirm", "Confirm").withDisabled(selectedSkins.isEmpty()))
        confirmButtons.add(Button.danger("clear", "Clear").withDisabled(selectedSkins.isEmpty()))

        if (!isAdd) {
            confirmButtons.add(Button.danger("remove", "Mass Remove").withDisabled(inventory.vanityRoles.isEmpty() || inventory.vanityRoles.size == selectedSkins.size))
        }

        confirmButtons.add(Button.secondary("back", "Back"))
        confirmButtons.add(Button.danger("close", "Close"))

        result.add(ActionRow.of(confirmButtons))

        return result
    }

    private fun getText() : String {
        val builder = StringBuilder(
            if (isAdd)
                "Please select skins that will be added"
            else
                "Please select skins that will be removed"
        )

        builder.append("\n\n### Selected Skins\n\n")

        if (selectedSkins.isEmpty()) {
            builder.append("- None\n")
        } else {
            for (role in selectedSkins.toSet()) {
                builder.append("- ")
                    .append(role.name)
                    .append("\n")
            }
        }

        builder.append("\n```md\n")

        if (skins.isNotEmpty()) {
            skins.forEachIndexed { i, role ->
                builder.append(i + 1)
                    .append(". ")
                    .append(role.name)
                    .append("\n")
            }
        } else {
            builder.append("No Skins To Select")
        }

        builder.append("```")

        return builder.toString()
    }
}