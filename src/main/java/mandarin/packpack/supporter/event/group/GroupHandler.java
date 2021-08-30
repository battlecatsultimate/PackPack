package mandarin.packpack.supporter.event.group;

import mandarin.packpack.supporter.event.Schedule;

import java.util.List;

public interface GroupHandler {
    boolean handleEvent(Schedule schedule);

    void clear();

    List<EventGroup> getGroups();
}
