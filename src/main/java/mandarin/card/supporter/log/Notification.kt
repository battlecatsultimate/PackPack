package mandarin.card.supporter.log

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Skin
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.sharding.ShardManager

object Notification {
    lateinit var notificationChannel: MessageChannel

    fun initialize(client: JDA) {
        val ch = client.getChannelById(MessageChannel::class.java, CardData.notification)

        if (ch == null) {
            StaticStore.logger.uploadLog("W/Notification::initialize - Failed to find notification channel ${CardData.notification}")

            return
        }

        notificationChannel = ch
    }

    fun handlePackSlotNotification() {
        val currentTime = CardData.getUnixEpochTime()
        val removeQueue = ArrayList<Long>()

        CardData.notifierGroup.filter { (_, notifier) -> notifier.any { b -> b } }.forEach { (id, notifier) ->
            if (!CardBot.test || id.toString() == StaticStore.MANDARIN_SMELL) {
                val messageContent = StringBuilder()

                if (notifier[0]) {
                    val packList = StringBuilder()

                    val cooldown = CardData.cooldown[id] ?: return@forEach
                    val resetQueue = ArrayList<String>()

                    cooldown.forEach { (uuid, cd) ->
                        val pack = CardData.cardPacks.find { pack -> pack.uuid == uuid }

                        if (pack != null && cd > 0 && cd - currentTime <= 0 && pack.activated) {
                            packList.append("- ")
                                .append(pack.packName)
                                .append("\n")

                            resetQueue.add(uuid)
                        }
                    }

                    resetQueue.forEach { uuid ->
                        cooldown[uuid] = 0
                    }

                    if (packList.isNotBlank()) {
                        messageContent.append("You can roll pack below!\n\n")
                            .append(packList)
                    }
                }

                if (notifier[1]) {
                    val slotList = StringBuilder()

                    val cooldown = CardData.slotCooldown[id] ?: return@forEach
                    val resetQueue = ArrayList<String>()

                    cooldown.forEach { (uuid, cd) ->
                        val slot = CardData.slotMachines.filter { slot -> slot.cooldown >= CardData.MINIMUM_NOTIFY_TIME }.find { slot -> slot.uuid == uuid }

                        if (slot != null && cd > 0 && cd - currentTime <= 0 && slot.activate) {
                            slotList.append("- ")
                                .append(slot.name)
                                .append("\n")

                            resetQueue.add(uuid)
                        }
                    }

                    resetQueue.forEach { uuid ->
                        cooldown[uuid] = 0
                    }

                    if (slotList.isNotBlank()) {
                        if (messageContent.isNotBlank()) {
                            messageContent.append("\n")
                        }

                        messageContent.append("You can roll slot machine below!\n\n")
                            .append(slotList)
                    }
                }

                if (messageContent.isNotBlank()) {
                    if (this::notificationChannel.isInitialized) {
                        notificationChannel.sendMessage("<@$id>\n\n$messageContent").queue(null) { e ->
                            StaticStore.logger.uploadErrorLog(e, "E/Notification::handlePackSlotNotification - Failed to send notification")
                        }
                    }
                }
            }
        }

        removeQueue.forEach { id -> CardData.notifierGroup.remove(id) }
    }

    fun handleCollectorRoleNotification(client: ShardManager) {
        val g = client.getGuildById(CardData.guild)
        val collectorRole = g?.roles?.find { r -> r.id == CardData.Role.LEGEND.id }
        val ccRole = g?.roles?.find { r -> r.id == CardData.cc }
        val eccRole = g?.roles?.find { r -> r.id == CardData.ecc }

        CardData.inventories.keys.forEach { userID ->
            val inventory = Inventory.getInventory(userID)
            var ccRemoved = false
            var eccRemoved = false

            if (!inventory.validForLegendCollector() && inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                inventory.vanityRoles.remove(CardData.Role.LEGEND)

                if (collectorRole != null) {
                    g.removeRoleFromMember(UserSnowflake.fromId(userID), collectorRole).queue()
                }

                if (inventory.ccValidationWay == Inventory.CCValidationWay.LEGENDARY_COLLECTOR && ccRole != null) {
                    g.removeRoleFromMember(UserSnowflake.fromId(userID), ccRole).queue()

                    ccRemoved = true

                    if (inventory.eccValidationWay != Inventory.ECCValidationWay.NONE && inventory.eccValidationWay != Inventory.ECCValidationWay.LEGENDARY_COLLECTOR && eccRole != null) {
                        inventory.cancelECC(g, userID)

                        eccRemoved = true
                    }
                }

                if (inventory.eccValidationWay == Inventory.ECCValidationWay.LEGENDARY_COLLECTOR && eccRole != null) {
                    g.removeRoleFromMember(UserSnowflake.fromId(userID), eccRole).queue()

                    eccRemoved = true
                }

                if (this::notificationChannel.isInitialized) {
                    var message = "<@$userID>, your Legendary Collector role has been removed from your inventory. There are 2 possible reasons for this decision\n\n" +
                            "1. You spent your card on trading, crafting, etc. so you don't meet condition of legendary collector now\n" +
                            "2. New cards have been added, so you have to collect those cards to retrieve role back\n\n"

                    if (ccRemoved && eccRemoved) {
                        message += "Additionally, your CC status was also removed because you have obtained both two roles with Legendary Collector way. ECC status could be lost as well because CC requires you to have ECC. If you have paid cards for ECC, cards should be retrieved to your inventory. You will have to obtain them back by getting Legendary Collector again, or other way\n\n"
                    } else if (ccRemoved) {
                        message += "Additionally, your CC status was also removed because you have obtained CC with Legendary Collector way. You will have to obtain it back by getting Legendary Collector again, or other way\n\n"
                    } else if (eccRemoved) {
                        message += "Additionally, your ECC status was also removed because you have obtained ECC with Legendary Collector way. You will have to obtain it back by getting Legendary Collector again, or other way\n\n"
                    }

                    message += "This is automated system. Please contact card managers if this seems to be incorrect automation\n\n${inventory.getInvalidReason()}"

                    notificationChannel.sendMessage(message).queue(null) { e ->
                        StaticStore.logger.uploadErrorLog(e, "E/Notification::sendCollectorRoleNotification - Failed to send notification")
                    }
                }
            }
        }
    }

    fun handleSkinPurchaseNotification(skin: Skin, purchaser: Long) {
        val purchaseSize = CardData.inventories.values.count { i -> skin in i.skins }

        val message = "<@${skin.creator}>, user <@$purchaser> has purchased your skin!\n\n### Skin Name : ${skin.name} [${skin.skinID}]\n### Total Purchase Count : $purchaseSize"

        if (this::notificationChannel.isInitialized) {
            notificationChannel.sendMessage(message).queue()
        }
    }

    fun handleNotificationTest(userID: Long) {
        if (this::notificationChannel.isInitialized) {
            notificationChannel.sendMessage("<@$userID> This is test notification. You will receive all the notifications in here").queue()
        }
    }
}