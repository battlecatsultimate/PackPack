package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.pack.CardPackManageHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.ceil
import kotlin.math.min

class ManagePack : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL)
            return

        replyToMessageSafely(loader.channel, getContent(), loader.message, { a -> a.addComponents(getComponents())}) { msg ->
            StaticStore.putHolder(m.id, CardPackManageHolder(loader.message, m.id, loader.channel.id, msg))
        }
    }

    fun getContent() : String {
        val builder = StringBuilder("Click `Add Card Pack` button to add new pack\nSelect pack to remove or modify the pack\n## List of card packs\n")

        if (CardData.cardPacks.isEmpty()) {
            builder.append("- No packs")
        } else {
            for (i in 0 until min(SearchHolder.PAGE_CHUNK, CardData.cardPacks.size)) {
                val emoji = EmojiStore.getPackEmoji(CardData.cardPacks[i])

                val formatted = emoji?.formatted ?: ""

                builder.append(i + 1).append(". ").append(formatted).append(" ").append(CardData.cardPacks[i].packName).append("\n")
            }

            if (CardData.cardPacks.size > SearchHolder.PAGE_CHUNK) {
                val totalPage = ceil(CardData.cardPacks.size * 1.0 / SearchHolder.PAGE_CHUNK)

                builder.append("\nPage : 1/").append(totalPage)
            }
        }

        return builder.toString()
    }

    fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("add", "Add Card Pack")
                .withEmoji(Emoji.fromUnicode("➕"))
        ))

        if (CardData.cardPacks.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            for (i in 0 until min(SearchHolder.PAGE_CHUNK, CardData.cardPacks.size)) {
                val packName = if (CardData.cardPacks[i].packName.length >= 100) {
                    CardData.cardPacks[i].packName.substring(0, 50) + "..."
                } else {
                    CardData.cardPacks[i].packName
                }

                options.add(SelectOption.of(packName, i.toString()))
            }

            result.add(ActionRow.of(
                StringSelectMenu.create("pack")
                    .addOptions(options)
                    .setPlaceholder("Select Pack To Modify")
                    .build()
            ))

            if (CardData.cardPacks.size > SearchHolder.PAGE_CHUNK) {
                val buttons = ArrayList<Button>()

                val totalPage = ceil(CardData.cardPacks.size * 1.0 / SearchHolder.PAGE_CHUNK)

                if (totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
                }

                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

                if (totalPage > 10) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }

                result.add(ActionRow.of(buttons))
            }
        }

        result.add(ActionRow.of(
            Button.primary("confirm", "Confirm")
        ))

        return result
    }
}