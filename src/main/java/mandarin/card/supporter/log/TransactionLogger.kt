package mandarin.card.supporter.log

import mandarin.card.supporter.AuctionSession
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.CardPayContainer
import mandarin.card.supporter.slot.*
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
        TRADED,
        EXPIRED
    }

    lateinit var logChannel: MessageChannel
    lateinit var tradeChannel: MessageChannel
    lateinit var modChannel: MessageChannel
    lateinit var catFoodChannel: MessageChannel
    lateinit var bidLogChannel: MessageChannel
    lateinit var slotChannel: MessageChannel

    fun logRoll(cards: List<Card>, pack: CardPack, member: Member, manual: Boolean) {
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

        builder.addField(MessageEmbed.Field("Pack", pack.packName, false))

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

        val st = when(status) {
            TradeStatus.CANCELED -> "Canceled"
            TradeStatus.TRADED -> "Trading Done"
            TradeStatus.EXPIRED -> "Expired"
        }

        builder.addField("Trading Result", st, false)

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

    fun logBannerActivate(banner: Banner, mod: Member, activated: Boolean) {
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

        builder.addField("Banner", banner.name, true)

        modChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logCardActivate(activator: Member, card: Card) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        val text = if (card.activated) {
            "Activated"
        } else {
            "Deactivated"
        }

        builder.setTitle("Card $text")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager ${activator.asMention} ${text.lowercase()} the card")

        builder.addField("Card", card.simpleCardInfo(), true)

        modChannel.sendMessageEmbeds(builder.build())
            .setAllowedMentions(ArrayList())
            .queue()
    }

    fun logCardsModify(cards: Map<Card, Int>, mod: Member, targetMember: Member, isAdd: Boolean, isMassRemove: Boolean) {
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

        cards.forEach { (card, amount) ->
            checker.append("- ")
                .append(card.cardInfo())

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

            cards.forEach { (card, amount) ->
                var line = "- ${card.cardInfo()}"

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

    fun logSkinsModify(skins: List<Skin>, mod: Member, targetMember: Member, isAdd: Boolean, isMassRemove: Boolean) {
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

        builder.setTitle("Skins $text")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Moderator ${mod.asMention} manually ${text.lowercase()} the skins")

        builder.addField("Target Member", targetMember.asMention + " [${targetMember.id}]", false)

        val checker = StringBuilder()

        for (skin in skins.toSet()) {
            checker.append("- ")
                .append(skin.name)

            checker.append("\n")
        }

        if (checker.length >= MessageEmbed.VALUE_MAX_LENGTH) {
            builder.addField("Skins", "Check skins messages below", false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()

            checker.clear()

            for (skin in skins.toSet()) {
                val line = "- ${skin.name}"

                if (checker.length + line.length > 1900) {
                    modChannel.sendMessage(checker.toString()).queue()

                    checker.clear()
                }

                checker.append(line)
                    .append("\n")
            }

            modChannel.sendMessage(checker.toString()).queue()
        } else {
            builder.addField("Skins", checker.toString(), false)

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
            CardData.SalvageMode.T4 -> "T4"
        }

        builder.setDescription("Member <@$member> salvaged $cardType cards")

        builder.addField("Number of Cards", "$cardAmount", true)
        builder.addField("Received Shards", "${EmojiStore.ABILITY["SHARD"]?.formatted} ${cardAmount * salvageMode.cost}", true)

        val checker = StringBuilder()

        for (card in cards.toSet()) {
            checker.append("- ")
                .append(card.cardInfo())

            val amount = cards.filter { c -> card.id == c.id }.size

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

                val amount = cards.filter { c -> card.id == c.id }.size

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

    fun logCraft(member: Long, cardAmount: Int, craftMode: CardData.CraftMode, craftedCards: List<Card>, shards: Long) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Tried to Craft")

        builder.setColor(StaticStore.rainbow.random())

        val name = when(craftMode) {
            CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
        }

        builder.setDescription("Member <@$member> tried to craft $name cards")

        builder.addField("Member", "<@$member> [$member]", true)
        builder.addField("Shard Spent", "${EmojiStore.ABILITY["SHARD"]?.formatted} $shards", true)
        builder.addField("Card Crafted", cardAmount.toString(), true)

        val cardList = StringBuilder()

        craftedCards.toSet().forEach { c ->
            val amount = craftedCards.filter { card -> card.id == c.id }.size

            cardList.append("- ").append(c.simpleCardInfo())

            if (amount >= 2) {
                cardList.append(" x")
                    .append(amount)
            }

            cardList.append("\n")
        }

        if (cardList.length >= MessageEmbed.VALUE_MAX_LENGTH) {
            builder.addField("Cards", "Check cards messages below", false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()

            modChannel.sendMessage(cardList.toString()).queue()
        } else {
            builder.addField("Cards", cardList.toString(), false)

            modChannel.sendMessageEmbeds(builder.build())
                .setAllowedMentions(ArrayList())
                .queue()
        }

        LogSession.session.logCraft(member, shards, craftedCards)
    }

    fun logMassRoll(manager: Member, people: Int, pack: CardPack) {
        if (!this::logChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Card Mass Rolled")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Successfully rolled card pack manually by moderator, and result is below")

        builder.addField(MessageEmbed.Field("Pack", pack.packName, false))
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

    fun logMassShardModify(manager: String, amount: Long, members: List<Member>) {
        if (!this::catFoodChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Mass Shard Given")

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
        builder.addField("Amount", "${EmojiStore.ABILITY["SHARD"]?.formatted} $amount", true)

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

        LogSession.session.logMassShardModify(members.map { m -> m.idLong }, amount)
    }

    fun logPlatinumShardModification(manager: String, targetMember: String, amount: Long, added: Boolean, oldAmount: Long, newAmount: Long) {
        if (!this::catFoodChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Platinum Shard Modified")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> modified <@$targetMember> user's platinum shard")

        builder.addField("Manager", "<@$manager> [$manager]", true)
        builder.addField("Target Member", "<@$targetMember> [$targetMember]", true)

        builder.addField("Added?", if (added) "True" else "False", false)

        builder.addField("Amount", "${EmojiStore.ABILITY["SHARD"]?.formatted} $amount\n\nFrom : ${EmojiStore.ABILITY["SHARD"]?.formatted} $oldAmount -> To : ${EmojiStore.ABILITY["SHARD"]?.formatted} $newAmount", false)

        catFoodChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSalvageCostModified(manager: String, mode: CardData.SalvageMode, oldCost: Int, newCost: Int) {
        if (!this::modChannel.isInitialized)
            return

        val cardType = when(mode) {
            CardData.SalvageMode.T1 -> "Tier 1 [Common]"
            CardData.SalvageMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.SalvageMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.SalvageMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.SalvageMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.SalvageMode.T4 -> "Tier 4 [Legend Rare]"
        }

        val builder = EmbedBuilder()

        builder.setTitle("Salvage Cost Changed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> modified salvage cost of $cardType cards")

        builder.addField("Manager", "<@$manager> [$manager]", false)

        builder.addField("Old Cost", "${EmojiStore.ABILITY["SHARD"]?.formatted} $oldCost", true)
        builder.addField("New Cost", "${EmojiStore.ABILITY["SHARD"]?.formatted} $newCost", true)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSalvageCostModified(manager: String, mode: CardData.CraftMode, oldCost: Int, newCost: Int) {
        if (!this::modChannel.isInitialized)
            return

        val cardType = when(mode) {
            CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
        }

        val builder = EmbedBuilder()

        builder.setTitle("Salvage Cost Changed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> modified craft cost of $cardType cards")

        builder.addField("Manager", "<@$manager> [$manager]", false)

        builder.addField("Old Cost", "${EmojiStore.ABILITY["SHARD"]?.formatted} $oldCost", true)
        builder.addField("New Cost", "${EmojiStore.ABILITY["SHARD"]?.formatted} $newCost", true)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCooldownReset(pack: CardPack, manager: String) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Pack Cooldown Reset")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> reset cooldown of pack for all users")

        builder.addField("Manager", "<@$manager> [$manager]", false)

        builder.addField("Pack", pack.packName, true)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logBid(session: AuctionSession, user: Long, amount: Long) {
        if (!this::bidLogChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Auction Bid")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$user> has bid the auction #${session.id}")

        builder.addField("Auction ID" , "${session.id} <#${session.channel}> [${session.channel}]", false)

        builder.addField("User", "<@$user> [$user]", true)
        builder.addField("Amount", "${EmojiStore.ABILITY["CF"]?.formatted} $amount", true)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        bidLogChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logBidCancel(session: AuctionSession, user: Long, previousBid: Long) {
        if (!this::bidLogChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction Bid")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$user> has canceled the bid in the auction #${session.id}")

        builder.addField("Auction ID" , "${session.id} <#${session.channel}> [${session.channel}]", false)

        builder.addField("User", "<@$user> [$user]", true)
        builder.addField("Previous Amount", "${EmojiStore.ABILITY["CF"]?.formatted} $previousBid", true)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        bidLogChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logBidForceCancel(session: AuctionSession, canceler: Long, user: Long, previousBid: Long) {
        if (!this::bidLogChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction Bid")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$canceler> force-canceled <@$user>'s bid in the auction #${session.id}")

        builder.addField("Canceler", "<@$canceler> [$canceler]", false)

        builder.addField("Auction ID" , "${session.id} <#${session.channel}> [${session.channel}]", false)

        builder.addField("User", "<@$user> [$user]", true)
        builder.addField("Previous Amount", "${EmojiStore.ABILITY["CF"]?.formatted} $previousBid", true)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        bidLogChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logAuctionCreate(creator: Long, session: AuctionSession) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction Created")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$creator> has created the auction #${session.id}")

        builder.addField("Creator", "<@$creator> [$creator]", false)

        builder.addField("Auction Session", "Auction Session #${session.id} <#${session.channel}> [${session.channel}]", false)

        builder.addField("Selected Card", session.card.simpleCardInfo(), true)
        builder.addField("Amount", session.amount.toString(), true)

        builder.addField("Author", if (session.author == -1L) "System" else "<@${session.author}> [${session.author}]", false)

        builder.addField("Initial Price", "${EmojiStore.ABILITY["CF"]?.formatted} ${session.initialPrice}", true)

        builder.addField("Minimum Bid Increase", "${EmojiStore.ABILITY["CF"]?.formatted} ${session.minimumBid}", true)

        builder.addField("Anonymous?", if (session.anonymous) "Yes" else "No", false)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logAuctionClose(closer: Long, auto: Boolean, session: AuctionSession) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction ${if (auto) "Auto" else "Force"} Closed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager ${if (closer != 0L) "<@$closer>" else "System"} has ${if (auto) "auto" else "force"}-closed the auction #${session.id}")

        builder.addField("Closer", if (closer != 0L) "<@$closer> [$closer]" else "System", false)

        builder.addField("Auction Session", "Auction Session #${session.id} <#${session.channel}> [${session.channel}]", false)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logAuctionCancel(canceler: Long, session: AuctionSession) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction Closed")

        builder.setColor(StaticStore.rainbow.random())

        if (canceler == -1L) {
            builder.setDescription("System has canceled the auction #${session.id} due to deletion of the card")

            builder.addField("Canceler", "System", false)
        } else {
            builder.setDescription("Manager <@$canceler> has canceled the auction #${session.id}")

            builder.addField("Canceler", "<@$canceler> [$canceler]", false)
        }

        builder.addField("Auction Session", "Auction Session #${session.id} <#${session.channel}> [${session.channel}]", false)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logAuctionApprove(approver: Long, session: AuctionSession) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction Closed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$approver> has approved the auction #${session.id}")

        builder.addField("Approver", "<@$approver> [$approver]", false)

        builder.addField("Auction Session", "Auction Session #${session.id} <#${session.channel}> [${session.channel}]", false)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logAuctionEndDateChange(changer: Long, previousEndDate: Long, session: AuctionSession) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Auction End Time Changed")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$changer> has changed end time of auction #${session.id}")

        builder.addField("Changer", "<@$changer> [$changer]", false)

        builder.addField("Auction Session", "Auction Session #${session.id} <#${session.channel}> [${session.channel}]", false)

        builder.addField("From", "<t:$previousEndDate:f>", true)
        builder.addField("To", "<t:${session.endDate}:f>", true)

        if (session.endDate - previousEndDate < 0) {
            builder.addField("Time Decreased By", CardData.convertMillisecondsToText((previousEndDate - session.endDate) * 1000L), false)
        } else {
            builder.addField("Time Increased By", CardData.convertMillisecondsToText((session.endDate - previousEndDate) * 1000L), false)
        }

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logAuctionResult(session: AuctionSession) {
        if (!this::tradeChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Auction Ended")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Auction #${session.id} has ended, and approved!")

        builder.addField("Auction Host", if (session.author == -1L) "System" else "<@${session.author}> [${session.author}]", false)

        builder.addField("Card", session.card.simpleCardInfo(), true)
        builder.addField("Amount", session.amount.toString(), true)

        builder.addField("Receiver", "<@${session.getMostBidMember()}> [${session.getMostBidMember()}]", false)

        builder.addField("Bid Amount", "${EmojiStore.ABILITY["CF"]?.formatted} ${session.currentBid}", false)

        val auctionMessage = session.getAuctionMessage()

        if (auctionMessage != null) {
            builder.addField("Reference", auctionMessage.jumpUrl, false)
        }

        tradeChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSlotMachineCreation(manager: Long, slotMachine: SlotMachine) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Slot Machine Created")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> has created slot machine")

        builder.addField("Slot Machine", slotMachine.name, true)
        builder.addField("UUID", slotMachine.uuid, true)

        builder.addField("Creator", "<@$manager> ($manager)", false)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSlotMachineDeletion(manager: Long, slotMachine: SlotMachine) {
        if (!this::modChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Slot Machine Deleted")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Manager <@$manager> has removed slot machine")

        builder.addField("Slot Machine", slotMachine.name, true)
        builder.addField("UUID", slotMachine.uuid, true)

        builder.addField("Remover", "<@$manager> ($manager)", false)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSlotMachineRollFail(user: Long, input: Long, slotMachine: SlotMachine, score: Int, compensation: Long) {
        if (!this::slotChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Slot Machine Rolled")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$user> rolled slot machine")

        builder.addField("Slot Machine", "${slotMachine.name} (${slotMachine.uuid})", false)

        builder.addField("Result", "Lost", false)

        val feeEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        builder.addField("Input", "$feeEmoji $input", false)

        builder.addField("Score", if (score >= 2) "$score Scores" else "$score Score", true)
        builder.addField("Compensation", "$feeEmoji $compensation", true)

        slotChannel.sendMessageEmbeds(builder.build()).queue()

        LogSession.session.logSlotMachineFail(user, input, compensation)
    }

    fun logSlotMachineWin(user: Long, input: Long, slotMachine: SlotMachine, pickedContents: List<SlotContent>, currencySum: Long, cardsSum: List<Card>) {
        if (!this::slotChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Slot Machine Rolled")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("User <@$user> rolled slot machine")

        builder.addField("Slot Machine", "${slotMachine.name} (${slotMachine.uuid})", false)

        val feeEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        builder.addField("Result", "Won", true)
        builder.addField("Input", "$feeEmoji $input", true)

        val feeName = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
        }

        val contentBuilder = StringBuilder()

        pickedContents.forEachIndexed { i, c ->
            val contentEmoji = (c.emoji ?: EmojiStore.UNKNOWN).formatted

            val content =  when(c) {
                is SlotCurrencyContent -> {
                    when (c.mode) {
                        SlotCurrencyContent.Mode.FLAT -> "${contentEmoji}x${c.slot} [Flat] : $feeEmoji ${c.amount}"
                        SlotCurrencyContent.Mode.PERCENTAGE -> "${contentEmoji}x${c.slot} [Percentage] : ${c.amount}% of Entry Fee"
                    }
                }
                is SlotCardContent -> {
                    "${contentEmoji}x${c.slot} [Card] : ${c.name}"
                }
                else -> "UNKNOWN"
            }

            contentBuilder.append(i + 1).append(". ").append(content).append("\n")
        }

        builder.addField("Picked Reward", contentBuilder.toString().trim(), false)

        val rewardBuilder = StringBuilder()

        rewardBuilder.append("**$feeName**\n")
            .append("$feeEmoji $currencySum\n")
            .append("**Cards**\n")

        if (cardsSum.isEmpty()) {
            rewardBuilder.append("- None")
        } else {
            cardsSum.forEach { c ->
                rewardBuilder.append("- ").append(c.simpleCardInfo()).append("\n")
            }
        }

        builder.addField("Reward", rewardBuilder.toString().trim(), true)

        slotChannel.sendMessageEmbeds(builder.build()).queue()

        LogSession.session.logSlotMachineWin(user, input, currencySum, cardsSum, slotMachine.entryFee.entryType)
    }

    fun logSlotMachineManualRoll(roller: Long, slotMachine: SlotMachine, userSize: Long) {
        if (!this::slotChannel.isInitialized)
            return

        val builder = EmbedBuilder()

        builder.setTitle("Slot Machine Rolled")

        builder.setColor(StaticStore.rainbow.random())

        builder.setDescription("Card manager <@$roller> manual rolled slot machine for $userSize user(s)")

        builder.addField("Roller", "<@$roller> [$roller]", false)
        builder.addField("Slot Machine", "${slotMachine.name} (${slotMachine.uuid})", false)
    }

    fun logSkinCreate(manager: Long, skin: Skin) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Skin Created")
            .setDescription("Manager <@$manager> created new skin ${skin.skinID}")
            .setColor(StaticStore.rainbow.random())

        builder.addField("Manager", "<@$manager> [$manager]", false)

        builder.addField("Skin ID", skin.skinID.toString(), true)
        builder.addField("Skin Name", skin.name, true)

        builder.addField("Creator", if (skin.creator == -1L) "Official Skin" else "<@${skin.creator}>", false)

        builder.addField("Is Public?", if (skin.public) "Yes" else "No", true)

        val free = skin.cost.catFoods == 0L && skin.cost.platinumShards == 0L && skin.cost.cardsCosts.isEmpty()

        builder.addField("Cost", if (free) "Free" else "Check cost info via `cd.mc`", true)

        builder.addField("Skin File", "** **", false)

        skin.cache(modChannel.jda, true)

        builder.setImage(skin.cacheLink)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSkinRemove(manager: Long, skin: Skin) {
        if (!this::modChannel.isInitialized) {
            return
        }

        val builder = EmbedBuilder()

        builder.setTitle("Skin Created")
            .setDescription("Manager <@$manager> remove the skin ${skin.skinID} - ${skin.name}")
            .setColor(StaticStore.rainbow.random())

        builder.addField("Manager", "<@$manager> [$manager]", false)

        builder.addField("Skin ID", skin.skinID.toString(), true)
        builder.addField("Skin Name", skin.name, true)

        modChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logSkinPurchase(purchaser: Long, skin: Skin, containers: Array<CardPayContainer>) {
        if (!this::logChannel.isInitialized) {
            return
        }

        skin.cache(logChannel.jda, true)

        val builder = EmbedBuilder()

        builder.setTitle("Skin Purchased")
            .setDescription("User <@$purchaser> purchased skin ${skin.skinID} - ${skin.name}")
            .setColor(StaticStore.rainbow.random())
            .setImage(skin.cacheLink)

        builder.addField("Purchaser", "<@$purchaser> [$purchaser]", false)

        builder.addField("Skin ID", skin.skinID.toString(), true)
        builder.addField("Skin Name", skin.name, true)

        if (skin.cost.catFoods != 0L) {
            builder.addField("Paid Cat Food", "${EmojiStore.ABILITY["CF"]?.formatted} ${skin.cost.catFoods}", false)
        }

        if (skin.cost.platinumShards != 0L) {
            builder.addField("Paid Platinum Shards", "${EmojiStore.ABILITY["SHARD"]?.formatted} ${skin.cost.platinumShards}", false)
        }

        if (skin.cost.cardsCosts.isNotEmpty()) {
            val cards = HashMap<Card, Int>()

            containers.forEach { container ->
                container.pickedCards.forEach { card ->
                    cards[card] = (cards[card] ?: 0) + 1
                }
            }

            val cardBuilder = StringBuilder()

            cards.entries.forEach { (card, amount) ->
                cardBuilder.append("- ").append(card.simpleCardInfo())

                if (amount >= 2) {
                    cardBuilder.append(" x").append(amount)
                }

                cardBuilder.append("\n")
            }

            if (cardBuilder.length > MessageEmbed.VALUE_MAX_LENGTH) {
                builder.addField("Cards", "Check Message Below", false)

                logChannel.sendMessageEmbeds(builder.build()).queue()
                logChannel.sendMessage(cardBuilder.toString().trim()).queue()
            } else {
                builder.addField("Cards", cardBuilder.toString().trim(), false)

                logChannel.sendMessageEmbeds(builder.build()).queue()
            }
        } else {
            logChannel.sendMessageEmbeds(builder.build()).queue()
        }
    }

    fun logCCObtain(obtainer: Long, inventory: Inventory) {
        if (!this::logChannel.isInitialized)
            return

        val cf = EmojiStore.ABILITY["CF"]?.formatted

        val way = when(inventory.ccValidationWay) {
            Inventory.CCValidationWay.SEASONAL_15 -> "15 Unique Seasonal Cards + $cf 150k"
            Inventory.CCValidationWay.COLLABORATION_12 -> "12 Unique Collaboration Cards + $cf 150k"
            Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards"
            Inventory.CCValidationWay.T3_3 -> "3 Unique T3 Cards + $cf 200k"
            Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
            Inventory.CCValidationWay.MANUAL -> "Manual"
            Inventory.CCValidationWay.NONE -> "None"
        }

        val builder = EmbedBuilder()

        builder.setTitle("CC Obtained")
            .setDescription("User <@$obtainer> obtained CC with way of `$way`")
            .setColor(StaticStore.rainbow.random())

        builder.addField("Obtainer", "<@$obtainer> [$obtainer]", false)
        builder.addField("Way", "**$way**", false)
        builder.addField("Obtain Time", "<t:${inventory.ccValidationTime / 1000}:F>", false)

        val cards = inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.CC || pair.first == Inventory.ShareStatus.BOTH }

        if (cards.isNotEmpty()) {
            val cardBuilder = StringBuilder()
            var index = 0

            cards.entries.forEach { (card, pair) ->
                val text = if (pair.second >= 2) {
                    "${card.simpleCardInfo()} x${pair.second}"
                } else {
                    card.simpleCardInfo()
                }

                if (cardBuilder.length + text.length >= MessageEmbed.VALUE_MAX_LENGTH) {
                    builder.addField("Selected Cards${if (index == 0) "" else " $index"}", cardBuilder.toString(), false)

                    index++

                    cardBuilder.clear()
                }

                cardBuilder.append(text).append("\n")
            }
        }

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logECCObtain(obtainer: Long, inventory: Inventory) {
        if (!this::logChannel.isInitialized)
            return

        val way = when(inventory.eccValidationWay) {
            Inventory.ECCValidationWay.SEASONAL_15_COLLAB_12_T4 -> "- 15 Unique Seasonal Cards + 12 Unique Collaboration Cards + 1 T4 Card"
            Inventory.ECCValidationWay.T4_2 -> "- 2 Unique T4 Cards"
            Inventory.ECCValidationWay.SAME_T4_3 -> "- 3 Same T4 Cards"
            Inventory.ECCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
            Inventory.ECCValidationWay.CUSTOM_ROLE -> "Custom Role"
            Inventory.ECCValidationWay.MANUAL -> "Manual"
            Inventory.ECCValidationWay.NONE -> "None"
        }

        val builder = EmbedBuilder()

        builder.setTitle("ECC Obtained")
            .setDescription("User <@$obtainer> obtained ECC with way of `$way`")
            .setColor(StaticStore.rainbow.random())

        builder.addField("Obtainer", "<@$obtainer> [$obtainer]", false)
        builder.addField("Way", "**$way**", false)
        builder.addField("Obtain Time", "<t:${inventory.eccValidationTime / 1000}:F>", false)

        val cards = inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.ECC || pair.first == Inventory.ShareStatus.BOTH }

        if (cards.isNotEmpty()) {
            val cardBuilder = StringBuilder()
            var index = 0

            cards.entries.forEach { (card, pair) ->
                val text = if (pair.second >= 2) {
                    "${card.simpleCardInfo()} x${pair.second}"
                } else {
                    card.simpleCardInfo()
                }

                if (cardBuilder.length + text.length >= MessageEmbed.VALUE_MAX_LENGTH) {
                    builder.addField("Selected Cards${if (index == 0) "" else " $index"}", cardBuilder.toString(), false)

                    index++

                    cardBuilder.clear()
                }

                cardBuilder.append(text).append("\n")
            }
        }

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCCCancel(canceler: Long, manager: Long, inventory: Inventory) {
        if (!this::logChannel.isInitialized)
            return

        val cf = EmojiStore.ABILITY["CF"]?.formatted

        val way = when(inventory.ccValidationWay) {
            Inventory.CCValidationWay.SEASONAL_15 -> "15 Unique Seasonal Cards + $cf 150k"
            Inventory.CCValidationWay.COLLABORATION_12 -> "12 Unique Collaboration Cards + $cf 150k"
            Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards"
            Inventory.CCValidationWay.T3_3 -> "3 Unique T3 Cards + $cf 200k"
            Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
            Inventory.CCValidationWay.MANUAL -> "Manual"
            Inventory.CCValidationWay.NONE -> "None"
        }

        val builder = EmbedBuilder()

        val description = if (manager == -1L) {
            "User <@$canceler> canceled CC with the way of `$way`"
        } else {
            "Manager <@$manager> removed CC from user <@$canceler> with the way of `$way`"
        }

        builder.setTitle("CC Canceled")
            .setDescription(description)
            .setColor(StaticStore.rainbow.random())

        if (manager != -1L) {
            builder.addField("Remover", "<@$manager> [$manager]", false)
        }

        builder.addField("Obtain Time", "<t:${inventory.ccValidationTime / 1000}:F>", false)

        if (inventory.ccValidationReason.isNotBlank()) {
            builder.addField("Validation Reason", inventory.ccValidationReason, false)
        }

        builder.addField("Canceler", "<@$canceler> [$canceler]", false)
        builder.addField("Way", "**$way**", false)

        if (inventory.validationCards.isNotEmpty()) {
            builder.addField("Retrieved Cards", inventory.validationCards.entries.joinToString("\n") { (card, pair) ->
                if (pair.second >= 2) {
                    "${card.simpleCardInfo()} x${pair.second}"
                } else {
                    card.simpleCardInfo()
                }
            }, false)
        }

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logECCCancel(canceler: Long, manager: Long, inventory: Inventory) {
        if (!this::logChannel.isInitialized)
            return

        val way = when(inventory.eccValidationWay) {
            Inventory.ECCValidationWay.SEASONAL_15_COLLAB_12_T4 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards + 1 T4 Card"
            Inventory.ECCValidationWay.T4_2 -> "2 Unique T4 Cards"
            Inventory.ECCValidationWay.SAME_T4_3 -> "3 Same T4 Cards"
            Inventory.ECCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
            Inventory.ECCValidationWay.CUSTOM_ROLE -> "Custom Role"
            Inventory.ECCValidationWay.MANUAL -> "Manual"
            Inventory.ECCValidationWay.NONE -> "None"
        }

        val builder = EmbedBuilder()

        val description = if (manager == -1L) {
            "User <@$canceler> canceled ECC with the way of `$way`"
        } else {
            "Manager <@$manager> removed ECC from user <@$canceler> with the way of `$way`"
        }

        builder.setTitle("ECC Canceled")
            .setDescription(description)
            .setColor(StaticStore.rainbow.random())

        if (manager != -1L) {
            builder.addField("Remover", "<@$manager> [$manager]", false)
        }

        builder.addField("Obtain Time", "<t:${inventory.eccValidationTime / 1000}:F>", false)

        if (inventory.eccValidationReason.isNotBlank()) {
            builder.addField("Validation Reason", inventory.eccValidationReason, false)
        }

        builder.addField("Canceler", "<@$canceler> [$canceler]", false)
        builder.addField("Way", "**$way**", false)

        val cards = inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.ECC }

        if (cards.isNotEmpty()) {
            builder.addField("Retrieved Cards", cards.entries.joinToString("\n") { (card, pair) ->
                if (pair.second >= 2) {
                    "${card.simpleCardInfo()} x${pair.second}"
                } else {
                    card.simpleCardInfo()
                }
            }, false)
        }

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logCCAdd(user: Long, manager: Long, inventory: Inventory) {
        if (!this::logChannel.isInitialized)
            return

        val way = if (inventory.ccValidationWay == Inventory.CCValidationWay.MANUAL) {
            "Manual"
        } else {
            throw IllegalStateException("E/TransactionLogger::logCCAdd - Invalid CC add reason : ${inventory.ccValidationWay}")
        }

        val builder = EmbedBuilder()

        val description = "Manager <@$manager> added CC to user <@$user> with the way of `$way`"

        builder.setTitle("CC Manually Added")
            .setDescription(description)
            .setColor(StaticStore.rainbow.random())

        builder.addField("Manager", "<@$manager> [$manager]", false)
        builder.addField("Obtain Time", "<t:${inventory.ccValidationTime / 1000}:F>", false)

        if (inventory.ccValidationReason.isNotBlank()) {
            builder.addField("Validation Reason", inventory.ccValidationReason, false)
        }

        builder.addField("User", "<@$user> [$user]", false)
        builder.addField("Way", "**$way**", false)

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }

    fun logECCAdd(user: Long, manager: Long, inventory: Inventory) {
        if (!this::logChannel.isInitialized)
            return

        val way = when(inventory.eccValidationWay) {
            Inventory.ECCValidationWay.CUSTOM_ROLE -> "Custom Role"
            Inventory.ECCValidationWay.MANUAL -> "Manual"
            else -> throw IllegalStateException("E/TransactionLogger::logECCAdd - Invalid ECC add reason : ${inventory.eccValidationWay}")
        }

        val builder = EmbedBuilder()

        val description = "Manager <@$manager> added ECC to user <@$user> with the way of `$way`"

        builder.setTitle("ECC Manually Added")
            .setDescription(description)
            .setColor(StaticStore.rainbow.random())

        builder.addField("Manager", "<@$manager> [$manager]", false)
        builder.addField("Obtain Time", "<t:${inventory.eccValidationTime / 1000}:F>", false)

        if (inventory.eccValidationReason.isNotBlank()) {
            builder.addField("Validation Reason", inventory.eccValidationReason, false)
        }

        if (inventory.eccValidationRoleID != 0L) {
            builder.addField("Validation Role", "<@&${inventory.eccValidationRoleID}> [${inventory.eccValidationRoleID}]", false)
        }

        builder.addField("User", "<@$user> [$user]", false)
        builder.addField("Way", "**$way**", false)

        logChannel.sendMessageEmbeds(builder.build()).queue()
    }
}