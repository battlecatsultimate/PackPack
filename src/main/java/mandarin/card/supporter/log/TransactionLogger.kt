package mandarin.card.supporter.log

import mandarin.card.supporter.Activator
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import kotlin.math.min

object TransactionLogger {
    enum class TradeStatus {
        CANCELED,
        TRADED
    }

    lateinit var logChannel: MessageChannel
    lateinit var tradeChannel: MessageChannel
    lateinit var modChannel: MessageChannel
    lateinit var catFoodChannel: MessageChannel

    fun logRoll(cards: List<Card>, pack: CardData.Pack, member: Member, manual: Boolean) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Card Rolled")

        builder.setColor(StaticStore.rainbow.random())

        if (manual) {
            builder.setDescription("User ${member.asMention} successfully got card pack manually by moderator, and result is below")
        } else {
            builder.setDescription("User ${member.asMention} successfully rolled card pack, and result is below")
        }

        builder.addField(MessageEmbed.Field("Pack", pack.getPackName(), false))

        val cardBuilder = StringBuilder()

        if (cards.isNotEmpty()) {
            cards.forEach { c -> cardBuilder.append("- ").append(c.cardInfo()).append("\n") }
        }

        builder.addField(MessageEmbed.Field("Result", cardBuilder.toString(), false))

        builder.setAuthor(member.user.effectiveName, null, member.user.effectiveAvatarUrl)

        logChannel.sendMessageEmbeds(builder.build()).queue()

        if (!manual) {
            LogSession.session.logRoll(member.idLong, pack, cards)
        } else {
            LogSession.session.logManualRoll(member.idLong, cards)
        }
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

            LogSession.session.logTrade(session)
        } else {
            logChannel.sendMessageEmbeds(builder.build()).queue()
        }
    }

    fun logTradeCancel(session: TradingSession, member: Long) {
        if (!this::logChannel.isInitialized || !this::tradeChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Trade Session Closed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Trading session has been closed by manager <@${member}>")

        builder.addField("Trader 1", "<@${session.member[0]}> [${session.member[0]}]", true)
        builder.addField("Trader 2", "<@${session.member[1]}> [${session.member[1]}]", true)
        builder.addField("Trader 1's Suggestion", session.suggestion[0].suggestionInfoCompacted(), false)
        builder.addField("Trader 2's Suggestion", session.suggestion[1].suggestionInfoCompacted(), false)

        builder.addField("Trading Result", "Canceled", false)

        builder.addField("Post", "<#${session.postID}> [${session.postID}]", false)

        logChannel.sendMessageEmbeds(builder.build()).queue()
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

            if (amount >= 2) {
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

                if (amount >= 2) {
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

        if (isAdd) {
            LogSession.session.logModifyAdd(targetMember.idLong, cards)
        } else {
            LogSession.session.logModifyRemove(targetMember.idLong, cards)
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

    fun logTradeTrialFailure(m: Long, cardEmpty: Boolean, cf: Long) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Trade Open Trial Failed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$m> tried to open trading session, but failed despite the initial warning")

        builder.addField("Card Was Empty?", if (cardEmpty) "True" else "False", false)
        builder.addField("CF", if (cf == -1L) "Unknown (API Limit Reached)" else cf.toString(), false)

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

    fun logSalvage(member: Long, cardAmount: Int, salvageMode: CardData.SalvageMode, cards: List<Card>) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Cards Salvaged")

        builder.setColor(StaticStore.rainbow.random())

        val cardType = when(salvageMode) {
            CardData.SalvageMode.T1 -> "T1"
            CardData.SalvageMode.T2 -> "Regular T2"
            CardData.SalvageMode.SEASONAL -> "Seasonal T2"
            CardData.SalvageMode.COLLAB -> "Collaboration T2"
            CardData.SalvageMode.T3 -> "T3"
        }

        builder.setDescription("Member <@$member> salvaged $cardType cards")

        builder.addField("Number of Cards", "$cardAmount", true)
        builder.addField("Received Shards", "${EmojiStore.ABILITY["SHARD"]?.formatted} ${cardAmount * salvageMode.cost}", true)

        val checker = StringBuilder()

        for (card in cards.toSet()) {
            checker.append("- ")
                .append(card.cardInfo())

            val amount = cards.filter { c -> card.unitID == c.unitID }.size

            if (amount >= 2) {
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

                if (amount >= 2) {
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

        logChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()

        LogSession.session.logSalvage(member, cards, cardAmount  * salvageMode.cost.toLong())
    }

    fun logCraft(member: Long, cardAmount: Int, craftedCard: Card?, cards: List<Card>) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Tried to Craft")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Member <@$member> tried to craft T2 cards")

        if (craftedCard != null) {
            builder.addField("Successful?", "Yes", true)
            builder.addField("Received Card", craftedCard.cardInfo(), true)
        } else {
            builder.addField("Successful?", "No", true)
            builder.addField("Received CF", "${cardAmount * CardData.SalvageMode.T1.cost}", true)
        }

        val checker = StringBuilder()

        for (card in cards.toSet()) {
            checker.append("- ")
                .append(card.cardInfo())

            val amount = cards.filter { c -> card.unitID == c.unitID }.size

            if (amount >= 2) {
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

                if (amount >= 2) {
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

        if (craftedCard == null) {
            LogSession.session.logCraftFail(member, cards, cardAmount * CardData.SalvageMode.T1.cost.toLong())
        } else {
            LogSession.session.logCraftSuccess(member, cards, craftedCard)
        }
    }

    fun logMassRoll(manager: Member, people: Int, pack: CardData.Pack) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Card Mass Rolled")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Successfully rolled card pack manually by moderator, and result is below")

        builder.addField(MessageEmbed.Field("Pack", pack.getPackName(), false))
        builder.addField(MessageEmbed.Field("Number of People", people.toString(), false))

        builder.setAuthor(manager.user.effectiveName, null, manager.user.effectiveAvatarUrl)

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCatFoodRateChange(manager: String, oldRate: LongArray, newRate: LongArray) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Cat Food Rate Changed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> changed cat food rate")

        builder.addField("Old", "Minimum : ${EmojiStore.ABILITY["CF"]?.formatted} ${oldRate[0]}\n\nMaximum : ${EmojiStore.ABILITY["CF"]?.formatted} ${oldRate[1]}", false)
        builder.addField("New", "Minimum : ${EmojiStore.ABILITY["CF"]?.formatted} ${newRate[0]}\n\nMaximum : ${EmojiStore.ABILITY["CF"]?.formatted} ${newRate[1]}", false)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCatFoodCooldownChange(manager: String, oldCooldown: Long, newCooldown: Long) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Cat Food Cooldown Changed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> changed cooldown about cat food that will be given while chatting")

        val oldDesc = if (oldCooldown <= 0L)
            "Every message"
        else
            "Every `${CardData.convertMillisecondsToText(oldCooldown)}`"

        val newDesc = if (newCooldown <= 0L)
            "Every message"
        else
            "Every `${CardData.convertMillisecondsToText(newCooldown)}`"

        builder.addField("Old", oldDesc, false)
        builder.addField("New", newDesc, false)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logChannelExcluded(manager: String, channels: List<String>, added: Boolean) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Excluded Channel List Updated")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@${manager}> ${if (added) "added channels below into" else "removed channels below from"} exclusion list. Exclusion list is channels where bot won't give cf when users chat in there")

        val text = StringBuilder()

        val size = min(25, channels.size)

        for (i in 0 until size) {
            text.append("<#").append(channels[i]).append(">")

            if (i < size - 1) {
                text.append("\n")
            }
        }

        if (channels.size > 25) {
            text.append("\n\n...and ${channels.size - 25} channel(s) more")
        }

        builder.addField("Channels", text.toString(), false)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCatFoodModification(manager: String, targetMember: String, amount: Long, added: Boolean, oldAmount: Long, newAmount: Long) {
        if (!this::catFoodChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Cat Food Modified")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> modified <@$targetMember> user's cat food")

        builder.addField("Manager", "<@$manager> [$manager]", true)
        builder.addField("Target Member", "<@$targetMember> [$targetMember]", true)

        builder.addField("Added?", if (added) "True" else "False", false)

        builder.addField("Amount", "${EmojiStore.ABILITY["CF"]?.formatted} $amount\n\nFrom : ${EmojiStore.ABILITY["CF"]?.formatted} $oldAmount -> To : ${EmojiStore.ABILITY["CF"]?.formatted} $newAmount", false)

        catFoodChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCatFoodTransfer(from: String, to: String, amount: Long) {
        if (!this::catFoodChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Cat Food Transferred")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$from> transferred cat food to user <@$to>")

        builder.addField("From", "<@$from> [$from]", true)
        builder.addField("To", "<@$to> [$to]", true)

        builder.addField("Amount", "${EmojiStore.ABILITY["CF"]?.formatted} $amount", false)

        catFoodChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logMassCatFoodModify(manager: String, amount: Long, members: List<Member>) {
        if (!this::catFoodChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Mass Cat Food Given")

        builder.setColor(StaticStore.rainbow.random())

        val verb = if (amount < 0)
            "took away"
        else
            "gave out"

        val connector = if (amount < 0)
            "from"
        else
            "to"

        builder.setDescription("Manager <@$manager> $verb $connector users")

        builder.addField("Manager", "<@$manager> [$manager]", true)
        builder.addField("Amount", "${EmojiStore.ABILITY["CF"]?.formatted} $amount", true)

        val list = StringBuilder()

        val size = min(25, members.size)

        for (m in 0 until size) {
            list.append(members[m].asMention)

            if (m < size - 1)
                list.append("\n")
        }

        if (members.size > size)
            list.append("\n...and ${members.size - size} member(s) more")

        builder.addField("Members", list.toString(), false)

        catFoodChannel.sendMessageEmbeds(builder.build()).queue()
    }
}