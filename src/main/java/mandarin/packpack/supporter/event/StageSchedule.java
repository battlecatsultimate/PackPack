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

    private final int locale;

    public ArrayList<EventSection> sections = new ArrayList<>();
    public EventDateSet date;
    public String minVersion, maxVersion;
    public TYPE type;
    public boolean isMission;

    public ArrayList<StageMap> stages = new ArrayList<>();
    public ArrayList<String> unknownStages = new ArrayList<>();

    public StageSchedule(String line, int locale) {
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

                if (date.dateEnd.equals(END) && section.weekDays.size() != 0 && type == null) {
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
                    result.append(LangID.getStringByID("event_even", lang));
                } else if(isOddDays(j)) {
                    result.append(LangID.getStringByID("event_odd", lang));
                } else {
                    for (int i = 0; i < section.days.size(); i++) {
                        result.append(section.days.get(i)).append(getNumberExtension(section.days.get(i), lang));

                        if (i < section.days.size() - 1)
                            result.append(", ");
                    }
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

    public String beautifyWithCustomName(String name, int lang) {
        StringBuilder result = new StringBuilder();

        result.append("[");

        if(date.dateStart.year != currentYear) {
            result.append(date.dateStart.year)
                    .append(" ");
        }

        result.append(getMonth(date.dateStart.month, lang))
                .append(" ")
                .append(date.dateStart.day)
                .append(getNumberExtension(date.dateStart.day, lang))
                .append(" ~ ");

        if(date.dateEnd.equals(END)) {
            result.append("] ");
        } else {

            if(date.dateStart.year != date.dateEnd.year) {
                result.append(date.dateEnd.year)
                        .append(" ");
            }

            if(date.dateStart.month != date.dateEnd.month) {
                result.append(getMonth(date.dateEnd.month, lang))
                        .append(" ");
            }

            result.append(date.dateEnd.day)
                    .append(getNumberExtension(date.dateEnd.day, lang))
                    .append("] ");
        }

        result.append(name);

        return result.toString();
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
                .append(getNumberExtension(date.dateStart.day, lang))
                .append(" ~ ");

        if(date.dateEnd.equals(END)) {
            result.append("] ");
        } else {

            if(date.dateStart.year != date.dateEnd.year) {
                result.append(date.dateEnd.year)
                        .append(" ");
            }

            if(date.dateStart.month != date.dateEnd.month) {
                result.append(getMonth(date.dateEnd.month, lang))
                        .append(" ");
            }

            result.append(date.dateEnd.day)
                    .append(getNumberExtension(date.dateEnd.day, lang))
                    .append("] ");
        }

        if(!isMission) {
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

                    int oldConfig = CommonStatic.getConfig().lang;
                    CommonStatic.getConfig().lang = lang;

                    String trueForm = MultiLangCont.getStatic().RWNAME.getCont(id);

                    CommonStatic.getConfig().lang = oldConfig;

                    if(trueForm == null || trueForm.isBlank()) {
                        trueForm = Integer.toString(id + 13000);
                    }

                    units.append(trueForm);

                    if(i < unknownStages.size() - 1)
                        units.append(", ");
                }

                result.append(LangID.getStringByID("printstage_unlock", lang).replace("_", units.toString()));
            } else {
                for(int i = 0; i < unknownStages.size(); i++) {
                    int id = StaticStore.safeParseInt(unknownStages.get(i));

                    if(id >= 28000 && id < 29000) {
                        id -= 13000;

                        int oldConfig = CommonStatic.getConfig().lang;
                        CommonStatic.getConfig().lang = lang;

                        String trueForm = MultiLangCont.getStatic().RWNAME.getCont(id);

                        CommonStatic.getConfig().lang = oldConfig;

                        if(trueForm == null || trueForm.isBlank()) {
                            trueForm = Integer.toString(id + 13000);
                        }

                        result.append(LangID.getStringByID("printstage_unlock", lang).replace("_", trueForm));
                    } else {
                        String langID = "sale_"+id;

                        String temp = LangID.getStringByID(langID, lang);

                        if(!temp.equals(langID)) {
                            result.append(temp);
                        } else {
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

            result.append("(");

            for (int i = 0; i < sections.size(); i++) {
                EventSection section = sections.get(i);

                if (!section.daySets.isEmpty()) {
                    for (int j = 0; j < section.daySets.size(); j++) {
                        EventDateSet set = section.daySets.get(j);

                        result.append(getMonth(set.dateStart.month, lang))
                                .append(" ")
                                .append(set.dateStart.day)
                                .append(getNumberExtension(set.dateStart.day, lang))
                                .append(" ~ ")
                                .append(getMonth(set.dateEnd.month, lang))
                                .append(" ")
                                .append(set.dateEnd.day)
                                .append(getNumberExtension(set.dateEnd.day, lang));

                        if (j < section.daySets.size() - 1)
                            result.append(", ");
                    }

                    if(!section.days.isEmpty() || !section.weekDays.isEmpty() || !section.times.isEmpty()) {
                        result.append(" / ");
                    }
                }

                if (!section.days.isEmpty()) {
                    if (isEvenDays(i)) {
                        result.append(LangID.getStringByID("event_even", lang));
                    } else if (isOddDays(i)) {
                        result.append(LangID.getStringByID("event_odd", lang));
                    } else {
                        for (int j = 0; j < section.days.size(); j++) {
                            result.append(section.days.get(j))
                                    .append(getNumberExtension(section.days.get(j), lang));

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
                        result.append(getWhichDay(section.weekDays.get(j), lang));

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

                        result.append(duo(time.start.hour))
                                .append(":")
                                .append(duo(time.start.minute))
                                .append(" ~ ")
                                .append(duo(time.end.hour))
                                .append(":")
                                .append(duo(time.end.minute));

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
            result.append(" <").append(LangID.getStringByID("event_newver", lang).replace("_", beautifyVersion(minVersion))).append(">");
        }

        if(isMission) {
            result.append("\n\n");

            for(int i = 0; i < unknownStages.size(); i++) {
                int id = StaticStore.safeParseInt(unknownStages.get(i));

                boolean permanent = id >= 15000;

                if(permanent)
                    id -= 15000;

                int oldConfig = CommonStatic.getConfig().lang;
                CommonStatic.getConfig().lang = lang;

                String mission = StaticStore.MISSIONNAME.getCont(id);

                CommonStatic.getConfig().lang = oldConfig;

                if(mission == null) {
                    mission = LangID.getStringByID("printstage_mission", lang).replace("_", unknownStages.get(i));
                }

                result.append("\t").append(mission);

                if(missionReward.containsKey(id)) {
                    result.append(" -> ");

                    int[] data = missionReward.get(id);

                    switch (data[0]) {
                        case REWARDNORMAL:
                            result.append(beautifyGameItem(lang, data[1], data[2]));
                            break;
                        case REWARDUNIT:
                            Unit u = UserProfile.getBCData().units.get(data[1]);

                            String unit;

                            if(u == null) {
                                unit = LangID.getStringByID("printstage_unit", lang).replace("_", "" + data[1]);
                            } else {
                                unit = StaticStore.safeMultiLangGet(u.forms[0], lang);

                                if(unit == null || unit.isBlank()) {
                                    unit = LangID.getStringByID("printstage_unit", lang).replace("_", "" + data[1]);
                                }
                            }

                            result.append(unit);
                            break;
                        case REWARDTRUE:
                            u = UserProfile.getBCData().units.get(data[1]);

                            if(u == null) {
                                unit = LangID.getStringByID("printstage_true", lang).replace("_", "" + data[1]);
                            } else {
                                if(u.forms.length == 3) {
                                    unit  = StaticStore.safeMultiLangGet(u.forms[2], lang);

                                    if(unit == null || unit.isBlank()) {
                                        unit = LangID.getStringByID("printstage_true", lang).replace("_", "" + data[1]);
                                    }
                                } else {
                                    unit = LangID.getStringByID("printstage_true", lang).replace("_", "" + data[1]);
                                }
                            }

                            result.append(unit);
                            break;
                    }
                }

                if(permanent) {
                    result.append(" ").append(LangID.getStringByID("printstage_perma", lang));
                }

                if(i < unknownStages.size() - 1)
                    result.append("\n");
            }
        }

        return preventDotDecimal(result.toString().replace("1s", "1\u200Bs").replace("'", "’").replace(":", "："));
    }

    public boolean isEvenDays(int index) {
        if(index < 0 || index >= sections.size())
            return false;

        EventSection section = sections.get(index);

        int startDay = date.dateStart.day;
        int endDay = date.dateEnd.day;

        if(date.dateEnd.month > date.dateStart.month || date.dateEnd.equals(END)) {
            startDay = 1;
            endDay = 31;
        }

        ArrayList<Integer> even = new ArrayList<>();

        for(int i = startDay; i <= endDay; i++) {
            if(i % 2 == 0)
                even.add(i);
        }

        return section.days.containsAll(even);
    }

    public boolean isOddDays(int index) {
        if(index < 0 || index >= sections.size())
            return false;

        EventSection section = sections.get(index);

        int startDay = date.dateStart.day;
        int endDay = date.dateEnd.day;

        if(date.dateEnd.month > date.dateStart.month || date.dateEnd.equals(END)) {
            startDay = 1;
            endDay = 31;
        }

        ArrayList<Integer> odd = new ArrayList<>();

        for(int i = startDay; i <= endDay; i++) {
            if(i % 2 == 1)
                odd.add(i);
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
            return ""+n;
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

    @Override
    public String toString() {
        return "{ Type : "+type+" | Contents : " + beautify(0) +" }";
    }
}
