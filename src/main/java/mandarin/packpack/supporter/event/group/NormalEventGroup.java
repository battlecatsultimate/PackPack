package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventDateSet;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;

import java.util.List;

public class NormalEventGroup extends EventGroup {
    private final List<List<Integer>> segmentID;

    public NormalEventGroup(List<List<Integer>> segmentID, String name) {
        super(new Schedule[segmentID.size()], name);

        this.segmentID = segmentID;
    }

    @Override
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

    @Override
    protected void checkFinish() {
        for(Schedule schedule : schedules) {
            if(schedule == null) {
                finished = false;
                return;
            }
        }

        finished = true;
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
}
