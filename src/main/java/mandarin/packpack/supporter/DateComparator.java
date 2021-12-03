package mandarin.packpack.supporter;

import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.lang.LangID;

import java.util.Comparator;

public class DateComparator implements Comparator<String> {
    private final int lang;

    public DateComparator(int lang) {
        this.lang = lang;
    }

    @Override
    public int compare(String o1, String o2) {
        if(o1 == null)
            return -1;

        if(o2 == null)
            return 1;

        if(o1.equals(o2))
            return 0;

        int[] v = extractDate(o1);
        int[] v2 = extractDate(o2);

        if(v[0] < v2[0])
            return -1;
        else if(v[0] > v2[0])
            return 1;

        if(v[1] < v2[1])
            return  -1;
        else if(v[1] > v2[1])
            return 1;

        return Integer.compare(v[2], v2[2]);
    }

    private int[] extractDate(String value) {
        value = value.replace("[","");

        String[] contents = value.split(" ~ ", 2);

        if(contents.length != 2)
            throw new IllegalStateException("Content has invalid format : "+value);

        String[] date = contents[0].split(" ");

        int[] result = new int[3];

        if(date.length == 3) {
            if(StaticStore.isNumeric(date[0]))
                result[0] = StaticStore.safeParseInt(date[0]);

            result[1] = monthToNumber(date[1], lang);

            result[2] = StaticStore.safeParseInt(date[2].replaceAll("st|nd|rd|th|일|日", ""));
        } else if(date.length == 2) {
            result[0] = EventFactor.currentYear;

            result[1] = monthToNumber(date[0], lang);

            result[2] = StaticStore.safeParseInt(date[1].replaceAll("st|nd|rd|th|일|日", ""));
        }

        return result;
    }

    private int monthToNumber(String month, int lang) {
        if(month.equals(LangID.getStringByID("january", lang)))
            return 1;
        else if(month.equals(LangID.getStringByID("february", lang)))
            return 2;
        else if(month.equals(LangID.getStringByID("march", lang)))
            return 3;
        else if(month.equals(LangID.getStringByID("april", lang)))
            return 4;
        else if(month.equals(LangID.getStringByID("may", lang)))
            return 5;
        else if(month.equals(LangID.getStringByID("june", lang)))
            return 6;
        else if(month.equals(LangID.getStringByID("july", lang)))
            return 7;
        else if(month.equals(LangID.getStringByID("august", lang)))
            return 8;
        else if(month.equals(LangID.getStringByID("september", lang)))
            return 9;
        else if(month.equals(LangID.getStringByID("october", lang)))
            return 10;
        else if(month.equals(LangID.getStringByID("november", lang)))
            return 11;
        else if(month.equals(LangID.getStringByID("december", lang)))
            return 12;
        else
            return -1;
    }
}
