package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class ValidationCheck : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        val contents = loader.content.split(" ")

        val value = if (contents.size >= 2 && (m.id == StaticStore.MANDARIN_SMELL || CardData.isManager(m))) {
            contents[1].replace(Regex("(<@|>)"), "")
        } else {
            m.id
        }

        if (!StaticStore.isNumeric(value)) {
            replyToMessageSafely(loader.channel, "User ID must be numeric!", loader.message) { a -> a }
        }

        val memberID = StaticStore.safeParseLong(value)

        val memberReference = AtomicReference<Member>(null)
        val countdown = CountDownLatch(1)

        loader.guild.retrieveMember(UserSnowflake.fromId(memberID)).queue({ member ->
            memberReference.set(member)

            countdown.countDown()
        }) { _ ->
            countdown.countDown()
        }

        countdown.await()

        if (memberReference.get() == null) {
            replyToMessageSafely(loader.channel, "Failed to find member with ID of $memberID!", loader.message) { a -> a }

            return
        }

        val inventory = Inventory.getInventory(memberID)

        val builder = StringBuilder("## <@$memberID> CC/ECC Status\n### CC Status")

        if (inventory.ccValidationWay == Inventory.CCValidationWay.NONE) {
            builder.append("- No CC\n")
        } else {
            val cf = EmojiStore.ABILITY["CF"]?.formatted

            val way = when(inventory.ccValidationWay) {
                Inventory.CCValidationWay.SEASONAL_15 -> "15 Unique Seasonal Cards + $cf 150k"
                Inventory.CCValidationWay.COLLABORATION_12 -> "12 Unique Collaboration Cards + $cf 150k"
                Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards"
                Inventory.CCValidationWay.T3_3 -> "3 Unique T3 Cards + $cf 200k"
                Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
                Inventory.CCValidationWay.NONE -> "None"
            }

            builder.append("**Validation Way**\n- ").append(way).append("\n")
            
            val ccCardList = inventory.validationCards.filterValues { p -> p.first == Inventory.ShareStatus.CC }.keys

            if (ccCardList.isNotEmpty()) {
                builder.append("**Selected Cards**\n")

                ccCardList.forEachIndexed { index, card ->
                    builder.append(index + 1).append(". ").append(card.simpleCardInfo()).append("\n")
                }
            }
        }

        builder.append("### ECC Status\n")

        if (inventory.eccValidationWay == Inventory.ECCValidationWay.NONE) {
            builder.append("- No ECC\n")
        } else {
            val way = when(inventory.eccValidationWay) {
                Inventory.ECCValidationWay.SEASONAL_15_COLLAB_12_T4 -> "- 15 Unique Seasonal Cards + 12 Unique Collaboration Cards + 1 T4 Card"
                Inventory.ECCValidationWay.T4_2 -> "- 2 Unique T4 Cards"
                Inventory.ECCValidationWay.SAME_T4_3 -> "- 3 Same T4 Cards"
                Inventory.ECCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
                Inventory.ECCValidationWay.NONE -> "None"
            }

            builder.append("**Validation Way**\n- ").append(way).append("\n")

            val eccCardList = inventory.validationCards.filterValues { p -> p.first == Inventory.ShareStatus.ECC }.keys

            if (eccCardList.isNotEmpty()) {
                builder.append("**Selected Cards**\n")

                eccCardList.forEachIndexed { index, card ->
                    builder.append(index + 1).append(". ").append(card.simpleCardInfo()).append("\n")
                }
            }
        }

        val sharedCards = inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.BOTH }.entries

        if (sharedCards.isNotEmpty()) {
            builder.append("### Shared Cards for CC/ECC\n")

            sharedCards.forEachIndexed { index, (card, pair) ->
                builder.append(index + 1).append(". ").append(card.simpleCardInfo())

                if (pair.second >= 2) {
                    builder.append(" x").append(pair.second)
                }

                builder.append("\n")
            }
        }

        replyToMessageSafely(loader.channel, builder.toString(), loader.message) { a -> a }
    }
}