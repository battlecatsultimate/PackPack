package mandarin.packpack.supporter.event;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import java.util.Comparator;

public class DateComparator implements Comparator<String> {
    private final CommonStatic.Lang.Locale lang;

    public DateComparator(CommonStatic.Lang.Locale lang) {
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
        String[] pre = value.split("m\\[", 2);

        if(pre.length != 2) {
            throw new IllegalStateException("Content has invalid format : "+value);
        }

        value = pre[1];

        String[] contents = value.split(" ~ ", 2);

        if(contents.length != 2) {
            contents = value.split("]", 2);

            if(contents.length != 2)
                throw new IllegalStateException("Content has invalid format : "+value);
        }

        String[] date = contents[0].split(" ");

        int[] result = new int[3];

        if(date.length == 3) {
            if(StaticStore.isNumeric(date[0]))
                result[0] = StaticStore.safeParseInt(date[0]);

            result[1] = monthToNumber(date[1], lang);

            result[2] = CommonStatic.parseIntN(date[2]);
        } else if(date.length == 2) {
            result[0] = EventFactor.currentYear;

            result[1] = monthToNumber(date[0], lang);

            result[2] = CommonStatic.parseIntN(date[1]);
        }

        return result;
    }

    private int monthToNumber(String month, CommonStatic.Lang.Locale lang) {
        if(month.equals(LangID.getStringByID("date.monthOfYear.january", lang)))
            return 1;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.february", lang)))
            return 2;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.march", lang)))
            return 3;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.april", lang)))
            return 4;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.may", lang)))
            return 5;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.june", lang)))
            return 6;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.july", lang)))
            return 7;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.august", lang)))
            return 8;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.september", lang)))
            return 9;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.october", lang)))
            return 10;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.november", lang)))
            return 11;
        else if(month.equals(LangID.getStringByID("date.monthOfYear.december", lang)))
            return 12;
        else
            return -1;
    }
}
