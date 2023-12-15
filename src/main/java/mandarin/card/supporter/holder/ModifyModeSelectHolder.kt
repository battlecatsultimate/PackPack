package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.CatFoodModifyHolder
import mandarin.card.supporter.holder.modal.PlatinumShardModifyHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.lang.IllegalStateException
import kotlin.math.min

class ModifyModeSelectHolder(author: Message, channelID: String, private val message: Message, private val category: CardData.ModifyCategory, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, channelID, message.id) {
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

                when (category) {
                    CardData.ModifyCategory.CARD -> {
                        val cards = if (isAdd)
                            CardData.cards.sortedWith(CardComparator())
                        else
                            inventory.cards.keys.sortedWith(CardComparator())

                        event.deferEdit()
                            .setContent(getCardText(cards, isAdd))
                            .setComponents(getCardComponents(cards, isAdd))
                            .mentionRepliedUser(false)
                            .setAllowedMentions(ArrayList())
                            .queue()

                        expired = true
                        expire(authorMessage.author.id)

                        StaticStore.putHolder(authorMessage.author.id, CardModifyHolder(authorMessage, channelID, message, isAdd, inventory, targetMember))
                    }
                    CardData.ModifyCategory.ROLE -> {
                        val roles = if (isAdd)
                            CardData.Role.entries.filter { r -> r !in inventory.vanityRoles && r != CardData.Role.NONE }
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

                        StaticStore.putHolder(authorMessage.author.id, RoleModifyHolder(authorMessage, channelID, message, isAdd, inventory, targetMember))
                    }
                    CardData.ModifyCategory.CF -> {
                        val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                            .setPlaceholder("Define amount of cat foods that will be ${if(isAdd) "added" else "removed"} from this user")
                            .build()

                        val modal = Modal.create("cf", "Modify Cat Food")
                            .addActionRow(input)
                            .build()

                        event.replyModal(modal).queue()

                        StaticStore.putHolder(authorMessage.author.id, CatFoodModifyHolder(authorMessage, channelID, message.id, inventory, isAdd, targetMember.id) {
                            message.editMessage("Do you want to add or remove cat foods?\n\nCurrently this user has ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods}")
                                .setComponents(getModeComponents())
                                .setAllowedMentions(ArrayList())
                                .mentionRepliedUser(false)
                                .queue()
                        })
                    }
                    CardData.ModifyCategory.SHARD -> {
                        val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                            .setPlaceholder("Define amount of platinum shards that will be ${if(isAdd) "added" else "removed"} from this user")
                            .build()

                        val modal = Modal.create("shard", "Modify Platinum Shard")
                            .addActionRow(input)
                            .build()

                        event.replyModal(modal).queue()

                        StaticStore.putHolder(authorMessage.author.id, PlatinumShardModifyHolder(authorMessage, channelID, message.id, inventory, isAdd, targetMember.id) {
                            message.editMessage("Do you want to add or remove platinum shards?\n\nCurrently this user has ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}")
                                .setComponents(getModeComponents())
                                .setAllowedMentions(ArrayList())
                                .mentionRepliedUser(false)
                                .queue()
                        })
                    }
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

                StaticStore.putHolder(authorMessage.author.id, ModifyCategoryHolder(authorMessage, channelID, message, inventory, targetMember))
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

    private fun getCardComponents(cards: List<Card>, isAdd: Boolean) : List<LayoutComponent> {
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

        if (!isAdd) {
            confirmButtons.add(Button.danger("remove", "Mass Remove").withDisabled(inventory.cards.isEmpty()))
        }

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

        if (!isAdd) {
            confirmButtons.add(Button.danger("remove", "Mass Remove").withDisabled(inventory.vanityRoles.isEmpty()))
        }

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
        modeOptions.add(SelectOption.of("Cat Foods", "cf"))

        val modes = StringSelectMenu.create("category")
            .addOptions(modeOptions)
            .setPlaceholder("Select category that you want to modify")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(Button.danger("close", "Close")))

        return rows
    }

    private fun getModeComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Add", "add").withEmoji(Emoji.fromUnicode("➕")))
        modeOptions.add(SelectOption.of("Remove", "remove").withEmoji(Emoji.fromUnicode("➖")))

        val modes = StringSelectMenu.create("mode")
            .addOptions(modeOptions)
            .setPlaceholder("Please select mode")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(
            Button.secondary("back", "Back"),
            Button.danger("close", "Close")
        ))

        return rows
    }
}