package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max

class Report : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val time = CardData.getUnixEpochTime()

        val sessionNumber = getSessionNumber(loader.content)

        val sessions = LogSession.gatherPreviousSessions(time, sessionNumber)

        val memberSize = sessions.flatMap { session -> session.activeMembers }.toSet().size

        val totalCatFoodPack = sessions.sumOf { session ->
            session.catFoodPack.map { (_, catFood) -> catFood }.sum()
        }

        val totalPlatinumShardPack = sessions.sumOf { session ->
            session.platinumShardPack.map { (_, platinumShard) -> platinumShard }.sum()
        }

        val totalGeneratedCards = sessions.sumOf { session ->
            session.generatedCards.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalRemovedSalvageCards = sessions.sumOf { session ->
            session.removedCardsSalvage.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalRemovedManagerCards = sessions.sumOf { session ->
            session.removedCardsManager.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalRemovedCards = sessions.sumOf { session ->
            session.removedCards.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCards = totalGeneratedCards - totalRemovedCards

        val verb = if (totalCards < 0) {
            "removed"
        } else {
            "generated"
        }

        val afterVerb = if (totalCards < 0) {
            "removed from"
        } else {
            "added into"
        }

        val totalShardSalvageT1 = sessions.sumOf { session ->
            session.shardSalvageT1.map { (_, amount) -> amount }.sum()
        }

        val totalShardSalvageT2Regular = sessions.sumOf { session ->
            session.shardSalvageT2Regular.map { (_, amount) -> amount }.sum()
        }

        val totalShardSalvageT2Seasonal = sessions.sumOf { session ->
            session.shardSalvageT2Seasonal.map { (_, amount) -> amount }.sum()
        }

        val totalShardSalvageT2Collaboration = sessions.sumOf { session ->
            session.shardSalvageT2Collaboration.map { (_, amount) -> amount }.sum()
        }

        val totalShardSalvageT3 = sessions.sumOf { session ->
            session.shardSalvageT3.map { (_, amount) -> amount }.sum()
        }

        val totalCardSalvageT1 = sessions.sumOf { session ->
            session.shardSalvageCardT1.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardSalvageT2Regular = sessions.sumOf { session ->
            session.shardSalvageCardT2Regular.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardSalvageT2Seasonal = sessions.sumOf { session ->
            session.shardSalvageCardT2Seasonal.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardSalvageT2Collaboration = sessions.sumOf { session ->
            session.shardSalvageCardT2Collaboration.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardSalvageT3 = sessions.sumOf { session ->
            session.shardSalvageCardT3.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalShards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory -> inventory.platinumShard }
        val totalCatFoods = CardData.inventories.entries.sumOf { (_, inventory) -> inventory.catFoods }

        val totalShardCraftT2Regular = sessions.sumOf { session ->
            session.shardCraftT2Regular.map { (_, amount) -> amount }.sum()
        }

        val totalShardCraftT2Seasonal = sessions.sumOf { session ->
            session.shardCraftT2Seasonal.map { (_, amount) -> amount }.sum()
        }

        val totalShardCraftT2Collaboration = sessions.sumOf { session ->
            session.shardCraftT2Collaboration.map { (_, amount) -> amount }.sum()
        }

        val totalShardCraftT3 = sessions.sumOf { session ->
            session.shardCraftT3.map { (_, amount) -> amount }.sum()
        }

        val totalShardCraftT4 = sessions.sumOf { session ->
            session.shardCraftT4.map { (_, amount) -> amount }.sum()
        }

        val totalCardCraftT2Regular = sessions.sumOf { session ->
            session.shardCraftCardT2Regular.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardCraftT2Seasonal = sessions.sumOf { session ->
            session.shardCraftCardT2Seasonal.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardCraftT2Collaboration = sessions.sumOf { session ->
            session.shardCraftCardT2Collaboration.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardCraftT3 = sessions.sumOf { session ->
            session.shardCraftCardT3.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCardCraftT4 = sessions.sumOf { session ->
            session.shardCraftCardT4.map { (_, cardMap) -> cardMap.map { (_, amount) -> amount }.sum() }.sum()
        }

        val totalCraftedCards = totalCardCraftT2Regular + totalCardCraftT2Seasonal + totalCardCraftT2Collaboration + totalCardCraftT3 + totalCardCraftT4

        val totalTransferredCatFoods = sessions.sumOf { session -> session.catFoodTradeSum }

        val totalTradeDone = sessions.sumOf { session -> session.tradeDone }

        val totalT1Cards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory ->
            inventory.cards.filter { (c, _) -> c.tier == CardData.Tier.COMMON }.map { (_, amount) -> amount }.sum() +
            inventory.favorites.filter { (c, _) -> c.tier == CardData.Tier.COMMON }.map { (_, amount) -> amount }.sum()
        }

        val totalT2RegularCards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory ->
            inventory.cards.filter { (c, _) -> c.isRegularUncommon() }.map { (_, amount) -> amount }.sum() +
            inventory.favorites.filter { (c, _) -> c.isRegularUncommon() }.map { (_, amount) -> amount }.sum()
        }

        val totalT2SeasonalCards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory ->
            inventory.cards.filter { (c, _) -> c.isSeasonalUncommon() }.map { (_, amount) -> amount }.sum() +
            inventory.favorites.filter { (c, _) -> c.isSeasonalUncommon() }.map { (_, amount) -> amount }.sum()
        }

        val totalT2CollaborationCards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory ->
            inventory.cards.filter { (c, _) -> c.isCollaborationUncommon() }.map { (_, amount) -> amount }.sum() +
            inventory.favorites.filter { (c, _) -> c.isCollaborationUncommon() }.map { (_, amount) -> amount }.sum()
        }

        val totalT3Cards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory ->
            inventory.cards.filter { (c, _) -> c.tier == CardData.Tier.ULTRA }.map { (_, amount) -> amount }.sum() +
            inventory.favorites.filter { (c, _) -> c.tier == CardData.Tier.ULTRA }.map { (_, amount) -> amount }.sum()
        }

        val totalT4Cards = CardData.inventories.map { (_, inventory) -> inventory }.sumOf { inventory ->
            inventory.cards.filter { (c, _) -> c.tier == CardData.Tier.LEGEND }.map { (_, amount) -> amount }.sum() +
            inventory.favorites.filter { (c, _) -> c.tier == CardData.Tier.LEGEND }.map { (_, amount) -> amount }.sum()
        }

        val totalCatFoodSlotMachineInput = sessions.sumOf { s ->
            s.catFoodSlotMachineInput.entries.sumOf { e -> e.value }
        }
        val totalCatFoodSlotMachineReward = sessions.sumOf { s ->
            s.catFoodSlotMachineReward.entries.sumOf { e -> e.value }
        }

        val totalPlatinumShardSlotMachineInput = sessions.sumOf { s ->
            s.platinumShardSlotMachineInput.entries.sumOf { e -> e.value }
        }
        val totalPlatinumShardSlotMachineReward = sessions.sumOf { s ->
            s.platinumShardSlotMachineReward.entries.sumOf { e -> e.value }
        }
        
        val shard = EmojiStore.ABILITY["SHARD"]?.formatted
        val cf = EmojiStore.ABILITY["CF"]?.formatted

        if (loader.content.contains("-f")) {
            loader.guild.loadMembers().onSuccess { allMembers ->
                val comparator: (Map.Entry<Card, Long>?, Map.Entry<Card, Long>?) -> Int = comparator@ { c0, c1 ->
                    if (c0 == null && c1 == null)
                        return@comparator -1

                    if (c0 == null)
                        return@comparator 1

                    if (c1 == null)
                        return@comparator -1

                    if (c0.key.tier != c1.key.tier) {
                        return@comparator -c0.key.tier.ordinal.compareTo(c1.key.tier.ordinal)
                    } else if (c0.value != c1.value) {
                        return@comparator -c0.value.compareTo(c1.value)
                    } else {
                        return@comparator -c0.key.id.compareTo(c1.key.id)
                    }
                }

                val memberIds = sessions.flatMap { session -> session.activeMembers }.toSet()

                val activeMembers = allMembers.filter { member -> member.idLong in memberIds }.sortedBy { member -> member.effectiveName }
                val existingMembers = allMembers.filter { member -> CardData.inventories.containsKey(member.idLong) }

                val catFoodPackMap = HashMap<Long, Long>()
                val platinumShardPackMap = HashMap<Long, Long>()

                val generatedCardsMap = HashMap<Card, Long>()

                val removedSalvageCardsMap = HashMap<Card, Long>()
                val removedManagerCardsMap = HashMap<Card, Long>()

                val removedCardsMap = HashMap<Card, Long>()

                val shardSalvageT1Map = HashMap<Long, Long>()
                val shardSalvageT2RegularMap = HashMap<Long, Long>()
                val shardSalvageT2SeasonalMap = HashMap<Long, Long>()
                val shardSalvageT2CollaborationMap = HashMap<Long, Long>()
                val shardSalvageT3Map = HashMap<Long, Long>()

                val cardSalvageT1Map = HashMap<Long, HashMap<Card, Long>>()
                val cardSalvageT2RegularMap = HashMap<Long, HashMap<Card, Long>>()
                val cardSalvageT2SeasonalMap = HashMap<Long, HashMap<Card, Long>>()
                val cardSalvageT2CollaborationMap = HashMap<Long, HashMap<Card, Long>>()
                val cardSalvageT3Map = HashMap<Long, HashMap<Card, Long>>()

                val shardCraftT2RegularMap = HashMap<Long, Long>()
                val shardCraftT2SeasonalMap = HashMap<Long, Long>()
                val shardCraftT2CollaborationMap = HashMap<Long, Long>()
                val shardCraftT3Map = HashMap<Long, Long>()
                val shardCraftT4Map = HashMap<Long, Long>()

                val cardCraftT2RegularMap = HashMap<Long, HashMap<Card, Long>>()
                val cardCraftT2SeasonalMap = HashMap<Long, HashMap<Card, Long>>()
                val cardCraftT2CollaborationMap = HashMap<Long, HashMap<Card, Long>>()
                val cardCraftT3Map = HashMap<Long, HashMap<Card, Long>>()
                val cardCraftT4Map = HashMap<Long, HashMap<Card, Long>>()

                val craftedCardsMap = HashMap<Card, Long>()

                val cardsT1Map = HashMap<Long, HashMap<Card, Long>>()
                val cardsT2RegularMap = HashMap<Long, HashMap<Card, Long>>()
                val cardsT2SeasonalMap = HashMap<Long, HashMap<Card, Long>>()
                val cardsT2CollaborationMap = HashMap<Long, HashMap<Card, Long>>()
                val cardsT3Map = HashMap<Long, HashMap<Card, Long>>()
                val cardsT4Map = HashMap<Long, HashMap<Card, Long>>()

                val catFoodSlotMachineInput = HashMap<Long, Long>()
                val catFoodSlotMachineReward = HashMap<Long, Long>()

                val platinumShardSlotMachineInput = HashMap<Long, Long>()
                val platinumShardSlotMachineReward = HashMap<Long, Long>()

                sessions.forEach { session ->
                    session.catFoodPack.forEach { (id, amount) ->
                        catFoodPackMap[id] = (catFoodPackMap[id] ?: 0) + amount
                    }

                    session.platinumShardPack.forEach { (id, amount) ->
                        platinumShardPackMap[id] = (platinumShardPackMap[id] ?: 0) + amount
                    }

                    session.generatedCards.map { (_, cardMap) -> cardMap }.forEach { cardMap ->
                        cardMap.forEach { (card, amount) ->
                            generatedCardsMap[card] = (generatedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.removedCardsSalvage.map { (_, cardMap) -> cardMap }.forEach { cardMap ->
                        cardMap.forEach { (card, amount) ->
                            removedSalvageCardsMap[card] = (removedSalvageCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.removedCardsManager.map { (_, cardMap) -> cardMap }.forEach { cardMap ->
                        cardMap.forEach { (card, amount) ->
                            removedManagerCardsMap[card] = (removedManagerCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.removedCards.map { (_, cardMap) -> cardMap }.forEach { cardMap ->
                        cardMap.forEach { (card, amount) ->
                            removedCardsMap[card] = (removedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.shardSalvageT1.forEach { (id, amount) ->
                        shardSalvageT1Map[id] = (shardSalvageT1Map[id] ?: 0) + amount
                    }

                    session.shardSalvageT2Regular.forEach { (id, amount) ->
                        shardSalvageT2RegularMap[id] = (shardSalvageT2RegularMap[id] ?: 0) + amount
                    }

                    session.shardSalvageT2Seasonal.forEach { (id, amount) ->
                        shardSalvageT2SeasonalMap[id] = (shardSalvageT2SeasonalMap[id] ?: 0) + amount
                    }

                    session.shardSalvageT2Collaboration.forEach { (id, amount) ->
                        shardSalvageT2CollaborationMap[id] = (shardSalvageT2CollaborationMap[id] ?: 0) + amount
                    }

                    session.shardSalvageT3.forEach { (id, amount) ->
                        shardSalvageT3Map[id] = (shardSalvageT3Map[id] ?: 0) + amount
                    }

                    session.shardSalvageCardT1.forEach { (id, cardMap) ->
                        val map = cardSalvageT1Map.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardSalvageCardT2Regular.forEach { (id, cardMap) ->
                        val map = cardSalvageT2RegularMap.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardSalvageCardT2Seasonal.forEach { (id, cardMap) ->
                        val map = cardSalvageT2SeasonalMap.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardSalvageCardT2Collaboration.forEach { (id, cardMap) ->
                        val map = cardSalvageT2CollaborationMap.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardSalvageCardT3.forEach { (id, cardMap) ->
                        val map = cardSalvageT3Map.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftT2Regular.forEach { (id, amount) ->
                        shardCraftT2RegularMap[id] = (shardCraftT2RegularMap[id] ?: 0) + amount
                    }

                    session.shardCraftT2Seasonal.forEach { (id, amount) ->
                        shardCraftT2SeasonalMap[id] = (shardCraftT2SeasonalMap[id] ?: 0) + amount
                    }

                    session.shardCraftT2Collaboration.forEach { (id, amount) ->
                        shardCraftT2CollaborationMap[id] = (shardCraftT2CollaborationMap[id] ?: 0) + amount
                    }

                    session.shardCraftT3.forEach { (id, amount) ->
                        shardCraftT3Map[id] = (shardCraftT3Map[id] ?: 0) + amount
                    }

                    session.shardCraftT4.forEach { (id, amount) ->
                        shardCraftT4Map[id] = (shardCraftT4Map[id] ?: 0) + amount
                    }

                    session.shardCraftCardT2Regular.forEach { (id, cardMap) ->
                        val map = cardCraftT2RegularMap.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT2Seasonal.forEach { (id, cardMap) ->
                        val map = cardCraftT2SeasonalMap.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT2Collaboration.forEach { (id, cardMap) ->
                        val map = cardCraftT2CollaborationMap.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT3.forEach { (id, cardMap) ->
                        val map = cardCraftT3Map.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT4.forEach { (id, cardMap) ->
                        val map = cardCraftT4Map.computeIfAbsent(id) { HashMap() }

                        cardMap.forEach { (card, amount) ->
                            map[card] = (map[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT2Regular.forEach { (_, cardMap) ->
                        cardMap.forEach { (card, amount) ->
                            craftedCardsMap[card] = (craftedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT2Seasonal.forEach { (_, cardMap) ->
                        cardMap.forEach { (card, amount) ->
                            craftedCardsMap[card] = (craftedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT2Collaboration.forEach { (_, cardMap) ->
                        cardMap.forEach { (card, amount) ->
                            craftedCardsMap[card] = (craftedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT3.forEach { (_, cardMap) ->
                        cardMap.forEach { (card, amount) ->
                            craftedCardsMap[card] = (craftedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.shardCraftCardT4.forEach { (_, cardMap) ->
                        cardMap.forEach { (card, amount) ->
                            craftedCardsMap[card] = (craftedCardsMap[card] ?: 0) + amount
                        }
                    }

                    session.catFoodSlotMachineInput.forEach { (id, amount) ->
                        catFoodSlotMachineInput[id] = (catFoodSlotMachineInput[id] ?: 0) + amount
                    }

                    session.catFoodSlotMachineReward.forEach { (id, amount) ->
                        catFoodSlotMachineReward[id] = (catFoodSlotMachineReward[id] ?: 0) + amount
                    }

                    session.platinumShardSlotMachineInput.forEach { (id, amount) ->
                        platinumShardSlotMachineInput[id] = (platinumShardSlotMachineInput[id] ?: 0) + amount
                    }

                    session.platinumShardSlotMachineReward.forEach { (id, amount) ->
                        platinumShardSlotMachineReward[id] = (platinumShardSlotMachineReward[id] ?: 0) + amount
                    }
                }

                CardData.inventories.filter { (_, inventory) -> inventory.cards.isNotEmpty() || inventory.favorites.isNotEmpty() }.forEach { (id, inventory) ->
                    val t1CardMap = cardsT1Map.computeIfAbsent(id) { HashMap() }
                    val t2RegularCardMap = cardsT2RegularMap.computeIfAbsent(id) { HashMap() }
                    val t2SeasonalCardMap = cardsT2SeasonalMap.computeIfAbsent(id) { HashMap() }
                    val t2CollaborationCardMap = cardsT2CollaborationMap.computeIfAbsent(id) { HashMap() }
                    val t3CardMap = cardsT3Map.computeIfAbsent(id) { HashMap() }
                    val t4CardMap = cardsT4Map.computeIfAbsent(id) { HashMap() }

                    inventory.cards.forEach { (card, amount) ->
                        when {
                            card.tier == CardData.Tier.COMMON -> t1CardMap[card] = (t1CardMap[card] ?: 0) + amount
                            card.tier == CardData.Tier.ULTRA -> t3CardMap[card] = (t3CardMap[card] ?: 0 ) + amount
                            card.tier == CardData.Tier.LEGEND -> t4CardMap[card] = (t4CardMap[card] ?: 0 ) + amount
                            card.isSeasonalUncommon() -> t2SeasonalCardMap[card] = (t2SeasonalCardMap[card] ?: 0) + amount
                            card.isCollaborationUncommon() -> t2CollaborationCardMap[card] = (t2CollaborationCardMap[card] ?: 0) + amount
                            card.isRegularUncommon() -> t2RegularCardMap[card] = (t2RegularCardMap[card] ?: 0) + amount
                        }
                    }

                    inventory.favorites.forEach { (card, amount) ->
                        when {
                            card.tier == CardData.Tier.COMMON -> t1CardMap[card] = (t1CardMap[card] ?: 0) + amount
                            card.tier == CardData.Tier.ULTRA -> t3CardMap[card] = (t3CardMap[card] ?: 0 ) + amount
                            card.tier == CardData.Tier.LEGEND -> t4CardMap[card] = (t4CardMap[card] ?: 0 ) + amount
                            card.isSeasonalUncommon() -> t2SeasonalCardMap[card] = (t2SeasonalCardMap[card] ?: 0) + amount
                            card.isCollaborationUncommon() -> t2CollaborationCardMap[card] = (t2CollaborationCardMap[card] ?: 0) + amount
                            card.isRegularUncommon() -> t2RegularCardMap[card] = (t2RegularCardMap[card] ?: 0) + amount
                        }
                    }
                }

                val reporter = StringBuilder("Gathered ")
                    .append(sessions.size)
                    .append(" log sessions in total before ")
                    .append(LogSession.globalFormat.format(time))
                    .append("\n\n========== REPORT ==========\n\n")
                    .append(memberSize)
                    .append(" members participated in BCTC\n\n===== Users who participated BCTC =====\n\n")

                activeMembers.forEachIndexed { index, member ->
                    reporter.append(index + 1)
                        .append(". ")
                        .append(member.effectiveName)
                        .append(" [")
                        .append(member.id).append("]\n")
                }

                reporter.append("\n=======================================\n\nOut of these people : \n\n")
                    .append(totalCatFoodPack)
                    .append(" cf have been consumed for generating pack\n\n===== CF consumed for each user =====\n\n")

                var i = 1

                catFoodPackMap.entries.sortedByDescending { e -> e.value }.forEach { (id, amount) ->
                    val member = existingMembers.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("] : ").append(amount).append("\n")
                    } else {
                        reporter.append(i).append(". UNKNOWN [").append(id).append("] : ").append(amount).append("\n")
                    }

                    i++
                }

                reporter.append("\n=====================================\n\n")
                    .append(totalPlatinumShardPack)
                    .append(" shards have been consumed for generating pack\n\n===== PS consumed for each user =====\n\n")

                i = 1

                platinumShardPackMap.entries.sortedByDescending { e -> e.value }.forEach { (id, amount) ->
                    val member = existingMembers.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("] : ").append(amount).append("\n")
                    } else {
                        reporter.append(i).append(". UNKNOWN [").append(id).append("] : ").append(amount).append("\n")
                    }

                    i++
                }


                reporter.append("\n=====================================\n\n")
                    .append(totalGeneratedCards)
                    .append(" cards have been generated, added into economy\n\n===== Cards generated =====\n\n")

                i = 1

                generatedCardsMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                    reporter.append(i).append(". ").append(card.cardInfo())

                    if (amount > 2)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")

                    i++
                }

                reporter.append("\n===========================\n\n")
                    .append(totalRemovedSalvageCards)
                    .append(" cards have been removed from salvaging\n\n===== Cards removed from salvage =====\n\n")

                i = 1

                removedSalvageCardsMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                    reporter.append(i).append(". ").append(card.cardInfo())

                    if (amount > 2)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")

                    i++
                }

                reporter.append("\n======================================\n\n")
                    .append(totalRemovedManagerCards)
                    .append(" cards have been removed by managers\n\n===== Cards removed by managers =====\n\n")

                i = 1

                removedManagerCardsMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                    reporter.append(i).append(". ").append(card.cardInfo())

                    if (amount > 2)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")

                    i++
                }

                reporter.append("\n=====================================\n\nIn total ")
                    .append(totalRemovedCards)
                    .append(" cards have been removed from economy\n\n===== Cards removed =====\n\n")

                i = 1

                removedCardsMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                    reporter.append(i).append(". ").append(card.cardInfo())

                    if (amount > 2)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")

                    i++
                }

                reporter.append("\n=========================\n\nSumming all, ")
                    .append(abs(totalCards))
                    .append(" cards have been ")
                    .append(verb)
                    .append(", ")
                    .append(afterVerb)
                    .append(" economy\n\n")

                for (j in 0 until 5) {
                    val shards = when(j) {
                        0 -> totalShardSalvageT1
                        1 -> totalShardSalvageT2Regular
                        2 -> totalShardSalvageT2Seasonal
                        3 -> totalShardSalvageT2Collaboration
                        else -> totalShardSalvageT3
                    }

                    val cards = when(j) {
                        0 -> totalCardSalvageT1
                        1 -> totalCardSalvageT2Regular
                        2 -> totalCardSalvageT2Seasonal
                        3 -> totalCardSalvageT2Collaboration
                        else -> totalCardSalvageT3
                    }

                    val cardType = when(j) {
                        0 -> "T1 [Common]"
                        1 -> "Regular T2 [Uncommon]"
                        2 -> "Seasonal T2 [Uncommon]"
                        3 -> "Collaboration T2 [Uncommon]"
                        else -> "T3 [Ultra Rare (Exclusives)]"
                    }

                    val shardMap = when(j) {
                        0 -> shardSalvageT1Map
                        1 -> shardSalvageT2RegularMap
                        2 -> shardSalvageT2SeasonalMap
                        3 -> shardSalvageT2CollaborationMap
                        else -> shardSalvageT3Map
                    }

                    val cardMap = when(j) {
                        0 -> cardSalvageT1Map
                        1 -> cardSalvageT2RegularMap
                        2 -> cardSalvageT2SeasonalMap
                        3 -> cardSalvageT2CollaborationMap
                        else -> cardSalvageT3Map
                    }

                    val shardTitle = "===== PS data for each user, gotten from $cardType ====="
                    val cardTitle = "===== Cards used from salvaging $cardType ====="

                    reporter.append(shards)
                        .append(" ps have been generated from")
                        .append(cardType)
                        .append(" cards, ")
                        .append(cards)
                        .append(" cards have been consumed\n\n")
                        .append(shardTitle)
                        .append("\n\n")

                    i = 1

                    shardMap.entries.sortedByDescending { e -> e.value }.forEach { (id, amount) ->
                        val member = existingMembers.find { member -> member.idLong == id }

                        if (member != null) {
                            reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("] : ").append(amount).append("\n")
                        } else {
                            reporter.append(i).append(". UNKNOWN [").append(id).append("] : ").append(amount).append("\n")
                        }

                        i++
                    }

                    reporter.append("\n")
                        .append("=".repeat(shardTitle.length))
                        .append("\n\n")
                        .append(cardTitle)
                        .append("\n\n")

                    i = 1

                    cardMap.entries.sortedByDescending { e -> e.value.size }.forEach { (id, cardMap) ->
                        val member = existingMembers.find { member -> member.idLong == id }

                        if (member != null) {
                            reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("]\n\n")
                        } else {
                            reporter.append(i).append(". UNKNOWN [").append(id).append("]\n\n")
                        }

                        var k = 1

                        cardMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                            reporter.append("\t").append(k).append(". ").append(card.cardInfo())

                            if (amount > 2)
                                reporter.append(" x").append(amount)

                            reporter.append("\n")

                            k++
                        }

                        reporter.append("\n")

                        i++
                    }

                    reporter.append("\n").append("=".repeat(cardTitle.length)).append("\n\n")
                }

                reporter.append("Total ")
                    .append(totalShards)
                    .append(" ps in current inventories\n\n===== PS data for each user =====\n\n")

                i = 1

                CardData.inventories.entries.filter { e -> e.value.platinumShard > 0 }.sortedByDescending { e -> e.value.platinumShard }.forEach { (id, inventory) ->
                    val member = existingMembers.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("] : ").append(inventory.platinumShard).append("\n")
                    } else {
                        reporter.append(i).append(". UNKNOWN [").append(id).append("] : ").append(inventory.platinumShard).append("\n")
                    }

                    i++
                }

                reporter.append("\n=================================\n\nTotal ")
                    .append(totalCatFoods)
                    .append(" cf in current inventories\n\n===== CF data for each user =====\n\n")

                i = 1

                CardData.inventories.entries.filter { e -> e.value.catFoods > 0 }.sortedByDescending { e -> e.value.catFoods }.forEach { (id, inventory) ->
                    val member = existingMembers.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("] : ").append(inventory.catFoods).append("\n")
                    } else {
                        reporter.append(i).append(". UNKNOWN [").append(id).append("] : ").append(inventory.platinumShard).append("\n")
                    }

                    i++
                }

                for (j in 0 until 5) {
                    val shards = when(j) {
                        0 -> totalShardCraftT2Regular
                        1 -> totalShardCraftT2Seasonal
                        2 -> totalShardCraftT2Collaboration
                        3 -> totalShardCraftT3
                        else -> totalShardCraftT4
                    }

                    val cards = when(j) {
                        0 -> totalCardCraftT2Regular
                        1 -> totalCardCraftT2Seasonal
                        2 -> totalCardCraftT2Collaboration
                        3 -> totalCardCraftT3
                        else -> totalCardCraftT4
                    }

                    val cardType = when(j) {
                        0 -> "Regular T2 [Uncommon]"
                        1 -> "Seasonal T2 [Uncommon]"
                        2 -> "Collaboration T2 [Uncommon]"
                        3 -> "T3 [Ultra Rare (Exclusives)]"
                        else -> "T4 [Legend Rare]"
                    }

                    val shardMap = when(j) {
                        0 -> shardCraftT2RegularMap
                        1 -> shardCraftT2SeasonalMap
                        2 -> shardCraftT2CollaborationMap
                        3 -> shardCraftT3Map
                        else -> shardCraftT4Map
                    }

                    val cardMap = when(j) {
                        0 -> cardCraftT2RegularMap
                        1 -> cardCraftT2SeasonalMap
                        2 -> cardCraftT2CollaborationMap
                        3 -> cardCraftT3Map
                        else -> cardCraftT4Map
                    }

                    val cardTitle = "===== $cardType card data for each user, gotten from crafting ====="
                    val shardTitle = "===== PS used for crafting ====="

                    reporter.append(cards)
                        .append(" ")
                        .append(cardType)
                        .append(" cards have been crafted, and ")
                        .append(shards)
                        .append(" ps have been consumed\n\n")
                        .append(shardTitle)
                        .append("\n\n")

                    i = 1

                    shardMap.entries.sortedByDescending { e -> e.value }.forEach { (id, amount) ->
                        val member = existingMembers.find { member -> member.idLong == id }

                        if (member != null) {
                            reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("] : ").append(amount).append("\n")
                        } else {
                            reporter.append(i).append(". UNKNOWN [").append(id).append("] : ").append(amount).append("\n")
                        }

                        i++
                    }

                    reporter.append("\n")
                        .append("=".repeat(shardTitle.length))
                        .append("\n\n")
                        .append(cardTitle)
                        .append("\n\n")

                    i = 1

                    cardMap.entries.sortedByDescending { e -> e.value.size }.forEach { (id, cardMap) ->
                        val member = existingMembers.find { member -> member.idLong == id }

                        if (member != null) {
                            reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("]\n\n")
                        } else {
                            reporter.append(i).append(". UNKNOWN [").append(id).append("]\n\n")
                        }

                        var k = 1

                        cardMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                            reporter.append("\t").append(k).append(". ").append(card.cardInfo())

                            if (amount > 2)
                                reporter.append(" x").append(amount)

                            reporter.append("\n")

                            k++
                        }

                        reporter.append("\n")

                        i++
                    }

                    reporter.append("\n").append("=".repeat(cardTitle.length)).append("\n\n")
                }

                reporter.append("Total ")
                    .append(totalCraftedCards)
                    .append(" cards have been crafted\n\n===== Card data generated by crafting =====\n\n")

                i = 1

                craftedCardsMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                    reporter.append(i).append(". ").append(card.cardInfo())

                    if (amount > 2)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")

                    i++
                }

                reporter.append("\n===========================================\n\n")
                    .append(totalTransferredCatFoods)
                    .append(" have been transferred among users via trading\n")
                    .append(totalTradeDone)
                    .append(" trades were done during this time\n\n")

                for (j in 0 until 6) {
                    val cardType = when(j) {
                        0 -> "Tier 1 [Common]"
                        1 -> "Regular Tier 2 [Uncommon]"
                        2 -> "Seasonal Tier 2 [Uncommon]"
                        3 -> "Collaboration Tier 2 [Uncommon]"
                        4 -> "Tier 3 [Ultra Rare (Exclusives)]"
                        else -> "Tier 4 [Legend Rare]"
                    }

                    val cards = when(j) {
                        0 -> totalT1Cards
                        1 -> totalT2RegularCards
                        2 -> totalT2SeasonalCards
                        3 -> totalT2CollaborationCards
                        4 -> totalT3Cards
                        else -> totalT4Cards
                    }

                    val totalCardMap = when(j) {
                        0 -> cardsT1Map
                        1 -> cardsT2RegularMap
                        2 -> cardsT2SeasonalMap
                        3 -> cardsT2CollaborationMap
                        4 -> cardsT3Map
                        else -> cardsT4Map
                    }

                    val title = "===== $cardType card data for each user ====="

                    reporter.append("There are ")
                        .append(cards)
                        .append(" ")
                        .append(cardType)
                        .append(" cards\n\n")
                        .append(title)
                        .append("\n\n")

                    i = 1

                    totalCardMap.entries.filter { (_, card) -> card.isNotEmpty() }.sortedByDescending { e -> e.value.size }.forEach { (id, cardMap) ->
                        val member = existingMembers.find { member -> member.idLong == id }

                        if (member != null) {
                            reporter.append(i).append(". ").append(member.effectiveName).append(" [").append(id).append("]\n\n")
                        } else {
                            reporter.append(i).append(". UNKNOWN [").append(id).append("]\n\n")
                        }

                        var k = 1

                        cardMap.entries.sortedWith(comparator).forEach { (card, amount) ->
                            reporter.append("\t").append(k).append(". ").append(card.cardInfo())

                            if (amount > 2)
                                reporter.append(" x").append(amount)

                            reporter.append("\n")

                            k++
                        }

                        reporter.append("\n")

                        i++
                    }

                    reporter.append("\n").append("=".repeat(title.length)).append("\n\n")
                }

                reporter.append("===== Cat food data spent for slot machine =====\n\n")
                    .append("Users have spent ").append(totalCatFoodSlotMachineInput).append(" cat foods for rolling slot machine\n\n")

                catFoodSlotMachineInput.entries.sortedBy { e -> e.value }.forEachIndexed { index, (u, amount) ->
                    val user = existingMembers.find { m -> m.idLong == u }

                    if (user == null) {
                        reporter.append(index + 1).append(". UNKNOWN [").append(u).append("] : ").append(amount)
                    } else {
                        reporter.append(index + 1).append(". ").append(user.effectiveName).append(" [").append(u).append("] : ").append(amount)
                    }

                    reporter.append("\n")
                }

                reporter.append("\n================================================\n\n===== Cat food data gotten from slot machine =====\n\n")
                    .append("User have gotten ").append(totalCatFoodSlotMachineReward).append(" cat foods from slot machine\n\n")

                catFoodSlotMachineReward.entries.sortedBy { e -> e.value }.forEachIndexed { index, (u, amount) ->
                    val user = existingMembers.find { m -> m.idLong == u }

                    if (user == null) {
                        reporter.append(index + 1).append(". UNKNOWN [").append(u).append("] : ").append(amount)
                    } else {
                        reporter.append(index + 1).append(". ").append(user.effectiveName).append(" [").append(u).append("] : ").append(amount)
                    }

                    reporter.append("\n")
                }

                reporter.append("\n==================================================\n\n===== Platinum shard data spent for slot machine =====\n\n")
                    .append("User have spent ").append(totalPlatinumShardSlotMachineInput).append(" platinum shards for slot machine\n\n")

                platinumShardSlotMachineInput.entries.sortedBy { e -> e.value }.forEachIndexed { index, (u, amount) ->
                    val user = existingMembers.find { m -> m.idLong == u }

                    if (user == null) {
                        reporter.append(index + 1).append(". UNKNOWN [").append(u).append("] : ").append(amount)
                    } else {
                        reporter.append(index + 1).append(". ").append(user.effectiveName).append(" [").append(u).append("] : ").append(amount)
                    }

                    reporter.append("\n")
                }

                reporter.append("\n======================================================\n\n===== Platinum shard data gotten from slot machine =====\n\n")
                    .append("User have gotten ").append(totalPlatinumShardSlotMachineReward).append(" platinum shards from slot machine\n\n")

                platinumShardSlotMachineReward.entries.sortedBy { e -> e.value }.forEachIndexed { index, (u, amount) ->
                    val user = existingMembers.find { m -> m.idLong == u }

                    if (user == null) {
                        reporter.append(index + 1).append(". UNKNOWN [").append(u).append("] : ").append(amount)
                    } else {
                        reporter.append(index + 1).append(". ").append(user.effectiveName).append(" [").append(u).append("] : ").append(amount)
                    }

                    reporter.append("\n")
                }

                reporter.append("\n========================================================\n\n============================")

                val folder = File("./temp")

                if (!folder.exists() && !folder.mkdirs()) {
                    StaticStore.logger.uploadLog("W/Report::doSomething - Failed to generate folder : ${folder.absolutePath}")

                    return@onSuccess
                }

                val file = StaticStore.generateTempFile(folder, "log", ".txt", false)

                val writer = BufferedWriter(FileWriter(file, StandardCharsets.UTF_8))

                writer.write(reporter.toString())

                writer.close()

                sendMessageWithFile(ch, "Uploading full report log", file, "report.txt", loader.message)
            }
        } else {
            val content = "Gathered ${sessions.size} log sessions in total before ${LogSession.globalFormat.format(time)}\n" +
                    "\n" +
                    "========== REPORT ==========\n" +
                    "\n" +
                    "$memberSize members participated in BCTC\n" +
                    "\n" +
                    "Out of these people :\n" +
                    "\n" +
                    "$totalCatFoodPack $cf have been consumed for generating pack\n" +
                    "$totalPlatinumShardPack $shard have been consumed for generating pack\n" +
                    "\n" +
                    "$totalGeneratedCards cards have been generated, added into economy\n" +
                    "\n" +
                    "$totalRemovedSalvageCards cards have been removed from salvaging\n" +
                    "$totalRemovedManagerCards cards have been removed by managers\n" +
                    "\n" +
                    "In total, $totalRemovedCards cards have been removed from economy\n" +
                    "\n" +
                    "Summing all, ${abs(totalCards)} cards have been $verb, $afterVerb economy\n" +
                    "\n" +
                    "$totalShardSalvageT1 $shard have been generated from T1, and $totalCardSalvageT1 cards have been consumed\n" +
                    "$totalShardSalvageT2Regular $shard have been generated from Regular T2, and $totalCardSalvageT2Regular cards have been consumed\n" +
                    "$totalShardSalvageT2Seasonal $shard have been generated from Seasonal T2, and $totalCardSalvageT2Seasonal cards have been consumed\n" +
                    "$totalShardSalvageT2Collaboration $shard have been generated from Collaboration T2, and $totalCardSalvageT2Collaboration cards have been consumed\n" +
                    "$totalShardSalvageT3 $shard have been generated from T3, and $totalCardSalvageT3 cards have been consumed\n" +
                    "\n" +
                    "Total $totalShards $shard in users' inventories\n" +
                    "Total $totalCatFoods $cf in users' inventories"

            val content2 = "$totalCardCraftT2Regular Regular T2 cards have been crafted, and $shard $totalShardCraftT2Regular have been consumed\n" +
                    "$totalCardCraftT2Seasonal Seasonal T2 cards have been crafted, and $shard $totalShardCraftT2Seasonal have been consumed\n" +
                    "$totalCardCraftT2Collaboration Collaboration T2 cards have been crafted, and $shard $totalShardCraftT2Collaboration have been consumed\n" +
                    "$totalCardCraftT3 T3 cards have been crafted, and $shard $totalShardCraftT3 have been consumed\n" +
                    "$totalCardCraftT4 T4 have been generated crafted, and $shard $totalShardCraftT4 have been consumed\n" +
                    "\n" +
                    "Total $totalCraftedCards cards have been crafted\n" +
                    "\n" +
                    "$totalTransferredCatFoods $cf have been transferred among users via trading\n" +
                    "$totalTradeDone trades were done during this time\n" +
                    "\n" +
                    "There are $totalT1Cards T1 cards\n" +
                    "There are $totalT2RegularCards Regular T2 cards\n" +
                    "There are $totalT2SeasonalCards Seasonal T2 cards\n" +
                    "There are $totalT2CollaborationCards Collaboration T2 cards\n" +
                    "There are $totalT3Cards T3 cards\n" +
                    "There are $totalT4Cards T4 cards\n" +
                    "\n" +
                    "Users have spent $cf $totalCatFoodSlotMachineInput for rolling slot machine\n" +
                    "Users have gotten $cf $totalCatFoodSlotMachineReward from slot machine\n" +
                    "\n" +
                    "Users have spent $shard $totalPlatinumShardSlotMachineInput for rolling slot machine\n" +
                    "Users have gotten $shard $totalPlatinumShardSlotMachineReward from slot machine" +
                    "\n" +
                    "============================"

            replyToMessageSafely(ch, content, loader.message) { a -> a }
            ch.sendMessage(content2).setAllowedMentions(ArrayList()).queue()
        }
    }

    private fun getSessionNumber(content: String) : Int {
        val contents = content.split(" ")

        contents.forEachIndexed { index, s ->
            if (s == "-n" && index < contents.size - 1 && StaticStore.isNumeric(contents[index + 1])) {
                return max(1, StaticStore.safeParseInt(contents[index + 1]))
            } else if (s == "-lf") {
                return -1
            }
        }

        return 30
    }
}