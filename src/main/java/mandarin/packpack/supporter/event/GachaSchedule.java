package mandarin.packpack.supporter.event;

import common.util.Data;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GachaSchedule extends EventFactor {
    private static final Pattern p = Pattern.compile("<h2>.+</h2>");

    public ArrayList<EventSection> sections = new ArrayList<>();
    public EventDateSet date;
    public String minVersion, maxVersion;

    public ArrayList<GachaSection> gacha = new ArrayList<>();

    public GachaSchedule(String line) {
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

            int existingGacha = parse(data[index]);

            index++;

            int categoryNum = parse(data[index]);

            int checker = 0;

            index++;

            for(int i = 0; i < categoryNum; i++) {
                int gachaID = parse(data[index]);

                if(gachaID == -1) {
                    index += 15;
                } else {
                    GachaSection section = new GachaSection();

                    section.gachaID = gachaID;
                    section.index = i;

                    index++;

                    section.requiredCatFruit = parse(data[index]);

                    index++;

                    section.addition[0] = parse(data[index]);

                    index++;

                    section.addition[1] = parse(data[index]);

                    index++;

                    for(int j = 0; j < 5; j++) {
                        section.rarityChances[j] = parse(data[index + 2 * j]);
                        section.rarityGuarantees[j] = parse(data[index + 1 + 2 * j]);
                    }

                    index += 10;

                    section.message = data[index];

                    gacha.add(section);

                    index++;
                    checker++;
                }
            }

            if(checker != existingGacha) {
                System.out.println("Warning : Existing number of gacha doesn't match : "+checker+" | "+existingGacha);
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

        for (EventSection section : sections) {
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

                for (int i = 0; i < section.days.size(); i++) {
                    result.append(section.days.get(i)).append(getNumberExtension(section.days.get(i)));

                    if (i < section.days.size() - 1)
                        result.append(", ");
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

        for (GachaSection section : gacha) {
            result.append("<").append(section.index + 1).append(getNumberExtension(section.index + 1)).append(" Gacha>\n\n");

            result.append("Gacha Name : ").append(tryGetGachaName(section.gachaID)).append("\n");
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

    public String tryGetGachaName(int gachaID) {
        if(gachaID <= 100)
            return "Gacha Code "+Data.trio(gachaID);

        String url = GACHAURL.replace("___", Data.trio(gachaID));

        try {
            String html = getHtmlFromUrl(url);

            Matcher m = p.matcher(html);

            boolean res = m.find();

            if(!res) {
                return "Gacha Code " + Data.trio(gachaID);
            }

            return m.group(0).replace("<h2>", "").replace("</h2>", "").replaceAll("<span.+</span>", "") + " ["+Data.trio(gachaID)+"]";
        } catch (Exception e) {
            e.printStackTrace();

            return "Gacha Code " + Data.trio(gachaID);
        }
    }

    private String getHtmlFromUrl(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();

        Scanner scan = new Scanner(connection.getInputStream());

        scan.useDelimiter("\\Z");

        String content = scan.next();

        scan.close();

        return content;
    }
}
