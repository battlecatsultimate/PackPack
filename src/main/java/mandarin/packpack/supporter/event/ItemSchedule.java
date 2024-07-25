package mandarin.packpack.supporter.event;

import common.CommonStatic;
import common.util.stage.MapColc;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.util.ArrayList;

public class ItemSchedule extends EventFactor implements Schedule {
    private final CommonStatic.Lang.Locale locale;

    public final ArrayList<EventSection> sections = new ArrayList<>();
    public final EventDateSet date;
    public final String minVersion, maxVersion;

    public int categoryID;
    public int itemID;
    public int itemAmount;
    public int eocClear;
    public boolean everyday;
    public String title;
    public String messsage;

    public ItemSchedule(String line, CommonStatic.Lang.Locale locale) {
        this.locale = locale;

        String[] data = line.split("\t");

        date = new EventDateSet(parse(data[0]), parse(data[2]), parse(data[1]), parse(data[3]));

        minVersion = convertVersion(parse(data[4]));
        maxVersion = convertVersion(parse(data[5]));

        if(parse(data[6]) == 0) {
            int sectionNumber = parse(data[7]);
            int index = 8;

            for (int j = 0; j < sectionNumber; j++) {
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

                    index += 2;
                }

                sections.add(section);
            }

            categoryID = parse(data[index]);
            itemID = parse(data[index + 1]);
            itemAmount = parse(data[index + 2]);

            title = data[index + 3];
            messsage = data[index + 4];

            eocClear = parse(data[index + 5]);
            everyday = parse(data[index + 7]) == 1;
        }
    }

    @Override
    public String beautify(CommonStatic.Lang.Locale lang) {
        StringBuilder result = new StringBuilder();

        result.append("\u001B[0;31m[");

        if(date.dateStart.year != currentYear) {
            result.append(date.dateStart.year)
                    .append(LangID.getStringByID("date.year", lang));

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
                            .append(LangID.getStringByID("date.year", lang));

                    if(lang != CommonStatic.Lang.Locale.ZH) {
                        result.append(" ");
                    }
                }

                if(date.dateStart.month != date.dateEnd.month || lang == CommonStatic.Lang.Locale.ZH) {
                    result.append(getMonth(date.dateEnd.month, lang));

                    if(lang != CommonStatic.Lang.Locale.ZH) {
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

        result.append("\u001B[1;38m").append(beautifyItem(lang, itemID, itemAmount));

        if(1000 <= categoryID && categoryID < 5000 && everyday) {
            result.append("\u001B[1;34m [").append(LangID.getStringByID("event.daily", lang)).append("]");
        }

        if(!title.isBlank() && !title.startsWith("←全画面")) {
            result.append("\u001B[0;33m {").append(title).append("}");
        }

        if(getVersionNumber(minVersion) > StaticStore.safeParseInt(StaticStore.getVersion(locale))) {
            result.append("\u001B[0;35m <").append(LangID.getStringByID("event.newVersion", lang).replace("_", beautifyVersion(minVersion))).append(">");
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

        result.append("Title : ").append(title).append("\n");
        result.append("Message : ").append(messsage).append("\n\n");

        result.append("CategoryID : ").append(categoryID).append("\n");
        result.append("ItemID : ").append(itemID);

        if(1000 <= categoryID && categoryID < 5000 && everyday)
            result.append(" [Everyday]");

        result.append("\n");

        result.append("Amount : ").append(itemAmount).append("\n");

        result.append("EoC Clear Status : ").append(getEoCName(eocClear));

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

    private String getEoCName(int status) {
        if(status == 100)
            return "Moon";
        else if(status < 48 && status > 0) {
            return StaticStore.safeMultiLangGet(MapColc.get("000003").maps.get(9).list.get(status-1), CommonStatic.Lang.Locale.EN);
        } else if(status == 0) {
            return "None";
        } else {
            return "Code "+status;
        }
    }
}
