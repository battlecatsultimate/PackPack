package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContainedEventGroup extends EventGroup {
    public final List<List<Schedule>> subSchedules = new ArrayList<>();

    private final List<List<Integer>> segmentID;

    public ContainedEventGroup(List<List<Integer>> group, String name) {
        super(new Schedule[1], name);

        this.segmentID = group;

        if(group.size() >= 2) {
            for(int i = 1; i < group.size(); i++) {
                subSchedules.add(new ArrayList<>());
            }
        }
    }

    @Override
    public boolean addStage(StageSchedule schedule, boolean raw) {
        int index;

        if(!raw) {
            index = findIdenticalGroup(schedule);
        } else {
            index = findRawGroup(schedule);
        }

        if(index == -2)
            return false;
        else if(index == -1) {
            if(schedules[0] != null)
                return false;

            schedules[0] = schedule;
            date = schedule.date;

            checkFinish();

            return true;
        } else {
            List<Schedule> subSchedule = subSchedules.get(index);

            if(subSchedule.contains(schedule))
                return false;

            subSchedule.add(schedule);

            checkFinish();

            return true;
        }
    }

    @Override
    protected void checkFinish() {
        boolean full = schedules[0] != null;

        if(!full)
            return;

        for(int i = 0; i < subSchedules.size(); i++) {
            if(subSchedules.get(i).isEmpty())
                return;
        }

        finished = true;
    }

    public List<List<Schedule>> getSubSchedules() {
        return subSchedules;
    }

    private int findIdenticalGroup(StageSchedule schedule) {
        if(segmentID.size() < 2) {
            return -2;
        }

        for(int i = 0; i < segmentID.size(); i++) {
            List<Integer> ids = segmentID.get(i);

            boolean canGo = true;

            for(StageMap stm : schedule.stages) {
                int id = stageToInteger(stm);

                if(id == -1)
                    return -2;

                if(!ids.contains(id)) {
                    canGo = false;
                    break;
                }
            }

            if(canGo) {
                return i - 1;
            }
        }

        return -2;
    }

    private int findRawGroup(StageSchedule schedule) {
        if(segmentID.size() < 2)
            return -2;

        for(int i = 0; i < segmentID.size(); i++) {
            List<Integer> ids = segmentID.get(i);

            boolean canGo = true;

            for(String unknown : schedule.unknownStages) {
                if(!StaticStore.isNumeric(unknown))
                    return -2;

                int id = StaticStore.safeParseInt(unknown);

                if(!ids.contains(id)) {
                    canGo = false;
                    break;
                }
            }

            if(canGo) {
                return i - 1;
            }
        }

        return -2;
    }

    @Override
    public String toString() {
        return "ContainedEventGroup{" +
                "subSchedules=" + subSchedules +
                ", segmentID=" + segmentID +
                ", name='" + name + '\'' +
                ", schedules=" + Arrays.toString(schedules) +
                ", finished=" + finished +
                ", date=" + date +
                '}';
    }
}
