package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.lang.IllegalStateException
import kotlin.math.min

class ModifyModeSelectHolder(author: Message, channelID: String, private val message: Message, private val isCard: Boolean, private val inventory: Inventory) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "mode" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val isAdd = when(val mode = event.values[0]) {
                    "add" -> {
                        true
                    }
                    "remove" -> {
                        false
                    }
                    else -> {
                        throw IllegalStateException("E/ModifyModeSelectHolder::onEvent - Invalid mode $mode")
                    }
                }

                if (isCard) {
                    val cards = if (isAdd)
                        CardData.cards.sortedWith(CardComparator())
                    else
                        inventory.cards.keys.sortedWith(CardComparator())

                    event.deferEdit()
                        .setContent(getCardText(cards, isAdd))
                        .setComponents(getCardComponents(cards))
                        .mentionRepliedUser(false)
                        .setAllowedMentions(ArrayList())
                        .queue()

                    expired = true
                    expire(authorMessage.author.id)

                    StaticStore.putHolder(authorMessage.author.id, CardModifyHolder(authorMessage, channelID, message, isAdd, inventory))
                } else {
                    val roles = if (isAdd)
                        CardData.Role.values().filter { r -> r !in inventory.vanityRoles && r != CardData.Role.NONE }
                    else
                        inventory.vanityRoles

                    event.deferEdit()
                        .setContent(getRoleText(roles, isAdd))
                        .setComponents(getRoleComponents(roles, isAdd))
                        .mentionRepliedUser(false)
                        .setAllowedMentions(ArrayList())
                        .queue()

                    expired = true
                    expire(authorMessage.author.id)

                    StaticStore.putHolder(authorMessage.author.id, RoleModifyHolder(authorMessage, channelID, message, isAdd, inventory))
                }
            }
            "back" -> {
                event.deferEdit()
                    .setContent("Select category that you want to modify")
                    .setComponents(getPreviousComponents())
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true
                expire(authorMessage.author.id)

                StaticStore.putHolder(authorMessage.author.id, ModifyCategoryHolder(authorMessage, channelID, message, inventory))
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Modify closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true

                expire(authorMessage.author.id)
            }
        }
    }

    private fun getCardComponents(cards: List<Card>) : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            tierCategoryElements.add(SelectOption.of(text, "tier${index}"))
        }

        val tierCategory = StringSelectMenu.create("tier")
            .addOptions(tierCategoryElements)
            .setPlaceholder("Filter Cards by Tiers")

        rows.add(ActionRow.of(tierCategory.build()))

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all"))

        CardData.bannerCategoryText.forEachIndexed { index, array ->
            array.forEachIndexed { i, a ->
                bannerCategoryElements.add(SelectOption.of(a, "category-$index-$i"))
            }
        }

        val bannerCategory = StringSelectMenu.create("category")
            .addOptions(bannerCategoryElements)
            .setPlaceholder("Filter Cards by Banners")

        rows.add(ActionRow.of(bannerCategory.build()))

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in 0 until min(dataSize, SearchHolder.PAGE_CHUNK)) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
            .addOptions(cardCategoryElements)
            .setPlaceholder(
                if (cards.isEmpty())
                    "No Cards To Select"
                else
                    "Select Card"
            )
            .setDisabled(cards.isEmpty())
            .build()

        rows.add(ActionRow.of(cardCategory))

        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.success("confirm", "Confirm").withDisabled(true))
        confirmButtons.add(Button.danger("clear", "Clear").withDisabled(true))
        confirmButtons.add(Button.secondary("back", "Back"))
        confirmButtons.add(Button.danger("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getCardText(cards: List<Card>, isAdd: Boolean) : String {
        val builder = StringBuilder(
            if (isAdd)
                "Please select cards that will be added"
            else
                "Please select cards that will be removed"
        )

        builder.append("\n\n### Selected Cards\n\n- None\n\n```md\n")

        if (cards.isNotEmpty()) {
            for(i in 0 until min(cards.size, SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1)
                    .append(". ")
                    .append(cards[i].cardInfo())

                val amount = if (isAdd)
                    1
                else
                    inventory.cards[cards[i]] ?: 1

                if (amount >= 2) {
                    builder.append(" x$amount\n")
                } else {
                    builder.append("\n")
                }
            }
        } else {
            builder.append("No Cards Found")
        }

        builder.append("```")

        return builder.toString()
    }

    private fun getRoleComponents(roles: List<CardData.Role>, isAdd: Boolean) : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val roleOptions = ArrayList<SelectOption>()

        if (roles.isNotEmpty()) {
            for (role in roles) {
                roleOptions.add(SelectOption.of(role.title, role.name).withEmoji(EmojiStore.ABILITY[role.key]))
            }
        } else {
            roleOptions.add(SelectOption.of("a", "a"))
        }

        val roleMenu = StringSelectMenu.create("role")
            .addOptions(roleOptions)
            .setPlaceholder(
                if (roles.isEmpty()) {
                    "No roles to select"
                } else {
                    "Select role to be ${if (isAdd) "added" else "removed"}"
                }
            )
            .setDisabled(roles.isEmpty())
            .build()

        rows.add(ActionRow.of(roleMenu))

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.success("confirm", "Confirm").asDisabled())
        confirmButtons.add(Button.danger("clear", "Clear").asDisabled())
        confirmButtons.add(Button.secondary("back", "Back"))
        confirmButtons.add(Button.danger("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getRoleText(roles: List<CardData.Role>, isAdd: Boolean) : String {
        val builder = StringBuilder(
            if (isAdd)
                "Please select roles that will be added"
            else
                "Please select roles that will be removed"
        )

        builder.append("\n\n### Selected Cards\n\n- None\n\n```md\n")

        if (roles.isNotEmpty()) {
            roles.forEachIndexed { i, role ->
                builder.append(i + 1)
                    .append(". ")
                    .append(role.title)
                    .append("\n")
            }
        } else {
            builder.append("No Roles To Select")
        }

        builder.append("```")

        return builder.toString()
    }

    private fun getPreviousComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Cards", "card"))
        modeOptions.add(SelectOption.of("Vanity Roles", "role"))

        val modes = StringSelectMenu.create("category")
            .addOptions(modeOptions)
            .setPlaceholder("Select category that you want to modify")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(Button.danger("close", "Close")))

        return rows
    }
}