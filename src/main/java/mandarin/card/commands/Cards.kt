package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.CardInventoryHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.requests.RestAction
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

class Cards : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val g = if (loader.hasGuild()) {
            loader.guild
        } else {
            loader.client.getGuildById(CardData.guild) ?: return
        }

        val memberAction = findMember(loader.content.split(" "), g)

        if (memberAction == null) {
            performCommand(loader, null)
        } else {
            memberAction.queue { member ->
                performCommand(loader, member)
            }
        }
    }

    private fun performCommand(loader: CommandLoader, member: Member?) {
        val ch = loader.channel
        val author = loader.message

        val m = if (loader.hasGuild()) {
            loader.member
        } else {
            val u = loader.user
            val g = loader.client.getGuildById(CardData.guild) ?: return

            val retriever = AtomicReference<Member>(null)
            val countdown = CountDownLatch(1)

            g.retrieveMember(u).queue({ m ->
                retriever.set(m)

                countdown.countDown()
            }) { e ->
                StaticStore.logger.uploadErrorLog(e, "E/Craft::doSomething - Failed to retrieve member data from user ID ${u.idLong}")

                countdown.countDown()
            }

            countdown.await()

            retriever.get() ?: return
        }

        val finalMember = member ?: m

        if (m.id != finalMember.id && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            replyToMessageSafely(ch, "You don't have permission to watch other user's inventory", loader.message) { a -> a }

            return
        }

        val inventory = Inventory.getInventory(finalMember.idLong)

        replyToMessageSafely(ch, getText(finalMember, inventory), author, { a ->
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

                tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
            }

            val tierCategory = StringSelectMenu.create("tier")
                .addOptions(tierCategoryElements)
                .setPlaceholder("Filter Cards by Tiers")

            rows.add(ActionRow.of(tierCategory.build()))

            val bannerCategoryElements = ArrayList<SelectOption>()

            bannerCategoryElements.add(SelectOption.of("All", "all").withDefault(true))
            bannerCategoryElements.addAll(CardData.banners.filter { b -> b.category }.map { SelectOption.of(it.name, CardData.banners.indexOf(it).toString()) })

            if (bannerCategoryElements.size > 1) {
                val bannerCategory = StringSelectMenu.create("category")
                    .addOptions(bannerCategoryElements)
                    .setPlaceholder("Filter Cards by Banners")

                rows.add(ActionRow.of(bannerCategory.build()))
            }

            val cards = inventory.cards.keys.union(inventory.favorites.keys).sortedWith(CardComparator())
            val dataSize = cards.size

            val cardCategoryElements = ArrayList<SelectOption>()

            if (cards.isEmpty()) {
                cardCategoryElements.add(SelectOption.of("a", "-1"))
            } else {
                for(i in 0 until min(dataSize, ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                    cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
                }
            }

            val cardCategory = StringSelectMenu.create("card")
                .addOptions(cardCategoryElements)
                .setPlaceholder(
                    if (cards.isEmpty())
                        "No Cards To Select"
                    else
                        "Select Card To See"
                )
                .setDisabled(cards.isEmpty())
                .build()

            rows.add(ActionRow.of(cardCategory))

            val totalPage = SearchHolder.getTotalPage(dataSize)

            if (dataSize > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                val buttons = ArrayList<Button>()

                if (totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
                }

                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

                if (totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }

                rows.add(ActionRow.of(buttons))
            }

            val confirmButtons = ArrayList<Button>()

            confirmButtons.add(Button.primary("confirm", "Confirm").withEmoji(EmojiStore.CROSS))
            confirmButtons.add(Button.secondary("filter", "Filter Mode : None"))

            rows.add(ActionRow.of(confirmButtons))

            return@replyToMessageSafely a.setComponents(rows)
        }, { msg ->
            StaticStore.putHolder(m.id, CardInventoryHolder(author, m.id, ch.id, msg, inventory, finalMember))
        })
    }

    private fun getText(member: Member, inventory: Inventory) : String {
        val cards = inventory.cards.keys.union(inventory.favorites.keys).sortedWith(CardComparator())

        val cardAmount = cards.sumOf { c -> (inventory.cards[c] ?: 0) + (inventory.favorites[c] ?: 0) }

        val start = if (cardAmount >= 2) {
            "Inventory of ${member.asMention}\n\nNumber of Filtered Cards : $cardAmount cards\n\n```md\n"
        } else if (cardAmount == 1) {
            "Inventory of ${member.asMention}\n\nNumber of Filtered Cards : $cardAmount card\n\n```md\n"
        } else {
            "Inventory of ${member.asMention}\n\n```md\n"
        }

        val builder = StringBuilder(start)

        if (cards.isNotEmpty()) {
            for (i in 0 until min(ConfigHolder.SearchLayout.COMPACTED.chunkSize, cards.size)) {
                builder.append("${i + 1}. ")

                if (inventory.favorites.containsKey(cards[i])) {
                    builder.append("⭐")
                }

                builder.append(cards[i].cardInfo())

                val amount = (inventory.cards[cards[i]] ?: 0) + (inventory.favorites[cards[i]] ?: 0)

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

    private fun findMember(contents: List<String>, g: Guild) : RestAction<Member>? {
        for (content in contents) {
            if (StaticStore.isNumeric(content)) {
                try {
                    return g.retrieveMember(UserSnowflake.fromId(content))
                } catch (_: Exception) {

                }
            } else if (content.matches(Regex("<@\\d+>"))) {
                try {
                    return g.retrieveMember(UserSnowflake.fromId(content.replace("<@", "").replace(">", "")))
                } catch (_: Exception) {

                }
            }
        }

        return null
    }
}