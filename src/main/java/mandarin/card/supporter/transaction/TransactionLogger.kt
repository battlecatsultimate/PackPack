package mandarin.card.supporter.transaction

import mandarin.card.supporter.Activator
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

object TransactionLogger {
    enum class TradeStatus {
        CANCELED,
        TRADED
    }

    lateinit var logChannel: MessageChannel
    lateinit var tradeChannel: MessageChannel
    lateinit var modChannel: MessageChannel

    fun logRoll(cards: List<Card>, pack: CardData.Pack, member: Member, manual: Boolean) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Card Rolled")

        builder.setColor(StaticStore.rainbow.random())

        if (manual) {
            builder.setDescription("Successfully rolled card pack manually by moderator, and result is below")
        } else {
            builder.setDescription("Successfully rolled card pack, and result is below")
        }

        builder.addField(MessageEmbed.Field("Pack", pack.getPackName(), false))

        val cardBuilder = StringBuilder()

        if (cards.isNotEmpty()) {
            cards.forEach { c -> cardBuilder.append("- ").append(c.cardInfo()).append("\n") }
        }

        builder.addField(MessageEmbed.Field("Result", cardBuilder.toString(), false))

        builder.setAuthor(member.user.effectiveName, null, member.user.effectiveAvatarUrl)

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logTrade(session: TradingSession, status: TradeStatus) {
        if (!this::logChannel.isInitialized || !this::tradeChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Trade Session Closed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Trading session has been closed")

        builder.addField("Trader 1", "<@${session.member[0]}> [${session.member[0]}]", true)
        builder.addField("Trader 2", "<@${session.member[1]}> [${session.member[1]}]", true)
        builder.addField("Trader 1's Suggestion", session.suggestion[0].suggestionInfoCompacted(), false)
        builder.addField("Trader 2's Suggestion", session.suggestion[1].suggestionInfoCompacted(), false)

        builder.addField("Trading Result", if (status == TradeStatus.CANCELED) "Canceled" else "Trading Done", false)

        builder.addField("Post", "<#${session.postID}> [${session.postID}]", false)

        if (status == TradeStatus.TRADED) {
            tradeChannel.sendMessageEmbeds(builder.build()).queue()
        } else {
            logChannel.sendMessageEmbeds(builder.build()).queue()
        }
    }

    fun logTradeStart(session: TradingSession, opener: Member) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Trade Session Opened")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Trading session has been opened by ${opener.asMention}")

        builder.addField(MessageEmbed.Field("Trader 1", "<@${session.member[0]}>", true))
        builder.addField(MessageEmbed.Field("Trader 2", "<@${session.member[1]}>", true))

        builder.addField(MessageEmbed.Field("Post", "<#${session.postID}> [${session.postID}]", false))

        logChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logPointsTransfer(member1: Long, member2: Long, removal: Boolean, add: Boolean) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Cat Food Transfer")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Cat food transferring has been done")

        builder.addField("From", "<@$member1> [$member1]", true)
        builder.addField("->", "** **", true)
        builder.addField("To", "<@$member2> [$member2]", true)

        builder.addField("Status", if (removal && add) "Success" else "Failed", false)
        builder.addField("Removing", if (removal) "Done" else "Failed", true)
        builder.addField("Adding", if (add) "Done" else "Failed", true)

        logChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logRolePurchase(member: Long, role: CardData.Role) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Role Purchased")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$member> bought a role <@&${role.id}>")

        logChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logItemPurchase(member: Long, itemName: String, amount: Int) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Item Purchased")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$member> bought $itemName")

        builder.addField("Amount", amount.toString(), true)

        logChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logBannerActivate(activator: Activator, mod: Member, activated: Boolean) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        val text = if (activated) {
            "Activated"
        } else {
            "Deactivated"
        }

        builder.setTitle("Banner $text")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Moderator ${mod.asMention} ${text.lowercase()} the banner")

        builder.addField("Banner", activator.title, true)

        modChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logCardsModify(cards: List<Card>, mod: Member, targetMember: Member, isAdd: Boolean, isMassRemove: Boolean) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        val text = if (isAdd) {
            "Added"
        } else if (isMassRemove) {
            "Mass Removed"
        } else {
            "Removed"
        }

        builder.setTitle("Cards $text")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Moderator ${mod.asMention} manually ${text.lowercase()} the cards")

        builder.addField("Target Member", targetMember.asMention + " [${targetMember.id}]", false)

        val checker = StringBuilder()

        for (card in cards.toSet()) {
            checker.append("- ")
                .append(card.cardInfo())

            val amount = cards.filter { c -> card.unitID == c.unitID }.size

            if (amount > 2) {
                checker.append(" x")
                    .append(amount)
            }

            checker.append("\n")
        }

        if (checker.length >= MessageEmbed.VALUE_MAX_LENGTH) {
            builder.addField("Cards", "Check cards messages below", false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()

            checker.clear()

            for (card in cards.toSet()) {
                var line = "- ${card.cardInfo()}"

                val amount = cards.filter { c -> card.unitID == c.unitID }.size

                if (amount > 2) {
                    line += " x$amount"
                }

                if (checker.length + line.length > 1900) {
                    modChannel.sendMessage(checker.toString()).queue()

                    checker.clear()
                }

                checker.append(line)
                    .append("\n")
            }

            modChannel.sendMessage(checker.toString()).queue()
        } else {
            builder.addField("Cards", checker.toString(), false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()
        }
    }

    fun logRolesModify(roles: List<CardData.Role>, mod: Member, targetMember: Member, isAdd: Boolean, isMassRemove: Boolean) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        val text = if (isAdd) {
            "Added"
        } else if (isMassRemove) {
            "Mass Removed"
        } else {
            "Removed"
        }

        builder.setTitle("Roles $text")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Moderator ${mod.asMention} manually ${text.lowercase()} the roles")

        builder.addField("Target Member", targetMember.asMention + " [${targetMember.id}]", false)

        val checker = StringBuilder()

        for (role in roles.toSet()) {
            checker.append("- ")
                .append(role.title)

            checker.append("\n")
        }

        if (checker.length >= MessageEmbed.VALUE_MAX_LENGTH) {
            builder.addField("Roles", "Check roles messages below", false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()

            checker.clear()

            for (role in roles.toSet()) {
                val line = "- ${role.title}"

                if (checker.length + line.length > 1900) {
                    modChannel.sendMessage(checker.toString()).queue()

                    checker.clear()
                }

                checker.append(line)
                    .append("\n")
            }

            modChannel.sendMessage(checker.toString()).queue()
        } else {
            builder.addField("Roles", checker.toString(), false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()
        }

        modChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logTradeTrialFailure(m: Long, cardEmpty: Boolean, cf: Int) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Trade Open Trial Failed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$m> tried to open trading session, but failed despite the initial warning")

        builder.addField("Card Was Empty?", if (cardEmpty) "True" else "False", false)
        builder.addField("CF", if (cf == -1) "Unknown (API Limit Reached)" else cf.toString(), false)

        logChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logApproval(session: TradingSession, manager: Member) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Session Approved")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager or Moderator ${manager.asMention} approved session")

        builder.addField("Session", "<#${session.postID}> [${session.postID}]", false)
        builder.addField("Manager", "${manager.asMention} [${manager.id}]", false)

        modChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }
}