package mandarin.packpack.supporter.event;

import common.CommonStatic;
import common.pack.UserProfile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.StageMap;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.util.ArrayList;

public class StageSchedule extends EventFactor implements Schedule {
    public enum TYPE {
        DAILY,
        YEARLY,
        MONTHLY,
        WEEKLY
    }

    private final CommonStatic.Lang.Locale locale;

    public final ArrayList<EventSection> sections = new ArrayList<>();
    public final EventDateSet date;
    public final String minVersion, maxVersion;
    public TYPE type;
    public boolean isMission;

    public final ArrayList<StageMap> stages = new ArrayList<>();
    public final ArrayList<String> unknownStages = new ArrayList<>();

    public StageSchedule(String line, CommonStatic.Lang.Locale locale) {
        this.locale = locale;

        int[] data = parseInts(line);

        date = new EventDateSet(data[0], data[2], data[1], data[3]);

        minVersion = convertVersion(data[4]);
        maxVersion = convertVersion(data[5]);

        if(data[6] == 0) {
            int sectionNumber = data[7];
            int index = 8;

            for(int j = 0; j < sectionNumber; j++) {
                EventSection section = new EventSection();

                int daySetNumber = data[index];

                index++;

                if (date.dateEnd.equals(END) && daySetNumber != 0 && type == null) {
                    type = TYPE.YEARLY;
                }

                for (int i = 0; i < daySetNumber; i++) {
                    EventDateSet set = new EventDateSet(data[index], data[index + 2], data[index + 1], data[index + 3]);

                    section.daySets.add(set);

                    index += 4;
                }

                int dayNumber = data[index];

                index++;

                if (date.dateEnd.equals(END) && dayNumber != 0 && type == null)
                    type = TYPE.MONTHLY;

                for (int i = 0; i < dayNumber; i++) {
                    int day = data[index];

                    section.days.add(day);

                    index++;
                }

                section.parseWeekDay(data[index]);

                if (date.dateEnd.equals(END) && !section.weekDays.isEmpty() && type == null) {
                    type = TYPE.WEEKLY;
                } else if (date.dateEnd.equals(END) && type == null)
                    type = TYPE.DAILY;

                index++;

                int timeNumber = data[index];

                index++;

                for (int i = 0; i < timeNumber; i++) {
                    EventTimeSection timeSection = new EventTimeSection(data[index], data[index + 1]);

                    section.times.add(timeSection);

                    index+= 2;
                }

                sections.add(section);
            }

            int stageNum = data[index];

            index++;

            for(int k = 0; k < stageNum; k++) {
                StageMap map = tryToGetMap(data[index]);

                if(map == null) {
                    unknownStages.add(String.valueOf(data[index]));
                } else {
                    stages.add(map);
                }

                index++;
            }

            isMission = onlyHoldMissions(this);
        }
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

        if(type == TYPE.YEARLY) {
            result.append("<Yearly>\n\n");
        } else if(type == TYPE.MONTHLY) {
            result.append("<Monthly>\n\n");
        } else if(type == TYPE.WEEKLY) {
            result.append("<Weekly>\n\n");
        } else if(type == TYPE.DAILY) {
            result.append("<Everyday>\n\n");
        } else if (type == null)
            result.append("<Normal>\n\n");

        for (int j = 0; j < sections.size(); j++) {
            EventSection section = sections.get(j);

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

                if(isEvenDays(j)) {
                    result.append(LangID.getStringByID("event.evenDay", lang));
                } else if(isOddDays(j)) {
                    result.append(LangID.getStringByID("event.oddDay", lang));
                } else {
                    for (int i = 0; i < section.days.size(); i++) {
                        result.append(section.days.get(i)).append(getNumberWithDayFormat(section.days.get(i), lang));

                        if (i < section.days.size() - 1)
                            result.append(", ");
                    }
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

        for(int i = 0; i < stages.size(); i++) {
            StageMap map = stages.get(i);

            if(map == null)
                continue;

            String mapName = StaticStore.safeMultiLangGet(map, lang);

            if(mapName == null || mapName.isBlank()) {
                mapName = map.getCont().getSID()+"-"+Data.trio(map.id.id);
            }

            result.append(mapName);

            if(i < stages.size() - 1)
                result.append(", ");
        }

        if(!stages.isEmpty()) {
            result.append("\n");
        }

        for(int i = 0; i < unknownStages.size(); i++) {
            int id = StaticStore.safeParseInt(unknownStages.get(i));

            if (id >= 8000 && id < 9000) {
                result.append("Weekly Mission Code ").append(unknownStages.get(i));
            } else if(id >= 9000 && id < 10000) {
                result.append("Special Mission Code ").append(unknownStages.get(i));
            } else {
                result.append("Unknown Stage Code ").append(unknownStages.get(i));
            }

            if(i < unknownStages.size() - 1)
                result.append(", ");
        }

        return result.toString();
    }

    public String beautifyWithCustomName(String name, CommonStatic.Lang.Locale lang) {
        StringBuilder result = new StringBuilder();

        result.append("\u001B[0;31m[");

        if(date.dateStart.year != currentYear || lang == CommonStatic.Lang.Locale.ZH) {
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
                .append(getNumberWithDayFormat(date.dateStart.day, lang))
                .append(" ~ ");

        if(date.dateEnd.equals(END)) {
            result.append("] ");
        } else {

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
                    .append(getNumberWithDayFormat(date.dateEnd.day, lang))
                    .append("] ");
        }

        result.append("\u001B[1;38m").append(name);

        return result.toString();
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

        final int startTime = date.section.start.hour * 100 + date.section.start.minute;

        if(startTime != 1100 && startTime != 0) {
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

                if(date.dateStart.month != date.dateEnd.month || (date.dateStart.year == currentYear && lang == CommonStatic.Lang.Locale.ZH)) {
                    result.append(getMonth(date.dateEnd.month, lang));

                    if(lang != CommonStatic.Lang.Locale.ZH) {
                        result.append(" ");
                    }
                }

                result.append(date.dateEnd.day)
                        .append(getNumberWithDayFormat(date.dateEnd.day, lang));

                final int endTime = date.section.end.hour * 100 + date.section.end.minute;

                if(endTime != 1100 && endTime != 2359) {
                    result.append(" ")
                            .append(duo(date.section.end.hour))
                            .append(":")
                            .append(duo(date.section.end.minute));
                }
            }
        }

        result.append("] ");

        if(!isMission) {
            result.append("\u001B[1;38m");

            for(int i = 0; i < stages.size(); i++) {
                StageMap map = stages.get(i);

                if(map == null || map.id == null || map.getCont() == null)
                    continue;

                String mapName = StaticStore.safeMultiLangGet(map, lang);

                if(mapName == null || mapName.isBlank()) {
                    mapName = map.getCont().getSID()+"-"+Data.trio(map.id.id);
                }

                result.append(mapName);

                if(i < stages.size() - 1) {
                    result.append(", ");
                }
            }

            if(!stages.isEmpty() && !unknownStages.isEmpty()) {
                result.append(", ");
            }

            if(isOnlyTrueForm()) {
                StringBuilder units = new StringBuilder();

                for(int i = 0; i < unknownStages.size(); i++) {
                    int id = StaticStore.safeParseInt(unknownStages.get(i)) - 13000;

                    String trueForm = MultiLangCont.getStatic().RWNAME.getCont(id, lang);

                    if (trueForm == null || trueForm.isBlank()) {
                        trueForm = Integer.toString(id + 13000);
                    }

                    units.append(trueForm);

                    if (i < unknownStages.size() - 1)
                        units.append(", ");
                }

                result.append(LangID.getStringByID("event.unlock", lang).replace("_", units.toString()));
            } else {
                for(int i = 0; i < unknownStages.size(); i++) {
                    int id = StaticStore.safeParseInt(unknownStages.get(i));

                    if(id >= 28000 && id < 29000) {
                        id -= 13000;

                        String trueForm = MultiLangCont.getStatic().RWNAME.getCont(id, lang);

                        if (trueForm == null || trueForm.isBlank()) {
                            trueForm = Integer.toString(id + 13000);
                        }

                        result.append(LangID.getStringByID("event.unlock", lang).replace("_", trueForm));
                    } else if(id >= 18000 && id < 18100) {
                        int year = id - 18000 + 7;

                        result.append(LangID.getStringByID("event.sale.18xxx", lang).replace("_", (String.valueOf(year)).repeat(3)));
                    } else {
                        String langID = "event.sale."+id;

                        String temp = LangID.getStringByID(langID, lang);

                        if(!temp.equals(langID)) {
                            result.append(temp);
                        } else {
                            if(id >= 18100 && id < 18200)
                                result.append(LangID.getStringByID("event.sale.181xx", lang));
                            else
                                result.append(id);
                        }
                    }

                    if(i < unknownStages.size() - 1)
                        result.append(", ");
                }
            }
        }

        if(!sections.isEmpty()) {
            if(!isMission)
                result.append(" ");

            result.append("\u001B[0;38m(");

            for (int i = 0; i < sections.size(); i++) {
                EventSection section = sections.get(i);

                if (!section.daySets.isEmpty()) {
                    for (int j = 0; j < section.daySets.size(); j++) {
                        EventDateSet set = section.daySets.get(j);

                        result.append("\u001B[0;36m");

                        result.append(getMonth(set.dateStart.month, lang))
                                .append(" ")
                                .append(set.dateStart.day)
                                .append(getNumberWithDayFormat(set.dateStart.day, lang))
                                .append(" ~ ")
                                .append(getMonth(set.dateEnd.month, lang))
                                .append(" ")
                                .append(set.dateEnd.day)
                                .append(getNumberWithDayFormat(set.dateEnd.day, lang));

                        result.append("\u001B[0;38m");

                        if (j < section.daySets.size() - 1)
                            result.append(", ");
                    }

                    if(!section.days.isEmpty() || !section.weekDays.isEmpty() || !section.times.isEmpty()) {
                        result.append(" / ");
                    }
                }

                if (!section.days.isEmpty()) {
                    if (isEvenDays(i)) {
                        result.append("\u001B[0;33m")
                                .append(LangID.getStringByID("event.evenDay", lang))
                                .append("\u001B[0;38m");
                    } else if (isOddDays(i)) {
                        result.append("\u001B[0;33m")
                                .append(LangID.getStringByID("event.oddDay", lang))
                                .append("\u001B[0;38m");
                    } else {
                        for (int j = 0; j < section.days.size(); j++) {
                            result.append("\u001B[0;36m")
                                    .append(section.days.get(j))
                                    .append(getNumberWithDayFormat(section.days.get(j), lang))
                                    .append("\u001B[0;38m");

                            if (j < section.days.size() - 1) {
                                result.append(", ");
                            }
                        }
                    }

                    if(!section.weekDays.isEmpty() || !section.times.isEmpty()) {
                        result.append(" / ");
                    }
                }

                if (!section.weekDays.isEmpty()) {
                    for (int j = 0; j < section.weekDays.size(); j++) {
                        result.append("\u001B[0;34m")
                                .append(getWhichDay(section.weekDays.get(j), lang))
                                .append("\u001B[0;38m");

                        if (j < section.weekDays.size() - 1) {
                            result.append(", ");
                        }
                    }

                    if(!section.times.isEmpty()) {
                        result.append(" / ");
                    }
                }

                if (!section.times.isEmpty()) {
                    for (int j = 0; j < section.times.size(); j++) {
                        EventTimeSection time = section.times.get(j);

                        result.append("\u001B[0;32m")
                                .append(duo(time.start.hour))
                                .append(":")
                                .append(duo(time.start.minute))
                                .append(" ~ ")
                                .append(duo(time.end.hour))
                                .append(":")
                                .append(duo(time.end.minute))
                                .append("\u001B[0;38m");

                        if (j < section.times.size() - 1) {
                            result.append(", ");
                        }
                    }
                }

                if (i < sections.size() - 1) {
                    result.append(" | ");
                }
            }

            result.append(")");
        }

        if(getVersionNumber(minVersion) > StaticStore.safeParseInt(StaticStore.getVersion(locale))) {
            result.append("\u001B[0;35m <").append(LangID.getStringByID("event.newVersion", lang).replace("_", beautifyVersion(minVersion))).append(">");
        }

        if(isMission) {
            result.append("\n\n");

            for(int i = 0; i < unknownStages.size(); i++) {
                int id = StaticStore.safeParseInt(unknownStages.get(i));

                boolean permanent = id >= 15000;

                if(permanent)
                    id -= 15000;

                String mission = StaticStore.MISSIONNAME.getCont(id, lang);

                if(mission == null) {
                    mission = LangID.getStringByID("event.dummy.mission", lang).replace("_", unknownStages.get(i));
                }

                result.append("  \u001B[1;38m").append(mission);

                if(missionReward.containsKey(id)) {
                    result.append("\u001B[0;38m -> ");

                    int[] data = missionReward.get(id);

                    result.append("\u001B[1;33m");

                    switch (data[0]) {
                        case REWARDNORMAL -> result.append(beautifyGameItem(lang, data[1], data[2]));
                        case REWARDUNIT -> {
                            Unit u = UserProfile.getBCData().units.get(data[1]);

                            String unit;

                            if (u == null) {
                                unit = LangID.getStringByID("event.unit", lang).replace("_", String.valueOf(data[1]));
                            } else {
                                unit = StaticStore.safeMultiLangGet(u.forms[0], lang);

                                if (unit == null || unit.isBlank()) {
                                    unit = LangID.getStringByID("event.unit", lang).replace("_", String.valueOf(data[1]));
                                }
                            }

                            result.append(unit);
                        }
                        case REWARDTRUE -> {
                            Unit u = UserProfile.getBCData().units.get(data[1]);

                            String unit;

                            if (u == null) {
                                unit = LangID.getStringByID("event.trueForm", lang).replace("_", String.valueOf(data[1]));
                            } else {
                                if (u.forms.length == 3) {
                                    unit = StaticStore.safeMultiLangGet(u.forms[2], lang);

                                    if (unit == null || unit.isBlank()) {
                                        unit = LangID.getStringByID("event.trueForm", lang).replace("_", String.valueOf(data[1]));
                                    }
                                } else {
                                    unit = LangID.getStringByID("event.trueForm", lang).replace("_", String.valueOf(data[1]));
                                }
                            }

                            result.append(unit);
                        }
                    }
                }

                if(permanent) {
                    result.append(" \u001B[0;34m").append(LangID.getStringByID("event.permanentTag", lang));
                }

                if(i < unknownStages.size() - 1)
                    result.append("\n");
            }
        }

        return result.toString();
    }

    public boolean isEvenDays(int index) {
        if(index < 0 || index >= sections.size())
            return false;

        EventSection section = sections.get(index);

        ArrayList<Integer> even = new ArrayList<>();

        if((date.dateStart.year == date.dateEnd.year && date.dateEnd.month - date.dateStart.month > 1) || (date.dateEnd.year - date.dateStart.year == 1  && date.dateEnd.month + 12 - date.dateStart.month > 1) || date.dateEnd.year - date.dateStart.year > 1 || date.dateEnd.equals(END)) {
            for(int i = 1; i <= 31; i++) {
                if(i % 2 == 0)
                    even.add(i);
            }
        } else {
            int startStart = date.dateStart.day;
            int startEnd = getMaxMonthDay(date.dateStart.year, date.dateStart.month);

            int endStart = 1;
            int endEnd = date.dateEnd.day;

            if(date.section.end.hour == 0 && date.section.end.minute == 0)
                endEnd--;

            for(int i = startStart; i <= startEnd; i++) {
                if(i % 2 == 0)
                    even.add(i);
            }

            for(int i = endStart; i <= endEnd; i++) {
                if(i % 2 == 0 && !even.contains(i))
                    even.add(i);
            }
        }

        return section.days.containsAll(even);
    }

    public boolean isOddDays(int index) {
        if(index < 0 || index >= sections.size())
            return false;

        EventSection section = sections.get(index);

        ArrayList<Integer> odd = new ArrayList<>();

        if((date.dateStart.year == date.dateEnd.year && date.dateEnd.month - date.dateStart.month > 1) || (date.dateEnd.year - date.dateStart.year == 1  && date.dateEnd.month + 12 - date.dateStart.month > 1) || date.dateEnd.year - date.dateStart.year > 1 || date.dateEnd.equals(END)) {
            for(int i = 1; i <= 31; i++) {
                if(i % 2 == 1)
                    odd.add(i);
            }
        } else {
            int startStart = date.dateStart.day;
            int startEnd = getMaxMonthDay(date.dateStart.year, date.dateStart.month);

            int endStart = 1;
            int endEnd = date.dateEnd.day;

            if(date.section.end.hour == 0 && date.section.end.minute == 0)
                endEnd--;

            for(int i = startStart; i <= startEnd; i++) {
                if(i % 2 == 1)
                    odd.add(i);
            }

            for(int i = endStart; i <= endEnd; i++) {
                if(i % 2 == 1 && !odd.contains(i))
                    odd.add(i);
            }
        }

        return section.days.containsAll(odd);
    }

    private int[] parseInts(String line) {
        String[] segments = line.split("\t");

        int[] result = new int[segments.length];

        for(int i = 0; i < result.length; i++) {
            if(StaticStore.isNumeric(segments[i])) {
                result[i] = StaticStore.safeParseInt(segments[i]);
            } else {
                System.out.println("WARNING : None integer in event data : "+line+" | Segment : "+segments[i]);
            }
        }

        return result;
    }

    private String convertVersion(int version) {
        int main = version / 10000;

        version -= main * 10000;

        int major = version / 100;
        int minor = version % 100;

        return main+"."+major+"."+minor;
    }

    private StageMap tryToGetMap(int value) {
        if(value / 1000 == 0 || value / 1000 == 3)
            return null;

        int mc = value/1000;
        int map = value % 1000;

        MapColc mapColc = MapColc.get(Data.hex(mc));

        if(mapColc == null)
            return null;

        if(map >= mapColc.maps.size())
            return null;

        return mapColc.maps.get(map);
    }

    private String duo(int n) {
        if(n < 10)
            return "0"+n;
        else
            return String.valueOf(n);
    }

    private boolean isOnlyTrueForm() {
        if(unknownStages.isEmpty())
            return false;

        for(int i = 0; i < unknownStages.size(); i++) {
            if (!StaticStore.isNumeric(unknownStages.get(i))) {
                return false;
            }

            int id = StaticStore.safeParseInt(unknownStages.get(i));

            if(id < 28000 || id >= 29000)
                return false;
        }

        return true;
    }

    private int getMaxMonthDay(int year, int month) {
        switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> {
                return 31;
            }
            case 2 -> {
                if (year % 4 == 0)
                    return 29;
                else
                    return 28;
            }
            case 4, 6, 9, 11 -> {
                return 30;
            }
            default -> throw new IllegalStateException("Wrong month value : " + month);
        }
    }

    @Override
    public String toString() {
        return "{ Type : "+type+" | Contents : " + beautify(CommonStatic.Lang.Locale.EN) +" }";
    }
}
