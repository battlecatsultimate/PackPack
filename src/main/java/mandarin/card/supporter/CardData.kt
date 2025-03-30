package mandarin.card.supporter

import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.Skin
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
        SKIN,
        CF,
        SHARD
    }

    enum class TransferMode {
        INJECT,
        OVERRIDE
    }

    val cards = ArrayList<Card>()
    val skins = ArrayList<Skin>()
    val banners = ArrayList<Banner>()

    val supportedFileFormat = arrayOf(
        "png", "jpg", "gif", "mp4"
    )
    val grade = arrayOf(
        rgb(155, 245, 66),
        rgb(204,124,84),
        rgb(206,209,210),
        rgb(213,171,98),
        rgb(218,232,240)
    )

    val bannedT3 = arrayOf(435, 484, 758)

    val busters = arrayOf(283, 286, 397, 559, 612, 686)

    val regularLegend = arrayOf(481, 450, 455, 478, 449, 463, 448, 461, 451, 544, 493)

    var inventories = HashMap<Long, Inventory>()

    val taxedCatFoods = PositiveMap<Long, Long>()

    val activatedBanners = ArrayList<Banner>()

    val cooldown = HashMap<Long, HashMap<String, Long>>()
    val slotCooldown = HashMap<Long, HashMap<String, Long>>()

    val lastMessageSent = HashMap<String, Long>()

    var minimumCatFoods = 20L
    var maximumCatFoods = 20L

    var catFoodCooldown = 120000L // 120 seconds as default

    const val MINIMUM_BID = 1000L
    const val AUTO_CLOSE_TIME = 12L * 60 * 60 * 1000 // 12 hours

    /*
    ----------------------------------------------------------------
    |                   Role ,Channel, and Member                  |
    ----------------------------------------------------------------
     */

    enum class Role(val id: String, val key: String, val title: String) {
        NONE("", "", ""),
        DOGE(ServerData.get("doge"), "DOGE", "Doge"),
        YOUCAN(ServerData.get("youcan"), "YOUCAN", "Youcan"),
        GOBBLE(ServerData.get("gobble"), "GOBBLE", "Gobble"),
        LILDOGE(ServerData.get("lilDoge"), "LILDOGE", "Li'l Doge"),
        AKUCYCLONE(ServerData.get("akuCyclone"), "AKUCYCLONE", "Aku Cyclone"),
        RELICBUN(ServerData.get("relicBun"), "RELICBUN", "Relic Bun-Bun"),
        EXIEL(ServerData.get("exiel"), "EXIEL", "Archangel Exiel"),
        LUZA(ServerData.get("luza"), "LUZA", "Zero Luza"),
        HERMIT(ServerData.get("hermit"), "HERMIT", "Hermit Cat"),
        RAMIEL(ServerData.get("ramiel"), "RAMIEL", "Ramiel"),
        LEGEND(ServerData.get("legend"), "LEGEND", "Legend Collector"),
        ZAMBONER(ServerData.get("zamboner"), "ZAMBONER", "Zamboner"),
        WHALELORD(ServerData.get("whaleLord"), "WHALELORD", "Whale Lord");

        fun getProduct() : Product {
            return when(this) {
                DOGE -> Product.doge
                YOUCAN -> Product.youcan
                GOBBLE -> Product.gobble
                LILDOGE -> Product.lilDoge
                AKUCYCLONE -> Product.akuCyclone
                RELICBUN -> Product.relicBun
                EXIEL -> Product.exiel
                LUZA -> Product.luza
                HERMIT -> Product.hermitCat
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

    val notification = ServerData.get("notification")

    val managerPlace = ServerData.get("managerPlace")

    val skinCache = ServerData.get("skinCache")

    private val globalCategory = arrayOf(
        ServerData.get("bctcCategory"),
        ServerData.get("bctcRaidCategory"),
        ServerData.get("bctcDevelopmentCategory"),
        ServerData.get("bctcgCategory"),
        ServerData.get("eventCategory")
    )

    const val MAX_CARD_TYPE = 10
    const val MAX_CAT_FOOD_SUGGESTION = 0
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
    val purchaseNotifier = ArrayList<Long>()

    val df = run {
        val nf = NumberFormat.getInstance(Locale.US)
        val decimal = nf as DecimalFormat
        decimal.applyPattern("#.###")
        decimal
    }

    val excludedCatFoodChannel = ArrayList<String>()

    val auctionPlaces = ArrayList<Long>()

    val bannedUser = ArrayList<Long>()

    val optOut = ArrayList<Long>()

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
