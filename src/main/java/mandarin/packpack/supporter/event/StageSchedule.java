package mandarin.packpack.supporter.event;

import common.util.Data;
import common.util.stage.MapColc;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;

import java.util.ArrayList;

public class StageSchedule extends EventFactor {
    public enum TYPE {
        DAILY,
        YEARLY,
        MONTHLY,
        WEEKLY
    }

    public ArrayList<EventSection> sections = new ArrayList<>();
    public EventDateSet date;
    public String minVersion, maxVersion;
    public TYPE type;

    public ArrayList<StageMap> stages = new ArrayList<>();
    public ArrayList<String> unknownStages = new ArrayList<>();

    public StageSchedule(String line) {
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
        }
    }

    public String dataToString() {
        StringBuilder result = new StringBuilder("```");

        result.append(date.dateStart.year)
                .append("-")
                .append(getMonth(date.dateStart.month))
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
                    .append(getMonth(date.dateEnd.month))
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
                            .append(getMonth(set.dateStart.month))
                            .append("-")
                            .append(duo(set.dateStart.day))
                            .append(" ")
                            .append(duo(set.section.start.hour))
                            .append(":")
                            .append(duo(set.section.start.minute))
                            .append(" ~ ")
                            .append(getMonth(set.dateEnd.month))
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
                    result.append("Every even day");
                } else if(isOddDays(j)) {
                    result.append("Every odd day");
                } else {
                    for (int i = 0; i < section.days.size(); i++) {
                        result.append(section.days.get(i)).append(getNumberExtension(section.days.get(i)));

                        if (i < section.days.size() - 1)
                            result.append(", ");
                    }
                }

                result.append("} ");
            }

            if (section.weekDays.size() != 0) {
                result.append("{");

                for(int i = 0; i < section.weekDays.size(); i++) {
                    result.append(getWhichDay(section.weekDays.get(i)));

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

            String mapName = StaticStore.safeMultiLangGet(map, 0);

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
                result.append("Speical Mission Code ").append(unknownStages.get(i));
            } else {
                result.append("Unknown Stage Code ").append(unknownStages.get(i));
            }

            if(i < unknownStages.size() - 1)
                result.append(", ");
        }

        return result.toString();
    }

    public boolean isEvenDays(int index) {
        if(index < 0 || index >= sections.size())
            return false;

        EventSection section = sections.get(index);

        int startDay = date.dateStart.day;
        int endDay = date.dateEnd.day;

        if(date.dateEnd.month > date.dateStart.month) {
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

        if(date.dateEnd.month > date.dateStart.month) {
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

    private String getMonth(int mon) {
        switch (mon) {
            case 1:
                return "January";
            case 2:
                return "February";
            case 3:
                return "March";
            case 4:
                return "April";
            case 5:
                return "May";
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            case 12:
                return "December";
            default:
                return "Unknown Month "+mon;
        }
    }

    private String getNumberExtension(int num) {
        if(num != 11 && num % 10 == 1)
            return "st";
        else if(num != 12 && num % 10 == 2)
            return "nd";
        else if(num != 13 && num % 10 == 3)
            return "rd";
        else
            return "th";
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

    private String getWhichDay(int data) {
        switch (data) {
            case MONDAY:
                return "Monday";
            case TUESDAY:
                return "Tuesday";
            case WEDNESDAY:
                return "Wednesday";
            case THURSDAY:
                return "Thursday";
            case FRIDAY:
                return "Friday";
            case SATURDAY:
                return "Saturday";
            case SUNDAY:
                return "Sunday";
            case WEEKEND:
                return "Weekend";
            default:
                return "Unknown day "+data;
        }
    }

    private StageMap tryToGetMap(int value) {
        int mc = value/1000;
        int map = value % 1000;

        MapColc mapColc = MapColc.get(Data.hex(mc));

        if(mapColc == null)
            return null;

        return mapColc.maps.get(map);
    }

    private String duo(int n) {
        if(n < 10)
            return "0"+n;
        else
            return ""+n;
    }
}
