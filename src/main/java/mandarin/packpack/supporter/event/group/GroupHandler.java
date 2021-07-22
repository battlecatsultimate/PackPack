package mandarin.packpack.supporter.event.group;

import mandarin.packpack.supporter.event.Schedule;

import java.util.ArrayList;

public interface GroupHandler {
    boolean handleEvent(Schedule schedule);

    void clear();

    ArrayList<EventGroup> getGroups();
}
