package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class Trade : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent) {
        val ch = getChannel(event) ?: return
        val g = getGuild(event) ?: return
        val m = getMember(event) ?: return

        if (CardBot.rollLocked && !CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val leftTime = (CardData.tradeCooldown[m.id] ?: 0) - CardData.getUnixEpochTime()

        if (leftTime > 0) {
            replyToMessageSafely(ch, "You can't trade with others because of cooldown\nCooldown left : ${CardData.convertMillisecondsToText(leftTime)}", getMessage(event)) { a -> a }

            return
        }

        val leftTrialTime = (CardData.tradeTrialCooldown[m.id] ?: 0) - CardData.getUnixEpochTime()

        if (leftTrialTime > 0) {
            replyToMessageSafely(ch, "You can't trade with others since you didn't meet the requirement to open the trading session\nCooldown left : ${CardData.convertMillisecondsToText(leftTrialTime)}", getMessage(event)) { a -> a }

            return
        }

        if (Inventory.getInventory(m.id).cards.isEmpty()) {
            if (TatsuHandler.canInteract(1, false)) {
                val cf = TatsuHandler.getPoints(g.idLong, m.idLong, false)

                if (cf < 1000) {
                    val additional = if (!CardData.tradeTrialCooldown.containsKey(m.id)) {
                        "If you call this command without meeting said requirements again, you won't be able to use this command for 1 hour, so be careful"
                    } else {
                        "You won't be able to use this command for 1 hour, please don't call this command unnecessarily"
                    }

                    replyToMessageSafely(ch, "It seems you don't have any cards yet, and you also don't have more than 1k cf. To open trading session, you have to have at least 1 card or over 1k cf\n\n$additional", getMessage(event)) { a -> a }

                    if (!CardData.tradeTrialCooldown.containsKey(m.id)) {
                        CardData.tradeTrialCooldown[m.id] = 0
                    } else {
                        CardData.tradeTrialCooldown[m.id] = CardData.getUnixEpochTime() + CardData.tradeTrialCooldownTerm

                        TransactionLogger.logTradeTrialFailure(m.idLong, Inventory.getInventory(m.id).cards.isEmpty(), cf)
                    }

                    CardBot.saveCardData()

                    return
                }
            } else {
                val additional = if (!CardData.tradeTrialCooldown.containsKey(m.id)) {
                    "If you call this command without meeting said requirements again, you won't be able to use this command for 1 hour, so be careful"
                } else {
                    "You won't be able to use this command for 1 hour, please don't call this command unnecessarily"
                }

                replyToMessageSafely(ch, "It seems you don't have any cards yet, and bot couldn't check your cf due to queue limit. To open trading session, you have to have at least 1 card or over 1k cf\n\n$additional", getMessage(event)) { a -> a }

                if (!CardData.tradeTrialCooldown.containsKey(m.id)) {
                    CardData.tradeTrialCooldown[m.id] = 0
                } else {
                    CardData.tradeTrialCooldown[m.id] = CardData.getUnixEpochTime() + CardData.tradeTrialCooldownTerm

                    TransactionLogger.logTradeTrialFailure(m.idLong, Inventory.getInventory(m.id).cards.isEmpty(), -1)
                }

                CardBot.saveCardData()

                return
            }
        }

        val contents = getContent(event).split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(ch, "Please specify the member whom you will want to trade with", getMessage(event)) { a -> a }

            return
        }

        val targetMember = findMember(contents, g)

        if (targetMember == null) {
            replyToMessageSafely(ch, "Bot failed to find member from the command", getMessage(event)) { a -> a }

            return
        }

        if (targetMember.user.isBot) {
            replyToMessageSafely(ch, "You can't trade with bot!", getMessage(event)) { a -> a }

            return
        }

        if (targetMember.id == m.id) {
            replyToMessageSafely(ch, "You can't trade with yourself!", getMessage(event)) { a -> a }

            return
        }

        val targetLeftTime = (CardData.tradeCooldown[targetMember.id] ?: 0) - CardData.getUnixEpochTime()

        if (targetLeftTime > 0) {
            replyToMessageSafely(ch, "Provided member can't trade with others due to cooldown\nCooldown left : ${CardData.convertMillisecondsToText(targetLeftTime)}", getMessage(event)) { a -> a }

            return
        }

        val targetTrialLeftTime = (CardData.tradeTrialCooldown[targetMember.id] ?: 0) - CardData.getUnixEpochTime()

        if (targetTrialLeftTime > 0) {
            replyToMessageSafely(ch, "Provided member can't trade with others due to cooldown\nCooldown left : ${CardData.convertMillisecondsToText(targetTrialLeftTime)}", getMessage(event)) { a -> a }

            return
        }

        if (Inventory.getInventory(targetMember.id).cards.isEmpty()) {
            if (TatsuHandler.canInteract(1, false)) {
                val cf = TatsuHandler.getPoints(g.idLong, targetMember.idLong, false)

                if (cf < 1000) {
                    replyToMessageSafely(ch, "It seems you can't trade with that member because they don't meet requirements; Having cards or more than 1k cf", getMessage(event)) { a -> a }

                    return
                }
            } else {
                replyToMessageSafely(ch, "It seems you can't trade with that member because they don't meet requirements; Having cards or more than 1k cf", getMessage(event)) { a -> a }

                return
            }
        }

        val forum = g.getForumChannelById(if (CardBot.test) CardData.testTradingPlace else CardData.tradingPlace) ?: return

        val postData = MessageCreateData.fromContent("## Welcome to trading session #${CardData.sessionNumber}\n" +
                "\n" +
                "${m.asMention} ${targetMember.asMention}\n" +
                "\n" +
                "This post has been created to focus on trading between both of you. You can suggest or discuss about cards or cf that will be traded\n" +
                "\n" +
                "### Guide for trading\n" +
                "\n" +
                "If you are trading cards for first time, this place may be quite new and confusing for you. Please read guide below thoroughly to properly trade cards with others!\n" +
                "\n" +
                "1. First, you and the other traders have to suggest which cards will be traded, and how much cf will be traded\n" +
                "\n" +
                "- This can be done by calling `${CardBot.globalPrefix}suggest` command. Once command is called, it will open up your inventory. You can select cards that will be traded, up to 10 cards in total. Both must suggest something at least to make trading done. If you want to edit what you've suggested, call the command again, and re-suggest\n" +
                "\n" +
                "- Cf can be handled by bot, so **we do not recommend users to perform manual cf transferring.** As this warning exists, we won't take any responsibilities from problems that can happen regarding manual transferring\n" +
                "\n" +
                "2. Second, both users must agree on what they are trading\n" +
                "\n" +
                "- **Please check what the other traders has suggested thoroughly, and check if it meets requirements that you wanted. Once trading is done, it cannot be undone, and mistakes won't be handled by anyone.** Both users must call `${CardBot.globalPrefix}confirm` to confirm their suggestion. If any of users edits their suggestion, this confirmation will be canceled, so suggestion must not be edited to make confirmation done\n" +
                "\n" +
                "If you want to cancel trading, please call `${CardBot.globalPrefix}cancel`. Any of user calling this command will directly cancel the trading, and this session will be closed\n" +
                "\n" +
                "**__DO NOT scam others.__**  Keep in mind that all transactions will be logged, and this post won't be deleted"
        )

        val post = forum.createForumPost("Trading Session #${CardData.sessionNumber}", postData).complete()

        CardData.sessionNumber++

        val session = TradingSession(post.threadChannel.idLong, arrayOf(m.idLong, targetMember.idLong))

        CardData.sessions.add(session)

        CardBot.saveCardData()

        TransactionLogger.logTradeStart(session, m)
    }

    private fun findMember(contents: List<String>, g: Guild) : Member? {
        for (content in contents) {
            val id = if (StaticStore.isNumeric(content))
                content
            else if (content.matches(Regex("<@\\d+>"))) {
                content.replace("<@", "").replace(">", "")
            } else {
                continue
            }

            try {
                return g.retrieveMember(UserSnowflake.fromId(id)).complete()
            } catch (_: Exception) {
            }
        }

        return null
    }
}