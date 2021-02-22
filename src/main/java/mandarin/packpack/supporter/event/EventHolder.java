package mandarin.packpack.supporter.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class EventHolder {
    public static int currentYear;

    public ArrayList<Integer> monthly = new ArrayList<>();
    public ArrayList<Integer> weekly = new ArrayList<>();

    static {
        Calendar c = Calendar.getInstance();

        currentYear = c.get(Calendar.YEAR);
    }

}
