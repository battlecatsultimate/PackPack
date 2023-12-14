package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
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
import kotlin.math.min

class SalvageTierSelectHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        if (event.componentId == "tier") {
            if (event !is StringSelectInteractionEvent)
                return

            if (event.values.isEmpty())
                return

            val mode = when(event.values[0]) {
                "t1" -> CardData.SalvageMode.T1
                "t2" -> CardData.SalvageMode.T2
                "seasonalT2" -> CardData.SalvageMode.SEASONAL
                "collaborationT2" -> CardData.SalvageMode.COLLAB
                else -> CardData.SalvageMode.T3
            }

            val inventory = Inventory.getInventory(authorMessage.author.id)

            val tier = when(mode) {
                CardData.SalvageMode.T1 -> CardData.Tier.COMMON
                CardData.SalvageMode.T2,
                CardData.SalvageMode.SEASONAL,
                CardData.SalvageMode.COLLAB -> CardData.Tier.UNCOMMON
                else -> CardData.Tier.ULTRA
            }

            val cards = inventory.cards.keys.filter { c ->
                if (tier != CardData.Tier.UNCOMMON)
                    c.tier == tier
                else
                    when (mode) {
                        CardData.SalvageMode.T2 -> c.tier == CardData.Tier.UNCOMMON && c.unitID !in BannerFilter.Banner.Seasonal.getBannerData() && c.unitID !in BannerFilter.Banner.Collaboration.getBannerData()
                        CardData.SalvageMode.SEASONAL -> c.tier == CardData.Tier.UNCOMMON && c.unitID in BannerFilter.Banner.Seasonal.getBannerData()
                        else -> c.tier == CardData.Tier.UNCOMMON && c.unitID in BannerFilter.Banner.Collaboration.getBannerData()
                    }
            }.sortedWith(CardComparator())

            if (cards.isEmpty()) {
                event.deferEdit()
                    .setContent("You don't have any cards meets condition of what you've selected!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                return
            }

            event.deferEdit()
                .setContent(getText(cards, inventory, mode))
                .setComponents(assignComponents(cards, mode, tier))
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            expired = true

            expire(authorMessage.author.id)

            StaticStore.putHolder(authorMessage.author.id, CardSalvageHolder(authorMessage, channelID, message, mode))
        }
    }

    private fun assignComponents(cards: List<Card>, salvageMode: CardData.SalvageMode, tier: CardData.Tier) : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        if (salvageMode != CardData.SalvageMode.SEASONAL && salvageMode != CardData.SalvageMode.COLLAB) {
            val bannerCategoryElements = ArrayList<SelectOption>()

            bannerCategoryElements.add(SelectOption.of("All", "all"))

            if (tier != CardData.Tier.UNCOMMON) {
                CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
                    bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
                }
            } else {
                CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
                    if (i < 2) {
                        bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
                    }
                }
            }

            val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

            rows.add(ActionRow.of(bannerCategory.build()))
        }

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

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("salvage", "Salvage").asDisabled().withEmoji(Emoji.fromUnicode("\uD83E\uDE84")))
        confirmButtons.add(Button.secondary("all", "Add All"))

        confirmButtons.add(Button.danger("reset", "Reset").asDisabled())
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText(cards: List<Card>, inventory: Inventory, salvageMode: CardData.SalvageMode) : String {
        val builder = StringBuilder(
            when (salvageMode) {
                CardData.SalvageMode.T1 -> "Select Tier 1 [Common] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.T2 -> "Select Regular Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.SEASONAL -> "Select Seasonal Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.COLLAB -> "Select Collaboration Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n"
                else -> "Select Tier 3 [Ultra Rare (Exclusives)] cards\n\n### Selected card\n\n"
            }
        ).append("```md\n")

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
            builder.append("No Cards Found")
        }

        builder.append("```")

        builder.append("\n${EmojiStore.ABILITY["SHARD"]?.formatted} 0 in total")

        return builder.toString()
    }
}