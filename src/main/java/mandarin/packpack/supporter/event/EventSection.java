package mandarin.packpack.supporter.event;

import java.util.ArrayList;

public class EventSection {
    public final ArrayList<EventDateSet> daySets = new ArrayList<>();
    public final ArrayList<Integer> days = new ArrayList<>();
    public final ArrayList<EventTimeSection> times = new ArrayList<>();
    public final ArrayList<Integer> weekDays = new ArrayList<>();

    public void parseWeekDay(int mask) {
        if(mask == 65) {
            weekDays.add(65);
            return;
        }

        for(int i = 0; i < 7; i++) {
            if(((mask >> i) & 1) > 0)
                weekDays.add(1 << i);
        }
    }
}
