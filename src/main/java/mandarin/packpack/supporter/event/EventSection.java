package mandarin.packpack.supporter.event;

import java.util.ArrayList;

public class EventSection {
    public ArrayList<EventDateSet> daySets = new ArrayList<>();
    public ArrayList<Integer> days = new ArrayList<>();
    public ArrayList<EventTimeSection> times = new ArrayList<>();
    public ArrayList<Integer> weekDays = new ArrayList<>();

    public void parseWeekDay(int mask) {
        for(int i = 0; i < 7; i++) {
            if(((mask >> i) & 1) > 0)
                weekDays.add(1 << i);
        }
    }
}
