package mandarin.card.commands

import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.CardInventoryHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class Cards : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return
        val author = getMessage(event) ?: return

        val inventory = Inventory.getInventory(m.id)

        val msg = getRepliedMessageSafely(ch, getText(author, inventory), author) { a ->
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

            val dataSize = inventory.cards.size

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

            confirmButtons.add(Button.primary("confirm", "Close"))

            rows.add(ActionRow.of(confirmButtons))

            return@getRepliedMessageSafely a.setComponents(rows)
        }

        StaticStore.putHolder(m.id, CardInventoryHolder(author, ch.id, msg, inventory))
    }

    private fun getText(message: Message, inventory: Inventory) : String {
        val cards = inventory.cards.keys.sortedWith(CardComparator())

        val authorMention = message.author.asMention

        val builder = StringBuilder("Inventory of ${authorMention}\n\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in 0 until min(SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = inventory.cards[cards[i]] ?: 1

                if (amount >= 2) {
                    builder.append(" x$amount\n")
                } else {
                    builder.append("\n")
                }
            }
        } else {
            builder.append("No Cards Found\n")
        }

        builder.append("```")

        return builder.toString()
    }
}