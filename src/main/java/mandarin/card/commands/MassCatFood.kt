package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.concurrent.Task
import kotlin.math.abs

class MassCatFood : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL)
            return

        val contents = loader.content.split(Regex(" "), 3)

        if (contents.size < 3) {
            replyToMessageSafely(loader.channel, "Not enough data! Proper format is : \n" +
                    "\n" +
                    "`${CardBot.globalPrefix}mcf [Amount] [User1], [User2], ...`\n" +
                    "\n" +
                    "If you want to take away cat food from users, use negative values", loader.message) { a -> a }

            return
        }

        if (!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(loader.channel, "Amount of cat food must be numeric!", loader.message) { a -> a }

            return
        }

        val amount = StaticStore.safeParseLong(contents[1])

        if (amount == 0L) {
            replyToMessageSafely(loader.channel, "Let's not give 0 cat foods to users", loader.message) { a -> a }

            return
        }

        getMembers(contents[2], loader.guild).onSuccess { members ->
            if (members.any { member ->
                    val inventory = Inventory.getInventory(member.idLong)

                    if (amount < 0) {
                        inventory.platinumShard < abs(amount)
                    } else {
                        false
                    }
                }) {
                replyToMessageSafely(loader.channel, "You are taking away more cat foods than what one of these members has!\n" +
                        "\n" +
                        "The reason of blocking such situation is because bot needs to log how many cat foods user got or lost properly", loader.message) { a -> a }

                return@onSuccess
            }

            val sentence = if (amount < 0) {
                "take away ${EmojiStore.ABILITY["CF"]?.formatted} ${-amount} from"
            } else {
                "give ${EmojiStore.ABILITY["CF"]?.formatted} $amount to"
            }

            replyToMessageSafely(loader.channel, "Are you sure you want to $sentence users?", loader.message, { a ->
                val components = ArrayList<ActionComponent>()

                components.add(Button.success("confirm", "Confirm"))
                components.add(Button.danger("cancel", "Cancel"))

                a.setActionRow(components)
            }) { msg ->
                StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, m.id, loader.channel.id, msg, lang) {
                    members.forEach { member ->
                        val inventory = Inventory.getInventory(member.idLong)

                        inventory.catFoods += amount
                    }

                    replyToMessageSafely(loader.channel, "Successfully gave out/took away ${EmojiStore.ABILITY["CF"]?.formatted} to/from users!", loader.message) { a -> a }

                    TransactionLogger.logMassCatFoodModify(m.id, amount, members)
                })
            }
        }.onError { _ ->
            replyToMessageSafely(loader.channel, "It seems bot failed to get user data from one of ID that you offered. Maybe this user isn't in this server?", loader.message) { a -> a }
        }
    }

    private fun getMembers(ids: String, guild: Guild) : Task<List<Member>> {
        val segments = ids.split(",")
            .map { id -> id.trim().replace("<@", "").replace(">", "") }
            .filter { id -> StaticStore.isNumeric(id) }
            .map { id -> UserSnowflake.fromId(id) }

        return guild.retrieveMembers(segments)
    }
}