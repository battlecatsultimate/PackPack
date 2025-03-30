package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.*
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.SuggestInventoryHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class Suggest(private val session: TradingSession) : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val author = loader.message

        val targetIndex = if (session.member[0] == m.idLong)
            1
        else
            0

        val g = loader.guild

        g.retrieveMember(UserSnowflake.fromId(session.member[targetIndex])).queue { targetMember ->
            val inventory = Inventory.getInventory(m.idLong)

            val index = session.member.indexOf(m.idLong)

            if (index == -1)
                return@queue

            val cards = ArrayList<Card>(inventory.cards.keys)

            for (card in inventory.cards.keys) {
                val amount = inventory.cards[card] ?: 0

                if (amount - (session.suggestion[index].cards[card] ?: 0) == 0) {
                    cards.remove(card)
                }
            }

            cards.sortWith(CardComparator())

            replyToMessageSafely(ch, session.suggestion[index].suggestionInfo(m), loader.message, { a -> a }, { suggestionMessage ->
                replyToMessageSafely(ch, getText(author, cards, inventory, index), author, Suggest@{ a ->
                    val rows = ArrayList<ActionRow>()

                    val tierCategoryElements = ArrayList<SelectOption>()

                    tierCategoryElements.add(SelectOption.of("All", "all"))

                    CardData.tierCategoryText.forEachIndexed { index, text ->
                        val emoji = EmojiStore.getCardEmoji(
                            when (index) {
                                0 -> null
                                1 -> CardPack.CardType.T1
                                2 -> CardPack.CardType.T2
                                3 -> CardPack.CardType.T3
                                4 -> CardPack.CardType.T4
                                else -> throw IllegalStateException("E/CardModifyHolder::assignComponents - Invalid tier index $index")
                            }
                        )

                        if (index == CardData.Tier.SPECIAL.ordinal) {
                            if (CardData.canTradeT0(m) && CardData.canTradeT0(targetMember)) {
                                tierCategoryElements.add(SelectOption.of(text, "tier${CardData.Tier.SPECIAL.ordinal}").withEmoji(emoji))
                            }
                        } else {
                            tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
                        }
                    }

                    val tierCategory = StringSelectMenu.create("tier")
                        .addOptions(tierCategoryElements)
                        .setPlaceholder("Filter Cards by Tiers")

                    rows.add(ActionRow.of(tierCategory.build()))

                    val bannerCategoryElements = ArrayList<SelectOption>()

                    bannerCategoryElements.add(SelectOption.of("All", "all").withDefault(true))
                    bannerCategoryElements.add(SelectOption.of("Seasonal Cards", "seasonal"))
                    bannerCategoryElements.add(SelectOption.of("Collaboration Cards", "collab"))

                    bannerCategoryElements.addAll(CardData.banners.filter { b -> b.category }.map { SelectOption.of(it.name, CardData.banners.indexOf(it).toString()) })

                    if (bannerCategoryElements.size > 1) {
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
                        for (i in 0 until min(dataSize, SearchHolder.PAGE_CHUNK)) {
                            cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
                        }
                    }

                    val cardCategory = StringSelectMenu.create("card")
                        .addOptions(cardCategoryElements)
                        .setPlaceholder(
                            if (session.suggestion[index].cards.size == CardData.MAX_CARD_TYPE)
                                "You can't suggest more than ${CardData.MAX_CARD_TYPE} cards!"
                            else if (cards.isEmpty())
                                "No Cards To Select"
                            else
                                "Select Card To Suggest"
                        )
                        .setDisabled(session.suggestion[index].cards.size == CardData.MAX_CARD_TYPE || cards.isEmpty())
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

                        buttons.add(Button.secondary("cf", "Suggest Cat Foods").withEmoji(EmojiStore.ABILITY["CF"]))

                        buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

                        if (totPage > 10) {
                            buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                        }

                        rows.add(ActionRow.of(buttons))
                    } else {
                        rows.add(ActionRow.of(Button.secondary("cf", "Suggest Cat Foods").withEmoji(EmojiStore.ABILITY["CF"])))
                    }

                    val possibleCards = cards.filter { c -> (inventory.cards[c] ?: 0) - (session.suggestion[index].cards[c] ?: 0) > 1 }

                    val confirmButtons = ArrayList<Button>()

                    confirmButtons.add(Button.primary("confirm", "Suggest"))
                    confirmButtons.add(Button.secondary("dupe", "Add Duplicated").withDisabled(session.suggestion[index].cards.size >= CardData.MAX_CARD_TYPE || possibleCards.isEmpty()))
                    confirmButtons.add(Button.secondary("all", "Add All").withDisabled(session.suggestion[index].cards.size >= CardData.MAX_CARD_TYPE || possibleCards.isEmpty()))
                    confirmButtons.add(Button.danger("reset", "Clear Suggestions"))
                    confirmButtons.add(Button.danger("cancel", "Cancel"))

                    rows.add(ActionRow.of(confirmButtons))

                    return@Suggest a.setComponents(rows)
                }, { msg ->
                    StaticStore.putHolder(m.id, SuggestInventoryHolder(author, m.id, ch.id, msg, targetMember, suggestionMessage, session, inventory))
                })
            })
        }
    }

    private fun getText(message: Message, cards: List<Card>, inventory: Inventory, index: Int) : String {
        val builder = StringBuilder("Inventory of ${message.author.asMention}, please select cards or suggest cf that will be traded\n\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in 0 until min(SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = (inventory.cards[cards[i]] ?: 0) - (session.suggestion[index].cards[cards[i]] ?: 0)

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