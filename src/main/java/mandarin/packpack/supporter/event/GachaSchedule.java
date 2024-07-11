package mandarin.packpack.supporter.event;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GachaSchedule extends EventFactor implements Schedule {
    public enum TYPE {
        NORMAL,
        EXTRA,
        RARE
    }

    private static final Pattern p = Pattern.compile("<h2>.+</h2>");

    private final CommonStatic.Lang.Locale locale;

    public final ArrayList<EventSection> sections = new ArrayList<>();
    public final EventDateSet date;
    public final String minVersion, maxVersion;

    public final ArrayList<GachaSection> gacha = new ArrayList<>();

    public GachaSchedule(String line, CommonStatic.Lang.Locale locale) {
        this.locale = locale;

        String[] data = line.split("\t");

        date = new EventDateSet(parse(data[0]), parse(data[2]), parse(data[1]), parse(data[3]));

        minVersion = convertVersion(parse(data[4]));
        maxVersion = convertVersion(parse(data[5]));

        if(parse(data[6]) == 0) {
            int sectionNumber = parse(data[7]);
            int index = 8;

            for(int j = 0; j < sectionNumber; j++) {
                EventSection section = new EventSection();

                int daySetNumber = parse(data[index]);

                index++;

                for (int i = 0; i < daySetNumber; i++) {
                    EventDateSet set = new EventDateSet(parse(data[index]), parse(data[index + 2]), parse(data[index + 1]), parse(data[index + 3]));

                    section.daySets.add(set);

                    index += 4;
                }

                int dayNumber = parse(data[index]);

                index++;

                for (int i = 0; i < dayNumber; i++) {
                    int day = parse(data[index]);

                    section.days.add(day);

                    index++;
                }

                section.parseWeekDay(parse(data[index]));

                index++;

                int timeNumber = parse(data[index]);

                index++;

                for (int i = 0; i < timeNumber; i++) {
                    EventTimeSection timeSection = new EventTimeSection(parse(data[index]), parse(data[index + 1]));

                    section.times.add(timeSection);

                    index+= 2;
                }

                sections.add(section);
            }

            int gachaType = parse(data[index]);

            index++;

            int categoryNum = parse(data[index]);

            index++;

            for(int i = 0; i < categoryNum; i++) {
                int gachaID = parse(data[index]);

                if(gachaID == -1 || gachaID == 0) {
                    if(gachaType == 4) {
                        index += 17;
                    } else {
                        index += 15;
                    }
                } else {
                    GachaSection section = new GachaSection();

                    switch (gachaType) {
                        case 4 -> section.gachaType = TYPE.EXTRA;
                        case 0 -> section.gachaType = TYPE.NORMAL;
                        default -> section.gachaType = TYPE.RARE;
                    }

                    section.gachaID = gachaID;
                    section.index = i;

                    index++;

                    section.requiredCatFruit = parse(data[index]);

                    index++;

                    section.addition = parse(data[index]);

                    index++;

                    section.additionalMask = parse(data[index]);

                    if((section.additionalMask & 4) > 0) {
                        section.additional.add(GachaSection.ADDITIONAL.STEP);
                    }

                    if((section.additionalMask & 4216) > 0) {
                        section.additional.add(GachaSection.ADDITIONAL.LUCKY);
                    }

                    if((section.additionalMask & 16384) > 0) {
                        section.additional.add(GachaSection.ADDITIONAL.SHARD);
                    }

                    if(GachaSet.gachaSet.containsKey(gachaID)) {
                        GachaSet set = GachaSet.gachaSet.get(gachaID);

                        if(containAll(set.data, 200, 144, 131, 129))
                            section.additional.add(GachaSection.ADDITIONAL.NENEKO);

                        if(containAll(set.data, 447, 446, 445, 444, 443))
                            section.additional.add(GachaSection.ADDITIONAL.GRANDON);

                        if(containAll(set.data, 239, 238, 237))
                            section.additional.add(GachaSection.ADDITIONAL.REINFORCE);
                    }

                    index++;

                    for(int j = 0; j < 5; j++) {
                        section.rarityChances[j] = parse(data[index + 2 * j]);
                        section.rarityGuarantees[j] = parse(data[index + 1 + 2 * j]);
                    }

                    double chanceSum = 0;

                    for(int j = 0; j < 5; j++) {
                        chanceSum += section.rarityChances[j];
                    }

                    for(int j = 0; j < 5; j++) {
                        section.rarityChances[j] = section.rarityChances[j] / chanceSum * 100.0;
                    }

                    index += 10;

                    if(index < data.length) {
                        section.message = data[index];
                    } else {
                        section.message = "";
                    }

                    gacha.add(section);

                    index++;
                }
            }
        }
    }

    @Override
    public String beautify(CommonStatic.Lang.Locale lang) {
        StringBuilder result = new StringBuilder();

        result.append("\u001B[0;31m[");

        if(date.dateStart.year != currentYear) {
            result.append(date.dateStart.year)
                    .append(LangID.getStringByID("year", lang));

            if(lang != CommonStatic.Lang.Locale.ZH) {
                result.append(" ");
            }
        }

        result.append(getMonth(date.dateStart.month, lang));

        if(lang != CommonStatic.Lang.Locale.ZH) {
            result.append(" ");
        }

        result.append(date.dateStart.day)
                .append(getNumberWithDayFormat(date.dateStart.day, lang));

        if(date.section.start.hour * 100 + date.section.start.minute != 1100 && date.section.start.hour * 100 + date.section.start.minute != 0) {
            result.append(" ")
                    .append(duo(date.section.start.hour))
                    .append(":")
                    .append(duo(date.section.start.minute));
        }

        if(date.dateStart.notSame(date.dateEnd)) {
            result.append(" ~ ");

            if(!date.dateEnd.equals(END)) {
                if(date.dateStart.year != date.dateEnd.year || (date.dateStart.year != currentYear && lang == CommonStatic.Lang.Locale.ZH)) {
                    result.append(date.dateEnd.year)
                            .append(LangID.getStringByID("year", lang));

                    if(lang != CommonStatic.Lang.Locale.ZH) {
                        result.append(" ");
                    }
                }

                if(date.dateStart.month != date.dateEnd.month || lang == CommonStatic.Lang.Locale.ZH) {
                    result.append(getMonth(date.dateEnd.month, lang));

                    if (lang != CommonStatic.Lang.Locale.ZH) {
                        result.append(" ");
                    }
                }

                result.append(date.dateEnd.day)
                        .append(getNumberWithDayFormat(date.dateEnd.day, lang));

                if(date.section.end.hour * 100 + date.section.end.minute != 1100 && date.section.end.hour * 100 + date.section.end.minute != 2359) {
                    result.append(" ")
                            .append(duo(date.section.end.hour))
                            .append(":")
                            .append(duo(date.section.end.minute));
                }
            }
        }

        result.append("] ");

        for(int i = 0; i < gacha.size(); i++) {
            GachaSection section = gacha.get(i);

            if(section == null)
                continue;

            String g = switch (section.gachaType) {
                case NORMAL -> StaticStore.NORMALGACHA.getCont(section.gachaID, lang);
                case EXTRA -> StaticStore.EXTRAGACHA.getCont(section.gachaID, lang);
                default -> StaticStore.GACHANAME.getCont(section.gachaID, lang);
            };

            if(g == null)
                g = tryGetGachaName(section, lang);

            result.append("\u001B[1;38m").append(g);

            if(newUnits.containsKey(section.gachaID)) {
                int[] units = newUnits.get(section.gachaID);

                result.append("\u001B[0;32m (+");

                for(int j = 0; j < units.length; j++) {
                    Unit u = UserProfile.getBCData().units.get(units[j]);

                    if(u == null)
                        continue;

                    String unit = StaticStore.safeMultiLangGet(u.forms[0], lang);

                    if(unit == null || unit.isBlank()) {
                        unit = LangID.getStringByID("printgacha_dummy", lang).replace("_", Data.trio(u.id.id));
                    }

                    result.append(unit);

                    if(j < units.length - 1) {
                        result.append(", ");
                    }
                }

                result.append(")");
            }

            if(hasAdditionalData(section)) {
                result.append("\u001B[0;36m [");

                if(section.rarityGuarantees.length > 3 && section.rarityGuarantees[3] == 1) {
                    result.append(LangID.getStringByID("printgacha_g", lang));

                    if(!section.additional.isEmpty())
                        result.append("|");
                }

                for(int j = 0; j < section.additional.size(); j++) {
                    result.append(getAdditionalCode(section.additional.get(j), lang));

                    if(j < section.additional.size() - 1)
                        result.append("|");
                }

                result.append("]");
            }

            if(GachaSet.gachaSet.containsKey(section.gachaID)) {
                GachaSet set = GachaSet.gachaSet.get(section.gachaID);

                if(!set.buffUnits.isEmpty()) {
                    result.append("\u001B[0;33m { ");

                    List<Integer> params = new ArrayList<>(set.buffUnits.keySet());

                    for(int k = 0; k < params.size(); k++) {
                        StringBuilder builder = new StringBuilder();

                        int param = params.get(k);

                        List<Unit> units = set.buffUnits.get(param);

                        for(int j = 0; j < units.size(); j++) {
                            String unitName = StaticStore.safeMultiLangGet(units.get(j).forms[0], lang);

                            if(unitName == null) {
                                unitName = LangID.getStringByID("printgacha_dummy", lang).replace("_", Data.trio(units.get(j).id.id));
                            }

                            builder.append(unitName);

                            if(j < units.size() - 1) {
                                builder.append(", ");
                            }
                        }

                        result.append(LangID.getStringByID("printgacha_buff", lang).replace("_BBB_", String.valueOf(param)).replace("_UUU_", builder.toString()));

                        if(k < params.size() - 1) {
                            result.append(" | ");
                        }
                    }

                    result.append(" }");
                }
            }

            if(section.gachaType == TYPE.RARE && hasWeirdChance(section)) {
                result.append(" ")
                        .append(getWeirdChanceData(section, lang));
            }

            if(i < gacha.size() - 1) {
                result.append(", ");
            }
        }

        if(getVersionNumber(minVersion) > StaticStore.safeParseInt(StaticStore.getVersion(locale))) {
            result.append("\u001B[0;35m <").append(LangID.getStringByID("event_newver", lang).replace("_", beautifyVersion(minVersion))).append(">");
        }

        return result.toString();
    }

    @Override
    public String dataToString(CommonStatic.Lang.Locale lang) {
        StringBuilder result = new StringBuilder("```");

        result.append(date.dateStart.year)
                .append("-")
                .append(getMonth(date.dateStart.month, lang))
                .append("-")
                .append(duo(date.dateStart.day))
                .append(" ")
                .append(duo(date.section.start.hour))
                .append(":")
                .append(duo(date.section.start.minute))
                .append(" ~ ");

        if(date.dateEnd.equals(END)) {
            result.append("```\n");
        } else {
            result.append(date.dateEnd.year)
                    .append("-")
                    .append(getMonth(date.dateEnd.month, lang))
                    .append("-")
                    .append(duo(date.dateEnd.day))
                    .append(" ")
                    .append(duo(date.section.start.hour))
                    .append(":")
                    .append(duo(date.section.start.minute))
                    .append("```\n");
        }

        result.append("From ").append(minVersion);

        if(!maxVersion.equals(NOMAX)) {
            result.append(" to ").append(maxVersion);
        }

        result.append("\n\n");

        for (EventSection section : sections) {
            if (!section.daySets.isEmpty()) {
                for (int i = 0; i < section.daySets.size(); i++) {
                    EventDateSet set = section.daySets.get(i);

                    result.append("[")
                            .append(getMonth(set.dateStart.month, lang))
                            .append("-")
                            .append(duo(set.dateStart.day))
                            .append(" ")
                            .append(duo(set.section.start.hour))
                            .append(":")
                            .append(duo(set.section.start.minute))
                            .append(" ~ ")
                            .append(getMonth(set.dateEnd.month, lang))
                            .append("-")
                            .append(duo(set.dateEnd.day))
                            .append(" ")
                            .append(duo(set.section.start.hour))
                            .append(":")
                            .append(duo(set.section.start.minute))
                            .append("]");

                    if (i < section.daySets.size() - 1)
                        result.append("\n");
                }
            }

            if(!section.daySets.isEmpty())
                result.append("\n");

            if (!section.days.isEmpty()) {
                result.append("{");

                for (int i = 0; i < section.days.size(); i++) {
                    result.append(section.days.get(i)).append(getNumberWithDayFormat(section.days.get(i), lang));

                    if (i < section.days.size() - 1)
                        result.append(", ");
                }

                result.append("} ");
            }

            if (!section.weekDays.isEmpty()) {
                result.append("{");

                for(int i = 0; i < section.weekDays.size(); i++) {
                    result.append(getWhichDay(section.weekDays.get(i), lang));

                    if(i < section.weekDays.size() - 1)
                        result.append(", ");
                }

                result.append("} ");
            }

            if (!section.times.isEmpty()) {
                result.append("(");

                for (int i = 0; i < section.times.size(); i++) {
                    EventTimeSection timeSection = section.times.get(i);

                    result.append(duo(timeSection.start.hour))
                            .append(":")
                            .append(duo(timeSection.start.minute))
                            .append(" ~ ")
                            .append(duo(timeSection.end.hour))
                            .append(":")
                            .append(duo(timeSection.end.minute));

                    if(i < section.times.size() - 1)
                        result.append(", ");
                }

                result.append(")");
            }

            result.append("\n\n");
        }

        for (GachaSection section : gacha) {
            result.append("<").append(section.index + 1).append(getNumberWithDayFormat(section.index + 1, lang)).append(" Gacha>\n\n");

            result.append("Gacha Name : ").append(tryGetGachaName(section, lang)).append("\n");
            result.append("Cf per roll : ").append(section.requiredCatFruit).append("\n");
            result.append("Rarity Data : ");

            for (int j = 0; j < section.rarityChances.length; j++) {
                if(section.rarityChances[j] != 0) {
                    if (j == 0)
                        result.append("Basic=").append(DataToString.df.format(section.rarityChances[j] / 100.0)).append("%");
                    else if (j == 1)
                        result.append("Rare=").append(DataToString.df.format(section.rarityChances[j] / 100.0)).append("%");
                    else if (j == 2)
                        result.append("SuperRare=").append(DataToString.df.format(section.rarityChances[j] / 100.0)).append("%");
                    else if (j == 3)
                        result.append("UberRare=").append(DataToString.df.format(section.rarityChances[j] / 100.0)).append("%");
                    else
                        result.append("LegendRare=").append(DataToString.df.format(section.rarityChances[j] / 100.0)).append("%");

                    if (section.rarityGuarantees[j] == 1)
                        result.append(" [Guaranteed]");

                    if (j < section.rarityChances.length - 1)
                        result.append(", ");
                }
            }

            result.append("\nMessage : ").append(section.message).append("\n\n");
        }

        return result.toString();
    }

    private int parse(String val) {
        return StaticStore.safeParseInt(val);
    }

    private String convertVersion(int version) {
        int main = version / 10000;

        version -= main * 10000;

        int major = version / 100;
        int minor = version % 100;

        return main+"."+major+"."+minor;
    }

    private String duo(int n) {
        if(n < 10)
            return "0"+n;
        else
            return String.valueOf(n);
    }

    public String tryGetGachaName(GachaSection section, CommonStatic.Lang.Locale lang) {
        int gachaID = section.gachaID;

        if(gachaID <= 100) {
            if(section.message != null && !section.message.trim().isBlank()) {
                return section.message.trim();
            } else {
                return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));
            }
        }

        String loc = switch (lang) {
            case EN -> "/en/";
            case ZH -> "/tw/";
            case KR -> "/kr/";
            default -> "/";
        };


        String url = GACHAURL.replace("_ID_", Data.trio(gachaID)).replace("_LLL_", loc);

        try {
            String html = getHtmlFromUrl(url);

            if(html == null) {
                if(section.message != null && !section.message.trim().isBlank()) {
                    return section.message.trim();
                } else {
                    return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));
                }
            }

            Matcher m = p.matcher(html);

            boolean res = m.find();

            if(!res) {
                if(section.message != null && !section.message.trim().isBlank()) {
                    return section.message.trim();
                } else {
                    return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));
                }
            }

            return m.group(0).replace("<h2>", "").replace("</h2>", "").replaceAll("<span.+</span>", "");
        } catch (Exception ignored) {
            if(section.message != null && !section.message.trim().isBlank()) {
                return section.message.trim();
            } else {
                return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));
            }
        }
    }

    private String getHtmlFromUrl(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();

        if(connection.getResponseCode() == 403)
            return null;

        Scanner scan = new Scanner(connection.getInputStream());

        scan.useDelimiter("\\Z");

        String content = scan.next();

        scan.close();

        return content;
    }

    private boolean hasAdditionalData(GachaSection section) {
        if(section.gachaType != TYPE.RARE)
            return false;

        return (section.rarityGuarantees.length > 3 && section.rarityGuarantees[3] == 1) ||
                (section.rarityGuarantees.length > 4 && section.rarityGuarantees[4] == 1) ||
                !section.additional.isEmpty();
    }

    private String getAdditionalCode(GachaSection.ADDITIONAL a, CommonStatic.Lang.Locale lang) {
        return switch (a) {
            case GRANDON -> LangID.getStringByID("printgacha_gr", lang);
            case STEP -> LangID.getStringByID("printgacha_s", lang);
            case LUCKY -> LangID.getStringByID("printgacha_l", lang);
            case SHARD -> LangID.getStringByID("printgacha_p", lang);
            case NENEKO -> LangID.getStringByID("printgacha_n", lang);
            default -> LangID.getStringByID("printgacha_r", lang);
        };
    }

    private boolean containAll(int[] data, int... ids) {
        for(int i = 0; i < ids.length; i++) {
            boolean found = false;

            for(int j = 0; j < data.length; j++) {
                if(data[j] == ids[i]) {
                    found = true;
                    break;
                }
            }

            if(!found)
                return false;
        }

        return true;
    }

    private boolean hasWeirdChance(GachaSection section) {
        if(section.rarityChances.length <= 3)
            return false;

        //Platinum/Legendary Gacha
        if(section.rarityChances[3] == 100 || section.rarityChances[3] == 95)
            return false;

        boolean weirdUR = section.rarityChances[3] != 5;

        if(!weirdUR && section.rarityGuarantees.length > 4) {
            return section.rarityChances[4] != 0 && section.rarityChances[4] != 0.3;
        }

        return weirdUR;
    }

    private String getWeirdChanceData(GachaSection section, CommonStatic.Lang.Locale lang) {
        String result = "\u001B[0;0m<";

        result += LangID.getStringByID("printgacha_chance", lang);

        if(section.rarityChances[3] != 5) {
            result += LangID.getStringByID("printgacha_uber", lang).replace("_", DataToString.df.format(section.rarityChances[3]));

            if(section.rarityChances.length > 4 && section.rarityChances[4] != 0 && section.rarityChances[4] != 0.3)
                result += " | ";
        }

        if(section.rarityChances.length > 4 && section.rarityChances[4] != 0 && section.rarityChances[4] != 0.3)
            result += LangID.getStringByID("printgacha_lr", lang).replace("_", DataToString.df.format(section.rarityChances[4]));

        return result + ">";
    }
}
