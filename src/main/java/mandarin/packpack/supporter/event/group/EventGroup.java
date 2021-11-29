package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventDateSet;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventGroup extends EventFactor {
    public final String name;
    public boolean finished = false;
    public final Schedule[] schedules;

    private EventDateSet date;
    private final List<List<Integer>> segmentID;

    public EventGroup(List<List<Integer>> segmentID, String name) {
        this.segmentID = segmentID;
        this.name = name;
        schedules = new Schedule[segmentID.size()];
    }

    public boolean addStage(StageSchedule schedule, boolean raw) {
        int index;

        if(!raw) {
            index = findIdenticalGroup(schedule);
        } else {
            index = findRawGroup(schedule);
        }

        //No valid segment found
        if(index == -1)
            return false;

        //Check if group is already occupied
        if(schedules[index] != null)
            return false;

        //Then finally allocate this schedule
        boolean empty = arrayIsEmpty();
        schedules[index] = schedule;

        if(empty) {
            //If array was empty, initialize date
            date = schedule.date;
        }

        checkFinish();

        return true;
    }

    public EventDateSet getDate() {
        return date;
    }

    public Schedule[] getSchedules() {
        return schedules;
    }

    private void checkFinish() {
        for(Schedule schedule : schedules) {
            if(schedule == null) {
                finished = false;
                return;
            }
        }

        finished = true;
    }

    private boolean arrayIsEmpty() {
        for(Schedule schedule : schedules) {
            if(schedule != null)
                return false;
        }

        return true;
    }

    private int findIdenticalGroup(StageSchedule schedule) {
        for(int i = 0; i < segmentID.size(); i++) {
            List<Integer> ids = segmentID.get(i);

            boolean canGo = true;

            for(StageMap stm : schedule.stages) {
                int id = stageToInteger(stm);

                if(id == -1)
                    return -1;

                if(!ids.contains(id)) {
                    canGo = false;
                    break;
                }
            }

            if(canGo) {
                return i;
            }
        }

        return -1;
    }

    private int findRawGroup(StageSchedule schedule) {
        for(int i = 0; i < segmentID.size(); i++) {
            List<Integer> ids = segmentID.get(i);

            boolean canGo = true;

            for(String unknown : schedule.unknownStages) {
                if(!StaticStore.isNumeric(unknown))
                    return -1;

                int id = StaticStore.safeParseInt(unknown);

                if(!ids.contains(id)) {
                    canGo = false;
                    break;
                }
            }

            if(canGo) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public String toString() {
        return "{ Finished : " + finished + " | Contents : " + Arrays.toString(schedules) + " }";
    }
}
