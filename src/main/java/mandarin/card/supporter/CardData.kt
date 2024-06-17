package mandarin.card.supporter

import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import java.awt.Color
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Clock
import java.time.Instant
import java.util.*

@Suppress("unused")
object CardData {
    /*
    -------------------------------------------------------
    |                  Card and Inventory                 |
    -------------------------------------------------------
     */

    enum class SalvageMode(var cost: Int) {
        T1(1),
        T2(20),
        SEASONAL(40),
        COLLAB(60),
        T3(300),
        T4(1600)
    }

    enum class CraftMode(var cost: Int) {
        T2(25),
        SEASONAL(50),
        COLLAB(100),
        T3(400),
        T4(1800)
    }

    enum class ModifyCategory {
        CARD,
        ROLE,
        CF,
        SHARD
    }

    enum class TransferMode {
        INJECT,
        OVERRIDE
    }

    val cards = ArrayList<Card>()

    val permanents = arrayOf(
        0 until 1,
        0 until 9,
        0 until 2,
        0 until 3,
        0 until 1
    )

    val grade = arrayOf(
        rgb(155, 245, 66),
        rgb(204,124,84),
        rgb(206,209,210),
        rgb(213,171,98),
        rgb(218,232,240)
    )

    //Stored as uber ID
    val bannerData = arrayOf(
        //Tier 0
        arrayOf(
            arrayOf()
        ),
        //Tier 1
        arrayOf(
            //Dark Heroes
            arrayOf(194, 195, 196, 212, 226, 261, 431, 533, 634, 698),
            //Dragon Emperors
            arrayOf(83, 84, 85, 86, 87, 177, 396, 505, 620, 660),
            //Dynamites
            arrayOf(42, 43, 44, 57, 59, 143, 427, 519, 617, 668),
            //Elemental Pixies
            arrayOf(359, 360, 361, 401, 569, 631, 655, 719),
            //Galaxy Gals
            arrayOf(75, 76, 105, 106, 107, 159, 351, 502, 619, 647, 733),
            //Iron Legion
            arrayOf(304, 305, 306, 355, 417, 594, 632, 674, 715),
            //Sengoku Wargods
            arrayOf(71, 72, 73, 124, 125, 158, 338, 496, 618, 649),
            //The Nekoluga Family
            arrayOf(34, 168, 169, 170, 171, 240, 436, 546, 625, 712),
            //Ultra Souls
            arrayOf(134, 135, 136, 137, 138, 203, 322, 525, 633, 692),
            //???
            arrayOf(-1000)
        ),
        //Tier 2
        arrayOf(
            //Girls and Monsters
            arrayOf(334, 335, 336, 357, 358, 607, 682, 725),
            //The Almighties
            arrayOf(257, 258, 259, 271, 272, 316, 439, 534, 642, 723),

            //Seasonal Total
            arrayOf(587, 588, 644, 648, 693, 330, 331, 595, 699, 661, 711, 274, 275, 354, 438, 494, 563, 564, 614, 666, 714, 229, 230, 302, 570, 683, 241, 242, 243, 310, 526, 584, 687, 736, 737),

            //Collaboration Total
            arrayOf(467, 468, 469, 470, 471, 555, 326, 362, 363, 364, 365, 366, 367, 368, 456, 535, 536, 537, 560, 582, 583, 590, 119, 185, 186, 187, 188, 345, 346, 506, 412, 413, 414, 415, 416, 487, 488, 547, 548, 549, 550, 551, 709, 710, 393, 394, 395, 596, 597, 598, 599, 600, 671, 624, 721, 180, 270, 341, 482, 511, 512, 513, 514, 515, 516, 517, 571, 572, 573, 574, 680, 681, 174, 222, 223, 224, 225, 66, 161, 337, 485, 530, 722, 741),

            //Season

            //Valentine
            arrayOf(587, 588, 644),
            //Whiteday
            arrayOf(648, 693, 736),
            //Easter
            arrayOf(330, 331, 595, 699, 737),
            //June Bride
            arrayOf(661, 711),
            //Summer Gals
            arrayOf(274, 275, 354, 438, 494, 563, 564, 614, 666, 714),
            //Halloweens
            arrayOf(229, 230, 302, 570, 683),
            //X-Mas
            arrayOf(241, 242, 243, 310, 526, 584, 687),

            //Collaboration

            //Bikkuriman
            arrayOf(467, 468, 469, 470, 471, 555),
            //Crash Fever
            arrayOf(326),
            //Fate
            arrayOf(362, 363, 364, 365, 366, 367, 368, 456),
            //Miku
            arrayOf(535, 536, 537, 560, 582, 583, 590, 722),
            //Merc
            arrayOf(119, 185, 186, 187, 188, 345, 346, 506),
            //Evangelion
            arrayOf(412, 413, 414, 415, 416, 487, 488, 547, 548, 549, 550, 551, 709, 710),
            //Power Pro
            arrayOf(393, 394, 395),
            //Ranma
            arrayOf(596, 597, 598, 599, 600, 671),
            //River City
            arrayOf(624, 721),
            //Shoumetsu
            arrayOf(180, 270, 341, 482),
            //SF
            arrayOf(511, 512, 513, 514, 515, 516, 517, 571, 572, 573, 574, 680, 681),
            //Mola
            arrayOf(174),
            //Metal
            arrayOf(222, 223, 224, 225),
            //Princess Punt
            arrayOf(66, 161, 337, 485, 530),
            //Tower of Savior
            arrayOf(741),
            //???
            arrayOf(-1001)
        ),
        //Tier 3
        arrayOf(
            //Epicfest Exclusives
            arrayOf(333, 378, 441, 543, 609, 657, 705),
            //Uberfest Exclusives
            arrayOf(269, 318, 380, 529, 585, 641, 690),
            //Other Exclusives
            arrayOf(283, 286, 397, 559, 612, 686),
            //LilValk
            arrayOf(435),
            //Dark Lil valk
            arrayOf(484),
            //???
            arrayOf(-1002)
        ),
        //Tier 4
        arrayOf(
            //Collab LR

            //Bikkuriman
            arrayOf(466),
            //SF
            arrayOf(510),
            //???
            arrayOf(-1003)
        )
    )

    val busters = arrayOf(283, 286, 397, 559, 612, 686)

    val regularLegend = arrayOf(481, 450, 455, 478, 449, 463, 448, 461, 451, 544, 493)

    var inventories = HashMap<Long, Inventory>()

    val activatedBanners = ArrayList<Activator>()

    val cooldown = HashMap<String, HashMap<String, Long>>()
    val slotCooldown = HashMap<String, HashMap<String, Long>>()

    val lastMessageSent = HashMap<String, Long>()

    var minimumCatFoods = 20L
    var maximumCatFoods = 20L

    var catFoodCooldown = 120000L // 120 seconds as default

    const val MINIMUM_BID = 1000L
    const val AUTO_CLOSE_TIME = 12L * 60 * 60 * 1000 // 12 hours

    const val EMOJI_SPACE = "   "

    /*
    -------------------------------------------------------
    |                   Role and Channel                  |
    -------------------------------------------------------
     */

    enum class Role(val id: String, val key: String, val title: String) {
        NONE("", "", ""),
        DOGE(ServerData.get("doge"), "DOGE", "Doge"),
        TWOCAN(ServerData.get("twocan"), "TWOCAN", "Twocan"),
        AHIRUJO(ServerData.get("ahirujo"), "AHIRUJO", "Casaurian Ahirujo"),
        BAKOO(ServerData.get("bakoo"), "BAKOO", "Bakoo"),
        YOUCAN(ServerData.get("youcan"), "YOUCAN", "Youcan"),
        GOBBLE(ServerData.get("gobble"), "GOBBLE", "Gobble"),
        SMH(ServerData.get("smh"), "SMH", "Super Metal Hippoe"),
        SCISSOR(ServerData.get("scissor"), "SCISSOR", "Crustaceous Scissorex"),
        LILDOGE(ServerData.get("lilDoge"), "LILDOGE", "Li'l Doge"),
        DABOO(ServerData.get("daboo"), "DABOO", "Daboo of the Dead"),
        AKUCYCLONE(ServerData.get("akuCyclone"), "AKUCYCLONE", "Aku Cyclone"),
        RELICBUN(ServerData.get("relicBun"), "RELICBUN", "Relic Bun-Bun"),
        WOGE(ServerData.get("wildDoge"), "WOGE", "Wild Doge"),
        EXIEL(ServerData.get("exiel"), "EXIEL", "Archangel Exiel"),
        OMENS(ServerData.get("omens"), "OMENS", "Omens"),
        LUZA(ServerData.get("luza"), "LUZA", "Zero Luza"),
        HERMIT(ServerData.get("hermit"), "HERMIT", "Hermit Cat"),
        EASTER(ServerData.get("easter"), "EASTER", "Easter Duche"),
        RAMIEL(ServerData.get("ramiel"), "RAMIEL", "Ramiel"),
        LEGEND(ServerData.get("legend"), "LEGEND", "Legend Collector"),
        ZAMBONER(ServerData.get("zamboner"), "ZAMBONER", "Zamboner"),
        WHALELORD(ServerData.get("whaleLord"), "WHALELORD", "Whale Lord");

        fun getProduct() : Product {
            return when(this) {
                DOGE -> Product.doge
                TWOCAN -> Product.twocan
                AHIRUJO -> Product.ahirujo
                BAKOO -> Product.bakoo
                YOUCAN -> Product.youcan
                GOBBLE -> Product.gobble
                SMH -> Product.smh
                SCISSOR -> Product.scissor
                LILDOGE -> Product.lilDoge
                DABOO -> Product.daboo
                AKUCYCLONE -> Product.akuCyclone
                RELICBUN -> Product.relicBun
                WOGE -> Product.wildDoge
                EXIEL -> Product.exiel
                OMENS -> Product.omens
                LUZA -> Product.luza
                HERMIT -> Product.hermitCat
                EASTER -> Product.seasonal
                RAMIEL -> Product.ramiel
                else -> throw IllegalStateException("You can't get product for $this")
            }
        }
    }

    val guild = ServerData.get("guild")

    private val cc = ServerData.get("cc")
    private val ecc = ServerData.get("ecc")

    val bankAccount = ServerData.get("bankAccount")

    private val banned = ServerData.get("banned")
    val dealer = ServerData.get("dealer")
    private val mod = ServerData.get("modRole")
    private val headerMod = ServerData.get("headerMod")
    private val organizer = ServerData.get("organizer")

    val statusChannel = ServerData.get("status")

    val tradingPlace = ServerData.get("tradingPlace")
    val testTradingPlace = ServerData.get("testTradingPlace")
    val transactionLog = ServerData.get("transactionLog")
    val tradingLog = ServerData.get("tradingLog")
    val modLog = ServerData.get("modLog")
    val catFoodLog = ServerData.get("catFoodLog")
    val bidLog = ServerData.get("bidLog")
    val slotLog = ServerData.get("slotLog")

    val managerPlace = ServerData.get("managerPlace")

    private val globalCategory = arrayOf(
        ServerData.get("bctcCategory"),
        ServerData.get("bctcRaidCategory"),
        ServerData.get("bctcDevelopmentCategory"),
        ServerData.get("bctcgCategory"),
        ServerData.get("eventCategory")
    )

    const val MAX_CARDS = 10
    const val TAX = 0.0

    var sessionNumber = 1L
    var auctionSessionNumber = 1L

    val sessions = ArrayList<TradingSession>()
    val auctionSessions = ArrayList<AuctionSession>()

    val tradeTrialCooldown = HashMap<String, Long>()

    const val TRADE_TRIAL_COOLDOWN = 1 * 60 * 60 * 1000 // 1 hour in milliseconds
    const val TRADE_EXPIRATION_TIME = 5 * 24 * 60 * 60 * 1000 // 5 days

    private val allowedChannel = ServerData.getArray("allowedChannel")

    val notifierGroup = HashMap<Long, BooleanArray>()

    val df = run {
        val nf = NumberFormat.getInstance(Locale.US)
        val decimal = nf as DecimalFormat
        decimal.applyPattern("#.###")
        decimal
    }

    val excludedCatFoodChannel = ArrayList<String>()

    val auctionPlaces = ArrayList<Long>()

    /*
    -------------------------------------------------------
    |                        Packs                        |
    -------------------------------------------------------
     */

    val cardPacks = ArrayList<CardPack>()
    val slotMachines = ArrayList<SlotMachine>()

    const val MINIMUM_NOTIFY_TIME = 1 * 60 * 60 * 1000 //1 hour

    /*
    -------------------------------------------------------
    |                        Tiers                        |
    -------------------------------------------------------
     */

    enum class Tier {
        SPECIAL,
        COMMON,
        UNCOMMON,
        ULTRA,
        LEGEND,
        NONE
    }

    val special = ArrayList<Card>()
    val common = ArrayList<Card>()
    val uncommon = ArrayList<Card>()
    val ultraRare = ArrayList<Card>()
    val legendRare = ArrayList<Card>()

    val tierCategoryText = arrayOf(
        "Tier 0 [Special]", "Tier 1 [Common]", "Tier 2 [Uncommon]", "Tier 3 [Ultra Rare (Exclusives)]", "Tier 4 [Legend Rare]"
    )

    val bannerCategoryText = arrayOf(
        arrayOf(),
        arrayOf("Dark Heroes", "Dragon Emperors", "Dynamites", "Elemental Pixies", "Galaxy Gals", "Iron Legion", "Sengoku Wargods", "The Nekoluga Family", "Ultra Souls"),
        arrayOf("Girls and Monsters", "The Almighties", "Seasonal", "Collaboration"),
        arrayOf("Epicfest Exclusives", "Uberfest Exclusives", "Other Exclusives"),
        arrayOf()
    )

    /*
    -------------------------------------------------------
    |                       Methods                       |
    -------------------------------------------------------
     */

    fun isManager(member: Member) : Boolean {
        val roleList = member.roles.map { r -> r.id }

        return dealer in roleList
    }

    fun hasAllPermission(member: Member?) : Boolean {
        member ?: return true

        val roleList = member.roles.map { r -> r.id }

        return dealer in roleList || mod in roleList || headerMod in roleList || member.id == StaticStore.MANDARIN_SMELL
    }

    fun isMod(member: Member) : Boolean {
        val roleList = member.roles.map { r -> r.id }

        return mod in roleList || headerMod in roleList || member.id == StaticStore.MANDARIN_SMELL
    }

    fun isBanned(member: Member) : Boolean {
        val roleList = member.roles.map { r -> r.id }

        return banned in roleList
    }

    fun isOrganizer(member: Member) : Boolean {
        val roleList = member.roles.map { r -> r.id }

        return organizer in roleList
    }

    fun appendUncommon(): List<Card> {
        val result = ArrayList(uncommon)

        activatedBanners.filter { b -> b.tier == Tier.UNCOMMON }.forEach { b ->
            result.addAll(bannerData[b.tier.ordinal][b.banner].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == b.tier } })
        }

        return result
    }

    fun appendUltra(): List<Card> {
        val result = ArrayList(ultraRare)

        activatedBanners.filter { b -> b.tier == Tier.ULTRA }.forEach { b ->
            result.addAll(bannerData[b.tier.ordinal][b.banner].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == b.tier } })
        }

        return result
    }

    fun appendLR(): List<Card> {
        val result = ArrayList(legendRare)

        if (Activator.Bikkuriman in activatedBanners) {
            result.addAll(bannerData[Tier.LEGEND.ordinal][0].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == Tier.LEGEND } })
        } else if (Activator.StreetFighters in activatedBanners) {
            result.addAll(bannerData[Tier.LEGEND.ordinal][1].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == Tier.LEGEND } })
        }

        return result
    }

    fun getUnixEpochTime() : Long {
        return Instant.now(Clock.systemUTC()).toEpochMilli()
    }

    fun convertMillisecondsToText(time: Long) : String {
        var leftTime = time

        val day = leftTime / (24 * 60 * 60 * 1000)

        leftTime -= day * 24 * 60 * 60 * 1000

        val hour = leftTime / (60 * 60 * 1000)

        leftTime -= hour * 60 * 60 * 1000

        val minute = leftTime / (60 * 1000)

        leftTime -= minute * 60 * 1000

        val second = leftTime / 1000

        leftTime -= second * 1000

        val secondTime = second + leftTime / 1000.0

        val result =  (if (day == 0L) "" else if (day == 1L) "$day Day " else "$day Days ") +
                (if (hour == 0L) "" else if (hour == 1L) "$hour Hour " else "$hour Hours ") +
                (if (minute == 0L) "" else if (minute == 1L) "$minute Minute " else "$minute Minutes ") +
                (if (secondTime == 0.0) "" else if (secondTime <= 1.0) "${df.format(secondTime)} Second " else "${df.format(secondTime)} Seconds ")

        return result.trim()
    }

    fun isAllowed(ch: MessageChannel) : Boolean {
        return when(ch) {
            is ThreadChannel -> ch.parentChannel.id in allowedChannel
            is PrivateChannel -> true
            else -> ch.id in allowedChannel
        }
    }

    fun canPerformGlobalCommand(member: Member, channel: MessageChannel) : Boolean {
        return member.id == StaticStore.MANDARIN_SMELL || hasAllPermission(member) || when (channel) {
            is StandardGuildMessageChannel -> channel.parentCategoryId in globalCategory
            is ThreadChannel -> {
                val parent = channel.parentChannel

                parent is StandardGuildChannel && parent.parentCategoryId in globalCategory
            }
            else -> false
        }
    }

    fun canTradeT0(member: Member) : Boolean {
        val roleList = member.roles.map { role -> role.id }

        return cc in roleList || ecc in roleList
    }

    fun findPossibleAuctionPlace() : Long {
        auctionPlaces.forEach { id ->
            if (!auctionSessions.any { s -> s.channel == id }) {
                return id
            }
        }

        return -1L
    }

    private fun rgb(r: Int, g: Int, b: Int) : Int {
        return Color(r, g, b).rgb
    }
}
