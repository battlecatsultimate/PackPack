package mandarin.packpack.supporter.event.group;

import mandarin.packpack.supporter.event.EventDateSet;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;

import java.util.Arrays;

public abstract class EventGroup extends EventFactor {
    public final String name;
    public final Schedule[] schedules;

    public boolean finished = false;

    protected EventDateSet date;

    public EventGroup(Schedule[] schedules, String name) {
        this.schedules = schedules;
        this.name = name;
    }

    public abstract boolean addStage(StageSchedule schedule, boolean raw);

    public EventDateSet getDate() {
        return date;
    }

    public Schedule[] getSchedules() {
        return schedules;
    }

    protected abstract void checkFinish();

    protected boolean arrayIsEmpty() {
        for(Schedule schedule : schedules) {
            if(schedule != null)
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "{ Finished : " + finished + " | Contents : " + Arrays.toString(schedules) + " }";
    }
}
