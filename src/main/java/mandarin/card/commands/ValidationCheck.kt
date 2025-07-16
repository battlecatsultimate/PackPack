package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CCValidation
import mandarin.card.supporter.CardData
import mandarin.card.supporter.ECCValidation
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

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(loader.channel, "Format : cd.vc [User ID/Mention]", loader.message) { a -> a }

            return
        }

        val value = contents[1].replace(Regex("(<@|>)"), "")

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

        val builder = StringBuilder("## <@$memberID> CC/ECC Status\n### CC Status\n")

        if (inventory.ccValidation.validationWay == CCValidation.ValidationWay.NONE) {
            builder.append("- No CC\n")
        } else {
            val cf = EmojiStore.ABILITY["CF"]?.formatted

            val way = when(inventory.ccValidation.validationWay) {
                CCValidation.ValidationWay.SEASONAL_15 -> "15 Unique Seasonal Cards + $cf 150k"
                CCValidation.ValidationWay.COLLABORATION_12 -> "12 Unique Collaboration Cards + $cf 150k"
                CCValidation.ValidationWay.SEASONAL_15_COLLABORATION_12 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards"
                CCValidation.ValidationWay.T3_3 -> "3 Unique T3 Cards + $cf 200k"
                CCValidation.ValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
                CCValidation.ValidationWay.NONE -> "None"
            }

            builder.append("**Validation Way**\n- ").append(way).append("\n")

            if (inventory.ccValidation.cardList.isNotEmpty()) {
                builder.append("**Selected Cards**\n")

                inventory.ccValidation.cardList.forEachIndexed { index, card ->
                    builder.append(index + 1).append(". ").append(card.simpleCardInfo()).append("\n")
                }
            }
        }

        builder.append("### ECC Status\n")

        if (inventory.eccValidation.validationWay == ECCValidation.ValidationWay.NONE) {
            builder.append("- No ECC\n")
        } else {
            val way = when(inventory.eccValidation.validationWay) {
                ECCValidation.ValidationWay.SEASONAL_15_COLLAB_12_T4 -> "- 15 Unique Seasonal Cards + 12 Unique Collaboration Cards + 1 T4 Card"
                ECCValidation.ValidationWay.T4_2 -> "- 2 Unique T4 Cards"
                ECCValidation.ValidationWay.SAME_T4_3 -> "- 3 Same T4 Cards"
                ECCValidation.ValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
                ECCValidation.ValidationWay.NONE -> "None"
            }

            builder.append("**Validation Way**\n- ").append(way).append("\n")

            if (inventory.eccValidation.cardList.isNotEmpty()) {
                builder.append("**Selected Cards**\n")

                inventory.eccValidation.cardList.forEachIndexed { index, card ->
                    builder.append(index + 1).append(". ").append(card.simpleCardInfo()).append("\n")
                }
            }
        }

        replyToMessageSafely(loader.channel, builder.toString(), loader.message) { a -> a }
    }
}