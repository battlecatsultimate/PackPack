package mandarin.card.supporter

enum class Activator(val tier: CardData.Tier, val banner: Int, val title: String) {
    //Season
    Valentine(CardData.Tier.UNCOMMON, 4, "Valentine Gals"),
    WhiteDay(CardData.Tier.UNCOMMON, 5, "White Day Capsules"),
    Easter(CardData.Tier.UNCOMMON, 6, "Easter Carnival"),
    JuneBride(CardData.Tier.UNCOMMON, 7, "June Bride"),
    SummerGals(CardData.Tier.UNCOMMON, 8, "Gals of Summer"),
    Halloweens(CardData.Tier.UNCOMMON, 9, "Halloweens Capsules"),
    XMas(CardData.Tier.UNCOMMON, 10, "X-Mas Gals"),

    //Collaboration
    Bikkuriman(CardData.Tier.UNCOMMON, 11, "Bikkuriman"),
    CrashFever(CardData.Tier.UNCOMMON, 12, "Crash Fever"),
    FateStayNight(CardData.Tier.UNCOMMON, 13, "Fate/Stay: Night"),
    HatsuneMiku(CardData.Tier.UNCOMMON, 14, "Hatsune Miku Capsules"),
    MercStoria(CardData.Tier.UNCOMMON, 15, "Merc Storia"),
    Evangelion(CardData.Tier.UNCOMMON, 16, "Neon Genesis Evangelion 1st/2nd"),
    PowerPro(CardData.Tier.UNCOMMON, 17, "Power Pro Baseball"),
    RanmaHalf(CardData.Tier.UNCOMMON, 18, "Ranma 1/2"),
    RiverCity(CardData.Tier.UNCOMMON, 19, "River City Clash Capsules"),
    ShoumetsuToshi(CardData.Tier.UNCOMMON, 20, "Shoumetsu Toshi"),
    StreetFighters(CardData.Tier.UNCOMMON, 21, "Street Fighters Red/Blue"),
    SurviveMola(CardData.Tier.UNCOMMON, 22, "Survive! Mola Mola!"),
    MetalSlug(CardData.Tier.UNCOMMON, 23, "Metal Slug"),
    PrincessPunt(CardData.Tier.UNCOMMON, 24, "Princess Punt"),
    TowerOfSavior(CardData.Tier.UNCOMMON, 25, "Tower of Savior"),
    RurouniKenshin(CardData.Tier.UNCOMMON, 26, "Rurouni Kenshin"),
    PuellaMagiMadokaMagica(CardData.Tier.UNCOMMON, 27, "Magica Madoka"),

    //Valkyrie
    LilValkyrie(CardData.Tier.ULTRA, 3, "Li'l Valkyrie"),
    //Dark Valkyrie
    LilValkyrieDark(CardData.Tier.ULTRA, 4, "Li'l Valkyrie Dark"),
    //Trixi
    Trixi(CardData.Tier.ULTRA, 5, "Trixi the Revenant");
}
