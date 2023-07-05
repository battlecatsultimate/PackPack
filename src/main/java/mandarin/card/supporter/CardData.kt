package mandarin.card.supporter

import net.dv8tion.jda.api.entities.Member

object CardData {
    /*
    -------------------------------------------------------
    |                  Card and Inventory                 |
    -------------------------------------------------------
     */

    val cards = ArrayList<Card>()

    val permanents = arrayOf(
        0 until 9,
        0 until 2,
        0 until 3
    )

    //Stored as uber ID
    val bannerData = arrayOf(
        //Tier 1
        arrayOf(
            //Dark Heroes
            arrayOf(194, 195, 196, 212, 226, 261, 431, 533, 634, 698),
            //Dragon Emperors
            arrayOf(83, 84, 85, 86, 87, 177, 396, 505, 620, 660),
            //Dynamites
            arrayOf(42, 43, 44, 57, 59, 143, 427, 519, 617, 668),
            //Elemental Pixies
            arrayOf(359, 360, 361, 401, 569, 631, 655),
            //Galaxy Gals
            arrayOf(75, 76, 105, 106, 107, 159, 351, 502, 619, 647),
            //Iron Legion
            arrayOf(304, 305, 306, 355, 417, 594, 632, 674),
            //Sengoku Wargods
            arrayOf(71, 72, 73, 124, 125, 158, 338, 496, 618, 649),
            //The Nekoluga Family
            arrayOf(34, 168, 169, 170, 171, 240, 436, 546, 625),
            //Ultra Souls
            arrayOf(134, 135, 136, 137, 138, 203, 322, 525, 633, 692)
        ),
        //Tier 2
        arrayOf(
            //Girls and Monsters
            arrayOf(334, 335, 336, 357, 358, 607, 682),
            //The Almighties
            arrayOf(257, 258, 259, 271, 272, 316, 439, 534, 642),

            //Seasonal Total
            arrayOf(587, 588, 644, 648, 693, 330, 331, 595, 699, 661, 711, 274, 275, 354, 438, 494, 563, 564, 614, 666, 229, 230, 302, 570, 683, 241, 242, 243, 310, 526, 584, 687),

            //Collaboration Total
            arrayOf(467, 468, 469, 470, 471, 555, 326, 362, 363, 364, 365, 366, 367, 368, 456, 535, 536, 537, 560, 582, 583, 590, 119, 185, 186, 187, 188, 345, 346, 506, 412, 413, 414, 415, 416, 487, 488, 547, 548, 549, 550, 551, 709, 710, 393, 394, 395, 596, 597, 598, 599, 600, 671, 624, 180, 270, 341, 482, 511, 512, 513, 514, 515, 516, 517, 571, 572, 573, 574, 680, 681, 174, 222, 223, 224, 225),

            //Season

            //Valentine
            arrayOf(587, 588, 644),
            //Whiteday
            arrayOf(648, 693),
            //Easter
            arrayOf(330, 331, 595, 699),
            //June Bride
            arrayOf(661, 711),
            //Summer Gals
            arrayOf(274, 275, 354, 438, 494, 563, 564, 614, 666),
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
            arrayOf(535, 536, 537, 560, 582, 583, 590),
            //Merc
            arrayOf(119, 185, 186, 187, 188, 345, 346, 506),
            //Evangelion
            arrayOf(412, 413, 414, 415, 416, 487, 488, 547, 548, 549, 550, 551, 709, 710),
            //Power Pro
            arrayOf(393, 394, 395),
            //Ranma
            arrayOf(596, 597, 598, 599, 600, 671),
            //River City
            arrayOf(624),
            //Shoumetsu
            arrayOf(180, 270, 341, 482),
            //SF
            arrayOf(511, 512, 513, 514, 515, 516, 517, 571, 572, 573, 574, 680, 681),
            //Mola
            arrayOf(174),
            //Metal
            arrayOf(222, 223, 224, 225)
        ),
        //Tier 3
        arrayOf(
            //Epicfest Exclusives
            arrayOf(333, 378, 441, 543, 609, 657),
            //Uberfest Exclusives
            arrayOf(269, 318, 380, 529, 585, 641, 690),
            //Other Exclusives
            arrayOf(283, 286, 397, 559, 612, 686),
            //LilValk
            arrayOf(435),
            //Dark Lil valk
            arrayOf(484)
        ),
        //Tier 4
        arrayOf(
            //Collab LR

            //Bikkuriman
            arrayOf(466),
            //SF
            arrayOf(510)
        )
    )

    val busters = arrayOf(283, 286, 397, 559, 612, 686)

    val regularLegend = arrayOf(481, 450, 455, 478, 449, 463, 448, 461, 451, 544, 493)

    var inventories = HashMap<String, Inventory>()

    val activatedBanners = ArrayList<Activator>()

    val cooldown = HashMap<String, LongArray>()

    const val cooldownTerm = 0 * 60 * 60 * 1000 // 72 hours in milliseconds

    /*
    -------------------------------------------------------
    |                   Role and Channel                  |
    -------------------------------------------------------
     */

    enum class Role(val id: String, val key: String, val title: String) {
        NONE("", "", ""),
        DOGE(ServerData.get("doge"), "DOGE", "Doge"),
        SIRSEAL(ServerData.get("sirSeal"), "SIRSEAL", "Sir Seal"),
        ASSASSIN(ServerData.get("assassin"), "ASSASSIN", "Assassin Bear"),
        ANGELIC(ServerData.get("angelic"), "ANGELIC", "Angelic Gory"),
        MOOTH(ServerData.get("mooth"), "MOOTH", "Mooth"),
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
        LEGEND(ServerData.get("legend"), "LEGEND", "Legend Collector");

        fun getProduct() : Product {
            return when(this) {
                DOGE -> Product.doge
                SIRSEAL -> Product.sirSeal
                ASSASSIN -> Product.assassin
                ANGELIC -> Product.angelic
                MOOTH -> Product.mooth
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

    val tradingPlace = ServerData.get("tradingPlace")
    val transactionLog = ServerData.get("transactionLog")
    val tradingLog = ServerData.get("tradingLog")
    val modLog = ServerData.get("modLog")

    const val MAX_CARDS = 10

    var sessionNumber = 1L

    val sessions = ArrayList<TradingSession>()

    val allowedChannel = ServerData.getArray("allowedChannel")

    /*
    -------------------------------------------------------
    |                        Packs                        |
    -------------------------------------------------------
     */

    enum class Pack(val cost: Int) {
        LARGE(0),
        SMALL(0),
        NONE(-1);

        fun getPackName() : String {
            return when(this) {
                LARGE -> "Large Card Pack"
                SMALL -> "Small Card Pack"
                else -> "Unknown Pack $this"
            }
        }
    }

    /*
    -------------------------------------------------------
    |                        Tiers                        |
    -------------------------------------------------------
     */

    enum class Tier {
        COMMON,
        UNCOMMON,
        ULTRA,
        LEGEND,
        NONE
    }

    val common = ArrayList<Card>()
    val uncommon = ArrayList<Card>()
    val ultraRare = ArrayList<Card>()
    val legendRare = ArrayList<Card>()

    val tierCategoryText = arrayOf(
        "Tier 1 [Common]", "Tier 2 [Uncommon]", "Tier 3 [Ultra Rare (Exclusives)]", "Tier 4 [Legend Rare]"
    )

    val bannerCategoryText = arrayOf(
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

    fun isMod(member: Member) : Boolean {
        val roleList = member.roles.map { r -> r.id }

        return roleList.contains(ServerData.get("modRole")) || roleList.contains(ServerData.get("headerMod"))
    }

    fun isDealer(member: Member) : Boolean {
        val roleList = member.roles.map { r -> r.id }

        return roleList.contains(ServerData.get("dealer")) || roleList.contains(ServerData.get("modRole")) || roleList.contains(ServerData.get("headerMod"))
    }

    fun appendUncommon(banner: List<Card>) : List<Card> {
        val result = ArrayList<Card>(banner)

        activatedBanners.forEach { b ->
            result.addAll(bannerData[b.tier.ordinal][b.banner].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == b.tier } })
        }

        return result
    }

    fun appendLR(banner: List<Card>) : List<Card> {
        val result = ArrayList<Card>(banner)

        if (Activator.Bikkuriman in activatedBanners) {
            result.addAll(bannerData[Tier.LEGEND.ordinal][0].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == Tier.LEGEND } })
        } else if (Activator.StreetFighters in activatedBanners) {
            result.addAll(bannerData[Tier.LEGEND.ordinal][1].mapNotNull { i -> cards.find { c -> c.unitID == i && c.tier == Tier.LEGEND } })
        }

        return result
    }
}
