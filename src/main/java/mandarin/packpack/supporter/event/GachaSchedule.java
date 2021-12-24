package mandarin.packpack.supporter.event;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.lang.LangID;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GachaSchedule extends EventFactor implements Schedule {
    private static final Pattern p = Pattern.compile("<h2>.+</h2>");

    private final int locale;

    public ArrayList<EventSection> sections = new ArrayList<>();
    public EventDateSet date;
    public String minVersion, maxVersion;

    public ArrayList<GachaSection> gacha = new ArrayList<>();

    public GachaSchedule(String line, int locale) {
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

                if(gachaID == -1 || gachaID == 0 || (gachaID > 5 && gachaID < 100 && !date.dateEnd.equals(END))) {
                    if(gachaType == 4) {
                        index += 17;
                    } else {
                        index += 15;
                    }
                } else {
                    GachaSection section = new GachaSection();

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
    public String beautify(int lang) {
        StringBuilder result = new StringBuilder();

        result.append("[");

        if(date.dateStart.year != currentYear) {
            result.append(date.dateStart.year)
                    .append(" ");
        }

        result.append(getMonth(date.dateStart.month, lang))
                .append(" ")
                .append(date.dateStart.day)
                .append(getNumberExtension(date.dateStart.day, lang));

        if(date.section.start.hour * 100 + date.section.start.minute != 1100 && date.section.start.hour * 100 + date.section.start.minute != 0) {
            result.append(" ")
                    .append(duo(date.section.start.hour))
                    .append(":")
                    .append(duo(date.section.start.minute));
        }

        result.append(" ~ ");

        if(!date.dateEnd.equals(END)) {
            if(date.dateStart.year != date.dateEnd.year) {
                result.append(date.dateEnd.year)
                        .append(" ");
            }

            if(date.dateStart.month != date.dateEnd.month) {
                result.append(getMonth(date.dateEnd.month, lang))
                        .append(" ");
            }

            result.append(date.dateEnd.day)
                    .append(getNumberExtension(date.dateEnd.day, lang));

            if(date.section.end.hour * 100 + date.section.end.minute != 1100) {
                result.append(" ")
                        .append(duo(date.section.end.hour))
                        .append(":")
                        .append(duo(date.section.end.minute));
            }
        }

        result.append("] ");

        for(int i = 0; i < gacha.size(); i++) {
            GachaSection section = gacha.get(i);

            if(section == null)
                continue;

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String g = StaticStore.GACHANAME.getCont(section.gachaID);

            CommonStatic.getConfig().lang = oldConfig;

            if(g == null)
                g = tryGetGachaName(section.gachaID, lang);

            result.append(g);

            if(newUnits.containsKey(section.gachaID)) {
                int[] units = newUnits.get(section.gachaID);

                result.append(" (+");

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
                result.append(" [");

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
                    result.append(" { ");

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

                        result.append(LangID.getStringByID("printgacha_buff", lang).replace("_BBB_", param+"").replace("_UUU_", builder.toString()));

                        if(k < params.size() - 1) {
                            result.append(" | ");
                        }
                    }

                    result.append(" }");
                }
            }

            if(i < gacha.size() - 1) {
                result.append(", ");
            }
        }

        if(getVersionNumber(minVersion) > StaticStore.safeParseInt(StaticStore.getVersion(locale))) {
            result.append(" <").append(LangID.getStringByID("event_newver", lang).replace("_", beautifyVersion(minVersion))).append(">");
        }

        return preventDotDecimal(result.toString().replace("1s", "1\u200Bs").replace("'", "’").replace(":", "："));
    }

    @Override
    public String dataToString(int lang) {
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
                    result.append(section.days.get(i)).append(getNumberExtension(section.days.get(i), lang));

                    if (i < section.days.size() - 1)
                        result.append(", ");
                }

                result.append("} ");
            }

            if (section.weekDays.size() != 0) {
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
            result.append("<").append(section.index + 1).append(getNumberExtension(section.index + 1, lang)).append(" Gacha>\n\n");

            result.append("Gacha Name : ").append(tryGetGachaName(section.gachaID, lang)).append("\n");
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
            return ""+n;
    }

    public String tryGetGachaName(int gachaID, int lang) {
        if(gachaID <= 100)
            return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));

        String loc;

        switch (lang) {
            case EN:
                loc = "/en/";
                break;
            case ZH:
                loc = "/tw/";
                break;
            case KR:
                loc = "/kr/";
                break;
            default:
                loc = "/";
        }


        String url = GACHAURL.replace("_ID_", Data.trio(gachaID)).replace("_LLL_", loc);

        try {
            String html = getHtmlFromUrl(url);

            if(html == null)
                return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));

            Matcher m = p.matcher(html);

            boolean res = m.find();

            if(!res) {
                return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));
            }

            return m.group(0).replace("<h2>", "").replace("</h2>", "").replaceAll("<span.+</span>", "");
        } catch (Exception ignored) {
            return LangID.getStringByID("printgacha_gacha", lang).replace("_", Data.trio(gachaID));
        }
    }

    private String getHtmlFromUrl(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if(connection.getResponseCode() == 403)
            return null;

        Scanner scan = new Scanner(connection.getInputStream());

        scan.useDelimiter("\\Z");

        String content = scan.next();

        scan.close();

        return content;
    }

    private boolean hasAdditionalData(GachaSection section) {
        return (section.rarityGuarantees.length > 3 && section.rarityGuarantees[3] == 1) ||
                (section.rarityGuarantees.length > 4 && section.rarityGuarantees[4] == 1) ||
                !section.additional.isEmpty();
    }

    private String getAdditionalCode(GachaSection.ADDITIONAL a, int lang) {
        switch (a) {
            case GRANDON:
                return LangID.getStringByID("printgacha_gr", lang);
            case STEP:
                return LangID.getStringByID("printgacha_s", lang);
            case LUCKY:
                return LangID.getStringByID("printgacha_l", lang);
            case SHARD:
                return LangID.getStringByID("printgacha_p", lang);
            case NENEKO:
                return LangID.getStringByID("printgacha_n", lang);
            default:
                return LangID.getStringByID("printgacha_r", lang);
        }
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
}
