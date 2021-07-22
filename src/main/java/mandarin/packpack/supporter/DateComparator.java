package mandarin.packpack.supporter;

import java.util.Comparator;

public class DateComparator implements Comparator<String> {
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

        String[] contents = value.split(" ", 4);

        if(contents.length != 4)
            throw new IllegalStateException("Content has invalid format : "+value);

        int[] result = new int[3];

        if(StaticStore.isNumeric(contents[0]))
            result[0] = StaticStore.safeParseInt(contents[0]);

        result[1] = monthToNumber(contents[1]);

        if(StaticStore.isNumeric(contents[2]))
            result[2] = StaticStore.safeParseInt(contents[2]);

        return result;
    }

    private int monthToNumber(String month) {
        switch (month) {
            case "January":
                return 1;
            case "February":
                return 2;
            case "March":
                return 3;
            case "April":
                return 4;
            case "May":
                return 5;
            case "June":
                return 6;
            case "July":
                return 7;
            case "August":
                return 8;
            case "September":
                return 9;
            case "October":
                return 10;
            case "November":
                return 11;
            default:
                return 12;
        }
    }
}
