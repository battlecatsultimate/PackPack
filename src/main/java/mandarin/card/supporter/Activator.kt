package mandarin.card.supporter

enum class Activator(val tier: CardData.Tier, val banner: Int, val title: String) {
    //Season
    Valentine(CardData.Tier.UNCOMMON, 2, "Valentine Gals"),
    WhiteDay(CardData.Tier.UNCOMMON, 3, "White Day Capsules"),
    Easter(CardData.Tier.UNCOMMON, 4, "Easter Carnival"),
    JuneBride(CardData.Tier.UNCOMMON, 5, "June Bride"),
    SummerGals(CardData.Tier.UNCOMMON, 6, "Gals of Summer"),
    Halloweens(CardData.Tier.UNCOMMON, 7, "Halloweens Capsules"),
    XMas(CardData.Tier.UNCOMMON, 8, "X-Mas Gals"),

    //Collaboration
    Bikkuriman(CardData.Tier.UNCOMMON, 9, "Bikkuriman"),
    CrashFever(CardData.Tier.UNCOMMON, 10, "Crash Fever"),
    FateStayNight(CardData.Tier.UNCOMMON, 11, "Fate/Stay: Night"),
    HatsuneMiku(CardData.Tier.UNCOMMON, 12, "Hatsune Miku Capsules"),
    MercStoria(CardData.Tier.UNCOMMON, 13, "Merc Storia"),
    Evangelion(CardData.Tier.UNCOMMON, 14, "Neon Genesis Evangelion 1st/2nd"),
    PowerPro(CardData.Tier.UNCOMMON, 15, "Power Pro Baseball"),
    RanmaHalf(CardData.Tier.UNCOMMON, 16, "Ranma 1/2"),
    RiverCity(CardData.Tier.UNCOMMON, 17, "River City Clash Capsules"),
    ShoumetsuToshi(CardData.Tier.UNCOMMON, 18, "Shoumetsu Toshi"),
    StreetFighters(CardData.Tier.UNCOMMON, 19, "Street Fighters Red/Blue"),
    SuriveMola(CardData.Tier.UNCOMMON, 20, "Survive! Mola Mola!"),
    MetalSlug(CardData.Tier.UNCOMMON, 21, "Metal Slug"),

    //Valkyrie
    LilValkyrie(CardData.Tier.ULTRA, 3, "Li'l Valkyrie"),
    //Dark Valkyrie
    LilValkyrieDark(CardData.Tier.ULTRA, 4, "Li'l Valkyrie Dark");
}