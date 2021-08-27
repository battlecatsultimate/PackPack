package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NormalGroupHandler implements GroupHandler {

    public final String name;

    private final List<List<Integer>> groupData;
    private final ArrayList<EventGroup> groups = new ArrayList<>();

    public NormalGroupHandler(@NotNull List<List<Integer>> groupData, @NotNull String name) {
        this.groupData = groupData;
        this.name = name;
    }

    @Override
    public boolean handleEvent(Schedule schedule) {
        if(schedule instanceof StageSchedule) {
            if(((StageSchedule) schedule).stages.isEmpty())
                return false;

            if(containsOnlyValidStages((StageSchedule) schedule)) {
                boolean added = false;

                for (EventGroup group : groups) {
                    if(group.finished)
                        continue;

                    //Existing group
                    if(group.getDate() != null) {
                        if (((StageSchedule) schedule).date.equals(group.getDate())) {
                            added |= group.addStage((StageSchedule) schedule);
                        }
                    }
                }

                if(!added) {
                    //No group where can put this schedule found

                    EventGroup newGroup = new EventGroup(groupData, name);

                    boolean result = newGroup.addStage((StageSchedule) schedule);

                    groups.add(newGroup);

                    return result;
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void clear() {
        groups.clear();
    }

    @Override
    public ArrayList<EventGroup> getGroups() {
        return groups;
    }

    private boolean containsOnlyValidStages(StageSchedule schedule) {
        for (List<Integer> group : groupData) {
            ArrayList<Integer> data = new ArrayList<>();

            for (StageMap stm : schedule.stages) {
                int id = EventFactor.stageToInteger(stm);

                if (id == -1) {
                    return false;
                } else {
                    data.add(id);
                }
            }

            if(data.containsAll(group))
                return true;
        }

        return false;
    }
}
