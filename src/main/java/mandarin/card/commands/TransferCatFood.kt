package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.CacheRestAction

class TransferCatFood : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        val contents = loader.content.split(" ")

        if (contents.size < 3) {
            replyToMessageSafely(loader.channel, "You have to provide both user who will receive cat food, and amount of cat food that you want to transfer to!\n\nFormat is `${CardBot.globalPrefix}tcf [User] [Amount]`", loader.message) { a -> a }

            return
        }

        if (!StaticStore.isNumeric(contents[2])) {
            replyToMessageSafely(loader.channel, "Amount of cat food must be numeric!", loader.message) { a -> a }

            return
        }

        val amount = contents[2].toDouble().toLong()

        if (amount <= 0) {
            replyToMessageSafely(loader.channel, "Amount of cat food must be positive!", loader.message) { a -> a }

            return
        }

        val thisInventory = Inventory.getInventory(m.id)

        if (thisInventory.catFoods - amount < 0) {
            replyToMessageSafely(loader.channel, "You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${thisInventory.catFoods}. You can't transfer ${EmojiStore.ABILITY["CF"]?.formatted} $amount to others!", loader.message) { a -> a }

            return
        }

        val memberGetter = tryToGetMember(contents[1], loader.guild)

        if (memberGetter == null) {
            replyToMessageSafely(loader.channel, "You have to pass member via either raw ID or mention of them!", loader.message) { a -> a }

            return
        }

        memberGetter.queue({ mem ->
            replyToMessageSafely(loader.channel, "You are transferring ${EmojiStore.ABILITY["CF"]?.formatted} $amount to user ${mem.asMention}\n" +
                    "\n" +
                    "After transfer, your amount of cat food will be ${EmojiStore.ABILITY["CF"]?.formatted} ${thisInventory.catFoods} -> ${EmojiStore.ABILITY["CF"]?.formatted} ${thisInventory.catFoods - amount}\n" +
                    "\n" +
                    "Are you sure you want to transfer cat food to this user? Once it's done, you can't undo your decision", loader.message, { a ->
                        val components = ArrayList<ActionComponent>()

                        components.add(Button.success("confirm", "Confirm"))
                        components.add(Button.danger("cancel", "Cancel"))

                        a.setActionRow(components)
                    }) { msg ->
                StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, loader.channel.id, lang) {
                    val thatInventory = Inventory.getInventory(mem.id)

                    thatInventory.catFoods += amount
                    thisInventory.catFoods -= amount

                    replyToMessageSafely(loader.channel, "Successfully transferred ${EmojiStore.ABILITY["CF"]?.formatted} $amount!\n" +
                            "\n" +
                            "Now you have ${EmojiStore.ABILITY["CF"]?.formatted} ${thisInventory.catFoods}, and user ${mem.asMention} has ${EmojiStore.ABILITY["CF"]?.formatted} ${thatInventory.catFoods}", loader.message) { a -> a }

                    TransactionLogger.logCatFoodTransfer(m.id, mem.id, amount)
                })
            }
        }) { _ ->
            replyToMessageSafely(loader.channel, "Failed to get member who will receive cat food... Maybe this user isn't in this server? Or you may have passed data with incorrect order. Proper format is :\n" +
                    "\n" +
                    "`${CardBot.globalPrefix}tcf [User] [Amount]`", loader.message) { a -> a }
        }
    }

    private fun tryToGetMember(id: String, g: Guild) : CacheRestAction<Member>? {
        val realId = id.replace("<@", "").replace(">", "")

        if (!StaticStore.isNumeric(realId))
            return null

        return g.retrieveMember(UserSnowflake.fromId(realId))
    }
}